package voidshield.world.blocks.voidshield

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.TextureRegion
import arc.math.geom.Rect
import arc.struct.Seq
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
import voidshield.core.VsVars
import voidshield.core.dateTypes.SpaceDate
import voidshield.core.interfaces.SpaceDateInterface
import voidshield.world.blocks.HeatStat
import kotlin.random.Random


class MicroVoid(name: String) : HeatBlock(name) {

    var maxFissureCount: Int = 50

    var maxArea: Float = 50f

    var defaultHeat: Float = 5f//待机时升温速度

    var glowRegion: MutableMap<Int, TextureRegion> = HashMap()

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

    override fun load() {
        super.load()
        glowRegion[0] = Core.atlas.find(this.name + "-glow1")
        glowRegion[1] = Core.atlas.find(this.name + "-glow2")
    }

    open inner class MicroVoidBuild : HeatBuild(), SpaceDateInterface {

        override var spaces: MutableMap<Int, SpaceDate.FieldZone> = HashMap()

        override var spacesAreaCache: Float = 0f
        override var spacesAreaDirty: Boolean = false

        var nowWattage: Float = 0f

        var practicalMaxArea: Float = maxArea * 64f

        var smoothAlpha = 0f

        // 复用的 hitbox 缓存,避免每 tick/每 zone 分配 Rect
        private val rectBuf = Rect()

        override fun canActiveZone(id: Int): Boolean = efficiency > 0

        fun heatChange(): Float = when {
            nowWattage == 0f -> defaultHeat//待机时
            nowWattage <= 1 -> defaultHeat + nowWattage * 18f//工作时
            nowWattage > 1 -> defaultHeat + nowWattage * 36f//超载时
            else -> 0f
        } / specificHeat * Time.delta / 0.5f

        override fun updateTile() {
            super.updateTile()
            if (spaces.isEmpty()) return

            // 先按每个 zone 的 hitbox 用空间索引粗筛子弹,再做精确 contains
            // 用 Seq.contains(item, true) 做引用去重,避免同一颗子弹被多 zone 命中重复处理
            val hits = Seq<Bullet>()
            for ((_, zone) in spaces) {
                zone.hitbox(rectBuf)
                Groups.bullet.intersect(rectBuf.x, rectBuf.y, rectBuf.width, rectBuf.height).each { bullet ->
                    if (bullet.team == team) return@each
                    if (hits.contains(bullet, true)) return@each
                    if (!zone.contains(bullet.x, bullet.y)) return@each
                    hits.add(bullet)
                }
            }
            hits.each { b ->
                Lightning.create(
                    Team.sharded,
                    Color.white,
                    b.damage / 10,
                    b.x,
                    b.y,
                    b.rotation() + Random.nextInt(-90, 90),
                    (b.damage / 10).toInt()
                )
                b.hit = true
                b.remove()
            }
        }

        override fun draw() {
            super.draw()
            drawZones()
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
            extraContent[extraContent.size] = content
        }

        override fun useExtraContent(run: (content: Any) -> Unit) {
            extraContent.values.forEach(run)
        }

        override fun canAddZone(): Boolean {
            return getAllAreas() < practicalMaxArea && maxFissureCount > spaces.size
        }

        override fun getMaxArea(): Float = practicalMaxArea - getAllAreas()

        override fun updateEffectValue(zone: SpaceDate.FieldZone): Float = zone.effectValue

        override fun shouldUpdateEffectValue(): Boolean = false

        override fun drawEffectStatus(zone: SpaceDate.FieldZone) {
//            if (zone !is SpaceDate.CircleZone) return
//            if (VsVars.shaders.spaceDistortion.meshMap.contains("[Zone]${zone.id}")) return
//            VsVars.shaders.spaceDistortion.addCircleRegion("[Zone]${zone.id}",zone.x,zone.y,zone.radius)
        }

        override fun shouldDrawEffectStatus(): Boolean = true

        fun addCircle(x: Float, y: Float, radius: Float, effect: Float, lifeTime: Float): SpaceDate.CircleZone? {
            val zone = addCircle(x, y, radius, effect) ?: return null
            VsVars.v1Shaders.spaceDistortion.addCircleRegion("[Zone]${zone.id}",zone.x,zone.y,zone.radius * 5)
            VsVars.v1Shaders.spaceDistortion.setLifeCycle("[Zone]${zone.id}",lifeTime * 40,0.5f,false)
            Time.run(lifeTime * 30) {
                removeZone(zone)
            }
            return zone
        }

        override fun remove() {
            super.remove()
            spaces.clear()
            spacesAreaCache = 0f
            spacesAreaDirty = false
        }

        //只允许圆形场
        override fun addPolygon(vertices: FloatArray, effect: Float): SpaceDate.PolygonZone? = null
        override fun addRect(x: Float, y: Float, width: Float, height: Float, effect: Float): SpaceDate.PolygonZone? = null
    }
}