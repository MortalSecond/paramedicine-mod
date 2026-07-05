package net.invinciblemoebius.traumaparamedicinemod.block.entities;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.block.ItemComposition;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlockEntities;
import net.invinciblemoebius.traumaparamedicinemod.item.ModItems;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class DryingRackBlockEntity extends BlockEntity
{
    private ItemStack stored = ItemStack.EMPTY;
    private int dryingTicks = 0;
    private boolean done = false;
    private int syncTimer = 0;
    private boolean pendingSync = false;

    public DryingRackBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.DRYING_RACK.get(), pos, state);
    }

    // === INTERACTION METHODS ===

    // For a stack to be dried, it must have a composition with WATER left.
    public static boolean isDryable(ItemStack stack)
    {
        if (!ItemComposition.has(stack))
            return false;

        return ItemComposition.read(stack).getOrDefault(SubstanceType.WATER, 0f) > ModConstants.DRYER_MIN_WATER_TO_DRY;
    }

    // Puts a single item on the rack. Returns true if accepted.
    public boolean place(ItemStack incoming)
    {
        if (!stored.isEmpty() || !isDryable(incoming))
            return false;

        stored = incoming.copyWithCount(1);
        dryingTicks = 0;
        done = false;

        markSynced();
        return true;
    }

    // Removes whatever's on the rack (wet or dried) and hands it back.
    public ItemStack retrieve()
    {
        ItemStack out = stored;
        stored = ItemStack.EMPTY;
        dryingTicks = 0;
        done = false;

        markSynced();
        return out;
    }

    // WATER leaves and everything else is conserved, so concentration rises.
    private void dry()
    {
        Map<SubstanceType, Float> comp = ItemComposition.read(stored);
        float water = comp.getOrDefault(SubstanceType.WATER, 0f);
        if (water > 0f)
            comp.put(SubstanceType.WATER, water * (1f - ModConstants.DRYER_WATER_REMOVAL_FRACTION));

        ItemStack dried = new ItemStack(ModItems.DRIED_PLANT.get());
        ItemComposition.stamp(dried, comp);
        stored = dried;
        done = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity be)
    {
        if (be.stored.isEmpty() || be.done)
        {
            be.flushSync(level, pos, state);
            return;
        }

        be.dryingTicks++;
        if (be.dryingTicks >= (int) (ModConstants.DRYER_DRYING_SECONDS * ModConstants.TICKS_PER_SECOND))
        {
            be.dry();
            be.setChanged();
            be.pendingSync = true;
        }
        be.flushSync(level, pos, state);
    }

    // === ACESSORS ===

    public boolean isEmpty() { return stored.isEmpty(); }
    public boolean isDone() { return done; }
    public ItemStack getStored() { return stored; }

    // === SYNC / SAVE ===

    private void markSynced()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void flushSync(Level level, BlockPos pos, BlockState state)
    {
        if (++syncTimer >= ModConstants.DRYER_SYNC_INTERVAL_TICKS)
        {
            syncTimer = 0;
            if (pendingSync)
            {
                pendingSync = false;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Stored", stored.save(new CompoundTag()));
        tag.putInt("DryingTicks", dryingTicks);
        tag.putBoolean("Done", done);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        stored = tag.contains("Stored") ? ItemStack.of(tag.getCompound("Stored")) : ItemStack.EMPTY;
        dryingTicks = tag.getInt("DryingTicks");
        done = tag.getBoolean("Done");
    }

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
}