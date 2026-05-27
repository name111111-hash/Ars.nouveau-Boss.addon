package net.omi25addon.arsboss.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.omi25addon.arsboss.block.entity.OmiJarBlockEntity;

public class omijar extends BaseEntityBlock {
    public static final MapCodec<omijar> CODEC = simpleCodec(omijar::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 11);
    // Variant encodes which surrounding configuration to use (0-15)
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public omijar(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FILL, 0)
                .setValue(VARIANT, 0)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FILL, VARIANT, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmiJarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return withConnections(state, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Prevent normal right-clicks from directly adding source to the jar.
        // Only allow creative players who are crouching to remove source manually for testing.
        if (!level.isClientSide && player.isCreative()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof OmiJarBlockEntity jar) {
                if (player.isCrouching()) {
                    jar.removeSource(1000);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Notify adjacent jars to re-normalize their networks after this jar is removed
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                BlockEntity be = level.getBlockEntity(np);
                if (be instanceof OmiJarBlockEntity oj) {
                    oj.onNeighborRemoved();
                }
            }
        }
    }

    private BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        boolean n = connectsTo(level, pos.north());
        boolean e = connectsTo(level, pos.east());
        boolean s = connectsTo(level, pos.south());
        boolean w = connectsTo(level, pos.west());
        boolean u = connectsTo(level, pos.above());
        boolean d = connectsTo(level, pos.below());

        BlockState next = state
                .setValue(NORTH, n)
                .setValue(EAST, e)
                .setValue(SOUTH, s)
                .setValue(WEST, w)
                .setValue(UP, u)
                .setValue(DOWN, d);

        // Compute variant based on up/down/left/right (use west as left, east as right)
        // Require that no other neighbors (north/south) are present for the "nothing else" cases
        int variant = 0;
        boolean noOther = !n && !s; // ensure north/south absent
        // Map requested patterns (left=west, right=east, above=up, below=down)
        if (!u && !d && !w && !e && noOther) variant = 0;
        else if (d && !u && !w && !e && noOther) variant = 1;
        else if (e && !u && !w && !d && noOther) variant = 2;
        else if (u && !d && !w && !e && noOther) variant = 3;
        else if (w && !u && !d && !e && noOther) variant = 4;
        else if (w && d && !u && !e && noOther) variant = 5;
        else if (u && w && !d && !e && noOther) variant = 6;
        else if (u && e && !d && !w && noOther) variant = 7;
        else if (d && e && !u && !w && noOther) variant = 8;
        else if (w && e && d && !u && noOther) variant = 9;
        else if (u && w && d && !e && noOther) variant = 10;
        else if (u && e && w && !d && noOther) variant = 11;
        else if (u && e && d && !w && noOther) variant = 12;
        else if (u && d && !w && !e && noOther) variant = 13;
        else if (w && e && !u && !d && noOther) variant = 14;
        else if (u && d && w && e && noOther) variant = 15;
        else variant = 0; // fallback

        next = next.setValue(VARIANT, variant);
        return next;
    }

    private boolean connectsTo(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof omijar;
    }
}
