package net.omi25addon.arsboss.block.entity;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.common.capability.SourceStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.omi25addon.arsboss.block.ModBlockEntities;
import net.omi25addon.arsboss.block.custom.omijar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OmiJarBlockEntity extends AbstractSourceMachine implements ITooltipProvider {
    private static final int MAX_SOURCE = 10000;
    private static final int TRANSFER_RATE = 1000;

    public OmiJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMI_JAR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            SourceManager.INSTANCE.addInterface(level, new OmiJarSourceProvider(level, worldPosition));
        }
    }

    @Override
    protected SourceStorage createDefaultStorage() {
        return new SourceStorage(Integer.MAX_VALUE, TRANSFER_RATE, TRANSFER_RATE);
    }

    @Override
    public int getSource() {
        return getNetworkController().getSourceStorage().getSource();
    }

    @Override
    public int getMaxSource() {
        return connectedJars().size() * MAX_SOURCE;
    }

    @Override
    public int getTransferRate() {
        return getMaxSource();
    }

    @Override
    public boolean canAcceptSource() {
        return getSource() < getMaxSource();
    }

    @Override
    public boolean canProvideSource() {
        return getSource() > 0;
    }

    @Override
    public int setSource(int source) {
        OmiJarBlockEntity controller = getNetworkController();
        controller.getSourceStorage().setMaxSource(getMaxSource());
        controller.getSourceStorage().setSource(Mth.clamp(source, 0, getMaxSource()));
        clearNonControllerStorage(controller);
        controller.syncSourceChange();
        updateConnectedFillStates();
        return getSource();
    }

    @Override
    public int addSource(int amount) {
        return addSource(amount, false);
    }

    @Override
    public int addSource(int amount, boolean simulate) {
        OmiJarBlockEntity controller = getNetworkController();
        int source = controller.getSourceStorage().getSource();
        int accepted = Mth.clamp(amount, 0, getMaxSource() - source);

        if (!simulate && accepted > 0) {
            controller.getSourceStorage().setMaxSource(getMaxSource());
            controller.getSourceStorage().setSource(source + accepted);
            clearNonControllerStorage(controller);
            controller.syncSourceChange();
            updateConnectedFillStates();
        }
        return accepted;
    }

    @Override
    public int removeSource(int amount) {
        return removeSource(amount, false);
    }

    @Override
    public int removeSource(int amount, boolean simulate) {
        OmiJarBlockEntity controller = getNetworkController();
        int source = controller.getSourceStorage().getSource();
        int removed = Mth.clamp(amount, 0, source);

        if (!simulate && removed > 0) {
            controller.getSourceStorage().setMaxSource(getMaxSource());
            controller.getSourceStorage().setSource(source - removed);
            clearNonControllerStorage(controller);
            controller.syncSourceChange();
            updateConnectedFillStates();
        }
        return removed;
    }

    @Override
    public boolean updateBlock() {
        super.updateBlock();
        normalizeNetworkStorage();
        updateConnectedFillStates();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(SOURCE_TAG, getSourceStorage().getSource());
    }

    @Override
    public void getTooltip(List<Component> tooltip) {
        int source = getSource();
        int maxSource = getMaxSource();
        int percent = maxSource == 0 ? 0 : Math.min(100, Math.round(source * 100.0f / maxSource));
        tooltip.add(Component.literal(source + " / " + maxSource + " Source (" + percent + "%)"));
    }

    public boolean isNetworkController() {
        return worldPosition.equals(getNetworkControllerPos());
    }

    private OmiJarBlockEntity getNetworkController() {
        BlockPos controllerPos = getNetworkControllerPos();
        if (level != null && level.getBlockEntity(controllerPos) instanceof OmiJarBlockEntity controller) {
            return controller;
        }
        return this;
    }

    private void normalizeNetworkStorage() {
        if (level == null || level.isClientSide) {
            return;
        }

        List<OmiJarBlockEntity> jars = connectedJars();
        OmiJarBlockEntity controller = getNetworkController();
        int maxSource = jars.size() * MAX_SOURCE;
        int totalSource = jars.stream()
                .mapToInt(jar -> jar.getSourceStorage().getSource())
                .sum();

        controller.getSourceStorage().setMaxSource(maxSource);
        controller.getSourceStorage().setSource(Mth.clamp(totalSource, 0, maxSource));
        clearNonControllerStorage(controller);

        for (OmiJarBlockEntity jar : jars) {
            jar.syncSourceChange();
        }
    }

    private void clearNonControllerStorage(OmiJarBlockEntity controller) {
        for (OmiJarBlockEntity jar : connectedJars()) {
            if (jar != controller && jar.getSourceStorage().getSource() != 0) {
                jar.getSourceStorage().setSource(0);
                jar.syncSourceChange();
            }
        }
    }

    private void updateConnectedFillStates() {
        if (level == null || level.isClientSide) {
            return;
        }

        normalizeNetworkStorage();
        for (OmiJarBlockEntity jar : connectedJars()) {
            jar.updateOwnFillState();
        }
    }

    private void updateOwnFillState() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof omijar)) {
            return;
        }

        // Distribute the controller's total source across connected jars so the visual fill
        // reflects per-jar content rather than only the controller showing as filled.
        OmiJarBlockEntity controller = getNetworkController();
        int totalSource = controller.getSourceStorage().getSource();

        List<OmiJarBlockEntity> jars = connectedJars();
        // Ensure stable ordering across clients/servers: sort by Y, then X, then Z (same as controller selection)
        jars.sort((left, right) -> {
            int y = Integer.compare(left.getBlockPos().getY(), right.getBlockPos().getY());
            if (y != 0) return y;
            int x = Integer.compare(left.getBlockPos().getX(), right.getBlockPos().getX());
            if (x != 0) return x;
            return Integer.compare(left.getBlockPos().getZ(), right.getBlockPos().getZ());
        });

        int index = jars.indexOf(this);
        int perJarSource = 0;
        if (index >= 0) {
            int start = index * MAX_SOURCE;
            perJarSource = Math.max(0, Math.min(MAX_SOURCE, totalSource - start));
        }

        int fill = perJarSource <= 0 ? 0 : Math.min(11, (perJarSource - 1) / 1000 + 1);
        if (state.getValue(omijar.FILL) != fill) {
            level.setBlock(worldPosition, state.setValue(omijar.FILL, fill), 3);
        }
    }

    private void syncSourceChange() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    // Called when a neighboring jar is removed to re-normalize the remaining network
    public void onNeighborRemoved() {
        if (level == null || level.isClientSide) return;
        normalizeNetworkStorage();
        updateConnectedFillStates();
    }

    private List<OmiJarBlockEntity> connectedJars() {
        List<OmiJarBlockEntity> jars = new ArrayList<>();
        if (level == null) {
            jars.add(this);
            return jars;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(current);
            if (!(blockEntity instanceof OmiJarBlockEntity jar)) {
                continue;
            }
            jars.add(jar);

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.contains(next) && level.getBlockState(next).getBlock() instanceof omijar) {
                    queue.add(next);
                }
            }
        }

        return jars;
    }

    private BlockPos getNetworkControllerPos() {
        // Prefer an existing jar that currently holds source so adding a new jar (e.g., from below)
        // doesn't change the network controller and cause a transient reset.
        List<OmiJarBlockEntity> jars = connectedJars();
        for (OmiJarBlockEntity jar : jars) {
            if (jar.getSourceStorage().getSource() > 0) {
                return jar.getBlockPos();
            }
        }

        // Fallback: choose the canonical position (lowest Y, then X, then Z)
        return jars.stream()
                .map(BlockEntity::getBlockPos)
                .min((left, right) -> {
                    int y = Integer.compare(left.getY(), right.getY());
                    if (y != 0) {
                        return y;
                    }
                    int x = Integer.compare(left.getX(), right.getX());
                    if (x != 0) {
                        return x;
                    }
                    return Integer.compare(left.getZ(), right.getZ());
                })
                .orElse(worldPosition);
    }
}
