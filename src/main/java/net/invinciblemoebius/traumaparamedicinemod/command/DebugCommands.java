package net.invinciblemoebius.traumaparamedicinemod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AirwayState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.ClientboundSyncHealthPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundType;
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
                        .then(cmdStatus())
                        .then(cmdHeal())
                        .then(cmdDrainBlood())
                        .then(cmdAddBlood())
                        .then(cmdCauseWound())
                        .then(cmdCausePneumothorax())
                        .then(cmdCauseHemothorax())
                        .then(cmdObstructAirway())
                        .then(cmdClearAirway())
                        .then(cmdSetFibrillations())
                        .then(cmdSetTemp())
                        .then(cmdSetSpO2())
                        .then(cmdTourniquet())
                        .then(cmdLimbStatus())
        );
    }

    // /paramedicine status
    private static ArgumentBuilder<CommandSourceStack, ?> cmdStatus()
    {
        return Commands.literal("status")
                .requires(src -> src.hasPermission(0))
                .executes(ctx ->
                {
                    ServerPlayer player = requirePlayer(ctx.getSource());
                    if (player == null) return 0;

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                            .ifPresent(data -> ctx.getSource().sendSuccess(
                                    () -> Component.literal(data.toString()), false));
                    return 1;
                });
    }

    // /paramedicine heal
    private static ArgumentBuilder<CommandSourceStack, ?> cmdHeal()
    {
        return Commands.literal("heal")
                .requires(src -> src.hasPermission(2))
                .executes(ctx ->
                {
                    ServerPlayer player = requirePlayer(ctx.getSource());
                    if (player == null) return 0;

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                            .ifPresent(data ->
                            {
                                data.resetToDefaults();
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("[Debug] All values reset to defaults."),
                                        false);
                                syncToClient(player, data);
                            });
                    return 1;
                });
    }

    // /paramedicine drainblood <liters>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdDrainBlood()
    {
        return Commands.literal("drainblood")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("liters", FloatArgumentType.floatArg(0f, 5f))
                        .executes(ctx ->
                        {
                            float amount = FloatArgumentType.getFloat(ctx, "liters");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        // Drain directly from UPPER_TORSO for simplicity.
                                        float ml = amount * 1000f;
                                        var torso = data.getLimb(LimbNode.UPPER_TORSO);
                                        float before = torso.getActualBloodVolume();
                                        torso.setActualBloodVolume(before - ml);
                                        data.recomputeBloodVolume();

                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(String.format(
                                                        "[Debug] Drained %.0fml from torso. Total blood: %.0fml",
                                                        before - torso.getActualBloodVolume(),
                                                        data.getBloodVolume())),
                                                false);
                                        syncToClient(player, data);
                                    });
                            return 1;
                        }));
    }

    // /paramedicine addblood <liters>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdAddBlood()
    {
        return Commands.literal("addblood")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("liters", FloatArgumentType.floatArg(0f, 5f))
                        .executes(ctx ->
                        {
                            float amount = FloatArgumentType.getFloat(ctx, "liters");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        float ml = amount * 1000f;
                                        var torso = data.getLimb(LimbNode.UPPER_TORSO);
                                        torso.setActualBloodVolume(torso.getActualBloodVolume() + ml);
                                        data.recomputeBloodVolume();

                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(String.format(
                                                        "[Debug] Added %.0fml to torso. Total blood: %.0fml",
                                                        ml, data.getBloodVolume())),
                                                false);
                                        syncToClient(player, data);
                                    });
                            return 1;
                        }));
    }

    // /paramedicine causewound <woundType> <depth> <limbNode> <size>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdCauseWound()
    {
        return Commands.literal("causewound")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("woundType", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            for (WoundType t : WoundType.values())
                                builder.suggest(t.name());
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("depth", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                {
                                    for (WoundDepth d : WoundDepth.values())
                                        builder.suggest(d.name());
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("limb", StringArgumentType.word())
                                        .suggests((ctx, builder) ->
                                        {
                                            for (LimbNode n : LimbNode.values())
                                                builder.suggest(n.name());
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("size", FloatArgumentType.floatArg(0.01f, 1f))
                                                .executes(ctx ->
                                                {
                                                    String typeStr  = StringArgumentType.getString(ctx, "woundType");
                                                    String depthStr = StringArgumentType.getString(ctx, "depth");
                                                    String limbStr  = StringArgumentType.getString(ctx, "limb");
                                                    float  size     = FloatArgumentType.getFloat(ctx, "size");

                                                    WoundType  type;
                                                    WoundDepth depth;
                                                    LimbNode   node;

                                                    try { type  = WoundType.valueOf(typeStr.toUpperCase()); }
                                                    catch (IllegalArgumentException e)
                                                    {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("Unknown wound type: " + typeStr));
                                                        return 0;
                                                    }

                                                    try { depth = WoundDepth.valueOf(depthStr.toUpperCase()); }
                                                    catch (IllegalArgumentException e)
                                                    {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("Unknown depth: " + depthStr));
                                                        return 0;
                                                    }

                                                    try { node = LimbNode.valueOf(limbStr.toUpperCase()); }
                                                    catch (IllegalArgumentException e)
                                                    {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("Unknown limb node: " + limbStr));
                                                        return 0;
                                                    }

                                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                                    if (player == null) return 0;

                                                    WoundType  finalType  = type;
                                                    WoundDepth finalDepth = depth;
                                                    LimbNode   finalNode  = node;

                                                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                                            .ifPresent(data ->
                                                            {
                                                                Wound wound = new Wound(finalType, finalDepth, size);
                                                                data.getLimb(finalNode).addWound(wound);

                                                                ctx.getSource().sendSuccess(
                                                                        () -> Component.literal(String.format(
                                                                                "[Debug] Applied %s %s wound (size %.2f) to %s. " +
                                                                                        "Bleed rate: %.3f ml/s",
                                                                                finalDepth, finalType, size, finalNode,
                                                                                wound.getBleedRateML())),
                                                                        false);
                                                                syncToClient(player, data);
                                                            });
                                                    return 1;
                                                })))));
    }

    // /paramedicine causepneumothorax <left|right> <amount>
    // Injects air into the specified lung's pleural cavity.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdCausePneumothorax()
    {
        return Commands.literal("causepneumothorax")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("side", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            builder.suggest("left");
                            builder.suggest("right");
                            builder.suggest("both");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("airML", FloatArgumentType.floatArg(0f, LungData.MAX_AIR_ML))
                                .executes(ctx ->
                                {
                                    String side  = StringArgumentType.getString(ctx, "side").toLowerCase();
                                    float  airML = FloatArgumentType.getFloat(ctx, "airML");

                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                    if (player == null) return 0;

                                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                            .ifPresent(data ->
                                            {
                                                if (side.equals("left") || side.equals("both"))
                                                    data.getLeftLung().addAir(airML);
                                                if (side.equals("right") || side.equals("both"))
                                                    data.getRightLung().addAir(airML);

                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal(String.format(
                                                                "[Debug] Injected %.0fml air into %s lung(s). " +
                                                                        "Left compromise: %.0f%% Right compromise: %.0f%%",
                                                                airML, side,
                                                                data.getLeftLung().getCompromise()  * 100f,
                                                                data.getRightLung().getCompromise() * 100f)),
                                                        false);
                                                syncToClient(player, data);
                                            });
                                    return 1;
                                })));
    }

    // /paramedicine causehemothorax <left|right> <bloodML>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdCauseHemothorax()
    {
        return Commands.literal("causehemothorax")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("side", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            builder.suggest("left");
                            builder.suggest("right");
                            builder.suggest("both");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("bloodML", FloatArgumentType.floatArg(0f, LungData.MAX_BLOOD_ML))
                                .executes(ctx ->
                                {
                                    String side    = StringArgumentType.getString(ctx, "side").toLowerCase();
                                    float  bloodML = FloatArgumentType.getFloat(ctx, "bloodML");

                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                    if (player == null) return 0;

                                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                            .ifPresent(data ->
                                            {
                                                if (side.equals("left") || side.equals("both"))
                                                    data.getLeftLung().addBlood(bloodML);
                                                if (side.equals("right") || side.equals("both"))
                                                    data.getRightLung().addBlood(bloodML);

                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal(String.format(
                                                                "[Debug] Added %.0fml blood to %s lung(s). " +
                                                                        "Left compromise: %.0f%% Right compromise: %.0f%%",
                                                                bloodML, side,
                                                                data.getLeftLung().getCompromise()  * 100f,
                                                                data.getRightLung().getCompromise() * 100f)),
                                                        false);
                                                syncToClient(player, data);
                                            });
                                    return 1;
                                })));
    }

    // /paramedicine obstructairway <partial|full>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdObstructAirway()
    {
        return Commands.literal("obstructairway")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("severity", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            builder.suggest("partial");
                            builder.suggest("full");
                            return builder.buildFuture();
                        })
                        .executes(ctx ->
                        {
                            String severity = StringArgumentType.getString(ctx, "severity").toLowerCase();
                            AirwayState state = severity.equals("full")
                                    ? AirwayState.FULLY_OBSTRUCTED
                                    : AirwayState.PARTIALLY_OBSTRUCTED;

                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        data.setAirwayState(state);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("[Debug] Airway set to: " + state),
                                                false);
                                        syncToClient(player, data);
                                    });
                            return 1;
                        }));
    }

    // /paramedicine clearairway
    private static ArgumentBuilder<CommandSourceStack, ?> cmdClearAirway()
    {
        return Commands.literal("clearairway")
                .requires(src -> src.hasPermission(2))
                .executes(ctx ->
                {
                    ServerPlayer player = requirePlayer(ctx.getSource());
                    if (player == null) return 0;

                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                            .ifPresent(data ->
                            {
                                data.setAirwayState(AirwayState.CLEAR);
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("[Debug] Airway cleared."),
                                        false);
                                syncToClient(player, data);
                            });
                    return 1;
                });
    }

    // /paramedicine setfibrillations <0.0 - 1.0> [forced]
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetFibrillations()
    {
        return Commands.literal("setfibrillations")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                        .executes(ctx ->
                        {
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            return applyFibrillations(ctx.getSource(), value, false);
                        })
                        .then(Commands.literal("forced")
                                .executes(ctx ->
                                {
                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                    return applyFibrillations(ctx.getSource(), value, true);
                                })));
    }

    private static int applyFibrillations(CommandSourceStack source,
            float value, boolean forced)
    {
        ServerPlayer player = requirePlayer(source);
        if (player == null) return 0;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                .ifPresent(data ->
                {
                    data.setFibrillations(value);
                    if (forced) data.setFibrillationsForced(true, value);

                    source.sendSuccess(
                            () -> Component.literal(String.format(
                                    "[Debug] Fibrillations set to %.2f%s",
                                    value, forced ? " (forced)" : "")),
                            false);
                    syncToClient(player, data);
                });
        return 1;
    }

    // /paramedicine settemp <celsius>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetTemp()
    {
        return Commands.literal("settemp")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("celsius", FloatArgumentType.floatArg(20f, 42f))
                        .executes(ctx ->
                        {
                            float temp = FloatArgumentType.getFloat(ctx, "celsius");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        data.setCoreTemperature(temp);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(String.format(
                                                        "[Debug] Core temperature set to %.1f°C", temp)),
                                                false);
                                        syncToClient(player, data);
                                    });
                            return 1;
                        }));
    }

    // /paramedicine setspo2 <0.0 - 1.0>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetSpO2()
    {
        return Commands.literal("setspo2")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                        .executes(ctx ->
                        {
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        data.setOxygenSaturation(value);
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(String.format(
                                                        "[Debug] SpO2 set to %.0f%%", value * 100f)),
                                                false);
                                        syncToClient(player, data);
                                    });
                            return 1;
                        }));
    }

    // /paramedicine tourniquet <limbNode> <on|off>
    // Toggles proximal circulation on a node to simulate tourniquet application.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdTourniquet()
    {
        return Commands.literal("tourniquet")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("limb", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            for (LimbNode n : LimbNode.values())
                                if (n.canApplyTourniquet) builder.suggest(n.name());
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests((ctx, builder) ->
                                {
                                    builder.suggest("on");
                                    builder.suggest("off");
                                    return builder.buildFuture();
                                })
                                .executes(ctx ->
                                {
                                    String limbStr = StringArgumentType.getString(ctx, "limb");
                                    String state   = StringArgumentType.getString(ctx, "state").toLowerCase();

                                    LimbNode node;
                                    try { node = LimbNode.valueOf(limbStr.toUpperCase()); }
                                    catch (IllegalArgumentException e)
                                    {
                                        ctx.getSource().sendFailure(
                                                Component.literal("Unknown limb node: " + limbStr));
                                        return 0;
                                    }

                                    if (!node.canApplyTourniquet)
                                    {
                                        ctx.getSource().sendFailure(
                                                Component.literal(node + " cannot have a tourniquet applied."));
                                        return 0;
                                    }

                                    boolean applied = state.equals("on");
                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                    if (player == null) return 0;

                                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                            .ifPresent(data ->
                                            {
                                                // Tourniquet on upper arm: blocks proximal inflow TO that node.
                                                data.getLimb(node).setCirculatingProximally(!applied);

                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal(String.format(
                                                                "[Debug] Tourniquet %s on %s.",
                                                                applied ? "applied" : "removed", node)),
                                                        false);
                                                syncToClient(player, data);
                                            });
                                    return 1;
                                })));
    }

    // /paramedicine limbstatus <limbNode>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdLimbStatus()
    {
        return Commands.literal("limbstatus")
                .requires(src -> src.hasPermission(0))
                .then(Commands.argument("limb", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                        {
                            for (LimbNode n : LimbNode.values())
                                builder.suggest(n.name());
                            return builder.buildFuture();
                        })
                        .executes(ctx ->
                        {
                            String limbStr = StringArgumentType.getString(ctx, "limb");

                            LimbNode node;
                            try { node = LimbNode.valueOf(limbStr.toUpperCase()); }
                            catch (IllegalArgumentException e)
                            {
                                ctx.getSource().sendFailure(
                                        Component.literal("Unknown limb node: " + limbStr));
                                return 0;
                            }

                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            LimbNode finalNode = node;
                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH)
                                    .ifPresent(data ->
                                    {
                                        var limb = data.getLimb(finalNode);
                                        StringBuilder sb = new StringBuilder();
                                        sb.append(String.format("[%s] %s\n", finalNode, limb));

                                        if (limb.getWounds().isEmpty())
                                        {
                                            sb.append("  No wounds.");
                                        }
                                        else
                                        {
                                            for (int i = 0; i < limb.getWounds().size(); i++)
                                                sb.append(String.format("  Wound %d: %s\n",
                                                        i + 1, limb.getWounds().get(i)));
                                        }

                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(sb.toString()), false);
                                    });
                            return 1;
                        }));
    }

    // === HELPERS ===
    private static ServerPlayer requirePlayer(CommandSourceStack source)
    {
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("Must be run by a player."));
            return null;
        }
        return player;
    }

    private static void syncToClient(ServerPlayer player, PlayerHealthData data)
    {
        ModNetwork.CHANNEL.sendTo(
                ClientboundSyncHealthPacket.fromData(data),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}