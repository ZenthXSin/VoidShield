package voidshield.world.blocks.heat

import arc.func.Floatp
import arc.func.Func
import arc.graphics.Color
import mindustry.gen.Building
import mindustry.graphics.Pal
import mindustry.ui.Bar
import voidshield.world.blocks.HeatBlock

class HeatCrossover(name: String) : HeatBlock(name) {
    override fun setBars() {
        addBar(
            "health"
        ) { entity: Building? ->
            Bar(
                "stat.health",
                Pal.health
            ) { entity!!.healthf() }.blink(Color.white)
        }
    }
}