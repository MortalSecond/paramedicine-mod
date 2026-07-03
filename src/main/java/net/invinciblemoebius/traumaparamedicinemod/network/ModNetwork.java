package net.invinciblemoebius.traumaparamedicinemod.network;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

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
        CHANNEL.registerMessage(id++,
                ServerboundNodeActionPacket.class,
                ServerboundNodeActionPacket::encode,
                ServerboundNodeActionPacket::decode,
                ServerboundNodeActionPacket::handle
        );
        CHANNEL.registerMessage(id++,
                ClientboundInteractionFeedbackPacket.class,
                ClientboundInteractionFeedbackPacket::encode,
                ClientboundInteractionFeedbackPacket::decode,
                ClientboundInteractionFeedbackPacket::handle
        );
        CHANNEL.registerMessage(id++,
                ClientboundPulseReadingPacket.class,
                ClientboundPulseReadingPacket::encode,
                ClientboundPulseReadingPacket::decode,
                ClientboundPulseReadingPacket::handle
        );
        CHANNEL.registerMessage(id++,
                ServerboundApplyItemToNodePacket.class,
                ServerboundApplyItemToNodePacket::encode,
                ServerboundApplyItemToNodePacket::decode,
                ServerboundApplyItemToNodePacket::handle
        );
        CHANNEL.registerMessage(id,
                ServerboundDeathPacket.class,
                ServerboundDeathPacket::encode,
                ServerboundDeathPacket::new,
                ServerboundDeathPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
