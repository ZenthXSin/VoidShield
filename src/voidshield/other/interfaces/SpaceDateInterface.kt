package voidshield.other.interfaces

import arc.math.Mathf
import arc.math.geom.Vec2
import arc.util.Log
import arc.util.io.Reads
import arc.util.io.Writes
import voidshield.other.VsVars
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.dateTypes.SpaceDate.PolygonZone
import java.io.IOException
import java.lang.reflect.Field

interface SpaceDateInterface {
    var spaces: MutableMap<Int, SpaceDate.FieldZone>

    var extraContent: MutableMap<Int, Any>

    fun clear() {
        spaces.forEach { (_, i) ->
            removeZone(i)
        }
    }

    fun update(run: (zone: SpaceDate.FieldZone) -> Unit) {
        spaces.forEach { (_, i) ->
            run(i)
        }
    }

    fun addExtraContent(content: Any)

    fun useExtraContent(run: (content: Any) -> Unit)

    fun canAddZone(): Boolean

    /** <0 时无限制 */
    fun getMaxArea(): Float

    fun updateEffectValue(zone: SpaceDate.FieldZone): Float

    fun shouldUpdateEffectValue(): Boolean

    fun drawEffectStatus(zone: SpaceDate.FieldZone)

    fun shouldDrawEffectStatus(): Boolean

    fun canActiveZone(id: Int): Boolean

    fun updateSpaceDate() {
        if (spaces.isEmpty()) return
        spaces.forEach { (id, i) ->
            if (shouldUpdateEffectValue()) updateEffectValue(i)
            if (canActiveZone(id)) VsVars.world.spaceDate.setZoneActive(id, true) else VsVars.world.spaceDate.setZoneActive(id, false)
        }
    }

    fun drawZones() {
        if (shouldDrawEffectStatus()) {
            spaces.forEach { (_, i) ->
                drawEffectStatus(i)
            }
        }
    }

    /**
     * 获取场内的随机一点
     * @param zone 场区域
     * @return 随机点坐标，如果区域无效返回 null
     */
    fun getRandomPoint(zone: SpaceDate.FieldZone): Vec2? {
        return when (zone) {
            is SpaceDate.CircleZone -> getRandomPointInCircle(zone)
            is PolygonZone -> getRandomPointInPolygon(zone)
            else -> null
        }
    }


    /**
     * 获取场内多个均匀分配的随机点
     * @param zone 场区域
     * @param count 点的数量
     * @param minDistance 点之间的最小距离（可选，用于更均匀分布）
     * @return 随机点列表，实际返回数量可能少于 count（当区域太小无法容纳时）
     */
    fun getRandomPoints(zone: SpaceDate.FieldZone, count: Int, minDistance: Float = 0f): List<Vec2> {
        val result = mutableListOf<Vec2>()

        when (zone) {
            is SpaceDate.CircleZone -> getRandomPointsInCircle(result, zone, count, minDistance)
            is PolygonZone -> getRandomPointsInPolygon(result, zone, count, minDistance)
            else -> {}
        }

        return result
    }

    /**
     * 在圆形场内获取随机点
     */
    private fun getRandomPointInCircle(zone: SpaceDate.CircleZone): Vec2 {
        val angle = Mathf.random() * Mathf.PI * 2f
        val radius = zone.radius * Mathf.sqrt(Mathf.random())
        val x = zone.x + radius * Mathf.cos(angle)
        val y = zone.y + radius * Mathf.sin(angle)
        return Vec2(x, y)
    }

    /**
     * 在多边形场内获取随机点（使用拒绝采样）
     * @param maxAttempts 最大尝试次数，防止凹多边形导致死循环
     */
    private fun getRandomPointInPolygon(zone: PolygonZone, maxAttempts: Int = 100): Vec2 {
        val hb = zone.tmpHitbox
        zone.hitbox(hb)

        for (i in 0 until maxAttempts) {
            val x = hb.x + Mathf.random() * hb.width
            val y = hb.y + Mathf.random() * hb.height
            if (zone.contains(x, y)) {
                return Vec2(x, y)
            }
        }
        // 尝试多次失败后，返回多边形中心作为备选
        return zone.center()
    }

    /**
     * 在圆形场内获取多个均匀分配的随机点
     */
    private fun getRandomPointsInCircle(
        result: MutableList<Vec2>,
        zone: SpaceDate.CircleZone,
        count: Int,
        minDistance: Float
    ) {
        if (minDistance <= 0) {
            // 无需距离检查，直接生成
            for (i in 0 until count) {
                result.add(getRandomPointInCircle(zone))
            }
        } else {
            // 需要距离检查，使用拒绝采样
            val maxAttempts = count * 50  // 每个点最多尝试 50 次
            var attempts = 0

            while (result.size < count && attempts < maxAttempts) {
                val newPoint = getRandomPointInCircle(zone)

                // 检查与已有点的距离
                if (result.all { it.dst(newPoint) >= minDistance }) {
                    result.add(newPoint)
                }
                attempts++
            }
        }
    }

