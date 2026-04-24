package voidshield

import arc.Events
import arc.util.Log
import voidshield.other.extends.logicExtend.LTeleport
import mindustry.Vars
import mindustry.game.EventType
import mindustry.mod.Mod
import voidshield.other.VsVars
import voidshield.world.effects.effect.TeleportEffect.loadJson


class OtherMod : Mod() {

    override fun init() {
        super.init()

        Log.info("[OtherMod] Mod initialized")

    }

    override fun loadContent() {
        super.loadContent()

        VsVars.load()
        VsVars.modName = Vars.mods.getMod(this::class.java).name

        LTeleport.TeleportStatement.create()

        //UnitTypes.beta.abilities.add(TeleportAbility())

    }
}