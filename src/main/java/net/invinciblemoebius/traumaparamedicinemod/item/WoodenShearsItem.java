package net.invinciblemoebius.traumaparamedicinemod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WoodenShearsItem extends Item
{
    public WoodenShearsItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Decide what this plant yields. Null = not a plant we harvest, so let vanilla handle the click.
        ItemStack yield = harvestYield(state);
        if (yield == null)
            return InteractionResult.PASS;

        // Client only plays the arm swing; the server does the real work.
        if (level.isClientSide)
            return InteractionResult.sidedSuccess(true);

        // Remove the plant WITHOUT its vanilla drops and handle double-high plants.
        level.destroyBlock(pos, false);

        // Pop the wooden shear's yield where the plant was.
        Block.popResource(level, pos, yield);

        // Wear down the tool.
        Player player = context.getPlayer();
        if (player != null)
            context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));

        return InteractionResult.CONSUME;
    }

    // Maps a clicked plant to the item it yields. Returns null if it isn't a harvestable plant.
    private static ItemStack harvestYield(BlockState state)
    {
        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN))
            return new ItemStack(ModItems.PLANT_FIBER.get());

        if (state.is(BlockTags.SMALL_FLOWERS))
            return new ItemStack(ModItems.LONG_LEAF.get());

        return null;
    }
}