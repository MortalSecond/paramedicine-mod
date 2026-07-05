package net.invinciblemoebius.traumaparamedicinemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class MolcajeteBlock extends Block implements EntityBlock
{
    public MolcajeteBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new MolcajeteBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof MolcajeteBlockEntity be && player instanceof ServerPlayer sp)
            NetworkHooks.openScreen(sp, be, buf -> buf.writeBlockPos(pos));

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof MolcajeteBlockEntity be)
            {
                SimpleContainer drops = new SimpleContainer(be.getItems().getSlots());
                for (int i = 0; i < be.getItems().getSlots(); i++)
                    drops.setItem(i, be.getItems().getStackInSlot(i));
                Containers.dropContents(level, pos, drops);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}