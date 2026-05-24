package voidshield.other.extends.other

import arc.util.Log
import mindustry.Vars
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable
import mindustry.ui.dialogs.SettingsMenuDialog.StringProcessor

/**
 * VoidShield 设置框架。
 *
 * 加新设置 3 步走:
 *   1. [buildPage] 里调 [addCheck] / [addSlider] 注册一个 widget
 *   2. [applyFromSettings] 里用 `Core.settings.getInt/getBool/getFloat(key("..."), <default>)` 把值写回对应 block 字段
 *   3. 两个 `bundle*.properties` 各加一对 `setting.voidshield-<name>.name = ...`
 *
 * 设置改动需要**重启游戏**才生效(因为 block 字段在初始化时 apply 一次)。
 */
class Settings {

    private val keyPrefix = "voidshield-"

    fun init() {
        try {
            applyFromSettings()
            if (!Vars.headless) registerUI()
            Log.info("[VoidShield] Settings initialized")
        } catch (e: Exception) {
            Log.err("[VoidShield] Settings init failed", e)
        }
    }

    /** 启动期把 `Core.settings` 中的存档值覆盖到 block 字段上。 */
    private fun applyFromSettings() {
        // 示例(后续按需补充):
        // VSBlocks.velumSolvent.maxFissureCount =
        //     Core.settings.getInt(key("velum-max-fissure"), 100)
    }

    /** 在原生设置弹窗里挂一个 "VoidShield" tab。 */
    private fun registerUI() {
        Vars.ui.settings.addCategory("voidshield") { table ->
            buildPage(table)
        }
    }

    private fun buildPage(table: SettingsTable) {
        // 示例(后续按需补充):
        // addCheck(table, "shader-enabled", true)
        // addSlider(table, "hit-duration", 30, 5, 120) { "$it tick" }
    }

    private fun key(name: String) = "$keyPrefix$name"

    fun addCheck(table: SettingsTable, name: String, default: Boolean) {
        table.checkPref(key(name), default)
    }

    fun addSlider(
        table: SettingsTable,
        name: String,
        default: Int,
        min: Int,
        max: Int,
        step: Int = 1,
        formatter: (Int) -> String = { it.toString() },
    ) {
        table.sliderPref(key(name), default, min, max, step, StringProcessor { formatter(it) })
    }
}
