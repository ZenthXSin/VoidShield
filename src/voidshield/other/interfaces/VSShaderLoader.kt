package voidshield.other.interfaces

import arc.graphics.Mesh
import arc.graphics.gl.Shader
import arc.util.Disposable

interface VSShaderLoader: Disposable {
    var shader: Shader
    var meshMap: HashMap<String, MeshData>

    data class MeshData(val mesh: Mesh, val type: Int, val onRender: (() -> Unit)? = null)

    fun update(newMesh: Mesh, id: String, type: Int, set: () -> Unit = {}) {
        // 正确释放旧资源
        meshMap[id]?.mesh?.dispose()

        meshMap[id] = MeshData(newMesh, type, set)
    }

    fun update(id: String, floatArray: FloatArray) {
        meshMap[id]?.mesh?.setVertices(floatArray)
    }

    fun draw() {
        meshMap.forEach { (_, data) ->
            data.onRender?.invoke()  // 每帧应用渲染设置
            data.mesh.render(shader, data.type)
        }
    }

    fun remove(id: String) {
        meshMap[id]?.mesh?.dispose()  // 正确释放
        meshMap.remove(id)
    }

    override fun dispose() {
        meshMap.values.forEach { it.mesh.dispose() }
        meshMap.clear()
        shader.dispose()
    }

    fun setUniformf()

    fun use() {
        shader.bind()
        setUniformf()
        draw()
    }
}