package com.dreu.planarcms.events;

import com.dreu.planarcms.network.PacketHandler;
import com.dreu.planarcms.network.SyncConfigS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.GeneralConfig.HOTSWAPPABLE;
import static com.dreu.planarcms.util.Helpers.configHasBeenParsed;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = MODID, bus = FORGE)
@SuppressWarnings("unused")
public class ForgeEvents {
  public static final Set<Player> playersToSendIssuesTo = new HashSet<>();
  public static boolean lastServerWasLocal = true;

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
    if (Minecraft.getInstance().isLocalServer() && !lastServerWasLocal) {
      configHasBeenParsed = false;
    }
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      playersToSendIssuesTo.add(serverPlayer);
      if (HOTSWAPPABLE) {
        for (Player player : serverPlayer.level().players())
          PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new SyncConfigS2CPacket());
      } else {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncConfigS2CPacket());
      }
    }
  }
}
