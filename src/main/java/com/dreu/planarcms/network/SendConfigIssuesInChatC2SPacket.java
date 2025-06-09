package com.dreu.planarcms.network;

import com.dreu.planarcms.events.ForgeEvents;
import com.dreu.planarcms.util.Helpers;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SendConfigIssuesInChatC2SPacket {

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      ForgeEvents.playersToSendIssuesTo.forEach(Helpers::sendConfigIssuesInChat);
      ForgeEvents.playersToSendIssuesTo.clear();
    });
  }
}
