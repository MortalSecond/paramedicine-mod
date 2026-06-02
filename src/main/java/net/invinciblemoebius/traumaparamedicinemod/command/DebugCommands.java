package net.invinciblemoebius.traumaparamedicinemod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
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
        event.getDispatcher().register(buildDrainCommand());
        event.getDispatcher().register(buildHealthCommand());
    }

    // /drainblood <liters>
    private static LiteralArgumentBuilder<CommandSourceStack> buildDrainCommand()
    {
        return Commands.literal("drainblood")
            .requires(src -> src.hasPermission(2))
            .then(Commands.argument("liters", FloatArgumentType.floatArg(0f, 5f))
            .executes(ctx ->
                {
                    float amount = FloatArgumentType.getFloat(ctx, "liters");
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getEntity() instanceof ServerPlayer player))
                    {
                        source.sendFailure(Component.literal("Must be run by a player."));
                        return 0;
                    }

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                    {
                        float drained = data.drainBlood(amount);
                        float remaining = data.getBloodVolume();

                        source.sendSuccess(
                                () -> Component.literal(String.format(
                                        "Drained %.3fL. Blood Volume: %.3fL / %.1fL",
                                        drained, remaining, 5.0f
                                )),
                                false
                        );

                        // Sync to client.
                        ModNetwork.CHANNEL.sendTo(
                                new ClientboundSyncHealthPacket(remaining),
                                player.connection.connection,
                                NetworkDirection.PLAY_TO_CLIENT
                        );
                    });
                    return 1;
                })
            );
    }
    // /healthstatus
    private static LiteralArgumentBuilder<CommandSourceStack> buildHealthCommand()
    {
        return Commands.literal("healthstatus")
                .requires(src -> src.hasPermission(0))
                .executes(ctx ->
                {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getEntity() instanceof ServerPlayer player))
                    {
                        source.sendFailure(Component.literal("Must be run by a player."));
                        return 0;
                    }

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                            source.sendSuccess(
                                    () -> Component.literal(data.toString()),
                                    false
                            ));
                    return 1;
                });
    }
}
