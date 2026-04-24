package voidshield.other.dateTypes

import arc.math.geom.*
import arc.struct.Seq
import arc.math.Mathf
import arc.util.Log
import arc.util.io.Reads
import arc.util.io.Writes
import kotlin.math.*

/**
 * 空间数据管理
 */
class SpaceDate(worldBounds: Rect = Rect(0f, 0f, 2000f, 2000f)) {

    // 使用 var 允许动态更新，私有 setter 控制更新逻辑
    var bounds: Rect = Rect(worldBounds)

    // ========== 数据定义 ==========

    interface FieldZone : QuadTree.QuadTreeObject {
        val id: Int
        var active: Boolean
        val effectValue: Float

        fun contains(x: Float, y: Float): Boolean
        fun center(): Vec2
        
        /** 将场序列化为字符串 */
        fun toStringSerialize(): String
    }

    data class CircleZone(
        override val id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        override var active: Boolean = true,
        override val effectValue: Float = 1f
    ) : FieldZone {

        override fun contains(x: Float, y: Float): Boolean {
            val dx = x - this@CircleZone.x
            val dy = y - this@CircleZone.y
            return dx * dx + dy * dy <= radius * radius
        }

        override fun hitbox(out: Rect) {
            out.set(x - radius, y - radius, radius * 2, radius * 2)
        }

        override fun center(): Vec2 = Vec2(x, y)

        fun setPosition(nx: Float, ny: Float) {
            x = nx; y = ny
        }

        /**
         * 序列化为字符串格式: "CIRCLE|id|x|y|radius|active|effectValue"
         */
        override fun toStringSerialize(): String {
            return "CIRCLE|$id|$x|$y|$radius|$active|$effectValue"
        }
    }

    class PolygonZone(
        override val id: Int,
        verticesArray: FloatArray,
        override var active: Boolean = true,
        override val effectValue: Float = 1f
    ) : FieldZone {

        var vertices: FloatArray = verticesArray

        val tmpHitbox = Rect()
        var cachedHitbox: Rect? = null

        init {
            require(vertices.size >= 6) { "多边形至少需要3个顶点(6个float)" }
            require(vertices.size % 2 == 0) { "顶点数组必须是x,y成对" }
        }

        override fun contains(x: Float, y: Float): Boolean {
            val hb = tmpHitbox
            hitbox(hb)
            if (!hb.contains(x, y)) return false
            return pointInPolygon(x, y, vertices)
        }

        override fun hitbox(out: Rect) {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE

            for (i in vertices.indices step 2) {
                val vx = vertices[i]
                val vy = vertices[i + 1]
                minX = min(minX, vx)
                maxX = max(maxX, vx)
                minY = min(minY, vy)
                maxY = max(maxY, vy)
            }

            out.set(minX, minY, maxX - minX, maxY - minY)
            cachedHitbox = Rect(out)
        }

        override fun center(): Vec2 {
            val hb = tmpHitbox
            hitbox(hb)
            return Vec2(hb.x + hb.width / 2, hb.y + hb.height / 2)
        }

        fun translate(dx: Float, dy: Float) {
            for (i in vertices.indices step 2) {
                vertices[i] += dx
                vertices[i + 1] += dy
            }
            cachedHitbox = null
        }

        fun setNewVertices(newVertices: FloatArray) {
            require(newVertices.size >= 6 && newVertices.size % 2 == 0)
            vertices = newVertices
            cachedHitbox = null
        }

        fun rotate(degrees: Float) {
            val c = center()
            val rad = degrees * Mathf.degreesToRadians
            val cos = cos(rad)
            val sin = sin(rad)

            for (i in vertices.indices step 2) {
                val dx = vertices[i] - c.x
                val dy = vertices[i + 1] - c.y
                vertices[i] = c.x + dx * cos - dy * sin
                vertices[i + 1] = c.y + dx * sin + dy * cos
            }
            cachedHitbox = null
        }

        /**
         * 序列化为字符串格式: "POLYGON|id|active|effectValue|vCount|v0,v1,v2,v3,..."
         */
        override fun toStringSerialize(): String {
            val verticesStr = vertices.joinToString(",")
            return "POLYGON|$id|$active|$effectValue|${vertices.size}|$verticesStr"
        }
    }

