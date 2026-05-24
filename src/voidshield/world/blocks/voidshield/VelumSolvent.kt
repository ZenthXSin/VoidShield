package voidshield.world.blocks.voidshield

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.math.Interp
import arc.math.geom.Polygon
import arc.util.Time
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.graphics.Drawf
import mindustry.graphics.Pal
import voidshield.world.blocks.HeatBlock
import mindustry.world.meta.Stat
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.interfaces.SpaceDateInterface
import voidshield.world.blocks.HeatStat
import kotlin.math.*
import mindustry.ui.Bar
import mindustry.gen.Building
import mindustry.graphics.Layer

class VelumSolvent(name: String) : HeatBlock(name) {

    var maxFissureCount: Int = 100//最大裂隙数量

    var maxArea: Int = 200//最大立场面积

    var defaultHeat: Float = 5f//待机时升温速度

    init {
        update = true
        solid = true
        hasPower = true
        consumesPower = true
    }

    override fun setStats() {
        super.setStats()
        stats.add(Stat("最大裂隙数量", VoidShield.voidShield), "$maxFissureCount")
        stats.add(Stat("最大立场面积", VoidShield.voidShield), "${maxArea}²")
        stats.add(Stat("待机时", HeatStat.catHeat), "+${defaultHeat * (1f / specificHeat)}°C/tick")
        stats.add(
            Stat("工作时", HeatStat.catHeat),
            "+($defaultHeat + 功率 * 18) * (1 / ${specificHeat}) °C/tick"
        )
        stats.add(Stat("超载时", HeatStat.catHeat), "+($defaultHeat + 功率 * 36) * (1 / ${specificHeat}) °C/tick")
    }

    override fun setBars() {
        super.setBars()
        addBar("wattage") { build: Building ->
            val hb = build as VelumSolventBuild
            Bar(
                { "功率：" + String.format("%.1f", hb.nowWattage * 100) + "%" },
                { Pal.accent },
                { hb.nowWattage }
            )
        }
    }

    open inner class VelumSolventBuild : HeatBuild(), SpaceDateInterface {

        var fissureCount: Int = 0

        var area: Float = 0f

        var practicalMaxArea: Float = maxArea * 64f

        var nowWattage: Float = 0f

        val hitDuration: Float = 30f

        // zoneId → 剩余 tick;每个 zone 独立计时,避免命中态在同一 build 的多 zone 间泄漏
        val hitTimers: MutableMap<Int, Float> = mutableMapOf()

        init {
            updateClipRadius(Vars.world.width() * 8f)
        }

        override var extraContent: MutableMap<Int, Any> = HashMap()

        override fun addExtraContent(content: Any) {
            extraContent[extraContent.size] = content
        }

        override fun useExtraContent(run: (content: Any) -> Unit) {
            extraContent.values.forEach(run)
        }

        fun getWattage(): Float {
            val areaW = (area / practicalMaxArea) * 0.4f
            val fissureW = (fissureCount.toFloat() / maxFissureCount) * 0.6f
            return (areaW + fissureW) * efficiency
        }

        override fun remove() {
            super.remove()
            spaces.clear()
            hitTimers.clear()
            spacesAreaCache = 0f
            spacesAreaDirty = false
        }

        override fun canActiveZone(id: Int): Boolean = efficiency > 0

        fun getSpaceFissureCount(zone: SpaceDate.FieldZone): Int {
            if (zone.effectValue <= 5) return 0
            return ((zone.effectValue - 5) / getFissureLowEffect()).toInt()
        }

        fun getFissureLowEffect(): Float = 5f / maxFissureCount

        override var spaces: MutableMap<Int, SpaceDate.FieldZone> = mutableMapOf()

        override var spacesAreaCache: Float = 0f
        override var spacesAreaDirty: Boolean = false

        override fun updateTile() {
            updateSpaceDate()

            area = getAllAreas()

            //功率逻辑(单 tick 内 wattage 不变,算一次复用)
            val w = getWattage()
            nowWattage = if (w < nowWattage) {
                max(w, nowWattage - 0.5f * 0.01f)
            } else {
                min(w, nowWattage + 0.5f * 0.01f)
            }

            if (efficiency > 0) {
                //升温逻辑
                temperature += heatChange()
                super.updateTile()
            }
        }

        fun heatChange(): Float = when {
            nowWattage == 0f -> defaultHeat//待机时
            nowWattage <= 1 -> defaultHeat + nowWattage * 18f//工作时
            nowWattage > 1 -> defaultHeat + nowWattage * 36f//超载时
            else -> 0f
        } / specificHeat * Time.delta / 0.5f

        override fun draw() {
            super.draw()
            //可视化场
            update {
                drawEffectStatus(it)
            }
        }

        override fun canAddZone(): Boolean = area < practicalMaxArea

        override fun getMaxArea(): Float = practicalMaxArea - area

        override fun updateEffectValue(zone: SpaceDate.FieldZone): Float = zone.effectValue

        override fun shouldUpdateEffectValue(): Boolean = false

        override fun drawEffectStatus(zone: SpaceDate.FieldZone) {
            fun drawZone() {
                when (zone) {
                    is SpaceDate.CircleZone -> {
                        Fill.circle(zone.x, zone.y, zone.radius)
                    }

                    is SpaceDate.PolygonZone -> {
                        val vertices = zone.vertices
                        if (vertices.size >= 6) {
                            val polygon = Polygon()
                            polygon.vertices = vertices
                            Fill.poly(polygon)
                        }
                    }
                }
            }

            Draw.color(Pal.accent)
            Draw.z(Layer.shields + 5f)
            drawZone()

            val remaining = hitTimers[zone.id]
            if (remaining != null && remaining > 0f) {
                val fadeAlpha = Interp.fade.apply(remaining / hitDuration)

                Draw.color(Color.white)
                Draw.z(Layer.shields + 7f)
                Draw.alpha(fadeAlpha * 0.5f)
                drawZone()

                val next = remaining - Time.delta
                if (next <= 0f) hitTimers.remove(zone.id) else hitTimers[zone.id] = next
            }
        }

        fun triggerHitEffect(zoneId: Int) {
            hitTimers[zoneId] = hitDuration
        }

        override fun shouldDrawEffectStatus(): Boolean = true

        override fun write(w: Writes) {
            super.write(w)
            writeSpace(w)
        }

        override fun read(r: Reads, revision: Byte) {
            super.read(r, revision)
            readSpace(r, revision)
        }
    }

}
