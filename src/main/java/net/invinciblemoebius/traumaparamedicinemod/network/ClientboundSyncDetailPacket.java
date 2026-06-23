package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthCapability;
import net.invinciblemoebius.traumaparamedicinemod.health.PlayerHealthData;
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
    private final List<SubstanceType> substances;

    private ClientboundSyncDetailPacket(Map<LimbNode, List<Wound>> woundsByNode, List<SubstanceType> substances)
    {
        this.woundsByNode = woundsByNode;
        this.substances = substances;
    }

    public static ClientboundSyncDetailPacket fromData(PlayerHealthData data)
    {
        Map<LimbNode, List<Wound>> map = new EnumMap<>(LimbNode.class);
        for (LimbNode node : LimbNode.values())
            map.put(node, new ArrayList<>(data.getLimb(node).getWounds()));
        return new ClientboundSyncDetailPacket(map, data.collectActiveSubstanceTypes());
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
        }
        buf.writeVarInt(p.substances.size());
        for (SubstanceType substance : p.substances)
            buf.writeEnum(substance);
    }

    public static ClientboundSyncDetailPacket decode(FriendlyByteBuf buf)
    {
        Map<LimbNode, List<Wound>> map = new EnumMap<>(LimbNode.class);
        for (LimbNode node : LimbNode.values())
        {
            int n = buf.readVarInt();
            List<Wound> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {
                Wound wound = new Wound();
                wound.loadFromNBT(buf.readNbt());
                list.add(wound);
            }
            map.put(node, list);
        }
        int sc = buf.readVarInt();
        List<SubstanceType> subs = new ArrayList<>(sc);
        for (int i = 0; i < sc; i++)
            subs.add(buf.readEnum(SubstanceType.class));
        return new ClientboundSyncDetailPacket(map, subs);
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
                    data.getLimb(node).setWoundsClientOnly(p.woundsByNode.getOrDefault(node, Collections.emptyList()));
                data.setClientActiveSubstances(p.substances);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}