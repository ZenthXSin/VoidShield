package voidshield.world.blocks.heat

import arc.Core
import arc.graphics.g2d.Draw
import arc.graphics.g2d.TextureRegion
import arc.math.geom.Geometry
import arc.util.Eachable
import arc.util.Log
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

    var rate = 0.9f

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
        return lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock is HeatCatheter
    }

    open inner class HeatCatheterBuild : HeatBuild() {
        var blendbits = 0
        var blendsclx = 0
        var blendscly = 0

        var linkBuild: MutableList<HeatBuild> = mutableListOf()

        override fun onProximityUpdate() {
            super.onProximityUpdate()

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

        fun transferHeat(build: HeatBuild?) {
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

            if (neighbor.block is HeatCrossover) return

            val selfBlock = block as HeatBlock
            val otherBlock = neighbor.block as? HeatBlock ?: return

            val diff = temperature - neighbor.temperature
            val selfHeat = selfBlock.specificHeat
            val otherHeat = otherBlock.specificHeat
            
            // 防护：避免除以零错误
            val heatSum = selfHeat + otherHeat
            if (heatSum <= 0) return
            
            val balance = (temperature * selfHeat + neighbor.temperature * otherHeat) / heatSum

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

        open fun heatExchange() {
            linkBuild.forEach {
                transferHeat(it)
            }
            temperature = min(temperature, maxTemperature)
        }

        override fun created() {
            super.created()
            updateLink()
        }

        override fun onNearbyBuildAdded(other: Building?) {
            super.onNearbyBuildAdded(other)
            updateLink()
        }

        override fun remove() {
            super.remove()
            linkBuild.clear()
        }

        fun updateLink() {
            linkBuild.clear()  // ✅ 修复：先清除旧链接
            getFront()?.let { linkBuild.add(it) }
            getBehind()?.let { linkBuild.add(it) }
        }

        override fun updateTile() {
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