package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.menu.StewpotMenu;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceEbullition;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import java.util.Map;

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

    // While boiling, each occupied ingredient slot materializes its item's composition
    // into a per-slot buffer (locks the item for this), then bleeds that buffer into the pot at
    // a fixed rate. Bigger composition = more ticks.
    // The bleed runs even off the boil, so a started item always finishes and unlocks.
    private boolean infuse(float dt, boolean boiling)
    {
        boolean changed = false;
        float perTick = ModConstants.STEWPOT_INFUSE_PER_SECOND * dt;

        for (int i = 0; i < INGREDIENT_SLOTS; i++)
        {
            FluidMixture buffer = dissolveBuffers[i];
            int slot = SLOT_INGREDIENT_START + i;
            ItemStack stack = items.getStackInSlot(slot);

            // Begin a new item dissolving, but only while boiling.
            if (buffer.isEmpty())
            {
                if (!boiling || stack.isEmpty() || !ItemComposition.has(stack))
                    continue;

                ItemComposition.materializeInto(buffer, stack);
                changed = true;
            }

            // Bleed the buffer into the pot (capped at pot's max size).
            float room = ModConstants.STEWPOT_CAPACITY_ML - contents.totalVolume();
            if (room > 0f)
            {
                FluidMixture slice = buffer.drain(Math.min(perTick, room));
                for (Map.Entry<SubstanceType, Float> e : slice.getComponents().entrySet())
                    contents.add(e.getKey(), e.getValue(), ModConstants.STEWPOT_CAPACITY_ML);
                if (!slice.isEmpty())
                    changed = true;
            }

            // Consume the source item and unlock (buffer is empty, so extract passes).
            if (buffer.isEmpty() && !stack.isEmpty())
            {
                items.extractItem(slot, 1, false);
                changed = true;
            }
        }

        return changed;
    }

    // Adds a single substance additively, capped at capacity. Returns how much was accepted.
    public float addFluid(SubstanceType type, float ml)
    {
        float oldTotal = contents.totalVolume();
        float accepted = contents.add(type, ml, ModConstants.STEWPOT_CAPACITY_ML);
        if (accepted > 0)
        {
            diluteHeat(oldTotal);
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

    // Drains the input-slot container into the pot, as much as fits this tick.
    private boolean processInput()
    {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        if (in.isEmpty())
            return false;

        float room = ModConstants.STEWPOT_CAPACITY_ML - contents.totalVolume();
        if (room <= 0f)
            return false;

        // Vanilla buckets. Dump their fluid, leave an empty bucket. Overflow past capacity spills.
        if (in.getItem() == Items.WATER_BUCKET || in.getItem() == Items.MILK_BUCKET)
        {
            SubstanceType type = (in.getItem() == Items.MILK_BUCKET) ? SubstanceType.MILK : SubstanceType.WATER;
            float oldTotal = contents.totalVolume();

            float accepted = contents.add(type, ModConstants.WATER_BUCKET_ML, ModConstants.STEWPOT_CAPACITY_ML);
            if (accepted <= 0f)
                return false;

            diluteHeat(oldTotal);
            items.setStackInSlot(SLOT_INPUT, new ItemStack(Items.BUCKET));
            return true;
        }

        // Fluid containers. Move what fits, write the reduced container back into the slot.
        if (in.getItem() instanceof FluidContainerItem)
        {
            FluidMixture mix = FluidContainerItem.getMixture(in);
            if (mix.isEmpty())
                return false;

            float oldTotal = contents.totalVolume();
            FluidMixture moved = mix.drain(Math.min(room, mix.totalVolume()));

            contents.merge(moved, ModConstants.STEWPOT_CAPACITY_ML);
            diluteHeat(oldTotal);

            FluidContainerItem.setMixture(in, mix);
            return true;
        }

        return false;
    }

    // Fills the output-slot container from the pot, as much as fits this tick.
    private boolean processOutput()
    {
        if (contents.isEmpty())
            return false;

        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        if (!(out.getItem() instanceof FluidContainerItem container))
            return false;

        FluidMixture mix = FluidContainerItem.getMixture(out);
        float room = container.getCapacityML() - mix.totalVolume();
        if (room <= 0f)
            return false;

        FluidMixture pulled = contents.drain(Math.min(room, contents.totalVolume()));
        for (Map.Entry<SubstanceType, Float> e : pulled.getComponents().entrySet())
            mix.add(e.getKey(), e.getValue(), container.getCapacityML());

        FluidContainerItem.setMixture(out, mix);
        return true;
    }

    private void updateFilledState()
    {
        if (level == null ||  level.isClientSide)
            return;

        BlockState state = getBlockState();
        if (!state.hasProperty(StewpotBlock.STATE))
            return;

        StewpotBlock.StewpotState desired;
        if (contents.isEmpty())
            desired = StewpotBlock.StewpotState.EMPTY;
        else if (!contents.isEmpty() && temperature <= ModConstants.STEWPOT_BOIL_THRESHOLD)
            desired = StewpotBlock.StewpotState.FILLED;
        else if (!contents.isEmpty() && temperature >= ModConstants.STEWPOT_BOIL_THRESHOLD)
            desired = StewpotBlock.StewpotState.BOILING;
        else
            desired = StewpotBlock.StewpotState.EMPTY;

        if (state.getValue(StewpotBlock.STATE) != desired)
            level.setBlock(worldPosition, state.setValue(StewpotBlock.STATE, desired), Block.UPDATE_CLIENTS);
    }

    // Per-slot dissolving buffers. A non-empty buffer means that slot is mid-infusion (locked).
    private final FluidMixture[] dissolveBuffers = new FluidMixture[INGREDIENT_SLOTS];
    {
        for (int i = 0; i < INGREDIENT_SLOTS; i++)
            dissolveBuffers[i] = new FluidMixture();
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

        // This locks stacks to be only one item, so whole stacks don't get locked out.
        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }

        // Locks an ingredient slot while its dissolve buffer is non-empty (mid-infusion).
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot >= SLOT_INGREDIENT_START && slot < SLOT_COUNT
                    && !StewpotBlockEntity.this.dissolveBuffers[slot - SLOT_INGREDIENT_START].isEmpty())
                return ItemStack.EMPTY;

            return super.extractItem(slot, amount, simulate);
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

    // Cold input cools the pot, mixing-style. Temperature scales by the fraction of old (hot) volume.
    // 250ml into 10L barely does anything, but 10L into a near-dry pot resets it hard.
    private void diluteHeat(float oldTotal)
    {
        float newTotal = contents.totalVolume();
        if (newTotal > oldTotal && newTotal > 0f)
            temperature *= oldTotal / newTotal;
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

        // Input slot into pot.
        if (pot.processInput())
            dirty = true;

        // Boiling only once hot enough. Cold input drops temperature, so it pauses until reheated.
        // Convert advances water through its chain, evaporate removes volume (concentrating dissolved
        // meds) or scorches the remainder to JUNK once boiled dry. Both run at the same time.
        // At the same time, it runs the infusion process.
        boolean boiling = pot.temperature >= ModConstants.STEWPOT_BOIL_THRESHOLD && !pot.contents.isEmpty();
        if (boiling)
        {
            if (SubstanceEbullition.convert(pot.contents, ModConstants.STEWPOT_BOIL_CONVERT_PER_SECOND * dt, ModConstants.STEWPOT_CAPACITY_ML))
                dirty = true;
            if (SubstanceEbullition.evaporate(pot.contents, ModConstants.STEWPOT_EVAPORATION_PER_SECOND * dt, ModConstants.STEWPOT_CAPACITY_ML))
                dirty = true;
            if (pot.infuse(dt, boiling))
                dirty = true;
        }

        // Pot into output slot.
        if (pot.processOutput())
            dirty = true;

        if (dirty)
        {
            pot.setChanged();
            pot.pendingSync = true;
            pot.updateFilledState();
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
        ListTag buffersTag = new ListTag();
        for (FluidMixture buffer : dissolveBuffers)
        {
            CompoundTag bt = new CompoundTag();
            buffer.writeToNBT(bt);
            buffersTag.add(bt);
        }
        tag.put("DissolveBuffers", buffersTag);
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
        if (tag.contains("DissolveBuffers"))
        {
            ListTag buffersTag = tag.getList("DissolveBuffers", Tag.TAG_COMPOUND);
            for (int i = 0; i < INGREDIENT_SLOTS && i < buffersTag.size(); i++)
            {
                dissolveBuffers[i].clear();
                dissolveBuffers[i].merge(FluidMixture.readFromNBT(buffersTag.getCompound(i)), Float.MAX_VALUE);
            }
        }
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
