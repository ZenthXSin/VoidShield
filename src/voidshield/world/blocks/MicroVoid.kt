package voidshield.world.blocks

import arc.graphics.Color
import voidshield.world.HeatBlock
import arc.util.Time
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.math.Rand
import arc.math.geom.Vec2
import arc.util.Log
import mindustry.Vars
import mindustry.entities.EntityGroup
import mindustry.entities.Lightning
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Bullet
import mindustry.gen.Groups
import mindustry.graphics.Drawf
import mindustry.graphics.Layer
import mindustry.graphics.Pal
import mindustry.graphics.Shaders
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.interfaces.SpaceDateInterface
import kotlin.random.Random


class MicroVoid(name: String) : HeatBlock(name) {

    var maxFissureCount: Int = 50

    var maxArea: Float = 50f

    init {
        updateClipRadius(Vars.world.width() * 8f)
    }

    open inner class MicroVoidBuild : HeatBuild(), SpaceDateInterface {

        override var spaces: MutableMap<Int, SpaceDate.FieldZone> = HashMap()


        var practicalMaxArea: Float = maxArea * 64f

        override fun canActiveZone(id: Int): Boolean = efficiency > 0

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
//            if (TestShader.meshMap.contains("[Zone]${zone.id}")) return
//            TestShader.addCircleRegion("[Zone]${zone.id}",zone.x,zone.y,zone.radius)
        }

        override fun shouldDrawEffectStatus(): Boolean = true

        fun addCircle(x: Float, y: Float, radius: Float, effect: Float, lifeTime: Float): SpaceDate.CircleZone? {
            val zone = addCircle(x, y, radius, effect) ?: return null
            Time.run(lifeTime * 60) {
                removeZone(zone.id)
//                TestShader.remove("[Zone]${zone.id}")
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