package voidshield.world.blocks.voidshield

import voidshield.world.blocks.HeatBlock
import mindustry.world.meta.StatCat

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
