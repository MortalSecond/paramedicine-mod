package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;

public class StewpotBlock extends Block implements EntityBlock
{

    public enum StewpotState implements StringRepresentable
    {
        EMPTY("empty"),
        FILLED("filled"),
        BOILING("boiling");

        private final String name;
        StewpotState(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }

    public static final EnumProperty<StewpotState> STATE = EnumProperty.create("state", StewpotState.class);

    public StewpotBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATE, StewpotState.EMPTY));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new StewpotBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(STATE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        ItemStack held = player.getItemInHand(hand);

        // Only intercept right-clicks we actually handle, so block placement still works.
        Item heldItem = held.getItem();
        boolean relevant = heldItem == Items.WATER_BUCKET || heldItem instanceof FluidContainerItem || held.isEmpty();
        if (!relevant)
            return InteractionResult.PASS;

        if (level.isClientSide)
         return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof StewpotBlockEntity pot))
            return InteractionResult.PASS;

        // Water bucket.
        if (heldItem == Items.WATER_BUCKET)
        {
            float accepted = pot.addWater(ModConstants.WATER_BUCKET_ML);
            if (accepted > 0f)
                return InteractionResult.CONSUME;

            if (!player.getAbilities().instabuild)
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));

            return InteractionResult.CONSUME;
        }

        // FluidMixture containers.
        if (heldItem instanceof FluidContainerItem container)
        {
            ItemStack single = held.copy();
            single.setCount(1);

            FluidMixture mix = FluidContainerItem.getMixture(single);
            float room = container.getCapacityML() - mix.totalVolume();
            if (room <= 0f)
            {
                player.displayClientMessage(Component.literal("Container is full."), true);
                return InteractionResult.CONSUME;
            }

            FluidMixture pulled = pot.extract(Math.min(room, pot.getContents().totalVolume()));
            if (pulled.isEmpty())
            {
                player.displayClientMessage(Component.literal("The stewpot is empty."), true);
                return InteractionResult.CONSUME;
            }

            for (Map.Entry<SubstanceType, Float> entry : pulled.getComponents().entrySet())
                mix.add(entry.getKey(), entry.getValue(), container.getCapacityML());
            FluidContainerItem.setMixture(single, mix);

            held.shrink(1);
            if (held.isEmpty())
                player.setItemInHand(hand, single);
            else if (!player.getInventory().add(single))
                player.drop(single, false);

            return InteractionResult.CONSUME;
        }

        // Empty hand. MOSTLY DEBUG.
        player.displayClientMessage(Component.literal(String.format(
                "Stewpot: %s (%.0f / %.0f mL)",
                pot.getContents().describe(),
                pot.getContents().totalVolume(),
                ModConstants.STEWPOT_CAPACITY_ML
        )), true);
        return InteractionResult.CONSUME;
    }
}
