<p style="text-align: center">
    <a href="https://discord.gg/RrY3rXuAH5" target="_blank" style="text-decoration:none">
        <img src="https://img.shields.io/badge/Discord-5865F2?logo=discord&logoColor=white&style=for-the-badge" alt="Planar Unknown Discord">
    </a>
    <a href="https://ko-fi.com/planarunknown" target="_blank" style="text-decoration:none">
        <img src="https://img.shields.io/badge/Ko--fi-F16061?logo=ko-fi&logoColor=white&style=for-the-badge" alt="Planar Unknown Ko-fi">
    </a>
    <a href="https://www.youtube.com/@silentstranger49" target="_blank" style="text-decoration:none">
        <img src="https://img.shields.io/badge/Youtube-F02222?logo=youtube&logoColor=white&style=for-the-badge" alt="Silent Stranger's Youtube">
    </a>
    <a href="https://www.curseforge.com/members/planarunknown/projects">
        <img src="https://img.shields.io/badge/CurseForge-F46537?logo=curseforge&logoColor=white&style=for-the-badge" alt="Planar Unknown CurseForge">
    </a>
</p>

## This mod is in **beta**!
Please report any bugs by creating an issue here or visiting our Discord

## What is Configurable Mining System?
CMS is a utility mod that lets you fine-tune mining progression by customizing tools, blocks, and enchantments through simple TOML config files
## Who is this mod for?
This mod is for you if:
- You are a modpack creator looking for granular control over mining progression
- You want to tweak a few blocks or tools from your favorite mods
- You are a nerd who loves tinkering with game mechanics (Like us!)

## What can I actually do with this mod?
There are 3 categories of configuration this mod provides: Tools, Blocks, and Enchants. You may use Tags, BlockFamilies, or custom Collections to easily modify large groups of things at once. The mod auto-generates example templates with detailed comments to guide you through.
### Tools
You can declare Tool Types, Power levels, and Mining speed for any item in this simple format.
```yaml
"minecraft:stick" = {Pickaxe = 40, Shovel = 20, MiningSpeed = 9}
```

With this, sticks would be able to mine anything that a vanilla Stone Pickaxe or Wooden Shovel could, with the mining speed of netherite tools
### Blocks
For blocks, you can get a little more in depth with properties such as: Hardness, ExplosionResistance, DefaultResistance, Tool Type specific Resistances, and whether tools apply mining speed.
```yaml
["minecraft:packed_mud"]
  Hardness = 1.0
  DefaultResistance = -1
  Shovel = {Resistance = 40, ApplyMiningSpeed = false}
  Pickaxe = {Resistance = 20, ApplyMiningSpeed = true}
```

With this, Packed Mud would take twice as long to mine as Dirt, and it can only be mined slowly by an item with at least 40 Shovel Power or quickly by an item with at least 20 Pickaxe Power. Due to the DefaultResistance of -1 it is completely unmineable by anything else.
### Enchants
You may also configure what enchantments can or cannot go on any item.
```yaml
"Arcane.50" = [
  "minecraft:fortune",
  "-minecraft:fire_aspect"
]

  "minecraft:stick" = [
  "minecraft:looting"
]
```
With this, items with Arcane power 50 or higher can have Fortune but cannot have Fire Aspect, even if they normally could. And sticks can have looting!

## Coming Soon:
- A Wiki containing detailed guides and examples
- NBT based system for greater control
- Compatibility with other utilities like Jade and JEI

### If you need help or have any feedback, come join the <a href="https://discord.gg/RrY3rXuAH5" style="color: #0099FF">Planar Unknown Discord</a>