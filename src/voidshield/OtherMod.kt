package voidshield

import arc.util.Log
import mindustry.Vars
import mindustry.mod.Mod
import voidshield.core.VsVars


class OtherMod : Mod() {

    override fun init() {
        super.init()

        Log.info("[OtherMod] Mod initialized")

    }

    override fun loadContent() {
        super.loadContent()

        load(Vars.mods.getMod(this::class.java).name)

        Log.info("[OtherMod] To be lib by @${VsVars.modName}")
    }

    companion object {
        fun load(name: String) {
            VsVars.modName = name
            VsVars.load()

            Log.info("[OtherMod] To be lib by @${VsVars.modName}")
        }
    }
}