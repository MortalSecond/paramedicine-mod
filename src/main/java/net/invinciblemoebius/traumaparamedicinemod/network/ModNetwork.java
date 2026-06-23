package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork
{
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ParamedicineMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register()
    {
        CHANNEL.registerMessage(id++,
            ClientboundSyncHealthPacket.class,
                ClientboundSyncHealthPacket::encode,
                ClientboundSyncHealthPacket::decode,
                ClientboundSyncHealthPacket::handle
        );
        CHANNEL.registerMessage(id++,
                ServerboundInspectPacket.class,
                ServerboundInspectPacket::encode,
                ServerboundInspectPacket::decode,
                ServerboundInspectPacket::handle
        );
        CHANNEL.registerMessage(id++,
                ClientboundSyncDetailPacket.class,
                ClientboundSyncDetailPacket::encode,
                ClientboundSyncDetailPacket::decode,
                ClientboundSyncDetailPacket::handle
        );
    }
}
