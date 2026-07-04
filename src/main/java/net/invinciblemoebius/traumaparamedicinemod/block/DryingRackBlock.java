package net.invinciblemoebius.traumaparamedicinemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class DryingRackBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Rough footprint matching the model bulk. Same for all facings (near-symmetric).
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public DryingRackBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide || type != ModBlockEntities.DRYING_RACK.get())
            return null;

        return (lvl, pos, st, be) -> DryingRackBlockEntity.serverTick(lvl, pos, st, (DryingRackBlockEntity) be);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Retrieve whatever's on the rack.
        if (!rack.isEmpty())
        {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;

            ItemStack out = rack.retrieve();
            if (!player.getInventory().add(out))
                player.drop(out, false);

            return InteractionResult.CONSUME;
        }

        // Otherwise try to place a dryable item.
        if (!held.isEmpty() && DryingRackBlockEntity.isDryable(held))
        {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;

            if (rack.place(held))
            {
                if (!player.getAbilities().instabuild) held.shrink(1);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && !rack.isEmpty())
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rack.getStored());
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}