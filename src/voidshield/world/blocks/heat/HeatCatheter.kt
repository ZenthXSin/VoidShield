package voidshield.world.blocks.heat

import arc.Core
import arc.graphics.g2d.Draw
import arc.graphics.g2d.TextureRegion
import arc.math.geom.Geometry
import arc.util.Eachable
import arc.util.Log
import arc.util.Time
import mindustry.Vars
import mindustry.entities.units.BuildPlan
import mindustry.gen.Building
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.Autotiler
import mindustry.world.meta.BlockGroup
import voidshield.world.blocks.HeatBlock
import kotlin.math.abs
import kotlin.math.min

open class HeatCatheter(name: String) : HeatBlock(name), Autotiler {
    lateinit var regions: Array<TextureRegion>

    init {
        rotate = true
        update = true
        group = BlockGroup.logic // 随便放个组
        specificHeat = 1f
    }

    override fun load() {
        super.load()
        regions = Array(7) { i ->
            val region = Core.atlas.find("${name}-top-$i")
            if (region == Core.atlas.find("error")) {
                Log.warn("Missing region: ${name}-top-$i")
            }
            region
        }
    }

    override fun icons(): Array<TextureRegion> {
        return arrayOf(regions[0])
    }

    override fun drawPlanRegion(plan: BuildPlan, list: Eachable<BuildPlan>) {
        val bits = getTiling(plan, list) ?: return

        val region: TextureRegion = regions[bits[0]]
        Draw.rect(
            region,
            plan.drawx(),
            plan.drawy(),
            region.width * bits[1] * region.scl(),
            region.height * bits[2] * region.scl(),
            (plan.rotation * 90).toFloat()
        )
    }


    override fun blends(
        tile: Tile?, rotation: Int, otherx: Int, othery: Int, otherrot: Int, otherblock: Block
    ): Boolean {
        return otherblock is HeatCatheter && (lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock) ||
                (rotation + 2) % 4 == otherrot)
    }

    open inner class HeatCatheterBuild : HeatBuild() {
        var blendbits = 0
        var blendsclx = 0
        var blendscly = 0

        var linkBuild: MutableList<HeatBuild> = mutableListOf()
        var isUpdate = false

        override fun onProximityUpdate() {
            super.onProximityUpdate()
            updateLink()

            val bits = buildBlending(tile, rotation, null, true)
            blendbits = bits[0]
            blendsclx = bits[1]
            blendscly = bits[2]
        }

        override fun draw() {
            //TODO bottom和heat的贴图效果
            val region = regions[blendbits]
            Draw.rect(
                region,
                x,
                y,
                region.width * blendsclx * region.scl(),
                region.height * blendscly * region.scl(),
                rotation * 90f
            )
        }

        open fun heatExchange() {
            linkBuild.forEach {
                transferHeat(it)
            }
            temperature = min(temperature, maxTemperature)
        }

        override fun created() {
            super.created()
            Time.run(50f) {
                updateLink()
            }
        }

        fun updateLink() {
            linkBuild.add(getFront() ?: return)
            linkBuild.add(getBehind() ?: return)
        }

        override fun updateTile() {
            if (!isUpdate) {
                isUpdate = true
                updateLink()
            }
            if (efficiency > 0) {
                heatExchange()
            }
            super.updateTile()
        }

        fun getFront(): HeatBuild? {
            return when (rotation) {
                1 -> Vars.world.tile(tile.x.toInt(), tile.y - block.size)?.build as? HeatBuild
                2 -> Vars.world.tile(tile.x - block.size, tile.y.toInt())?.build as? HeatBuild
                3 -> Vars.world.tile(tile.x.toInt(), tile.y + block.size)?.build as? HeatBuild
                0 -> Vars.world.tile(tile.x + block.size, tile.y.toInt())?.build as? HeatBuild
                else -> null
            }
        }

        fun getBehind(): HeatBuild? {
            return when (rotation) {
                3 -> Vars.world.tile(tile.x.toInt(), tile.y - block.size)?.build as? HeatBuild
                0 -> Vars.world.tile(tile.x - block.size, tile.y.toInt())?.build as? HeatBuild
                1 -> Vars.world.tile(tile.x.toInt(), tile.y + block.size)?.build as? HeatBuild
                2 -> Vars.world.tile(tile.x + block.size, tile.y.toInt())?.build as? HeatBuild
                else -> null
            }
        }
    }
}