    // ========== 存储结构 ==========

    val circles = Seq<CircleZone>()
    val polygons = Seq<PolygonZone>()
    var spatialIndex: QuadTree<ZoneWrapper>
    val tmpResult = Seq<ZoneWrapper>()
    val tmpRect = Rect()

    // 初始化空间索引
    init {
        Log.info("[SpaceDate] Loading Spatial Index")
        spatialIndex = QuadTree<ZoneWrapper>(bounds)
    }

    data class ZoneWrapper(
        val zone: FieldZone,
        val type: ZoneType
    ) : QuadTree.QuadTreeObject {
        enum class ZoneType { CIRCLE, POLYGON }

        override fun hitbox(out: Rect) {
            zone.hitbox(out)
        }
    }

    var idCounter = 0

    // ========== WorldBounds 更新 ==========

    /**
     * 更新世界边界（会重建空间索引）
     * @param newBounds 新的边界矩形
     * @param keepOutOfBounds 是否保留超出新边界的场（默认false：删除越界场）
     * @return 被删除的场数量（如果keepOutOfBounds=false）
     */
    fun updateBounds(newBounds: Rect, keepOutOfBounds: Boolean = false): Int {
        var removedCount = 0

        // 如果不保留越界场，先删除
        if (!keepOutOfBounds) {
            // 检查并删除越界的圆形
            val circlesToRemove = Seq<CircleZone>()
            circles.each { c ->
                if (!newBounds.contains(c.x, c.y)) {
                    circlesToRemove.add(c)
                }
            }
            circlesToRemove.each { c ->
                removeCircle(c)
                removedCount++
            }

            // 检查并删除越界的多边形
            val polysToRemove = Seq<PolygonZone>()
            polygons.each { p ->
                val center = p.center()
                if (!newBounds.contains(center.x, center.y)) {
                    polysToRemove.add(p)
                }
            }
            polysToRemove.each { p ->
                removePolygon(p)
                removedCount++
            }
        }

        // 更新边界
        bounds.set(newBounds)

        // 重建空间索引
        rebuildIndexWithNewBounds()

        return removedCount
    }

    /**
     * 扩展边界（取当前边界和新边界的并集）
     * @param additionalBounds 需要包含的额外边界
     */
    fun expandBounds(additionalBounds: Rect) {
        bounds.merge(additionalBounds)
        rebuildIndexWithNewBounds()
    }

    /**
     * 缩放边界（中心点不变，宽高缩放）
     * @param scale 缩放比例（1.0 = 不变，2.0 = 双倍）
     */
    fun scaleBounds(scale: Float) {
        val centerX = bounds.x + bounds.width / 2
        val centerY = bounds.y + bounds.height / 2
        val newWidth = bounds.width * scale
        val newHeight = bounds.height * scale

        bounds.set(
            centerX - newWidth / 2,
            centerY - newHeight / 2,
            newWidth,
            newHeight
        )

        rebuildIndexWithNewBounds()
    }

    /**
     * 移动边界（平移）
     * @param dx X方向偏移
     * @param dy Y方向偏移
     */
    fun translateBounds(dx: Float, dy: Float) {
        bounds.x += dx
        bounds.y += dy
        rebuildIndexWithNewBounds()
    }

    /**
     * 内部方法：用当前 bounds 重建空间索引
     */
    fun rebuildIndexWithNewBounds() {
        // 创建新的空间索引
        val newIndex = QuadTree<ZoneWrapper>(bounds)

        // 将所有激活的场添加到新索引
        circles.each { c ->
            if (c.active) {
                newIndex.insert(ZoneWrapper(c, ZoneWrapper.ZoneType.CIRCLE))
            }
        }
        polygons.each { p ->
            if (p.active) {
                newIndex.insert(ZoneWrapper(p, ZoneWrapper.ZoneType.POLYGON))
            }
        }

        // 替换旧索引
        spatialIndex = newIndex
    }

    /**
     * 检查场是否在边界内
     */
    fun isInBounds(zone: FieldZone): Boolean {
        val hb = tmpRect
        zone.hitbox(hb)
        return bounds.overlaps(hb)
    }

