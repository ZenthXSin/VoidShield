package voidshield.other.drawers

import arc.util.Log
import mindustry.gen.Building
import mindustry.world.Block
import mindustry.world.draw.DrawBlock
import voidshield.other.VsVars
import voidshield.other.interfaces.BlockShader
import voidshield.shader.v1.DefaultShader

class DrawShader : DrawBlock(), BlockShader {

    override var hasShader: Boolean = false
    var shaderName: String = "HeatShader"

    var data: ShaderData = ShaderData()

    fun hasMesh(id: String): Boolean = getShader().meshMap.contains(id)

    override fun draw(build: Building) {
        val id = "[${build.id}]"
        val alpha = if (build.shouldConsume()) build.warmup() else 1f
        getShader().setMeshAlpha(id, alpha.coerceIn(0f, 1f))
        if (build.efficiency == 0f) return
        if (!hasMesh(id)) {
            when (data.type) {
                "Circle" -> {
                        getShader().addCircleRegion(id, build.x, build.y, data.r, data.seg)
                }

                 "Quad" -> {
                        getShader().addQuadRegion(id, build.x, build.y, data.width, data.height)
                }

                 "Polygon" -> {
                        getShader().addPolygonRegion(id, build.x, build.y, data.r, data.sides)
                }

                 "Ring" -> {
                        getShader().addRingRegion(id, build.x, build.y, data.innerRadius, data.outerRadius, data.seg)
                }

                 "ScreenQuad" -> {
                        getShader().addScreenQuadRegion(id, build.x, build.y, data.width, data.height)
                }

                 "TexturedRegion" -> {
                        getShader().addTexturedRegionArea(
                            id,
                            build.x,
                            build.y,
                            data.width,
                            data.height,
                            data.u1,
                            data.v1,
                            data.u2,
                            data.v2
                        )
                }

                else -> Log.info("[DrawShader]Unknown shader data: $data")
            }
        }

    }

    fun getShader(): DefaultShader {
        return VsVars.shaders.shaders[shaderName] ?: VsVars.shaders.defaultShader
    }

    override fun setGradient() {
        TODO("Not yet implemented")
    }

    override fun getMeshId(): String {
        TODO("Not yet implemented")
    }

    override fun setShader() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun load(block: Block) {
        Log.info("[${block.name}]加载着色器动画")
    }

}
open class ShaderData {
    var type = ""
    var r: Float = 0f
    var seg: Int = 48
    var width: Float = 0f
    var height: Float = 0f
    var innerRadius: Float = 0f
    var outerRadius: Float = 0f
    var u1: Float = 0f
    var v1: Float = 0f
    var u2: Float = 1f
    var v2: Float = 1f
    var sides: Int = 0
}