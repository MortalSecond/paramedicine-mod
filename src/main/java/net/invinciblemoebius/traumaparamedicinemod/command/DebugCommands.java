package net.invinciblemoebius.traumaparamedicinemod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.item.FluidContainerItem;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AirwayState;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LungData;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.ClientboundSyncHealthPacket;
import net.invinciblemoebius.traumaparamedicinemod.network.ModNetwork;
import net.invinciblemoebius.traumaparamedicinemod.substance.CirculatingSubstance;
import net.invinciblemoebius.traumaparamedicinemod.substance.FluidMixture;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundDepth;
import net.invinciblemoebius.traumaparamedicinemod.wound.WoundType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;

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
                        .then(cmdSetInstability())
                        .then(cmdSetHeartReserve())
                        .then(cmdSetTemp())
                        .then(cmdSetSpO2())
                        .then(cmdTourniquet())
                        .then(cmdLimbStatus())
                        .then(cmdSetReserve())
                        .then(cmdSetBacteremia())
                        .then(cmdInfectWounds())
                        .then(cmdGiveSubstance())
                        .then(cmdFill())
                        .then(cmdSetHydration())
                        .then(cmdSetNutrition())
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
                .then(enumArg("woundType", WoundType.class)
                        .then(enumArg("depth", WoundDepth.class)
                                .then(enumArg("limb", LimbNode.class)
                                        .then(Commands.argument("size", FloatArgumentType.floatArg(0.01f, 1f))
                                                .executes(ctx ->
                                                {
                                                    WoundType  type  = getEnum(ctx, "woundType", WoundType.class);
                                                    WoundDepth depth = getEnum(ctx, "depth", WoundDepth.class);
                                                    LimbNode   node  = getEnum(ctx, "limb", LimbNode.class);
                                                    float      size  = FloatArgumentType.getFloat(ctx, "size");
                                                    return withData(ctx, (player, data) ->
                                                    {
                                                        Wound w = new Wound(type, depth, size);
                                                        data.getLimb(node).addWound(w);
                                                        msg(ctx.getSource(), String.format(
                                                                "[Debug] %s %s (%.2f) on %s — bleed %.3f ml/s",
                                                                depth, type, size, node, w.getBleedRateML()));
                                                    });
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

    // /paramedicine setinstability <0.0 - 1.0>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetInstability()
    {
        return Commands.literal("setinstability")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                        .executes(ctx -> withData(ctx, (player, data) ->
                        {
                            float v = FloatArgumentType.getFloat(ctx, "value");
                            data.setElectricalInstability(v);
                            msg(ctx.getSource(), String.format(
                                    "[Debug] Electrical instability = %.2f  (rhythm: %s)", v, data.getRhythm()));
                        })));
    }

    // /paramedicine setheartreserve <0.0 - 1.0>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetHeartReserve()
    {
        return Commands.literal("setheartreserve")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 1f))
                        .executes(ctx -> withData(ctx, (player, data) ->
                        {
                            float v = FloatArgumentType.getFloat(ctx, "value");
                            data.setHeartReserve(v);
                            msg(ctx.getSource(), String.format(
                                    "[Debug] Heart reserve = %.2f  (rhythm: %s)", v, data.getRhythm()));
                        })));
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
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(LimbNode.values()).map(Enum::name), b))
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
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(LimbNode.values()).map(Enum::name), b))
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

    // /paramedicine setreserve <0.0 - MAX>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetReserve()
    {
        return Commands.literal("setreserve")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, ModConstants.IMMUNE_RESERVE_MAX))
                        .executes(ctx ->
                        {
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                            {
                                data.setImmuneReserve(value);
                                ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                                        "[Debug] Immune reserve set to %.2f / %.1f", value, ModConstants.IMMUNE_RESERVE_MAX)), false);
                                syncToClient(player, data);
                            });
                            return 1;
                        }));
    }

    // /paramedicine setbacteremia <0.0 - 5.0>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetBacteremia()
    {
        return Commands.literal("setbacteremia")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, 5f))
                        .executes(ctx ->
                        {
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            ServerPlayer player = requirePlayer(ctx.getSource());
                            if (player == null) return 0;

                            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                            {
                                data.setBacteremia(value);
                                ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                                        "[Debug] Bacteremia set to %.3f", value)), false);
                                syncToClient(player, data);
                            });
                            return 1;
                        }));
    }

    // /paramedicine infectwounds <limb> <0.0 - 1.0>
    // Forces infection onto every wound of a limb so the systemic pass has something to chew on.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdInfectWounds()
    {
        return Commands.literal("infectwounds")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("limb", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(LimbNode.values()).map(Enum::name), b))
                        .then(Commands.argument("level", FloatArgumentType.floatArg(0f, 1f))
                                .executes(ctx ->
                                {
                                    String limbStr = StringArgumentType.getString(ctx, "limb");
                                    float  level   = FloatArgumentType.getFloat(ctx, "level");

                                    LimbNode node;
                                    try { node = LimbNode.valueOf(limbStr.toUpperCase()); }
                                    catch (IllegalArgumentException e)
                                    {
                                        ctx.getSource().sendFailure(Component.literal("Unknown limb node: " + limbStr));
                                        return 0;
                                    }

                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                    if (player == null) return 0;

                                    LimbNode finalNode = node;
                                    player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
                                    {
                                        var limb = data.getLimb(finalNode);
                                        int count = 0;
                                        for (var wound : limb.getWounds())
                                        {
                                            wound.setInfectionLevel(level);
                                            count++;
                                        }
                                        int finalCount = count;
                                        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                                                "[Debug] Set infection to %.2f on %d wound(s) in %s.", level, finalCount, finalNode)), false);
                                        syncToClient(player, data);
                                    });
                                    return 1;
                                })));
    }

    // /paramedicine give <type> <ml> <limb>
    // Drops a substance into a node's LOCAL list.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdGiveSubstance()
    {
        return Commands.literal("give")
                .requires(src -> src.hasPermission(2))
                .then(enumArg("type", SubstanceType.class)
                        .then(Commands.argument("ml", FloatArgumentType.floatArg(0.0001f, 5000f))
                                .then(enumArg("limb", LimbNode.class)
                                        .executes(ctx ->
                                        {
                                            SubstanceType type = getEnum(ctx, "type", SubstanceType.class);
                                            LimbNode node = getEnum(ctx, "limb", LimbNode.class);
                                            float ml = FloatArgumentType.getFloat(ctx, "ml");
                                            return withData(ctx, (player, data) ->
                                            {
                                                data.getLimb(node).addLocalSubstance(new CirculatingSubstance(type, ml, 5f));
                                                msg(ctx.getSource(), String.format(
                                                        "[Debug] Gave %.2fml %s to %s (local).", ml, type, node));
                                            });
                                        }))));
    }

    // /paramedicine fill <substance> <ml>
    // Tops up the held fluid container up with a substance, capped at the container's capacity.
    // Works on a single container peeled off the held stack it doesn't produce a stack of identical filled ones.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdFill()
    {
        return Commands.literal("fill")
                .requires(src -> src.hasPermission(2))
                .then(enumArg("substance", SubstanceType.class)
                        .then(Commands.argument("ml", FloatArgumentType.floatArg(0.01f, 5000f))
                                .executes(ctx ->
                                {
                                    SubstanceType type = getEnum(ctx, "substance", SubstanceType.class);
                                    float ml = FloatArgumentType.getFloat(ctx, "ml");

                                    ServerPlayer player = requirePlayer(ctx.getSource());
                                    if (player == null) return 0;

                                    ItemStack held = player.getMainHandItem();
                                    if (!(held.getItem() instanceof FluidContainerItem container))
                                    {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "Hold a fluid container (syringe, glass) in your main hand."));
                                        return 0;
                                    }

                                    // Operate on one unit so stacked empties don't all become filled.
                                    ItemStack single = held.copy();
                                    single.setCount(1);

                                    FluidMixture mix = FluidContainerItem.getMixture(single);
                                    float accepted = mix.add(type, ml, container.getCapacityML());
                                    FluidContainerItem.setMixture(single, mix);

                                    // Put the filled one back, return the remainder to the hand.
                                    held.shrink(1);
                                    if (held.isEmpty())
                                        player.setItemInHand(InteractionHand.MAIN_HAND, single);
                                    else if (!player.getInventory().add(single))
                                        player.drop(single, false);

                                    float finalAccepted = accepted;
                                    msg(ctx.getSource(), String.format(
                                            "[Debug] Added %.2fml %s. Now: %s", finalAccepted, type, mix.describe()));
                                    return 1;
                                })));
    }

    // /paramedicine sethydration <ml>
    // Sets the body-water reserve directly.
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetHydration()
    {
        return Commands.literal("sethydration")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("ml", FloatArgumentType.floatArg(0f, 6000f))
                        .executes(ctx -> withData(ctx, (player, data) ->
                        {
                            float ml = FloatArgumentType.getFloat(ctx, "ml");
                            data.addBodyWater(ml - data.getBodyWater()); // set-to via signed add
                            msg(ctx.getSource(), String.format(
                                    "[Debug] Body water = %.0fml (%.0f%% of normal)",
                                    data.getBodyWater(), data.getHydrationFraction() * 100f));
                        })));
    }

    // /paramedicine setnutrition <0.0 - 1.5>
    private static ArgumentBuilder<CommandSourceStack, ?> cmdSetNutrition()
    {
        return Commands.literal("setnutrition")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0f, ModConstants.NUTRITION_MAX))
                        .executes(ctx -> withData(ctx, (player, data) ->
                        {
                            float v = FloatArgumentType.getFloat(ctx, "value");
                            data.addNutrition(v - data.getNutrition());
                            msg(ctx.getSource(), String.format(
                                    "[Debug] Nutrition = %.0f%% (immune factor %.2f)", data.getNutrition() * 100f, data.getNutritionFactor()));
                        })));
    }

    // === HELPERS ===

    // Filtered enum suggestions. THIS is what makes autocomplete narrow.
    private static <E extends Enum<E>> SuggestionProvider<CommandSourceStack> enumSuggest(Class<E> cls)
    {
        return (ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(cls.getEnumConstants()).map(Enum::name), b);
    }

    // An enum argument (word and filtered suggestions) in one call.
    private static <E extends Enum<E>> RequiredArgumentBuilder<CommandSourceStack, String> enumArg(String name, Class<E> cls)
    {
        return Commands.argument(name, StringArgumentType.word()).suggests(enumSuggest(cls));
    }

    // Parse it back, throwing a clean Brigadier error (no try/catch/sendFailure/return 0).
    private static <E extends Enum<E>> E getEnum(CommandContext<CommandSourceStack> ctx, String name, Class<E> cls) throws CommandSyntaxException
    {
        String raw = StringArgumentType.getString(ctx, name);
        try
        {
            return Enum.valueOf(cls, raw.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw new SimpleCommandExceptionType(Component.literal("Unknown " + cls.getSimpleName() + ": " + raw)).create();
        }
    }

    // Wrap the player + capability + sync boilerplate. Sync happens automatically.
    private static int withData(CommandContext<CommandSourceStack> ctx, BiConsumer<ServerPlayer, PlayerHealthData> action)
    {
        ServerPlayer player = requirePlayer(ctx.getSource());
        if (player == null)
            return 0;

        player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
        {
            action.accept(player, data);
            syncToClient(player, data);
        });

        return 1;
    }

    private static void msg(CommandSourceStack src, String s)
    {
        src.sendSuccess(() -> Component.literal(s), false);
    }

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