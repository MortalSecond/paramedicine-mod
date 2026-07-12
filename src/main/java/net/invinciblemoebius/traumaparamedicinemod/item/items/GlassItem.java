package net.invinciblemoebius.traumaparamedicinemod.item.items;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.treatment.TreatmentInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class GlassItem extends FluidContainerItem
{
    public GlassItem(Properties properties)
    {
        super(properties, ModConstants.GLASS_CAPACITY_ML);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.DRINK;
    }
    @Override public int getUseDuration(ItemStack stack)
    {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (getMixture(stack).isEmpty())
            return tryFill(level, player, stack);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // Raycasts for a water source block to fill the glass with water.
    // SOURCE_ONLY so flowing water doesn't count.
    private InteractionResultHolder<ItemStack> tryFill(Level level, Player player, ItemStack stack)
    {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);

        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER))
            return InteractionResultHolder.pass(stack);

        if (!level.isClientSide)
        {
            FluidMixture mixture = new FluidMixture();

            // Raw, untreated water. Hypotonic, a little dirty. IF i ever do
            // get around to doing some sort of ThirstWasTaken soft dependency,
            // this will probably check for water purity so that it can be DIRTY_WATER.
            // or just regular WATER according to TWT's values.
            mixture.add(SubstanceType.WATER, getCapacityML(), getCapacityML());
            setMixture(stack, mixture);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (!level.isClientSide && entity instanceof Player player)
        {
            FluidMixture mixture = getMixture(stack);
            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                    .ifPresent(data -> TreatmentInstruction.oral(mixture).apply(data));
            setMixture(stack, new FluidMixture());
        }
        return stack;
    }
}