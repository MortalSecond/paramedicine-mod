package net.invinciblemoebius.traumaparamedicinemod.network.packets;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
import net.invinciblemoebius.traumaparamedicinemod.limbs.AppliedDressing;
import net.invinciblemoebius.traumaparamedicinemod.limbs.LimbNode;
import net.invinciblemoebius.traumaparamedicinemod.substance.SubstanceType;
import net.invinciblemoebius.traumaparamedicinemod.wound.Wound;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Heavy, on-demand. Streamed only while a viewer is inspecting this target.
public class ClientboundSyncDetailPacket
{
    private final Map<LimbNode, List<Wound>> woundsByNode;
    private final Map<LimbNode, List<AppliedDressing>> dressingsByNode;
    private final List<SubstanceType> substances;

    private ClientboundSyncDetailPacket(Map<LimbNode, List<Wound>> woundsByNode, Map<LimbNode, List<AppliedDressing>> dressingsByNode, List<SubstanceType> substances)
    {
        this.woundsByNode = woundsByNode;
        this.dressingsByNode = dressingsByNode;
        this.substances = substances;
    }

    public static ClientboundSyncDetailPacket fromData(PlayerHealthData data)
    {
        Map<LimbNode, List<Wound>> wounds = new EnumMap<>(LimbNode.class);
        Map<LimbNode, List<AppliedDressing>> dressings = new EnumMap<>(LimbNode.class);
        for (LimbNode node : LimbNode.values())
        {
            wounds.put(node, new ArrayList<>(data.getLimb(node).getWounds()));
            dressings.put(node, new ArrayList<>(data.getLimb(node).getDressings()));
        }
        return new ClientboundSyncDetailPacket(wounds, dressings, data.collectActiveSubstanceTypes());
    }

    public static void encode(ClientboundSyncDetailPacket p, FriendlyByteBuf buf)
    {
        for (LimbNode node : LimbNode.values())
        {
            List<Wound> list = p.woundsByNode.getOrDefault(node, Collections.emptyList());
            buf.writeVarInt(list.size());
            for (Wound wound : list)
            {
                CompoundTag t = new CompoundTag();
                wound.saveToNBT(t);
                buf.writeNbt(t);
            }

            List<AppliedDressing> appliedDressingsList = p.dressingsByNode.getOrDefault(node, Collections.emptyList());
            buf.writeVarInt(appliedDressingsList.size());
            for (AppliedDressing appliedDressing : appliedDressingsList)
            {
                CompoundTag t = new CompoundTag();
                appliedDressing.saveToNBT(t);
                buf.writeNbt(t);
            }
        }
        buf.writeVarInt(p.substances.size());
        for (SubstanceType substance : p.substances)
            buf.writeEnum(substance);
    }

    public static ClientboundSyncDetailPacket decode(FriendlyByteBuf buf)
    {
        Map<LimbNode, List<Wound>> wounds = new EnumMap<>(LimbNode.class);
        Map<LimbNode, List<AppliedDressing>> dressings = new EnumMap<>(LimbNode.class);
        for (LimbNode node : LimbNode.values())
        {
            int wn = buf.readVarInt();
            List<Wound> wlist = new ArrayList<>(wn);
            for (int i = 0; i < wn; i++)
            {
                Wound wound = new Wound();
                wound.loadFromNBT(buf.readNbt());
                wlist.add(wound);
            }
            wounds.put(node, wlist);

            int dn = buf.readVarInt();
            List<AppliedDressing> dlist = new ArrayList<>(dn);
            for (int i = 0; i < dn; i++)
                dlist.add(AppliedDressing.loadFromNBT(buf.readNbt()));
            dressings.put(node, dlist);
        }
        int sc = buf.readVarInt();
        List<SubstanceType> subs = new ArrayList<>(sc);
        for (int i = 0; i < sc; i++)
            subs.add(buf.readEnum(SubstanceType.class));
        return new ClientboundSyncDetailPacket(wounds, dressings, subs);
    }

    public static void handle(ClientboundSyncDetailPacket p, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            var player = Minecraft.getInstance().player;
            if (player == null)
                return;

            player.getCapability(PlayerHealthCapability.PLAYER_HEALTH).ifPresent(data ->
            {
                for (LimbNode node : LimbNode.values())
                {
                    data.getLimb(node).setWoundsClientOnly(p.woundsByNode.getOrDefault(node, Collections.emptyList()));
                    data.getLimb(node).setDressingsClientOnly(p.dressingsByNode.getOrDefault(node, Collections.emptyList()));
                }
                data.setClientActiveSubstances(p.substances);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}