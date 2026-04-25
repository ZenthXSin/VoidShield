package voidshield.world.blocks

import arc.graphics.g2d.Draw
import mindustry.world.meta.Stat
import mindustry.world.meta.StatCat
import voidshield.world.HeatBlock
import voidshield.world.HeatStat
import arc.util.Log
import mindustry.graphics.Drawf
import mindustry.graphics.Layer
import voidshield.shader.v2.DefaultShader

/**
 * 热力生产方块 - 基础模板
 */
class HeaterBlock(name: String) : HeatBlock(name) {

    /** 加热速率 (摄氏度/tick) - 支持负数（降温模式） */
    var heatingRate: Float = 5f

    init {
        update = true
        solid = true
    }

    override fun setStats() {
        super.setStats()
        stats.add(Stat("rate-heating", HeatStat.catHeat), "${heatingRate / specificHeat}°C/tick")
    }

    open inner class HeaterBuild : HeatBuild() {
        override fun draw() {
            super.draw()
            Draw.draw(Layer.effect) {
                Draw.flush()
                Draw.shader(DefaultShader)
                Drawf.circles(x,y,400f)
                Draw.flush()
                Draw.shader()
            }
        }
        override fun updateTile() {
            super.updateTile()
            if (heatingRate > 0f) {
                if (efficiency > 0f && temperature < maxTemperature * overheatThreshold) {
                    temperature += heatingRate / specificHeat
                }
            } else {
                if (efficiency > 0f && temperature > 20f) {
                    temperature += heatingRate / specificHeat
                }
            }
        }
    }
}
