package net.invinciblemoebius.traumaparamedicinemod.menu.designs;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.invinciblemoebius.traumaparamedicinemod.block.entities.StewpotBlockEntity;
import net.invinciblemoebius.traumaparamedicinemod.menu.ModMenus;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
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

public class StewpotMenu extends AbstractContainerMenu
{
    public static final int BE_SLOT_COUNT = StewpotBlockEntity.SLOT_COUNT;

    private final StewpotBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    // Client constructor.
    public StewpotMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf)
    {
        this(windowId, playerInventory, fromBuffer(playerInventory, buf));
    }

    // Server constructor.
    public StewpotMenu(int windowId, Inventory playerInventory, StewpotBlockEntity be)
    {
        super(ModMenus.STEWPOT.get(), windowId);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler h = be.getItemHandler();

        // Six ingredient slots across the top (raised up).
        int[] ingredientX = { 35, 55, 75, 95, 115, 135 };
        for (int i = 0; i < StewpotBlockEntity.INGREDIENT_SLOTS; i++)
            addSlot(new SlotItemHandler(h, StewpotBlockEntity.SLOT_INGREDIENT_START + i, ingredientX[i], 10));

        // Input on the left and output on the right, flanking the fluid bar.
        addSlot(new SlotItemHandler(h, StewpotBlockEntity.SLOT_INPUT, 10, 57));
        addSlot(new SlotItemHandler(h, StewpotBlockEntity.SLOT_OUTPUT, 151, 57));

        addPlayerInventory(playerInventory);
    }

    private static StewpotBlockEntity fromBuffer(Inventory playerInventory, FriendlyByteBuf buf)
    {
        BlockEntity be = playerInventory.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof StewpotBlockEntity pot)
            return pot;
        throw new IllegalStateException("No stewpot block entity at the opened position.");
    }

    // This is the actual player inventory grid.
    private void addPlayerInventory(Inventory inv)
    {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 17));

        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 158));
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
            // Block entity to player inventory.
            if (!moveItemStackTo(stack, BE_SLOT_COUNT, slots.size(), true))
                return ItemStack.EMPTY;
        }
        else
        {
            // Player inventory to block entity.
            if (!moveItemStackTo(stack, 0, BE_SLOT_COUNT, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return result;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(access, player, ModBlocks.STEWPOT.get());
    }

    // === ACCESSORS ===

    public FluidMixture getContents() { return blockEntity.getContents(); }
    public float getTemperature() { return blockEntity.getTemperature(); }
    public float getCapacity() { return ModConstants.STEWPOT_CAPACITY_ML; }
}