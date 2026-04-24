package voidshield

import arc.util.Log
import mindustry.Vars
import mindustry.mod.Mod
import voidshield.content.VSBlocks
import voidshield.other.VsVars
import voidshield.other.extends.categoryExtend.applyCategory
import voidshield.other.extends.logicExtend.LTeleport
import voidshield.other.extends.logicExtend.voidShield.VSControl
import voidshield.other.extends.logicExtend.voidShield.VSSensor

class VoidShieldMod : Mod() {

    override fun init() {
        super.init()
        VsVars.load()
        VsVars.modName = Vars.mods.getMod(this::class.java).name
        LTeleport.TeleportStatement.create()
        VSSensor.create()
        VSControl.create()
        applyCategory(VSBlocks.heat)
        applyCategory(VSBlocks.voidShield)
        Log.info("[Void Shield] Mod initialized")
    }

    override fun loadContent() {
        // 加载方块
        VSBlocks.load()

        Log.info("[Void Shield] Content loaded successfully")
    }
}
