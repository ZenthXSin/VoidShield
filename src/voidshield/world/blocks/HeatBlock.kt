package voidshield.world.blocks

import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import arc.math.Mathf
import arc.struct.Seq
import arc.util.Eachable
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.entities.units.BuildPlan
import mindustry.gen.Building
import mindustry.graphics.Pal
import mindustry.ui.Bar
import mindustry.world.Block
import mindustry.world.draw.DrawBlock
import mindustry.world.draw.DrawDefault
import mindustry.world.meta.Stat
import mindustry.world.meta.StatCat
import mindustry.world.meta.StatUnit
import voidshield.other.VsVars
import voidshield.other.interfaces.BindBuilding
import voidshield.other.interfaces.BlockShader
import kotlin.math.*

/**
 * 热力方块 - 简化的热力学系统
 * 温度单位：摄氏度 (°C)
 */
open class HeatBlock(name: String) : Block(name) {

    /** 最高温度 (摄氏度) */
    var maxTemperature: Float = 2000f

    /** 比热容 - 升温速度 = 1 / specificHeat, 降温速度 = 0.1f / specificHeat  */
    var specificHeat: Float = 10f

    /** 过热时每帧受到的伤害 */
    var overheatDamage: Float = 0.5f

    /** 过热阈值 (最高温度的百分比) */
    var overheatThreshold: Float = 0.9f

    /** 绘制器 - 用于自定义方块外观 */
    var drawer: DrawBlock = DrawDefault()

    var warmupSpeed = 0.019f

    var legacyReadWarmup = false

    init {
        update = true
        solid = true
        hasPower = true
        consumesPower = true
    }

    override fun setStats() {
        super.setStats()

        // 最高温度
        stats.add(HeatStat.maxTemp, maxTemperature, StatUnit.degrees)

        // 比热容
        stats.add(HeatStat.specificHeatStat, specificHeat, StatUnit.none)

        // 过热阈值
        val overheatTemp = (maxTemperature * overheatThreshold).toInt()
        stats.add(
            HeatStat.overheatThresholdStat, overheatTemp.toString() + "°C (" + (overheatThreshold * 100).toInt() + "%)"
        )

        // 过热伤害
        stats.add(HeatStat.overheatDamageStat, "$overheatDamage/tick")
    }

    override fun setBars() {
        super.setBars()
        addBar("temperature") { build: Building ->
            val hb = build as HeatBuild
            Bar({ "温度：" + hb.temperature.toInt() + "°C" }, { getTemperatureColor(hb) }, { hb.temperaturePercent })
        }
    }

    override fun load() {
        super.load()
        drawer.load(this)
    }

    override fun drawPlanRegion(plan: BuildPlan, list: Eachable<BuildPlan>) {
        drawer.drawPlan(this, plan, list)
    }

    override fun icons(): Array<TextureRegion> {
        return drawer.finalIcons(this)
    }

    override fun getRegionsToOutline(out: Seq<TextureRegion>) {
        drawer.getRegionsToOutline(this, out)
    }

    /** 根据温度返回颜色 */
    fun getTemperatureColor(hb: HeatBuild): Color {
        return when {
            hb.temperaturePercent < 0.25f -> Color.valueOf("4488ff")
            hb.temperaturePercent < 0.5f -> Color.valueOf("88ff88")
            hb.temperaturePercent < 0.75f -> Color.valueOf("ffaa00")
            else -> Pal.lightOrange
        }
    }

    open inner class HeatBuild : BindBuilding(), BlockShader {

        /** 当前温度 (摄氏度) */
        var temperature: Float = 20f

        /** 目标温度 (摄氏度) */
        var targetTemperature: Float = 20f

        var totalProgress = 0f

        var warmup = 0f

        val hb = this@HeatBlock

        override fun totalProgress(): Float = totalProgress

        override fun warmup(): Float = warmup

        fun warmupTarget(): Float = 1f

        override fun updateTile() {
            super.updateTile()

            temperature = min(temperature, maxTemperature)

            warmup = if (efficiency > 0f) {
                Mathf.approachDelta(warmup, warmupTarget(), hb.warmupSpeed)
            } else {
                Mathf.approachDelta(warmup, 0.0f, hb.warmupSpeed)
            }

            totalProgress += warmup * Time.delta

            // 基础降温逻辑：当温度高于环境温度 (20°C) 时，以 0.5℃/tick 的速率降温
            if (temperature > 20f) {
                temperature -= 0.1f / specificHeat
                if (temperature < 20f) temperature = 20f
            }

            // 温度趋近目标温度（加热逻辑）
            if (temperature < targetTemperature) {
                temperature = Mathf.approachDelta(temperature, targetTemperature, 1 / hb.specificHeat)
            }

            // 过热处理 - 超出阈值 5% 才开始受伤
            if (isOverheating()) {
                damage(hb.overheatDamage * ((temperature - hb.maxTemperature * hb.overheatThreshold) / hb.maxTemperature * hb.overheatThreshold))
            }

            setGradient()
        }

        override fun remove() {
            super.remove()
            dispose()
        }

        override fun write(w: Writes) {
            super.write(w)
            w.f(temperature)
            w.f(targetTemperature)
            w.f(this.warmup)
            if (hb.legacyReadWarmup) {
                w.f(0.0f)
            }
        }

        override fun read(r: Reads, revision: Byte) {
            super.read(r, revision)
            temperature = r.f()
            targetTemperature = r.f()
            warmup = r.f()
            if (hb.legacyReadWarmup) {
                r.f()
            }
        }

        override fun draw() {
            drawer.draw(this)
        }

        override fun drawLight() {
            super.drawLight()
            drawer.drawLight(this)
        }

        /**
         * 获取温度百分比 (0-1)
         */
        val temperaturePercent: Float
            get() {
                return temperature / hb.maxTemperature
            }

        /**
         * 获取当前温度 (摄氏度)
         */
        fun getCurrentTemperature(): Float = temperature

        /**
         * 是否处于过热状态
         */
        fun isOverheating(): Boolean {
            return temperature > hb.maxTemperature * hb.overheatThreshold * 1.05f
        }

        override var hasShader: Boolean = false
        override fun created() {
            super.created()
            setShader()
        }
        override fun getMeshId(): String = "[${this.block.name}][$x][$y]"
        override fun setGradient() {
            VsVars.shaders.heatShader.setMeshAlpha(getMeshId(),temperaturePercent)
        }
        override fun setShader() {
            VsVars.shaders.heatShader.addCircleRegion(getMeshId(), x, y, size * 8f)
        }

        override fun dispose() {
            VsVars.shaders.heatShader.remove(getMeshId())
        }
    }
}

object HeatStat {
    // 热量统计分类
    val catHeat = StatCat("heat")

    // 统计项
    val maxTemp = Stat("max-temperature", catHeat)
    val specificHeatStat = Stat("specific-heat", catHeat)
    val overheatThresholdStat = Stat("overheat-threshold", catHeat)
    val overheatDamageStat = Stat("overheat-damage", catHeat)
}
