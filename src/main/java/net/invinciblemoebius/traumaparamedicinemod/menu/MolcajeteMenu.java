package net.invinciblemoebius.traumaparamedicinemod.menu;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.invinciblemoebius.traumaparamedicinemod.block.MolcajeteBlockEntity;
import net.invinciblemoebius.traumaparamedicinemod.substance.PowderMixture;
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

public class MolcajeteMenu extends AbstractContainerMenu
{
    public static final int BE_SLOT_COUNT = MolcajeteBlockEntity.SLOT_COUNT;
    public static final int BUTTON_GRIND = 0;
    public static final int BUTTON_EXPORT = 1;
    private final MolcajeteBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public MolcajeteMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf)
    {
        this(windowId, playerInventory, fromBuffer(playerInventory, buf));
    }

    public MolcajeteMenu(int windowId, Inventory playerInventory, MolcajeteBlockEntity be)
    {
        super(ModMenus.MOLCAJETE.get(), windowId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler h = be.getItemHandler();
        // Slot for the flowers.
        addSlot(new SlotItemHandler(h, MolcajeteBlockEntity.SLOT_INPUT, 80, 12));
        // Slot for the jar.
        addSlot(new SlotItemHandler(h, MolcajeteBlockEntity.SLOT_OUTPUT, 151, 57));

        addPlayerInventory(playerInventory);
    }

    private static MolcajeteBlockEntity fromBuffer(Inventory inv, FriendlyByteBuf buf)
    {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof MolcajeteBlockEntity m)
            return m;

        throw new IllegalStateException("No molcajete block entity at the opened position.");
    }

    private void addPlayerInventory(Inventory inv)
    {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 17));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 158));
    }

    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if (id == BUTTON_GRIND)
            return blockEntity.grind();
        if (id == BUTTON_EXPORT)
            return blockEntity.export();

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

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(access, player, ModBlocks.MOLCAJETE.get());
    }

    // === ACCESSORS ===

    public PowderMixture getContents() { return blockEntity.getContents(); }
    public float getCapacity() { return ModConstants.MOLCAJETE_CAPACITY_MG; }
    public boolean canGrind() { return blockEntity.canGrind(); }
    public boolean canExport() { return blockEntity.canExport(); }
}