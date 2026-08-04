package voidshield

import arc.util.Log
import mindustry.Vars
import mindustry.mod.Mod
import voidshield.content.VSBlocks
import voidshield.core.VsVars
import voidshield.core.extends.category.applyCategory
import voidshield.core.extends.logic.LTeleport
import voidshield.core.extends.logic.voidShield.VSControl
import voidshield.core.extends.logic.voidShield.VSSensor

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
        VSBlocks.load()

        Log.info("[Void Shield] Content loaded successfully")
    }
}
