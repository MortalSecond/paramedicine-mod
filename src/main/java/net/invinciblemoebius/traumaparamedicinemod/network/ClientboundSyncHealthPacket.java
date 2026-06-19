package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AirwayState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundSyncHealthPacket
{
    // === VALUES ===
    private final float bloodVolume;
    private final float heartRateBPM;
    private final float systolicBP;
    private final float diastolicBP;
    private final float fibrillations;
    private final boolean fibrillationsForced;
    private final float oxygenSaturation;
    private final float actualRespiratoryRate;
    private final float breathReserveSeconds;
    private final AirwayState airwayState;
    private final float immunity;
    private final float immuneReserve;
    private final float bacteremia;
    private final float aggregatedPain;
    private final float coreTemperature;
    private final float consciousness;
    // Full LungData detail sent separately when inspect screen is open.
    // HUD only needs total compromise to show the lung moodlet.
    private final float leftLungCompromise;
    private final float rightLungCompromise;
    private final float painShock;
    private final float septicShock;
    private final float stamina;
    private final boolean leftTension;
    private final boolean rightTension;
    private final float overexertionPain;

    // === CONSTRUCTOR ===
    private ClientboundSyncHealthPacket(float bloodVolume,
            float heartRateBPM,
            float systolicBP,
            float diastolicBP,
            float fibrillations,
            boolean fibrillationsForced,
            float oxygenSaturation,
            float actualRespiratoryRate,
            float breathReserveSeconds,
            AirwayState airwayState,
            float coreTemperature,
            float consciousness,
            float immunity,
            float immuneReserve,
            float bacteremia,
            float aggregatedPain,
            float leftLungCompromise,
            float rightLungCompromise,
            float painShock,
            float septicShock,
            float stamina,
            boolean leftTension,
            boolean rightTension,
            float overexertionPain
    )
    {
        this.bloodVolume = bloodVolume;
        this.heartRateBPM = heartRateBPM;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.fibrillations = fibrillations;
        this.fibrillationsForced = fibrillationsForced;
        this.oxygenSaturation = oxygenSaturation;
        this.actualRespiratoryRate = actualRespiratoryRate;
        this.breathReserveSeconds = breathReserveSeconds;
        this.airwayState = airwayState;
        this.coreTemperature = coreTemperature;
        this.consciousness = consciousness;
        this.immunity = immunity;
        this.immuneReserve = immuneReserve;
        this.bacteremia = bacteremia;
        this.aggregatedPain = aggregatedPain;
        this.leftLungCompromise = leftLungCompromise;
        this.rightLungCompromise = rightLungCompromise;
        this.painShock = painShock;
        this.septicShock = septicShock;
        this.stamina = stamina;
        this.leftTension = leftTension;
        this.rightTension = rightTension;
        this.overexertionPain = overexertionPain;
    }

    // === FACTORY ===
    public static ClientboundSyncHealthPacket fromData(PlayerHealthData data)
    {
        return new ClientboundSyncHealthPacket(
                data.getBloodVolume(),
                data.getHeartRateBPM(),
                data.getSystolicBP(),
                data.getDiastolicBP(),
                data.getFibrillations(),
                data.isFibrillationsForced(),
                data.getOxygenSaturation(),
                data.getActualRespiratoryRate(),
                data.getBreathReserveSeconds(),
                data.getAirwayState(),
                data.getCoreTemperature(),
                data.getConsciousness(),
                data.getImmunity(),
                data.getImmuneReserve(),
                data.getBacteremia(),
                data.getAggregatedPain(),
                data.getLeftLung().getCompromise(),
                data.getRightLung().getCompromise(),
                data.getPainShock(),
                data.getSepticShock(),
                data.getStamina(),
                data.getLeftLung().hasTensionPneumothorax(),
                data.getRightLung().hasTensionPneumothorax(),
                data.getOverexertionPain()
        );
    }

    // === NBT STUFF ===

    public static void encode(ClientboundSyncHealthPacket p, FriendlyByteBuf buf)
    {
        buf.writeFloat(p.bloodVolume);
        buf.writeFloat(p.heartRateBPM);
        buf.writeFloat(p.systolicBP);
        buf.writeFloat(p.diastolicBP);
        buf.writeFloat(p.fibrillations);
        buf.writeBoolean(p.fibrillationsForced);
        buf.writeFloat(p.oxygenSaturation);
        buf.writeFloat(p.actualRespiratoryRate);
        buf.writeFloat(p.breathReserveSeconds);
        buf.writeEnum(p.airwayState);
        buf.writeFloat(p.coreTemperature);
        buf.writeFloat(p.consciousness);
        buf.writeFloat(p.immunity);
        buf.writeFloat(p.immuneReserve);
        buf.writeFloat(p.bacteremia);
        buf.writeFloat(p.aggregatedPain);
        buf.writeFloat(p.leftLungCompromise);
        buf.writeFloat(p.rightLungCompromise);
        buf.writeFloat(p.painShock);
        buf.writeFloat(p.septicShock);
        buf.writeFloat(p.stamina);
        buf.writeBoolean(p.leftTension);
        buf.writeBoolean(p.rightTension);
        buf.writeFloat(p.overexertionPain);
    }

    public static ClientboundSyncHealthPacket decode(FriendlyByteBuf buf)
    {
        return new ClientboundSyncHealthPacket(
                buf.readFloat(),                        // bloodVolume
                buf.readFloat(),                        // heartRateBPM
                buf.readFloat(),                        // systolicBP
                buf.readFloat(),                        // diastolicBP
                buf.readFloat(),                        // fibrillations
                buf.readBoolean(),                      // fibrillationsForced
                buf.readFloat(),                        // oxygenSaturation
                buf.readFloat(),                        // actualRespiratoryRate
                buf.readFloat(),                        // breathReserveSeconds
                buf.readEnum(AirwayState.class),        // airwayState
                buf.readFloat(),                        // coreTemperature
                buf.readFloat(),                        // consciousness
                buf.readFloat(),                        // immunity
                buf.readFloat(),                        // immuneReserve
                buf.readFloat(),                        // bacteremia
                buf.readFloat(),                        // aggregatedPain
                buf.readFloat(),                        // leftLungCompromise
                buf.readFloat(),                        // rightLungCompromise
                buf.readFloat(),                        // painShock
                buf.readFloat(),                        // septicShock
                buf.readFloat(),                        // stamina
                buf.readBoolean(),                      // leftTension
                buf.readBoolean(),                      // rightTension
                buf.readFloat()                         // overexertionPain
        );
    }

    // === HANDLER ===
    public static void handle(ClientboundSyncHealthPacket packet, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data -> applyToData(packet, data));
        });
        ctx.get().setPacketHandled(true);
    }

    // Writes packets into clientside PlayerHealthData instance.
    // The client copy is just for display, the server owns the math and logic.
    private static void applyToData(ClientboundSyncHealthPacket p, PlayerHealthData data)
    {
        // Client copy uses the direct setter since recomputeBloodVolume()
        // is a server-only operation. The packet carries the already-computed sum.
        data.setBloodVolumeClientOnly(p.bloodVolume);

        // Cardiovascular.
        data.setHeartRateBPMClientOnly(p.heartRateBPM);
        data.setSystolicBPClientOnly(p.systolicBP);
        data.setDiastolicBPClientOnly(p.diastolicBP);
        data.setFibrillations(p.fibrillations);
        data.setFibrillationsForced(p.fibrillationsForced, 0f); // Target not needed client-side.
        data.setOxygenSaturation(p.oxygenSaturation);
        data.setActualRespiratoryRateClientOnly(p.actualRespiratoryRate);
        data.setBreathReserveSecondsClientOnly(p.breathReserveSeconds);
        data.setAirwayState(p.airwayState);
        data.setImmunity(p.immunity);
        data.setImmuneReserve(p.immuneReserve);
        data.setBacteremia(p.bacteremia);
        data.setAggregatedPainClientOnly(p.aggregatedPain);
        data.setCoreTemperature(p.coreTemperature);
        data.setConsciousnessClientOnly(p.consciousness);
        data.setPainShock(p.painShock);
        data.setSepticShock(p.septicShock);
        data.setStamina(p.stamina);
        data.getLeftLung().setTensionPneumothorax(p.leftTension);
        data.getRightLung().setTensionPneumothorax(p.rightTension);
        data.setOverexertionPain(p.overexertionPain);

        // Lung compromise. Applied to LungData directly.
        data.getLeftLung().setCompromiseClientOnly(p.leftLungCompromise);
        data.getRightLung().setCompromiseClientOnly(p.rightLungCompromise);
    }
}
