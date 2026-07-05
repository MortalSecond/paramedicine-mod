package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.item.DressingItem;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.item.PowderContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.menu.DressingStationMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class DressingStationBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int SLOT_BASE = 0;
    public static final int SLOT_FLUID = 1;
    public static final int SLOT_POWDER = 2;
    public static final int SLOT_COUNT = 3;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT)
    {
        @Override protected void onContentsChanged(int slot) { setChanged(); }

        @Override public boolean isItemValid(int slot, ItemStack stack)
        {
            return switch (slot)
            {
                case SLOT_BASE   -> stack.getItem() instanceof DressingItem;
                case SLOT_FLUID  -> stack.getItem() instanceof FluidContainerItem;
                case SLOT_POWDER -> stack.getItem() instanceof PowderContainerItem;
                default -> false;
            };
        }
    };

    private final LazyOptional<IItemHandler> cap = LazyOptional.of(() -> items);

    public DressingStationBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.DRESSING_STATION.get(), pos, state);
    }

    // Applies up to one fluid coat and one powder coat (respecting the cap), consuming agent by what the base can hold.
    public boolean make()
    {
        ItemStack base = items.getStackInSlot(SLOT_BASE);
        if (!(base.getItem() instanceof DressingItem)) return false;

        Dressing d = DressingItem.getDressing(base);
        boolean did = false;

        ItemStack fc = items.getStackInSlot(SLOT_FLUID);
        if (d.canEnhance() && fc.getItem() instanceof FluidContainerItem)
        {
            FluidMixture mix = FluidContainerItem.getMixture(fc);
            float cap = d.getAbsorption() * ModConstants.DRESSING_ABSORB_CAPACITY_ML;
            FluidMixture taken = mix.drain(Math.min(cap, mix.totalVolume()));
            if (!taken.isEmpty())
            {
                d.enhanceFluid(taken);
                FluidContainerItem.setMixture(fc, mix);
                did = true;
            }
        }

        ItemStack pc = items.getStackInSlot(SLOT_POWDER);
        if (d.canEnhance() && pc.getItem() instanceof PowderContainerItem)
        {
            PowderMixture mix = PowderContainerItem.getPowder(pc);
            PowderMixture taken = mix.drain(Math.min(ModConstants.DRESSING_POWDER_CAPACITY_MG, mix.totalMass()));
            if (!taken.isEmpty())
            {
                d.enhancePowder(taken);
                PowderContainerItem.setPowder(pc, mix);
                did = true;
            }
        }

        if (did) DressingItem.setDressing(base, d);
        return did;
    }

    public boolean canMake()
    {
        ItemStack base = items.getStackInSlot(SLOT_BASE);
        if (!(base.getItem() instanceof DressingItem)) return false;
        if (!DressingItem.getDressing(base).canEnhance()) return false;
        boolean fluid = items.getStackInSlot(SLOT_FLUID).getItem() instanceof FluidContainerItem
                && !FluidContainerItem.getMixture(items.getStackInSlot(SLOT_FLUID)).isEmpty();
        boolean powder = items.getStackInSlot(SLOT_POWDER).getItem() instanceof PowderContainerItem
                && !PowderContainerItem.getPowder(items.getStackInSlot(SLOT_POWDER)).isEmpty();
        return fluid || powder;
    }

    public ItemStack getBaseStack() { return items.getStackInSlot(SLOT_BASE); }

    // === MENU / CAPS ===

    @Override public Component getDisplayName() { return Component.translatable("block.traumaparamedicinemod.dressing_station"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new DressingStationMenu(id, inv, this); }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> c, @Nullable Direction side)
    {
        if (c == ForgeCapabilities.ITEM_HANDLER) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }

    // === SAVE ===

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Inventory", items.serializeNBT()); }
    @Override public void load(CompoundTag tag) { super.load(tag); if (tag.contains("Inventory")) items.deserializeNBT(tag.getCompound("Inventory")); }

    public IItemHandler getItemHandler() { return items; }
    public ItemStackHandler getItems() { return items; }
}