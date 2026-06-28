package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// This holds a container block's bulk stock as a FluidMixture.
public class StewpotBlockEntity extends BlockEntity
{
    private final FluidMixture contents = new FluidMixture();

    public StewpotBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.STEWPOT.get(), pos, state);
    }

    // === INTERACTION METHODS ===

    // Returns how much was actually accepted.
    public float addWater(float ml)
    {
        float accepted = contents.add(SubstanceType.WATER, ml, ModConstants.STEWPOT_CAPACITY_ML);
        if (accepted > 0)
        {
            markChangedAndSynced();
            updateFilledState();
        }

        return accepted;
    }

    public FluidMixture extract(float ml)
    {
        FluidMixture pulled = contents.drain(ml);
        if (pulled.isEmpty())
        {
            markChangedAndSynced();
            updateFilledState();
        }

        return pulled;
    }

    private void updateFilledState()
    {
        if (level == null ||  level.isClientSide)
            return;

        BlockState state = getBlockState();
        if (!state.hasProperty(StewpotBlock.STATE))
            return;

        StewpotBlock.StewpotState desired = contents.isEmpty()
                ? StewpotBlock.StewpotState.EMPTY
                : StewpotBlock.StewpotState.FILLED;

        if (state.getValue(StewpotBlock.STATE) != desired)
            level.setBlock(worldPosition, state.setValue(StewpotBlock.STATE, desired), Block.UPDATE_CLIENTS);
    }

    private void markChangedAndSynced()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // === SAVING STUFF ===

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        CompoundTag contentsTag = new CompoundTag();
        contents.writeToNBT(contentsTag);
        tag.put("Contents", contentsTag);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        contents.clear();
        if (tag.contains("Contents"))
            contents.merge(FluidMixture.readFromNBT(tag.getCompound("Contents")), ModConstants.STEWPOT_CAPACITY_ML);
    }

    // === SYNC STUFF ===

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === ACESSORS ===

    public FluidMixture getContents() { return contents; }
}