    /**
     * 获取所有越界的场
     */
    fun getOutOfBoundsZones(): Seq<FieldZone> {
        val result = Seq<FieldZone>()

        circles.each { c ->
            if (!bounds.contains(c.x, c.y)) {
                result.add(c)
            }
        }

        polygons.each { p ->
            val center = p.center()
            if (!bounds.contains(center.x, center.y)) {
                result.add(p)
            }
        }

        return result
    }

    // ========== 序列化/反序列化 ==========

    fun writeZone(writer: Writes, id: Int): Boolean {
        val circle = getCircleById(id)
        if (circle != null) {
            writer.b(0)
            writer.i(circle.id)
            writer.f(circle.x)
            writer.f(circle.y)
            writer.f(circle.radius)
            writer.bool(circle.active)
            writer.f(circle.effectValue)
            return true
        }

        val poly = getPolygonById(id)
        if (poly != null) {
            writer.b(1)
            writer.i(poly.id)
            writer.i(poly.vertices.size)
            poly.vertices.forEach { writer.f(it) }
            writer.bool(poly.active)
            writer.f(poly.effectValue)
            return true
        }

        return false
    }

    fun readZone(reader: Reads): Int {
        val type = reader.b()

        return when (type.toInt()) {
            0 -> {
                reader.i()
                val x = reader.f()
                val y = reader.f()
                val radius = reader.f()
                val active = reader.bool()
                val effect = reader.f()

                val zone = CircleZone(++idCounter, x, y, radius, active, effect)
                circles.add(zone)
                spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
                zone.id
            }
            1 -> {
                reader.i()
                val count = reader.i()
                val vertices = FloatArray(count) { reader.f() }
                val active = reader.bool()
                val effect = reader.f()

                val zone = PolygonZone(++idCounter, vertices, active, effect)
                polygons.add(zone)
                spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
                zone.id
            }
            else -> -1
        }
    }

