package net.omi25addon.arsboss.block.entity;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

public class OmiJarSourceProvider implements ISpecialSourceProvider {
    private final Level level;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;

    public OmiJarSourceProvider(Level level, BlockPos pos) {
        this.level = level;
        this.dimension = level.dimension();
        this.pos = pos.immutable();
    }

    @Override
    public ISourceTile getSource() {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof OmiJarBlockEntity jar && jar.isNetworkController() ? jar : null;
    }

    @Override
    public boolean isValid() {
        return level.isLoaded(pos) && getSource() != null;
    }

    @Override
    public BlockPos getCurrentPos() {
        return pos;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof OmiJarSourceProvider other)) {
            return false;
        }
        return Objects.equals(dimension, other.dimension) && Objects.equals(pos, other.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }
}
