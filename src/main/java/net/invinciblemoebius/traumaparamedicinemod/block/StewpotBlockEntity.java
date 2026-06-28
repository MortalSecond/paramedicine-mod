package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.menu.StewpotMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

// This holds a container block's bulk stock as a FluidMixture.
public class StewpotBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_INGREDIENT_START = 2;
    public static final int INGREDIENT_SLOTS = 6;
    public static final int SLOT_COUNT = SLOT_INGREDIENT_START + INGREDIENT_SLOTS; // 8
    private final FluidMixture contents = new FluidMixture();
    // 0 = Cold, 1 = boiling.
    private float temperature = 0f;
    private int syncTimer = 0;
    private boolean pendingSync = false;

    public StewpotBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.STEWPOT.get(), pos, state);
    }

    // === INTERACTION METHODS ===

    // Returns how much was actually accepted.
    public float addFluid(float ml)
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
        if (!pulled.isEmpty())
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

    // === UI STUFF ===

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return StewpotBlockEntity.this.isItemValidForSlot(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return (slot == SLOT_INPUT || slot == SLOT_OUTPUT) ? 1 : super.getSlotLimit(slot);
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.traumaparamedicinemod.stewpot");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player)
    {
        return new StewpotMenu(windowId, playerInventory, this);
    }

    // Not technically part of the UI, since this is the capability.
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemHandlerCap.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
    }

    // === HELPER METHODS ===

    private static boolean hasHeatSourceBelow(Level level, BlockPos pos)
    {
        BlockState below = level.getBlockState(pos.below());

        if (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT))
            return true;
        if (below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.MAGMA_BLOCK))
            return true;

        return below.getFluidState().is(FluidTags.LAVA);
    }

    private boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        if (slot == SLOT_INPUT)
            return stack.getItem() instanceof FluidContainerItem || stack.getItem() == Items.WATER_BUCKET || stack.getItem() == Items.MILK_BUCKET;

        if (slot == SLOT_OUTPUT)
            return stack.getItem() instanceof FluidContainerItem;

        // Ingredient slots. Anything, for now.
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StewpotBlockEntity pot)
    {
        float dt = ModConstants.SECONDS_PER_TICK;
        boolean heated = hasHeatSourceBelow(level, pos);
        boolean dirty = false;

        // Temperature tracks heat. For now this ONLY drives the gauge.
        float prevTemp = pot.temperature;
        pot.temperature = heated
                ? Math.min(1f, pot.temperature + ModConstants.STEWPOT_TEMP_RISE_PER_SECOND * dt)
                : Math.max(0f, pot.temperature - ModConstants.STEWPOT_TEMP_FALL_PER_SECOND * dt);
        if (pot.temperature != prevTemp)
            dirty = true;

        if (heated && !pot.contents.isEmpty())
        {
            // Still coming to a boil...
            float toBoil = pot.contents.remove(SubstanceType.WATER, ModConstants.STEWPOT_BOIL_CONVERT_PER_SECOND * dt);
            if (toBoil > 0f)
            {
                pot.contents.add(SubstanceType.BOILED_WATER, toBoil, ModConstants.STEWPOT_CAPACITY_ML);
                dirty = true;
            }
            // And done! Begin evaporation.
            else
            {
                float evaporated = pot.contents.remove(SubstanceType.BOILED_WATER, ModConstants.STEWPOT_EVAPORATION_PER_SECOND * dt);
                if (evaporated > 0f)
                    dirty = true;
            }
        }

        if (dirty)
        {
            pot.setChanged();
            pot.pendingSync = true;
        }

        // The client UI push is a little throttled, so it doesn't send a packet every single tick.
        if (++pot.syncTimer >= ModConstants.STEWPOT_SYNC_INTERVAL_TICKS)
        {
            pot.syncTimer = 0;
            if (pot.pendingSync)
            {
                pot.pendingSync = false;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void markChangedAndSynced()
    {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // === ACESSORS ===

    public IItemHandler getItemHandler() { return items; }
    public ItemStackHandler getItems() { return items; }
    public FluidMixture getContents() { return contents; }
    public float getTemperature() { return temperature; }

    // === SAVING STUFF ===

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        CompoundTag contentsTag = new CompoundTag();
        contents.writeToNBT(contentsTag);
        tag.put("Contents", contentsTag);
        tag.put("Inventory", items.serializeNBT());
        tag.putFloat("Temperature", temperature);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        contents.clear();
        if (tag.contains("Contents"))
            contents.merge(FluidMixture.readFromNBT(tag.getCompound("Contents")), ModConstants.STEWPOT_CAPACITY_ML);
        if (tag.contains("Inventory"))
            items.deserializeNBT(tag.getCompound("Inventory"));
        temperature = tag.getFloat("Temperature");
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
}
