package net.invinciblemoebius.traumaparamedicinemod.block.entities;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.block.ItemComposition;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlockEntities;
import net.invinciblemoebius.traumaparamedicinemod.item.PowderContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.menu.designs.MolcajeteMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Map;

public class MolcajeteBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    private final PowderMixture contents = new PowderMixture();

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT)
    {
        @Override protected void onContentsChanged(int slot) { setChanged(); }

        @Override public boolean isItemValid(int slot, ItemStack stack)
        {
            if (slot == SLOT_INPUT)  return ItemComposition.has(stack);
            if (slot == SLOT_OUTPUT) return stack.getItem() instanceof PowderContainerItem;
            return false;
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);

    public MolcajeteBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.MOLCAJETE.get(), pos, state);
    }

    // === ACTIONS (button-driven) ===

    // Grind: pull ONE item from the input stack, sum its composition into the internal mix. Instant.
    public boolean grind()
    {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (in.isEmpty() || !ItemComposition.has(in)) return false;
        if (contents.totalMass() >= ModConstants.MOLCAJETE_CAPACITY_MG) return false; // full

        Map<SubstanceType, Float> comp = ItemComposition.read(in);
        for (Map.Entry<SubstanceType, Float> e : comp.entrySet())
            contents.add(e.getKey(), e.getValue(), ModConstants.MOLCAJETE_CAPACITY_MG);

        items.extractItem(SLOT_INPUT, 1, false);
        markSynced();
        return true;
    }

    // Export: merge the internal mix into the jar in the output slot. Manual, so grinds don't auto-contaminate a jar.
    public boolean export()
    {
        if (contents.isEmpty()) return false;
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        if (!(out.getItem() instanceof PowderContainerItem jar)) return false;

        PowderMixture inJar = PowderContainerItem.getPowder(out);
        float room = jar.getCapacityMg() - inJar.totalMass();
        if (room <= 0f) return false;

        PowderMixture moved = contents.drain(Math.min(room, contents.totalMass()));
        inJar.merge(moved, jar.getCapacityMg());
        PowderContainerItem.setPowder(out, inJar);
        markSynced();
        return true;
    }

    public boolean canGrind()
    {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        return !in.isEmpty() && ItemComposition.has(in) && contents.totalMass() < ModConstants.MOLCAJETE_CAPACITY_MG;
    }

    public boolean canExport()
    {
        return !contents.isEmpty() && items.getStackInSlot(SLOT_OUTPUT).getItem() instanceof PowderContainerItem;
    }

    // === MENU / CAPS ===

    @Override public Component getDisplayName() { return Component.translatable("block.traumaparamedicinemod.molcajete"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player)
    {
        return new MolcajeteMenu(windowId, inv, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); itemHandlerCap.invalidate(); }

    // === SYNC / SAVE ===

    private void markSynced()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        CompoundTag c = new CompoundTag();
        contents.writeToNBT(c);
        tag.put("Contents", c);
        tag.put("Inventory", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        contents.clear();
        if (tag.contains("Contents"))
            contents.merge(PowderMixture.readFromNBT(tag.getCompound("Contents")), ModConstants.MOLCAJETE_CAPACITY_MG);
        if (tag.contains("Inventory"))
            items.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    // === ACCESSORS ===

    public IItemHandler getItemHandler() { return items; }
    public ItemStackHandler getItems() { return items; }
    public PowderMixture getContents() { return contents; }
}