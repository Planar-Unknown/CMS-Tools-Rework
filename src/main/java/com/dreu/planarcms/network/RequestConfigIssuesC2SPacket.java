package com.dreu.planarcms.network;

import com.dreu.planarcms.util.Helpers;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.dreu.planarcms.events.ServerForgeEvents.playersToSendIssuesTo;

public class RequestConfigIssuesC2SPacket {

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      playersToSendIssuesTo.forEach(Helpers::sendConfigIssuesInChat);
      playersToSendIssuesTo.clear();
    });
    context.get().setPacketHandled(true);
  }
}
