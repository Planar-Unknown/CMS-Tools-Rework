package com.dreu.planarcms.events;

import com.dreu.planarcms.network.PacketHandler;
import com.dreu.planarcms.network.SyncConfigS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.GeneralConfig.HOTSWAPPABLE;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = MODID, bus = FORGE, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused")
public class ServerForgeEvents {
  public static final Set<Player> playersToSendIssuesTo = new HashSet<>();

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
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
