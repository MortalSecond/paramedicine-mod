package net.invinciblemoebius.traumaparamedicinemod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.network.ClientboundSyncHealthPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid = ParamedicineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugCommands
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
                Commands.literal("paramedicine")
                        .then(heal())
                        .then(healthStatus())
        );
    }

    // /paramedicine heal
    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> heal()
    {
        return Commands.literal("heal")
                .requires(src -> src.hasPermission(2))
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player = requirePlayer(source);
                    if (player == null) return 0;

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                    {
                        data.resetToDefaults();

                        source.sendSuccess(
                                () -> Component.literal("All values reset to defaults."),
                                false
                        );

                        syncToClient(player, data.getBloodVolume());
                    });

                    return 1;
                });
    }

    // /paramedicine status
    private static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> healthStatus()
    {
        return Commands.literal("status")
                .requires(src -> src.hasPermission(0))
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player = requirePlayer(source);
                    if (player == null) return 0;

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                            source.sendSuccess(
                                    () -> Component.literal(data.toString()),
                                    false
                            ));
                    return 1;
                });
    }

    // === HELPER METHODS ===

    private static ServerPlayer requirePlayer(CommandSourceStack source)
    {
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return null;
        }
        return player;
    }

    private static void syncToClient(ServerPlayer player, float bloodVolume)
    {
        ModNetwork.CHANNEL.sendTo(
                new ClientboundSyncHealthPacket(bloodVolume),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}
