package voidshield.world.blocks.heat

import arc.math.geom.Geometry
import arc.math.geom.Point2
import arc.util.Time
import arc.util.Tmp
import mindustry.Vars
import mindustry.gen.Building
import voidshield.world.blocks.HeatBlock
import kotlin.math.abs
import kotlin.math.min

class HeatRouter(name: String) : HeatBlock(name) {
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