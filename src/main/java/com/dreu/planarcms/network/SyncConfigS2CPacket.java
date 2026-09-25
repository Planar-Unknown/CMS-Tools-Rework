package com.dreu.planarcms.network;

import com.dreu.planarcms.config.BlocksConfig;
import com.dreu.planarcms.config.DisplayConfig;
import com.dreu.planarcms.config.ToolsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.dreu.planarcms.PlanarCMS.populateTagKeys;
import static com.dreu.planarcms.config.BlocksConfig.BLOCKS;
import static com.dreu.planarcms.config.ToolsConfig.*;
import static com.dreu.planarcms.util.Helpers.*;

public class SyncConfigS2CPacket {

  public SyncConfigS2CPacket(FriendlyByteBuf buf) {
    Map<String, BlocksConfig.Properties> blockProperties = new HashMap<>();
    Map<String, ToolsConfig.Properties> toolProperties = new HashMap<>();

    int size = buf.readInt();
    for (int i = 0; i < size; i++)
      blockProperties.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), BlocksConfig.Properties.read(buf));
    BLOCKS = blockProperties;

    size = buf.readInt();
    for (int i = 0; i < size; i++)
      toolProperties.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), ToolsConfig.Properties.readFromBuffer(buf));
    TOOLS = toolProperties;

    size = buf.readInt();
    CONFIG_ISSUES.clear();
    for (int i = 0; i < size; i++)
      CONFIG_ISSUES.add(new Issue(
          LogLevel.values()[buf.readByte()],
          buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(),
          buf.readByte()
      ));

    size = buf.readInt();
    if (size < 0 || size > MAX_TOOL_TYPES)
      throw new IllegalArgumentException("Invalid tool type count");
    REGISTERED_TOOL_TYPES.clear();
    REGISTERED_TOOL_COLORS.clear();
    for (int i = 0; i < size; i++) {
      REGISTERED_TOOL_TYPES.add(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString());
      REGISTERED_TOOL_COLORS.add(buf.readInt());
    }
    populateTagKeys();
  }

  public SyncConfigS2CPacket() {
    if (!configHasBeenPopulated)
      parseAndPopulateConfig();
  }

  public void toBytes(FriendlyByteBuf buf) {
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
      props.getValue().writeToBuffer(buf);
    }

    buf.writeInt(CONFIG_ISSUES.size());
    for (Issue issue : CONFIG_ISSUES) {
      buf.writeByte(issue.level().ordinal());
      buf.writeInt(issue.contents().length());
      buf.writeCharSequence(issue.contents(), StandardCharsets.UTF_8);
      buf.writeByte(issue.priority());
    }

    buf.writeInt(REGISTERED_TOOL_TYPES.size());
    for (int i = 0; i < REGISTERED_TOOL_TYPES.size(); i++) {
      buf.writeInt(REGISTERED_TOOL_TYPES.get(i).length());
      buf.writeCharSequence(REGISTERED_TOOL_TYPES.get(i), StandardCharsets.UTF_8);
      buf.writeInt(REGISTERED_TOOL_COLORS.get(i));
    }
  }

  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(() -> {
      DisplayConfig.parse();
      DisplayConfig.populate();
      PacketHandler.CHANNEL.sendToServer(new RequestConfigIssuesC2SPacket());
      configHasBeenPopulated = false;
    });
    context.get().setPacketHandled(true);
  }
}
