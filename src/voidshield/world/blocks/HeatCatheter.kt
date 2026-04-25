package voidshield.world.blocks

import mindustry.Vars
import mindustry.gen.Building
import mindustry.world.Tiles
import voidshield.world.HeatBlock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class HeatCatheter(nane: String): HeatBlock(nane) {
    /** 每tick传导的热量（比热容不参与计算）*/
    var rate = 0.9f

    init {
        rotate = true
        specificHeat = 1f
    }

    open inner class HeatCatheterBuild: HeatBuild() {

        fun transferHeat(neighbor: HeatBuild?) {
            neighbor ?: return

            val selfBlock = block as HeatBlock
            val otherBlock = neighbor.block as? HeatBlock ?: return

            val diff = temperature - neighbor.temperature
            //if (abs(diff) < 0.0000001f) return

            val selfHeat = selfBlock.specificHeat
            val otherHeat = otherBlock.specificHeat

            // 两者最终不会超过的平衡温度
            val balance = (temperature * selfHeat + neighbor.temperature * otherHeat) / (selfHeat + otherHeat)

            if (diff > 0f) {
                // 自身更热，热量流向 neighbor

                // 到达平衡最多能传多少热量，防止过冲
                val maxTransfer = (temperature - balance) * selfHeat

                // rate 越大，导热越快；温差越大，传热越快
                val transfer = min(abs(diff) * rate, maxTransfer)

                temperature -= transfer / selfHeat
                neighbor.temperature += transfer / otherHeat
            } else {
                // neighbor 更热，热量流向自身

                val maxTransfer = (neighbor.temperature - balance) * otherHeat
                val transfer = min(abs(diff) * rate, maxTransfer)

                temperature += transfer / selfHeat
                neighbor.temperature -= transfer / otherHeat
            }

            temperature = temperature.coerceIn(20f, maxTemperature)
            neighbor.temperature = neighbor.temperature.coerceIn(20f, otherBlock.maxTemperature)
        }

        fun heatExchange() {
            transferHeat(getFront())
            transferHeat(getBehind())
            temperature = min(temperature, maxTemperature)
        }

        override fun updateTile() {
            if (efficiency > 0) {
                heatExchange()
            }
            super.updateTile()
        }

        fun getFront(): HeatBuild? {
         when (rotation) {
             1 -> return Vars.world.tile(tile.x.toInt(),tile.y - block.size).build as? HeatBuild
             2 -> return Vars.world.tile(tile.x - block.size,tile.y.toInt()).build as? HeatBuild
             3 -> return Vars.world.tile(tile.x.toInt(),tile.y + block.size).build as? HeatBuild
             0 -> return Vars.world.tile(tile.x + block.size,tile.y.toInt()).build as? HeatBuild
         }
            return null
        }

        fun getBehind(): HeatBuild? {
            when (rotation) {
                3 -> return Vars.world.tile(tile.x.toInt(),tile.y - block.size).build as? HeatBuild
                0 -> return Vars.world.tile(tile.x - block.size,tile.y.toInt()).build as? HeatBuild
                1 -> return Vars.world.tile(tile.x.toInt(),tile.y + block.size).build as? HeatBuild
                2 -> return Vars.world.tile(tile.x + block.size,tile.y.toInt()).build as? HeatBuild
            }
            return null
        }
    }
}