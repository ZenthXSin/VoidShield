package voidshield.world.blocks

import mindustry.world.blocks.production.GenericCrafter
import voidshield.world.HeatBlock
import mindustry.world.meta.Stat
import mindustry.world.meta.StatCat
import mindustry.world.meta.StatUnit

class CorVacuum(name: String) : HeatBlock(name) {

    init {
        update = true
        solid = true
        hasPower = true
        consumesPower = true
    }

    open inner class CorVacuumBuild : HeatBuild()
}

object VoidShield {
    val voidShield = StatCat("VoidShield")
}
