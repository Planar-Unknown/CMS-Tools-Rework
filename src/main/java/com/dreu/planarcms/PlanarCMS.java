package com.dreu.planarcms;

import com.dreu.planarcms.config.*;
import com.dreu.planarcms.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.dreu.planarcms.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;
import static com.dreu.planarcms.util.Helpers.addConfigIssue;

@Mod(PlanarCMS.MODID)
public class PlanarCMS {
    //Todo: Overhaul core system, make every property predicated by Power
    //Todo: Add /planarConfigReload command to reload configs without closing the world
    //Todo: Fix bugs with Blocks and Tooltypes not being handled correctly

    //ChromaMoon will pay MAX of $50 for:
    //Todo: Mixin to ItemStack#getAttributeModifiers to allow config of tool attack damage
    //Todo: Mixin to ItemStack#getMaxDamage to allow config of tool durability
    //Todo: Allow config of tool attack speed
    //Todo: Add "MiningSpeedBonus" property to toolProperties declared for a Block
    //Todo: Make Hardness able to be predicated by tool type

    //Todo: Rigorously test new collection handling (with -'s) and "modid:*" support
    //Todo: Scrub for forEach() calls that could be replaced with enhanced for's
    //Todo: Implement ability to force certain tools to be required
    //Todo: Add defaultCanDrop to blocks and CanDrop to each tool type of blocks
    //Todo: Nbt system for upgrading tools
    //Todo: Nbt for players too
    //Todo: Config for modifying projectile properties

    //Todo: optimize SyncConfigPacket by creating Map<Properties, List<String>> first,
    //      then reversing it to reduce instances of Properties
    //Todo: JEI compat
    //Todo: Jade compat
    //Eventually make blocks store their destroy progress on config option

    public static final String MODID = "planar_cms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ArrayList<TagKey<Block>> TAG_KEYS_BY_TOOL_TYPE = new ArrayList<>();

    public PlanarCMS() {
        CollectionsConfig.parseAndPopulate();
        PacketHandler.register();
        GeneralConfig.parse();
        GeneralConfig.populate();
        ToolsConfig.parse();
        BlocksConfig.parse();
        EnchantsConfig.parse();
        resetTemplate(EnchantsConfig.TEMPLATE_FILE_NAME, EnchantsConfig.getCommentedTemplateConfigString());
        resetTemplate(BlocksConfig.TEMPLATE_FILE_NAME, BlocksConfig.getCommentedTemplateConfig());
        resetTemplate(ToolsConfig.TEMPLATE_FILE_NAME, ToolsConfig.getCommentedTemplateConfigString());
    }

    public static void populateTagKeys() {
        TAG_KEYS_BY_TOOL_TYPE.clear();
        for (String toolType : REGISTERED_TOOL_TYPES) {
            String tagKeyName = "mineable/" + toolType.toLowerCase();
            TAG_KEYS_BY_TOOL_TYPE.add(BlockTags.create(new ResourceLocation(tagKeyName)));
        }
    }

    public static void resetTemplate(String fileName, String contents) {
        try {
            Files.createDirectories(Path.of(fileName).getParent());
            FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath());
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            addConfigIssue(WARN, (byte) 5, "Exception during template replacement: {}", e.getMessage());
        }
    }
}
