package voidshield.world.blocks.heat

import arc.Core
import arc.graphics.g2d.Draw
import arc.math.geom.Geometry
import arc.math.geom.Point2
import arc.util.Time
import arc.util.Tmp
import mindustry.Vars
import mindustry.gen.Building
import mindustry.graphics.Drawf
import mindustry.graphics.Layer
import voidshield.world.blocks.HeatBlock
import java.awt.Color
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
            //super.draw()
            Draw.z(Layer.shields + 5f)
            Draw.rect(Core.atlas.white(),x,y, 80f, 80f)
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