    fun readZonePreserveId(reader: Reads): Int {
        val type = reader.b()

        return when (type.toInt()) {
            0 -> {
                val savedId = reader.i()
                val x = reader.f()
                val y = reader.f()
                val radius = reader.f()
                val active = reader.bool()
                val effect = reader.f()

                removeById(savedId)

                val zone = CircleZone(savedId, x, y, radius, active, effect)
                circles.add(zone)
                spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))

                if (savedId > idCounter) idCounter = savedId
                savedId
            }
            1 -> {
                val savedId = reader.i()
                val count = reader.i()
                val vertices = FloatArray(count) { reader.f() }
                val active = reader.bool()
                val effect = reader.f()

                removeById(savedId)

                val zone = PolygonZone(savedId, vertices, active, effect)
                polygons.add(zone)
                spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))

                if (savedId > idCounter) idCounter = savedId
                savedId
            }
            else -> -1
        }
    }

    // ========== 字符串序列化/反序列化 ==========
    
    /**
     * 将指定场序列化为字符串
     * @param id 场ID
     * @return 序列化字符串，若场不存在返回 null
     */
    fun serializeZoneToString(id: Int): String? {
        val circle = getCircleById(id)
        if (circle != null) return circle.toStringSerialize()
        
        val poly = getPolygonById(id)
        if (poly != null) return poly.toStringSerialize()
        
        return null
    }
    
    /**
     * 将所有场序列化为字符串列表
     * @return 所有场的序列化字符串列表
     */
    fun serializeAllZonesToString(): Seq<String> {
        val result = Seq<String>()
        circles.each { result.add(it.toStringSerialize()) }
        polygons.each { result.add(it.toStringSerialize()) }
        return result
    }
    
    /**
     * 统一反序列化函数 - 从字符串解析并创建场
     * 支持格式:
     * - 圆形: "CIRCLE|id|x|y|radius|active|effectValue"
     * - 多边形: "POLYGON|id|active|effectValue|vCount|v0,v1,v2,v3,..."
     * @param data 序列化字符串
     * @param preserveId 是否保留原ID(true)或重新分配ID(false)
     * @return 创建成功的场ID，失败返回 -1
     */
    fun deserializeZoneFromString(data: String, preserveId: Boolean = false): Int {
        val parts = data.split("|")
        if (parts.size < 2) return -1
        
        val type = parts[0]
        
        return when (type) {
            "CIRCLE" -> deserializeCircleFromString(parts, preserveId)
            "POLYGON" -> deserializePolygonFromString(parts, preserveId)
            else -> -1
        }
    }
    
    /**
     * 反序列化圆形场
     * 格式: "CIRCLE|id|x|y|radius|active|effectValue"
     */
    private fun deserializeCircleFromString(parts: List<String>, preserveId: Boolean): Int {
        // 圆形格式有7个字段：CIRCLE|id|x|y|radius|active|effectValue
        if (parts.size != 7) {
            Log.warn("[SpaceDate] 圆形反序列化字段数错误: ${parts.size}, 期望 7")
            return -1
        }
        
        return try {
            val savedId = parts[1].toInt()
            val x = parts[2].toFloat()
            val y = parts[3].toFloat()
            val radius = parts[4].toFloat()
            val active = parts[5].toBoolean()
            val effect = parts[6].toFloat()
            
            val finalId = if (preserveId) savedId else ++idCounter
            
            if (preserveId && savedId > idCounter) {
                idCounter = savedId
            }
            
            // 如果保留ID且已存在，先移除
            if (preserveId) {
                removeById(savedId)
            }
            
            val zone = CircleZone(finalId, x, y, radius, active, effect)
            circles.add(zone)
            spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
            
            finalId
        } catch (e: Exception) {
            Log.err("[SpaceDate] 反序列化圆形场失败: ", e)
            -1
        }
    }
    
    /**
     * 反序列化多边形场
     * 格式: "POLYGON|id|active|effectValue|vCount|v0,v1,v2,v3,..."
     */
    private fun deserializePolygonFromString(parts: List<String>, preserveId: Boolean): Int {
        // 多边形格式有6个字段：POLYGON|id|active|effectValue|vCount|vertices
        if (parts.size != 6) {
            Log.warn("[SpaceDate] 多边形反序列化字段数错误: ${parts.size}, 期望 6")
            return -1
        }
        
        return try {
            val savedId = parts[1].toInt()
            val active = parts[2].toBoolean()
            val effect = parts[3].toFloat()
            val vCount = parts[4].toInt()
            val verticesStr = parts[5]
            
            val vertices = verticesStr.split(",").map { it.toFloat() }.toFloatArray()
            
            if (vertices.size != vCount) {
                Log.warn("[SpaceDate] 多边形顶点数量不匹配: 期望 $vCount, 实际 ${vertices.size}")
            }
            
            val finalId = if (preserveId) savedId else ++idCounter
            
            if (preserveId && savedId > idCounter) {
                idCounter = savedId
            }
            
            // 如果保留ID且已存在，先移除
            if (preserveId) {
                removeById(savedId)
            }
            
            val zone = PolygonZone(finalId, vertices, active, effect)
            polygons.add(zone)
            spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
            
            finalId
        } catch (e: Exception) {
            Log.err("[SpaceDate] 反序列化多边形场失败: ", e)
            -1
        }
    }
    
    /**
     * 批量反序列化多个场
     * @param dataList 序列化字符串列表
     * @param preserveId 是否保留原ID
     * @return 成功创建的场ID列表
     */
    fun deserializeZonesFromString(dataList: Seq<String>, preserveId: Boolean = false): Seq<Int> {
        val result = Seq<Int>()
        dataList.each { data ->
            val id = deserializeZoneFromString(data, preserveId)
            if (id != -1) result.add(id)
        }
        return result
    }

    // ========== 核心API ==========

    /**
     * 创建圆形场
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param radius 半径
     * @param effect 场强度
     * @param limitMaxArea 是否限制最大面积
     * @param maxArea 最大面积值（仅在 limitMaxArea=true 时生效）
     * @return 创建成功的圆形场，如果面积超限返回 null
     */
    fun addCircle(
        x: Float, y: Float, radius: Float, effect: Float = 1f,
        limitMaxArea: Boolean = false, maxArea: Float = 0f
    ): CircleZone? {
        val area = calculateCircleArea(radius)
        if (limitMaxArea && area > maxArea) {
            return null
        }

        val zone = CircleZone(++idCounter, x, y, radius, true, effect)
        circles.add(zone)
        spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
        return zone
    }

    /**
     * 创建矩形场
     * @param x 左下角 X 坐标
     * @param y 左下角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param effect 场强度
     * @param limitMaxArea 是否限制最大面积
     * @param maxArea 最大面积值（仅在 limitMaxArea=true 时生效）
     * @return 创建成功的矩形场，如果面积超限返回 null
     */
    fun addRect(
        x: Float, y: Float, width: Float, height: Float, effect: Float = 1f,
        limitMaxArea: Boolean = false, maxArea: Float = 0f
    ): PolygonZone? {
        val vertices = floatArrayOf(
            x, y,
            x + width, y,
            x + width, y + height,
            x, y + height
        )
        return addPolygon(vertices, effect, limitMaxArea, maxArea)
    }

    /**
     * 创建旋转矩形场
     * @param centerX 中心 X 坐标
     * @param centerY 中心 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param rotation 旋转角度（度）
     * @param effect 场强度
     * @param limitMaxArea 是否限制最大面积
     * @param maxArea 最大面积值（仅在 limitMaxArea=true 时生效）
     * @return 创建成功的旋转矩形场，如果面积超限返回 null
     */
    fun addRotatedRect(
        centerX: Float, centerY: Float, width: Float, height: Float,
        rotation: Float, effect: Float = 1f,
        limitMaxArea: Boolean = false, maxArea: Float = 0f
    ): PolygonZone? {
        val hw = width / 2
        val hh = height / 2
        val rad = rotation * Mathf.degreesToRadians
        val cos = cos(rad)
        val sin = sin(rad)

        val corners = arrayOf(
            floatArrayOf(-hw, -hh),
            floatArrayOf(hw, -hh),
            floatArrayOf(hw, hh),
            floatArrayOf(-hw, hh)
        )

        val vertices = FloatArray(8)
        for (i in corners.indices) {
            val rx = corners[i][0] * cos - corners[i][1] * sin
            val ry = corners[i][0] * sin + corners[i][1] * cos
            vertices[i * 2] = centerX + rx
            vertices[i * 2 + 1] = centerY + ry
        }

        return addPolygon(vertices, effect, limitMaxArea, maxArea)
    }

    /**
     * 创建自定义多边形场
     * @param vertices 顶点数组 [x0,y0,x1,y1,...]
     * @param effect 场强度
     * @param limitMaxArea 是否限制最大面积
     * @param maxArea 最大面积值（仅在 limitMaxArea=true 时生效）
     * @return 创建成功的多边形场，如果面积超限返回 null
     */
    fun addPolygon(
        vertices: FloatArray, effect: Float = 1f,
        limitMaxArea: Boolean = false, maxArea: Float = 0f
    ): PolygonZone? {
        val area = calculatePolygonArea(vertices)
        if (limitMaxArea && area > maxArea) {
            return null
        }

        val zone = PolygonZone(++idCounter, vertices, true, effect)
        polygons.add(zone)
        spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
        return zone
    }

    // ----- 删除 -----

    fun removeById(id: Int): Boolean {
        val circle = circles.find { it.id == id }
        if (circle != null) {
            circles.remove(circle)
            spatialIndex.remove(ZoneWrapper(circle, ZoneWrapper.ZoneType.CIRCLE))
            return true
        }

        val poly = polygons.find { it.id == id }
        if (poly != null) {
            polygons.remove(poly)
            spatialIndex.remove(ZoneWrapper(poly, ZoneWrapper.ZoneType.POLYGON))
            return true
        }
        return false
    }

    fun removeCircle(zone: CircleZone): Boolean {
        circles.remove(zone, true)
        return spatialIndex.remove(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
    }

    fun removePolygon(zone: PolygonZone): Boolean {
        polygons.remove(zone, true)
        return spatialIndex.remove(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
    }

    fun clear() {
        circles.clear()
        polygons.clear()
        spatialIndex.clear()
        idCounter = 0
    }

    // ----- 查询API -----

    fun contains(x: Float, y: Float): Boolean {
        // 快速边界检查
        if (!bounds.contains(x, y)) return false

        spatialIndex.intersect(x, y, 0.1f, 0.1f, tmpResult)

        val found = tmpResult.any { wrapper ->
            wrapper.zone.active && wrapper.zone.contains(x, y)
        }
        tmpResult.clear()
        return found
    }

    fun contains(id: Int, x: Float, y: Float): Boolean {
        // 快速边界检查
        if (!bounds.contains(x, y)) return false

        val circle = getCircleById(id)
        if (circle != null) {
            return circle.active && circle.contains(x, y)
        }

        val poly = getPolygonById(id)
        if (poly != null) {
            return poly.active && poly.contains(x, y)
        }

        return false
    }

    fun queryPoint(x: Float, y: Float): Seq<FieldZone> {
        val result = Seq<FieldZone>()

        // 边界外直接返回空
        if (!bounds.contains(x, y)) return result

        spatialIndex.intersect(x, y, 0.1f, 0.1f, tmpResult)

        for (wrapper in tmpResult) {
            val zone = wrapper.zone
            if (zone.active && zone.contains(x, y)) {
                result.add(zone)
            }
        }
        tmpResult.clear()

        result.sort { z -> -z.effectValue }
        return result
    }

    fun getTotalEffect(x: Float, y: Float): Float {
        // 边界外直接返回0
        if (!bounds.contains(x, y)) return 0f

        var total = 0f
        spatialIndex.intersect(x, y, 0.1f, 0.1f, tmpResult)

        for (wrapper in tmpResult) {
            val zone = wrapper.zone
            if (zone.active && zone.contains(x, y)) {
                total = max(total, zone.effectValue)
            }
        }
        tmpResult.clear()
        return total
    }

    fun queryRegion(x: Float, y: Float, width: Float, height: Float): Seq<FieldZone> {
        val result = Seq<FieldZone>()

        // 快速边界检查
        val queryRect = Rect(x, y, width, height)
        if (!bounds.overlaps(queryRect)) return result

        spatialIndex.intersect(x, y, width, height, tmpResult)

        for (wrapper in tmpResult) {
            if (wrapper.zone.active) {
                val hb = tmpRect
                wrapper.zone.hitbox(hb)
                if (hb.overlaps(x, y, width, height)) {
                    result.add(wrapper.zone)
                }
            }
        }
        tmpResult.clear()
        return result
    }

    fun queryCircle(cx: Float, cy: Float, radius: Float): Seq<FieldZone> {
        val result = Seq<FieldZone>()

        // 快速边界检查
        val queryBounds = Rect(cx - radius, cy - radius, radius * 2, radius * 2)
        if (!bounds.overlaps(queryBounds)) return result

        spatialIndex.intersect(cx - radius, cy - radius, radius * 2, radius * 2, tmpResult)

        for (wrapper in tmpResult) {
            val zone = wrapper.zone
            if (!zone.active) continue

            val intersects = when (zone) {
                is CircleZone -> {
                    val dx = zone.x - cx
                    val dy = zone.y - cy
                    val dist = sqrt(dx*dx + dy*dy)
                    dist < zone.radius + radius
                }
                is PolygonZone -> circleIntersectsPolygon(cx, cy, radius, zone.vertices)
                else -> false
            }

            if (intersects) result.add(zone)
        }
        tmpResult.clear()
        return result
    }

    // ----- 更新移动范围 -----

    fun updateCirclePosition(zone: CircleZone, newX: Float, newY: Float) {
        // 检查新位置是否在边界内
        if (!bounds.contains(newX, newY)) {
            // 可以选择：截断到边界内，或允许但标记为越界
            // 这里选择允许，但查询时会过滤
        }

        spatialIndex.remove(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
        zone.setPosition(newX, newY)
        spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.CIRCLE))
    }

    fun updatePolygonPosition(zone: PolygonZone, dx: Float, dy: Float) {
        spatialIndex.remove(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
        zone.translate(dx, dy)
        spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
    }

    fun updatePolygonRotation(zone: PolygonZone, degrees: Float) {
        spatialIndex.remove(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
        zone.rotate(degrees)
        spatialIndex.insert(ZoneWrapper(zone, ZoneWrapper.ZoneType.POLYGON))
    }

    /**
     * 标准重建索引（不修改bounds）
     */
    fun rebuildIndex() {
        spatialIndex.clear()

        circles.each { c ->
            if (c.active) spatialIndex.insert(ZoneWrapper(c, ZoneWrapper.ZoneType.CIRCLE))
        }
        polygons.each { p ->
            if (p.active) spatialIndex.insert(ZoneWrapper(p, ZoneWrapper.ZoneType.POLYGON))
        }
    }

    // ----- 管理 -----

    fun getAllCircles(): Seq<CircleZone> = circles.copy()
    fun getAllPolygons(): Seq<PolygonZone> = polygons.copy()
    fun getCircleById(id: Int): CircleZone? = circles.find { it.id == id }
    fun getPolygonById(id: Int): PolygonZone? = polygons.find { it.id == id }

    fun setZoneActive(id: Int, active: Boolean): Boolean {
        getCircleById(id)?.let { it.active = active; return true }
        getPolygonById(id)?.let { it.active = active; return true }
        return false
    }

    fun getMaxId(): Int = idCounter

    // ========== 面积查询 API ==========

    /**
     * 获取场的面积
     * @param zone 场对象
     * @return 场的面积值
     */
    fun getArea(zone: FieldZone): Float {
        return when (zone) {
            is CircleZone -> calculateCircleArea(zone.radius)
            is PolygonZone -> calculatePolygonArea(zone.vertices)
            else -> 0f
        }
    }

    /**
     * 根据场 ID 获取场的面积
     * @param id 场 ID
     * @return 场的面积值，如果场不存在返回 null
     */
    fun getArea(id: Int): Float? {
        return getCircleById(id)?.let { getArea(it) }
            ?: getPolygonById(id)?.let { getArea(it) }
    }

    // ========== 辅助方法 ==========

    /**
     * 计算圆形场的面积
     */
    fun calculateCircleArea(radius: Float): Float {
        return Mathf.PI * radius * radius
    }

    /**
     * 使用鞋带公式 (Shoelace Formula) 计算多边形面积
     */
    fun calculatePolygonArea(vertices: FloatArray): Float {
        require(vertices.size >= 6 && vertices.size % 2 == 0) { "多边形顶点数组必须至少包含 3 个点 (6 个 float) 且长度为偶数" }

        var area = 0f
        val n = vertices.size / 2

        for (i in 0 until n) {
            val j = (i + 1) % n
            val xi = vertices[i * 2]
            val yi = vertices[i * 2 + 1]
            val xj = vertices[j * 2]
            val yj = vertices[j * 2 + 1]
            area += xi * yj - xj * yi
        }

        return abs(area) / 2f
    }

    companion object {
        fun pointInPolygon(px: Float, py: Float, vertices: FloatArray): Boolean {
            var inside = false
            var j = vertices.size - 2

            for (i in vertices.indices step 2) {
                val xi = vertices[i]
                val yi = vertices[i + 1]
                val xj = vertices[j]
                val yj = vertices[j + 1]

                if (((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                    inside = !inside
                }
                j = i
            }
            return inside
        }

        fun circleIntersectsPolygon(cx: Float, cy: Float, r: Float, vertices: FloatArray): Boolean {
            if (pointInPolygon(cx, cy, vertices)) return true

            for (i in vertices.indices step 2) {
                val j = (i + 2) % vertices.size
                val x1 = vertices[i]
                val y1 = vertices[i + 1]
                val x2 = vertices[j]
                val y2 = vertices[j + 1]

                if (pointToSegmentDistance(cx, cy, x1, y1, x2, y2) <= r) return true
            }
            return false
        }

        fun pointToSegmentDistance(px: Float, py: Float,
                                   x1: Float, y1: Float,
                                   x2: Float, y2: Float): Float {
            val dx = x2 - x1
            val dy = y2 - y1
            val len2 = dx * dx + dy * dy

            if (len2 == 0f) return sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1))

            var t = ((px - x1) * dx + (py - y1) * dy) / len2
            t = t.coerceIn(0f, 1f)

            val projX = x1 + t * dx
            val projY = y1 + t * dy

            return sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY))
        }
    }
}
