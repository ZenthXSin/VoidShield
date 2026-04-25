package voidshield.shader.v1

import arc.Core
import arc.Events
import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Mesh
import arc.graphics.Texture
import arc.graphics.VertexAttribute
import arc.graphics.gl.FrameBuffer
import arc.math.Mathf
import arc.math.Mat
import arc.util.Time
import mindustry.Vars
import mindustry.game.EventType
import voidshield.other.interfaces.VSShaderLoader
import mindustry.gen.Groups
import voidshield.other.VsVars
import voidshield.shader.ScreenSampler
import voidshield.shader.ShaderManage


// 生命周期数据类，更清晰
data class LifeCycleData(
    val lifeTime: Float,           // 总生命周期时长
    val startTime: Float,          // 开始时间
    val fadeOutStartPercent: Float, // 开始淡出的时间百分比 (0-1)
    val mode: Boolean
)

open class DefaultShader(vert: String = "default", frag: String = "default") : VSShaderLoader {

    override var shader = ShaderManage.getShader("default", "default", VsVars.modName)
    override var meshMap: HashMap<String, VSShaderLoader.MeshData> = hashMapOf()
    val meshVerticesCache = HashMap<String, FloatArray>()

    val transform = Mat()
    val whiteBits = Color.white.toFloatBits()

    var screenBuffer: FrameBuffer? = null
    var captured = false
    val tmpColor = Color()
    val lifeCycle = HashMap<String, LifeCycleData>()
    var time = 0f

    var isReady = false

    open fun setMeshAlpha(id: String, alpha: Float) {
        val data = meshMap[id] ?: return
        val mesh = data.mesh

        val buffer = mesh.verticesBuffer
        val vertexSize = 5
        val vertexCount = buffer.limit() / vertexSize
        val a = alpha.coerceIn(0f, 1f)

        for (i in 0 until vertexCount) {
            val colorIndex = i * vertexSize + 2
            val colorBits = buffer.get(colorIndex)

            tmpColor.abgr8888ToColor(java.lang.Float.floatToIntBits(colorBits))
            tmpColor.a = a
            buffer.put(colorIndex, tmpColor.toFloatBits())
        }

        buffer.position(0)
    }

    open fun setMeshScale(id: String, scale: Float) = setMeshScale(id, scale, scale)

    open fun setMeshScale(id: String, scaleX: Float, scaleY: Float) {
        val data = meshMap[id] ?: return
        val baseVertices = meshVerticesCache[id] ?: cacheMeshVertices(id, data.mesh)
        if (baseVertices.isEmpty()) return

        val scaledVertices = baseVertices.copyOf()
        val vertexSize = data.mesh.vertexSize / java.lang.Float.BYTES
        if (vertexSize < 2) return

        val vertexCount = scaledVertices.size / vertexSize
        if (vertexCount == 0) return

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (i in 0 until vertexCount) {
            val index = i * vertexSize
            val x = baseVertices[index]
            val y = baseVertices[index + 1]
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
        }

        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f

        for (i in 0 until vertexCount) {
            val index = i * vertexSize
            val x = baseVertices[index]
            val y = baseVertices[index + 1]
            scaledVertices[index] = centerX + (x - centerX) * scaleX
            scaledVertices[index + 1] = centerY + (y - centerY) * scaleY
        }

        data.mesh.setVertices(scaledVertices)
    }

    protected open fun cacheMeshVertices(id: String, mesh: Mesh): FloatArray {
        val buffer = mesh.verticesBuffer
        val size = buffer.limit()
        val vertices = FloatArray(size)
        val previousPosition = buffer.position()
        buffer.position(0)
        buffer.get(vertices)
        buffer.position(previousPosition)
        meshVerticesCache[id] = vertices
        return vertices
    }

    override fun update(newMesh: Mesh, id: String, type: Int, set: () -> Unit) {
        meshMap[id]?.mesh?.dispose()
        meshMap[id] = VSShaderLoader.MeshData(newMesh, type, set)
        cacheMeshVertices(id, newMesh)
    }

    override fun update(id: String, floatArray: FloatArray) {
        meshMap[id]?.mesh?.setVertices(floatArray)
        meshVerticesCache[id] = floatArray.copyOf()
    }

    override fun remove(id: String) {
        meshMap[id]?.mesh?.dispose()
        meshMap.remove(id)
        meshVerticesCache.remove(id)
    }

    override fun dispose() {
        meshMap.values.forEach { it.mesh.dispose() }
        meshMap.clear()
        meshVerticesCache.clear()
        shader.dispose()
        disposeBuffer()
    }


