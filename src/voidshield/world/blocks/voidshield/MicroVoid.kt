package voidshield.world.blocks.voidshield

import arc.graphics.Color
import voidshield.world.blocks.HeatBlock
import arc.util.Time
import mindustry.Vars
import mindustry.entities.EntityGroup
import mindustry.entities.Lightning
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Bullet
import mindustry.gen.Groups
import mindustry.graphics.Pal
import mindustry.ui.Bar
import mindustry.world.meta.Stat
import voidshield.other.VsVars
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.interfaces.SpaceDateInterface
import voidshield.world.blocks.HeatStat
import kotlin.math.roundToInt
import kotlin.random.Random


class MicroVoid(name: String) : HeatBlock(name) {

    var maxFissureCount: Int = 50

    var maxArea: Float = 50f

    var defaultHeat: Float = 5f//待机时升温速度

    init {
        updateClipRadius(Vars.world.width() * 8f)
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
            val hb = build as MicroVoidBuild
            Bar(
                { "功率：" + String.format("%.1f", hb.nowWattage * 100) + "%" },
                { Pal.accent },
                { hb.nowWattage }
            )
        }
    }

    open inner class MicroVoidBuild : HeatBuild(), SpaceDateInterface {

        override var spaces: MutableMap<Int, SpaceDate.FieldZone> = HashMap()

        var nowWattage: Float = 0f

        var practicalMaxArea: Float = maxArea * 64f

        override fun canActiveZone(id: Int): Boolean = efficiency > 0

        fun heatChange(): Float = when {
            nowWattage == 0f -> defaultHeat//待机时
            nowWattage <= 1 -> defaultHeat + nowWattage * 18f//工作时
            nowWattage > 1 -> defaultHeat + nowWattage * 36f//超载时
            else -> 0f
        } / specificHeat * Time.delta / 0.5f

        override fun draw() {
            super.draw()
            if (efficiency > 0) {
                
            }
        }

        override fun updateTile() {
            super.updateTile()
            drawZones()
            Groups.bullet.forEach {
                if (it.team != team) {
                    spaces.forEach { (_, zone) ->
                        if (zone.contains(it.x, it.y)) {
                            Lightning.create(
                                Team.sharded,
                                Color.white,
                                it.damage / 10,
                                it.x,
                                it.y,
                                it.rotation() + Random.nextInt(-90,90),
                                (it.damage / 10).toInt()
                            )
                            it.hit = true
                            it.remove()
                        }
                    }
                }
            }

        }

        // 扩展函数，方便复用
        inline fun EntityGroup<Bullet>.intersectCircle(
            centerX: Float,
            centerY: Float,
            radius: Float,
            consumer: (Bullet) -> Unit
        ) {
            val r2 = radius * radius
            // 先用矩形空间索引粗筛，再精确计算圆形
            intersect(centerX - radius, centerY - radius, radius * 2, radius * 2).forEach { bullet ->
                if (bullet.dst2(centerX, centerY) < r2) consumer(bullet)
            }
        }

        override var extraContent: MutableMap<Int, Any> = HashMap()

        override fun addExtraContent(content: Any) {
            TODO("Not yet implemented")
        }

        override fun useExtraContent(run: (content: Any) -> Unit) {
            TODO("Not yet implemented")
        }

        override fun canAddZone(): Boolean {
            return getAllAreas() < practicalMaxArea && maxFissureCount > spaces.size
        }

        override fun getMaxArea(): Float = practicalMaxArea - getAllAreas()

        override fun updateEffectValue(zone: SpaceDate.FieldZone): Float {
            TODO("Not yet implemented")
        }

        override fun shouldUpdateEffectValue(): Boolean = false

        override fun drawEffectStatus(zone: SpaceDate.FieldZone) {
//            if (zone !is SpaceDate.CircleZone) return
//            if (VsVars.shaders.spaceDistortion.meshMap.contains("[Zone]${zone.id}")) return
//            VsVars.shaders.spaceDistortion.addCircleRegion("[Zone]${zone.id}",zone.x,zone.y,zone.radius)
        }

        override fun shouldDrawEffectStatus(): Boolean = true

        fun addCircle(x: Float, y: Float, radius: Float, effect: Float, lifeTime: Float): SpaceDate.CircleZone? {
            val zone = addCircle(x, y, radius, effect) ?: return null
            VsVars.shaders.spaceDistortion.addCircleRegion("[Zone]${zone.id}",zone.x,zone.y,zone.radius * 5)
            VsVars.shaders.spaceDistortion.setLifeCycle("[Zone]${zone.id}",lifeTime * 40,0.5f,false)
            Time.run(lifeTime * 30) {
                removeZone(zone)
            }
            return zone
        }

        override fun remove() {
            super.remove()
            spaces.clear()
        }

        //只允许圆形场
        override fun addPolygon(vertices: FloatArray, effect: Float): SpaceDate.PolygonZone? = null
        override fun addRect(x: Float, y: Float, width: Float, height: Float, effect: Float): SpaceDate.PolygonZone? = null
    }
}