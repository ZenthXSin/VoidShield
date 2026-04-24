package voidshield.other.extends.other

import arc.Core
import arc.scene.ui.layout.Table
import arc.util.Log
import mindustry.Vars
import mindustry.ui.dialogs.SettingsMenuDialog

class Settings {
    fun init() {
        // 延迟到游戏完全加载后注入
            try {
                injectGraphicsSettings()
                Log.info("[VoidShield] Settings injected successfully")
            } catch (e: Exception) {
                Log.err("[VoidShield] Failed to inject settings: $e")
            }
    }
    private fun injectGraphicsSettings() {
        //Core.scene.root.children.get(0).children.get(0).children.get(2).children.get(0).table
    }
}