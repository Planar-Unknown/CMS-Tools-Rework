package com.dreu.planarcms.events;

import com.dreu.planarcms.network.PacketHandler;
import com.dreu.planarcms.network.SyncConfigS2CPacket;
import com.dreu.planarcms.util.Helpers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MODID, bus = FORGE)
public class ServerForgeEvents {
  public static final Set<Player> playersToSendIssuesTo = new HashSet<>();

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      playersToSendIssuesTo.add(serverPlayer);
      PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncConfigS2CPacket());
    }
  }

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("cms")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("reload")
            .executes(context -> {
              Helpers.parseAndPopulateConfig();
              for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers())
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigS2CPacket());
              context.getSource().sendSuccess(() -> Component.literal("Reloaded Planar Tools config"), true);
              if (context.getSource().getEntity() instanceof ServerPlayer player)
                Helpers.sendConfigIssuesInChat(player);
              return 1;
            })));
  }
}
