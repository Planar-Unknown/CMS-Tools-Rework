package com.dreu.planartools.network;

import com.dreu.planartools.Util;
import com.dreu.planartools.events.ForgeEvents;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SendConfigIssuesInChatC2SPacket {

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      ForgeEvents.playersToSendIssuesTo.forEach(Util::sendConfigIssuesInChat);
      ForgeEvents.playersToSendIssuesTo.clear();
    });
  }
}
