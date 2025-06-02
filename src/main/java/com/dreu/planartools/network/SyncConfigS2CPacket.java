package com.dreu.planartools.network;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.GeneralConfig.*;
import static com.dreu.planartools.config.ToolsConfig.*;
import static com.dreu.planartools.util.Helpers.*;

public class SyncConfigS2CPacket {


  public SyncConfigS2CPacket(FriendlyByteBuf buf) {
    USE_GLOBAL_DEFAULT = buf.readBoolean();
    GLOBAL_DEFAULT_RESISTANCE = USE_GLOBAL_DEFAULT ? buf.readInt() : 0;
    Map<String, BlocksConfig.Properties> bp = new HashMap<>();
    Map<String, ToolsConfig.Properties> tp = new HashMap<>();

    int bounds = buf.readInt();
    for (int i = 0; i < bounds; i++)
      bp.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), BlocksConfig.Properties.read(buf));
    BLOCKS = bp;

    bounds = buf.readInt();
    for (int i = 0; i < bounds; i++)
      tp.put(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(), ToolsConfig.Properties.readFromBuffer(buf));
    TOOLS = tp;

    bounds = buf.readInt();
    CONFIG_ISSUES.clear();
    for (int i = 0; i < bounds; i++)
      CONFIG_ISSUES.add(new Issue(
          LogLevel.values()[buf.readByte()],
          buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString(),
          buf.readByte()
      ));

    bounds = buf.readInt();
    REGISTERED_TOOL_TYPES.clear();
    REGISTERED_TOOL_COLORS.clear();
    for (int i = 0; i < bounds; i++) {
      REGISTERED_TOOL_TYPES.add(buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString());
      REGISTERED_TOOL_COLORS.add(buf.readInt());
    }
    shouldUpdateTime = getUpdateTime();
  }

  public SyncConfigS2CPacket() {
    if (!configHasBeenParsed || HOTSWAPPABLE)
      parseAndProcessConfig();
  }

  public void toBytes(FriendlyByteBuf buf) {
    buf.writeBoolean(USE_GLOBAL_DEFAULT);
    if (USE_GLOBAL_DEFAULT)
      buf.writeInt(GLOBAL_DEFAULT_RESISTANCE);

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
    context.get().enqueueWork(() -> PacketHandler.CHANNEL.sendToServer(new SendConfigIssuesInChatC2SPacket()));
    context.get().setPacketHandled(true);
  }
}
