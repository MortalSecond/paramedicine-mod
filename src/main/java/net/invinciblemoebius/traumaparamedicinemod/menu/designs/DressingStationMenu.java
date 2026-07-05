package net.invinciblemoebius.traumaparamedicinemod.menu.designs;

import net.invinciblemoebius.traumaparamedicinemod.block.entities.DressingStationBlockEntity;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.invinciblemoebius.traumaparamedicinemod.item.items.DressingItem;
import net.invinciblemoebius.traumaparamedicinemod.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

public class DressingStationMenu extends AbstractContainerMenu
{
    public static final int BE_SLOT_COUNT = DressingStationBlockEntity.SLOT_COUNT;
    public static final int BUTTON_MAKE = 0;
    private final DressingStationBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public DressingStationMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, fromBuffer(inv, buf)); }

    public DressingStationMenu(int id, Inventory inv, DressingStationBlockEntity be)
    {
        super(ModMenus.DRESSING_STATION.get(), id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler h = be.getItemHandler();
        addSlot(new SlotItemHandler(h, DressingStationBlockEntity.SLOT_BASE, 14, 38));
        addSlot(new SlotItemHandler(h, DressingStationBlockEntity.SLOT_FLUID, 141, 20));
        addSlot(new SlotItemHandler(h, DressingStationBlockEntity.SLOT_POWDER, 141, 57));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 17));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 158));
    }

    private static DressingStationBlockEntity fromBuffer(Inventory inv, FriendlyByteBuf buf)
    {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof DressingStationBlockEntity s)
            return s;

        throw new IllegalStateException("No dressing station block entity at the opened position.");
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == BUTTON_MAKE)
            return blockEntity.make();

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < BE_SLOT_COUNT)
        {
            if (!moveItemStackTo(stack, BE_SLOT_COUNT, slots.size(), true))
                return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(stack, 0, BE_SLOT_COUNT, false))
            return ItemStack.EMPTY;

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return result;
    }

    @Override public boolean stillValid(Player player)
    {
        return stillValid(access, player, ModBlocks.DRESSING_STATION.get());
    }

    // === ACCESSORS ===

    public boolean canMake() { return blockEntity.canMake(); }

    public List<String> getSummary()
    {
        ItemStack base = blockEntity.getBaseStack();
        if (base.getItem() instanceof DressingItem)
            return DressingItem.getDressing(base).summaryBands();

        return List.of();
    }
}