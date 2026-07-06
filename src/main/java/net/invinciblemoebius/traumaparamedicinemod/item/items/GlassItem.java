package net.invinciblemoebius.traumaparamedicinemod.item.items;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.treatment.TreatmentInstruction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

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
            return InteractionResultHolder.pass(stack);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
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