    override fun setUniformf() {
        shader.setUniformf("u_color", Color.white)
        shader.setUniformMatrix4("u_proj", Core.camera.mat)
        shader.setUniformMatrix4("u_trns", transform.idt())
        shader.setUniformf("u_camsize", Core.camera.width, Core.camera.height)

        shader.setUniformf("u_time", Time.time / 60f)
    }

    open fun ensureBuffer() {
        val w = Core.graphics.width
        val h = Core.graphics.height
        if (screenBuffer == null || screenBuffer!!.width != w || screenBuffer!!.height != h) {
            screenBuffer?.dispose()
            screenBuffer = FrameBuffer(w, h)
            captured = false
        }
    }

    open fun disposeBuffer() {
        screenBuffer?.dispose()
        screenBuffer = null
        captured = false
    }

    open fun captureScreen() {
        ensureBuffer()
        screenBuffer?.let { ScreenSampler.toBuffer(it) }
        captured = true
    }

    open fun bindScreenTexture(unit: Int = 0, uniformName: String = "u_texture") {
        val tex = if (captured) screenBuffer!!.texture else Core.atlas.white().texture
        tex.bind(unit)
        shader.setUniformi(uniformName, unit)
    }

    open fun bindTexture(texture: Texture, unit: Int = 0, name: String = "u_texture") {
        texture.bind(unit)
        shader.setUniformi(name, unit)
    }

    open fun getScreenTexture() = screenBuffer?.texture

    open fun createMesh(vertexCount: Int, vertices: FloatArray): Mesh {
        return Mesh(
            true, vertexCount, 0,
            VertexAttribute.position,
            VertexAttribute.color,
            VertexAttribute.texCoords
        ).apply { setVertices(vertices) }
    }

    open fun regionVertex(x: Float, y: Float, u: Float, v: Float) =
        floatArrayOf(x, y, whiteBits, u, v)

    open fun concatVertices(list: List<FloatArray>): FloatArray {
        val total = list.sumOf { it.size }
        val out = FloatArray(total)
        var idx = 0
        list.forEach { arr -> arr.forEach { out[idx++] = it } }
        return out
    }

    open fun addQuadRegion(
        id: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        u1: Float = 0f,
        v1: Float = 0f,
        u2: Float = 1f,
        v2: Float = 1f
    ) {
        val x2 = x + w;
        val y2 = y + h
        val mesh = createMesh(
            6, floatArrayOf(
                x, y, whiteBits, u1, v1, x2, y, whiteBits, u2, v1, x2, y2, whiteBits, u2, v2,
                x, y, whiteBits, u1, v1, x2, y2, whiteBits, u2, v2, x, y2, whiteBits, u1, v2
            )
        )
        update(mesh, id, Gl.triangles)
    }

    open fun addScreenQuadRegion(id: String, x: Float, y: Float, w: Float, h: Float) = addQuadRegion(id, x, y, w, h)
    open fun addTexturedRegionArea(
        id: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        u1: Float,
        v1: Float,
        u2: Float,
        v2: Float
    ) = addQuadRegion(id, x, y, w, h, u1, v1, u2, v2)

    open fun addCircleRegion(id: String, cx: Float, cy: Float, r: Float, seg: Int = 48) {
        if (seg < 3) return
        val v = mutableListOf<FloatArray>()
        for (i in 0 until seg) {
            val a1 = Mathf.PI2 * i / seg
            val a2 = Mathf.PI2 * (i + 1) / seg
            val x1 = cx + Mathf.cos(a1) * r;
            val y1 = cy + Mathf.sin(a1) * r
            val x2 = cx + Mathf.cos(a2) * r;
            val y2 = cy + Mathf.sin(a2) * r
            v += regionVertex(cx, cy, 0.5f, 0.5f)
            v += regionVertex(x1, y1, 0.5f + Mathf.cos(a1) * 0.5f, 0.5f + Mathf.sin(a1) * 0.5f)
            v += regionVertex(x2, y2, 0.5f + Mathf.cos(a2) * 0.5f, 0.5f + Mathf.sin(a2) * 0.5f)
        }
        update(createMesh(seg * 3, concatVertices(v)), id, Gl.triangles)
    }

    open fun addPolygonRegion(id: String, cx: Float, cy: Float, r: Float, sides: Int) =
        if (sides >= 3) addCircleRegion(id, cx, cy, r, sides) else {
        }