    /**
     * 在多边形场内获取多个均匀分配的随机点
     */
    private fun getRandomPointsInPolygon(
        result: MutableList<Vec2>,
        zone: PolygonZone,
        count: Int,
        minDistance: Float
    ) {
        val hb = zone.tmpHitbox
        zone.hitbox(hb)

        if (minDistance <= 0) {
            // 无需距离检查，直接生成
            val maxAttemptsPerPoint = 100
            for (i in 0 until count) {
                var attempts = 0
                while (attempts < maxAttemptsPerPoint) {
                    val x = hb.x + Mathf.random() * hb.width
                    val y = hb.y + Mathf.random() * hb.height
                    if (zone.contains(x, y)) {
                        result.add(Vec2(x, y))
                        break
                    }
                    attempts++
                }
                // 如果尝试太多次失败，返回中心点
                if (attempts >= maxAttemptsPerPoint) {
                    result.add(zone.center())
                }
            }
        } else {
            // 需要距离检查，使用拒绝采样
            val maxAttempts = count * 50  // 每个点最多尝试 50 次
            var attempts = 0

            while (result.size < count && attempts < maxAttempts) {
                val x = hb.x + Mathf.random() * hb.width
                val y = hb.y + Mathf.random() * hb.height

                if (zone.contains(x, y)) {
                    val newPoint = Vec2(x, y)
                    // 检查与已有点的距离
                    if (result.all { it.dst(newPoint) >= minDistance }) {
                        result.add(newPoint)
                    }
                }
                attempts++
            }
        }
    }

    fun readSpace(r: Reads, revision: Byte) {
        try {
            val count = r.i()
            repeat(count) {
                // r.str() 读取字符串，不是 r.s()（r.s() 读取short）
                val datap = r.str()
                val id = VsVars.world.spaceDate.deserializeZoneFromString(datap, preserveId = true)
                if (id != -1) {
                    val zone = VsVars.world.spaceDate.getPolygonById(id)
                        ?: VsVars.world.spaceDate.getCircleById(id)
                    if (zone != null) {
                        spaces[id] = zone
                    }
                }
            }
            VsVars.world.spaceDate.rebuildIndex()
        } catch (e: IOException) {
            Log.warn("[SpaceDateInterface] Error while reading space date", e)
        }
    }

    fun writeSpace(w: Writes) {
        try {
            w.i(spaces.size)
            // spaces 应该是 Map<FieldZone, Int>，遍历 entry
            spaces.forEach { (zone, _) ->
                // w.str() 写入字符串，不是 w.s()（w.s() 写入short）
                val datap = VsVars.world.spaceDate.serializeZoneToString(zone) ?: ""
                w.str(datap)
            }
        } catch (e: IOException) {
            Log.warn("[SpaceDateInterface] Error while writing space date", e)
        }
    }


    fun getAllAreas(): Float {
        var area = 0f
        spaces.forEach { (_, i) ->
            area += VsVars.world.spaceDate.getArea(i)
        }
        return area
    }

    /**
     * 创建圆形场
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param radius 半径
     * @param effect 场强度
     * @return 创建成功的圆形场，如果面积超限返回 null
     */
    fun addCircle(
        x: Float, y: Float, radius: Float, effect: Float = 1f
    ): SpaceDate.CircleZone? {
        if (canAddZone()) {
            val zone =
                VsVars.world.spaceDate.addCircle(x, y, radius, effect/* getMaxArea() > 0, getMaxArea()*/) ?: return null
            val id = zone.id
            spaces += Pair(id, zone)
            return zone
        }
        return null
    }

    /**
     * 创建矩形场
     * @param x 左下角 X 坐标
     * @param y 左下角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param effect 场强度
     * @return 创建成功的矩形场，如果面积超限返回 null
     */
    fun addRect(
        x: Float, y: Float, width: Float, height: Float, effect: Float = 1f
    ): PolygonZone? {
        if (canAddZone()) {
            val zone = VsVars.world.spaceDate.addRect(x, y, width, height, effect, getMaxArea() > 0, getMaxArea())
                ?: return null
            val id = zone.id
            spaces += Pair(id, zone)
            return zone
        }
        return null
    }

    /**
     * 创建自定义多边形场
     * @param vertices 顶点数组 [x0,y0,x1,y1,...]
     * @param effect 场强度
     * @return 创建成功的多边形场，如果面积超限返回 null
     */
    fun addPolygon(
        vertices: FloatArray, effect: Float = 1f
    ): PolygonZone? {
        if (canAddZone()) {
            val zone =
                VsVars.world.spaceDate.addPolygon(vertices, effect, getMaxArea() > 0, getMaxArea()) ?: return null
            val id = zone.id
            spaces += Pair(id, zone)
            return zone
        }
        return null
    }

    fun removeZone(zone: SpaceDate.FieldZone) {
        when (zone) {
            is SpaceDate.CircleZone -> VsVars.world.spaceDate.removeCircle(zone)
            is PolygonZone -> VsVars.world.spaceDate.removePolygon(zone)
            else -> return
        }
        spaces.remove(zone.id)
    }

    fun removeZone(id: Int) {
        VsVars.world.spaceDate.removeById(id)
        spaces.remove(id)
    }

    fun containsZone(x: Float, y: Float): Boolean {
        return VsVars.world.spaceDate.contains(x, y)
    }

    fun containsZone(id: Int, x: Float, y: Float): Boolean {
        return VsVars.world.spaceDate.contains(id, x, y)
    }
}
