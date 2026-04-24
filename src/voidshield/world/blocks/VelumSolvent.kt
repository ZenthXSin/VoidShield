package voidshield.world.blocks

import arc.graphics.g2d.Draw
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars
import mindustry.graphics.Drawf
import mindustry.graphics.Pal
import voidshield.world.HeatBlock
import mindustry.world.meta.Stat
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.interfaces.SpaceDateInterface
import voidshield.world.HeatStat
import kotlin.math.*
import mindustry.ui.Bar
import mindustry.gen.Building

class VelumSolvent(name: String) : HeatBlock(name) {

    var maxFissureCount: Int = 100//最大裂隙数量

    var maxArea: Int = 200//最大立场面积

    var defaultHeat: Float = 2f//待机时升温速度

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
        stats.add(Stat("[blue]待机时", HeatStat.catHeat), "+${defaultHeat * (1f / specificHeat)}°C/tick")
        stats.add(
            Stat("[green]工作时", HeatStat.catHeat),
            "+($defaultHeat + 功率 * 18) * (1 / ${specificHeat}) °C/tick"
        )
        stats.add(Stat("[red]超载时", HeatStat.catHeat), "+($defaultHeat + 功率 * 36) * (1 / ${specificHeat}) °C/tick")
    }

    override fun setBars() {
        super.setBars()
        addBar("wattage") { build: Building ->
            val hb = build as VelumSolventBuild
            Bar(
                { "功率：" + hb.nowWattage * 100f + "%" },
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

        init {
            updateClipRadius(Vars.world.width() * 8f)
        }

        override var extraContent: MutableMap<Int, Any> = HashMap()

        override fun addExtraContent(content: Any) {
            TODO("Not yet implemented")
        }

        override fun useExtraContent(run: (content: Any) -> Unit) {
            TODO("Not yet implemented")
        }

        fun getWattage(): Float {
            val areaW = (area / practicalMaxArea) * 0.4f
            val fissureW = (fissureCount / maxFissureCount) * 0.6f
            return (areaW + fissureW) * efficiency
        }

        override fun remove() {
            super.remove()
            spaces.clear()
        }

        override fun canActiveZone(id: Int): Boolean = efficiency > 0

        fun getSpaceFissureCount(zone: SpaceDate.FieldZone): Int {
            if (zone.effectValue <= 5) return 0
            return ((zone.effectValue - 5) / getFissureLowEffect()).toInt()
        }

        fun getFissureLowEffect(): Float = 5f / maxFissureCount

        override var spaces: MutableMap<Int, SpaceDate.FieldZone> = mutableMapOf()

        override fun updateTile() {
            super.updateTile()

            updateSpaceDate()

            area = getAllAreas()

            //功率逻辑
            nowWattage = if (getWattage() < nowWattage) {
                max(getWattage(), nowWattage - 0.5f * 0.01f)
            } else {
                min(getWattage(), nowWattage + 0.5f * 0.01f)
            }

            if (efficiency > 0) {
//                if (getAllAreas() < 1) {
//                    Vars.player.unit() ?: return
//                    addCircle(Vars.player.unit().x, Vars.player.unit().y, 40f, 1f)
//                } else {
//                    Vars.player.unit() ?: return
//                    update {
//                        if (it !is SpaceDate.CircleZone) return@update
//                        VsVars.world.spaceDate.updateCirclePosition(it, Vars.player.unit().x, Vars.player.unit().y)
//                    }
//                }
                //升温逻辑
                if (nowWattage == 0f) {
                    temperature += defaultHeat//待机时
                } else if (nowWattage <= 1) {
                    temperature += defaultHeat + nowWattage * 18f//工作时
                } else if (nowWattage > 1) {
                    temperature += defaultHeat + nowWattage * 36f//超载时
                }
            }

        }

        override fun draw() {
            super.draw()
            //可视化场
            update {
                drawEffectStatus(it)
            }
        }

        override fun canAddZone(): Boolean = area < practicalMaxArea

        override fun getMaxArea(): Float = practicalMaxArea - area

        override fun updateEffectValue(zone: SpaceDate.FieldZone): Float {
            TODO("Not yet implemented")
        }

        override fun shouldUpdateEffectValue(): Boolean = false

        override fun drawEffectStatus(zone: SpaceDate.FieldZone) {
            if (zone !is SpaceDate.CircleZone) return
            Draw.color(Pal.accent)
            Drawf.circles(zone.x, zone.y, zone.radius)
        }

        override fun shouldDrawEffectStatus(): Boolean {
            TODO("Not yet implemented")
        }

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