    open fun addRingRegion(id: String, cx: Float, cy: Float, ir: Float, or: Float, seg: Int = 48) {
        if (seg < 3 || ir <= 0 || or <= ir) return
        val v = mutableListOf<FloatArray>()
        fun u(x: Float) = 0.5f + (x - cx) / (or * 2)
        fun v(y: Float) = 0.5f + (y - cy) / (or * 2)
        for (i in 0 until seg) {
            val a1 = Mathf.PI2 * i / seg;
            val a2 = Mathf.PI2 * (i + 1) / seg
            val ix1 = cx + Mathf.cos(a1) * ir;
            val iy1 = cy + Mathf.sin(a1) * ir
            val ox1 = cx + Mathf.cos(a1) * or;
            val oy1 = cy + Mathf.sin(a1) * or
            val ix2 = cx + Mathf.cos(a2) * ir;
            val iy2 = cy + Mathf.sin(a2) * ir
            val ox2 = cx + Mathf.cos(a2) * or;
            val oy2 = cy + Mathf.sin(a2) * or
            v += regionVertex(ix1, iy1, u(ix1), v(iy1))
            v += regionVertex(ox1, oy1, u(ox1), v(oy1))
            v += regionVertex(ox2, oy2, u(ox2), v(oy2))
            v += regionVertex(ix1, iy1, u(ix1), v(iy1))
            v += regionVertex(ox2, oy2, u(ox2), v(oy2))
            v += regionVertex(ix2, iy2, u(ix2), v(iy2))
        }
        update(createMesh(seg * 6, concatVertices(v)), id, Gl.triangles)
    }

    open fun getBuildIsAdded(id: String): Boolean {
        val buildId = id.removePrefix("[").removeSuffix("]").toIntOrNull() ?: return false
        return Groups.build.find { it.id == buildId } == null
    }

    /**
     * 设置生命周期
     * @param id Mesh ID
     * @param lifeTime 总存活时间（毫秒）
     * @param fadeOutStartPercent 从生命周期的哪个百分比开始淡出 (0-1, 0表示不淡出，直接消失)
     * @param mode 消失动画，true为淡出，false为缩小
     */
    open fun setLifeCycle(
        id: String,
        lifeTime: Float,
        fadeOutStartPercent: Float = 0f,
        mode: Boolean = true,
        run: () -> Unit = {}
    ) {
        lifeCycle[id] = LifeCycleData(lifeTime, time, fadeOutStartPercent.coerceIn(0f, 1f), mode)
        if (run == {}) return
        Time.run(lifeTime, run)
    }

    open fun lifeCycleUpdate() {
        if (Vars.state.isPaused) return
        if (lifeCycle.isEmpty()) {
            time = 0f
            return
        }
        time += Time.delta

        val toRemove = mutableListOf<String>()

        lifeCycle.forEach { (id, data) ->
            val elapsed = time - data.startTime
            val progress = elapsed / data.lifeTime

            when {
                // 生命周期结束，标记移除
                progress >= 0.99f -> {
                    toRemove += id
                }
                // 进入淡出阶段
                progress >= data.fadeOutStartPercent -> {
                    // 计算淡出的进度 (0-1)
                    val fadeProgress = (progress - data.fadeOutStartPercent) / (1f - data.fadeOutStartPercent)
                    // alpha 从 1 降到 0
                    val alpha = 1f - fadeProgress.coerceIn(0f, 1f)
                    if (data.mode) {
                        setMeshAlpha(id, alpha)
                    } else {
                        setMeshScale(id, alpha)
                    }
                }
            }
        }

        // 清理已结束的
        toRemove.forEach { id ->
            lifeCycle.remove(id)
            remove(id)
        }
    }

    override fun use() {
        shader.bind()
        if (!isReady) {
            setUniformf()
            captureScreen()
            bindScreenTexture()
        }
        draw()
    }

    init {
        Events.run(EventType.ResetEvent::class.java) {
            disposeBuffer()
            meshMap.values.forEach { it.mesh.dispose() }
            meshMap.clear()
            meshVerticesCache.clear()
            lifeCycle.clear()
            isReady = false
            time = 0f
        }
        Events.on(EventType.ResizeEvent::class.java) { disposeBuffer() }
        Events.run(EventType.Trigger.update) {
            lifeCycleUpdate()
            val iterator = meshMap.iterator()
            while (iterator.hasNext()) {
                val (mesh, _) = iterator.next()
                if (getBuildIsAdded(mesh)) {
                    remove(mesh)
                }
            }
        }
    }
}

fun Color.abgr8888ToColor(value: Int): Color {
    val a = (value ushr 24 and 0xff) / 255f
    val b = (value ushr 16 and 0xff) / 255f
    val g = (value ushr 8 and 0xff) / 255f
    val r = (value and 0xff) / 255f

    return this.set(r, g, b, a)
}
