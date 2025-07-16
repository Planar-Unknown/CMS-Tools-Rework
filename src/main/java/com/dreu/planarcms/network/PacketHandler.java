package com.dreu.planarcms.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

import static com.dreu.planarcms.PlanarCMS.MODID;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, SyncConfigS2CPacket.class, SyncConfigS2CPacket::toBytes, SyncConfigS2CPacket::new, SyncConfigS2CPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, RequestConfigIssuesC2SPacket.class, (c2SPacket, buf) -> {}, buf -> new RequestConfigIssuesC2SPacket(), RequestConfigIssuesC2SPacket::handle);
    }
}
