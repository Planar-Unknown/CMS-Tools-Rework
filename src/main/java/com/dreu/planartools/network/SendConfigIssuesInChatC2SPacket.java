package com.dreu.planartools.network;

import com.dreu.planartools.util.Helpers;
import com.dreu.planartools.events.ForgeEvents;
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
