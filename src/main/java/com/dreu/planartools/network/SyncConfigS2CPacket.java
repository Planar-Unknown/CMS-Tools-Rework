package com.dreu.planartools.network;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.dreu.planartools.Util.getUpdateTime;
import static com.dreu.planartools.Util.shouldUpdateTime;
import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.GeneralConfig.GLOBAL_DEFAULT_RESISTANCE;
import static com.dreu.planartools.config.GeneralConfig.USE_GLOBAL_DEFAULT;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

public class SyncConfigS2CPacket {
  private final Map<String, BlocksConfig.Properties> blockProperties;
  private final Map<String, ToolsConfig.Properties> toolProperties;
  private final boolean useGlobalDefault;
  private final int globalDefaultResistance;

  public void toBytes(FriendlyByteBuf buf) {
    buf.writeInt(this.globalDefaultResistance);
    buf.writeBoolean(this.useGlobalDefault);

    buf.writeInt(BLOCKS.size());
    for (Map.Entry<String, BlocksConfig.Properties> props : BLOCKS.entrySet()) {
      buf.writeInt(props.getKey().length());
      buf.writeCharSequence(props.getKey(), StandardCharsets.UTF_8);
      props.getValue().write(buf);
    }

    buf.writeInt(TOOLS.size());
    for (Map.Entry<String, ToolsConfig.Properties> props : TOOLS.entrySet()) {
      buf.writeInt(props.getKey().length());
      buf.writeCharSequence(props.getKey(), StandardCharsets.UTF_8);
      props.getValue().write(buf);
    }
  }

  public SyncConfigS2CPacket (FriendlyByteBuf buf) {
    this.globalDefaultResistance = buf.readInt();
    this.useGlobalDefault = buf.readBoolean();
    Map<String, BlocksConfig.Properties> bp = new HashMap<>();
    Map<String, ToolsConfig.Properties> tp = new HashMap<>();

    int bounds = buf.readInt();
    for (int i = 0; i < bounds; i++)
      bp.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), BlocksConfig.Properties.read(buf));

    bounds = buf.readInt();
    for (int i = 0; i < bounds; i++)
      tp.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), ToolsConfig.Properties.read(buf));

    this.blockProperties = bp;
    this.toolProperties = tp;
  }

  public SyncConfigS2CPacket (Map<String, BlocksConfig.Properties> blockProperties, Map<String, ToolsConfig.Properties> toolProperties, boolean useGlobalDefault, int globalDefaultResistance) {
    this.blockProperties = blockProperties;
    this.toolProperties = toolProperties;
    this.useGlobalDefault = useGlobalDefault;
    this.globalDefaultResistance = globalDefaultResistance;
  }

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      shouldUpdateTime = getUpdateTime();

      BLOCKS = blockProperties;
      TOOLS = toolProperties;
      USE_GLOBAL_DEFAULT = useGlobalDefault;
      GLOBAL_DEFAULT_RESISTANCE = useGlobalDefault ? globalDefaultResistance : 0;
    });
    context.get().setPacketHandled(true);
  }
}
