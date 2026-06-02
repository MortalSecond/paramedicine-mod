package net.invinciblemoebius.traumaparamedicinemod.health;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerHealthEvents
{
    // Attach a fresh capability instance to every player entity that loads.
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event)
    {
        if(!(event.getObject() instanceof Player)) return;

        ResourceLocation key = new ResourceLocation(ParamedicineMod.MOD_ID, "player_health");
        if(!event.getCapabilities().containsKey(key))
        {
            PlayerHealthCapability provider = new PlayerHealthCapability();
            event.addCapability(key, provider);
            event.addListener(provider::invalidate);
        }
    }

    // Copies health data on death and respawn so values persist through death.
    // (Or resets, it really depends on how things turn out)
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        // Edge case for dimension traveling; this will be handled on another method.
        if(!event.isWasDeath())
            return;

        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent((oldData ->
                event.getEntity().getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(newData ->
                        newData.copyFrom(oldData))));

        event.getOriginal().invalidateCaps();
    }

    // Syncs to client when a player enters a level (either spawn or dimension jump).
    @SubscribeEvent
    public static void onPlayerJoinLevel(EntityJoinLevelEvent event)
    {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        if(event.getLevel().isClientSide()) return;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(PlayerHealthData::markDirty);
    }
}
