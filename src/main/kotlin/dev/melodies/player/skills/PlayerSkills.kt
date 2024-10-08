package dev.melodies.player.skills

import dev.melodies.lostitems.PickaxeGrantListener
import dev.melodies.lostprison.LostPrison
import dev.melodies.player.ActionBarManager
import dev.melodies.utils.TitleDisplayManager
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class PlayerSkills(private val plugin: LostPrison) : Listener {

    private val blocks: Map<Material, Double> = plugin.config.getConfigurationSection("block-xp")?.let { section ->
        section.getKeys(false).mapNotNull { key ->
            val material = Material.getMaterial(key) ?: return@mapNotNull null
            val xp = section.getDouble(key)
            material to xp
        }.toMap()
    } ?: emptyMap()

    private val actionBarManager = ActionBarManager(plugin)

    @EventHandler
    private fun miningSkill(event: BlockBreakEvent) {
        if (!Tag.MINEABLE_PICKAXE.isTagged(event.block.type)) return
        if (event.player.inventory.itemInMainHand.itemMeta?.persistentDataContainer?.has(PickaxeGrantListener.KEY) == false) return

        val xp = blocks[event.block.type] ?: 1.0
        val data = plugin.playerSkillDataStorage.getSkillData(event.player.uniqueId, SkillType.MINING)

        var newXP = data.xp + xp
        var newLevel = data.level

        val xpToLevelUp = plugin.config.getDouble("level-xp.${data.level}", 1000.0)
        if (newXP >= xpToLevelUp) {
            newLevel++
            newXP -= xpToLevelUp

            TitleDisplayManager.handleLevelUp(event.player, data.level, newLevel)
        }

        plugin.playerSkillDataStorage.setPlayerStats(event.player.uniqueId, SkillType.MINING, newLevel, newXP)
        actionBarManager.displaySkillBar(event.player, newXP, xpToLevelUp, xp)
    }
}