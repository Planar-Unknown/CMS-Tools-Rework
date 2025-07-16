package com.dreu.planarcms.network;

import com.dreu.planarcms.events.ServerForgeEvents;
import com.dreu.planarcms.util.Helpers;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestConfigIssuesC2SPacket {

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      ServerForgeEvents.playersToSendIssuesTo.forEach(Helpers::sendConfigIssuesInChat);
      ServerForgeEvents.playersToSendIssuesTo.clear();
    });
  }
}
