package voidshield.shader.v1

/**
 * 继承重写 DefaultShader
 * 功能：全屏单着色器 + 仅Mesh区域显示着色器
 */
open class ScreenShader : DefaultShader() {
    var mode: ShaderMode = ShaderMode.MeshOnly
    override fun use() {
        when (mode) {
            ShaderMode.MeshOnly -> drawMeshOnly()
            ShaderMode.FullScreen -> drawFullScreen()
        }
    }
    open fun drawFullScreen() {

    }
    open fun drawMeshOnly() {
        super.use()
    }

    override fun setUniformf() {
        when (mode) {
            ShaderMode.MeshOnly -> super.setUniformf()
            ShaderMode.FullScreen -> {

            }
        }
    }
}

enum class ShaderMode{
    FullScreen,
    MeshOnly
}