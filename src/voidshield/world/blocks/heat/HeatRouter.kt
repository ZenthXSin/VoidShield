package voidshield.world.blocks.heat

import arc.math.geom.Geometry
import arc.math.geom.Point2
import arc.util.Tmp
import mindustry.Vars
import mindustry.gen.Building
import voidshield.world.blocks.HeatBlock
import kotlin.math.abs
import kotlin.math.min

class HeatRouter(name: String) : HeatBlock(name) {
    var rate = 0.9f

    init {
        rotate = false
    }

    open inner class HeatRouterBuild : HeatBuild() {
        var linkBuild: MutableList<HeatBuild> = mutableListOf()
        override fun updateTile() {
            super.updateTile()
            linkBuild.forEach {
                transferHeat(it)
            }
        }

        override fun draw() {
            super.draw()
            //TODO bottom和heat的贴图效果
        }
        fun transferHeat(build: HeatBuild?) {
            //TODO 修复帧率影响
            var neighbor = build ?: return
            if (neighbor === this) return

            if (neighbor.block is HeatCrossover) {
                val dx = neighbor.tileX() - tileX()
                val dy = neighbor.tileY() - tileY()

                neighbor = when {
                    dx == 1 && dy == 0 -> Vars.world.tile(neighbor.tileX() + 1, neighbor.tileY()).build as? HeatBuild
                    dx == -1 && dy == 0 -> Vars.world.tile(neighbor.tileX() - 1, neighbor.tileY()).build as? HeatBuild
                    dx == 0 && dy == 1 -> Vars.world.tile(neighbor.tileX(), neighbor.tileY() + 1).build as? HeatBuild
                    dx == 0 && dy == -1 -> Vars.world.tile(neighbor.tileX(), neighbor.tileY() - 1).build as? HeatBuild
                    else -> null
                } ?: return
            }

            if (neighbor.block is HeatRouter) return

            val selfBlock = block as HeatBlock
            val otherBlock = neighbor.block as? HeatBlock ?: return

            val diff = temperature - neighbor.temperature
            val selfHeat = selfBlock.specificHeat
            val otherHeat = otherBlock.specificHeat
            val balance = (temperature * selfHeat + neighbor.temperature * otherHeat) / (selfHeat + otherHeat)

            if (diff > 0f) {
                val maxTransfer = (temperature - balance) * selfHeat
                val transfer = min(abs(diff) * rate, maxTransfer)
                temperature -= transfer / selfHeat
                neighbor.temperature += transfer / otherHeat
            } else {
                val maxTransfer = (neighbor.temperature - balance) * otherHeat
                val transfer = min(abs(diff) * rate, maxTransfer)
                temperature += transfer / selfHeat
                neighbor.temperature -= transfer / otherHeat
            }

            temperature = temperature.coerceIn(20f, maxTemperature)
            neighbor.temperature = neighbor.temperature.coerceIn(20f, otherBlock.maxTemperature)
        }

        override fun created() {
            super.created()
            updateLink()
        }

        override fun onNearbyBuildAdded(other: Building?) {
            super.onNearbyBuildAdded(other)
            updateLink()
        }

        fun updateLink() {
            for (i in 0 until 4) {
                val off = Geometry.d4(i)
                val t = Vars.world.tile(tileX() + off.x, tileY() + off.y)
                if (t != null && t.build is HeatBuild) {
                    linkBuild.add(t.build as HeatBuild)
                }
            }
        }
    }
}