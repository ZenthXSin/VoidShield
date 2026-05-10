package voidshield.shader.v2

import arc.Core
import arc.Events
import arc.assets.loaders.TextureLoader.TextureParameter
import arc.func.Cons
import arc.graphics.Texture
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.g2d.Draw
import arc.graphics.gl.Shader
import arc.scene.ui.layout.Scl
import arc.util.Time
import mindustry.Vars
import mindustry.game.EventType
import mindustry.graphics.Layer
import voidshield.shader.ShaderManage

class V2Shaders {
    companion object {
        val shaderList: MutableList<LoadShader> = mutableListOf()
    }

    open class LoadShader(vertName: String = "screenspace.vert", fragName: String, var layzer: Float,var run: Shader.() -> Unit? = {null}): Shader(
        ShaderManage.getShaderFi(vertName)
        , ShaderManage.getShaderFi(fragName)
    ) {
        init {
            shaderList.add(this)
        }

        override fun apply() {
            super.apply()
            if (run(this) == null) {
                this.setUniformf("u_dp", Scl.scl(1.0f))
                this.setUniformf("u_time", Time.time / Scl.scl(1.0f) / 5f)
                this.setUniformf(
                    "u_offset",
                    Core.camera.position.x - Core.camera.width / 2.0f,
                    Core.camera.position.y - Core.camera.height / 2.0f
                )
                this.setUniformf("u_texsize", Core.camera.width, Core.camera.height)
                this.setUniformf("u_invsize", 1.0f / Core.camera.width, 1.0f / Core.camera.height)
                this.setUniformf("u_quality", 0.5f)
            }
        }
    }

    init {
        //着色器加载
        LoadShader(fragName = "voidShield.frag", layzer = Layer.shields + 5f)
        //渲染加载
        shaderList.forEach {
            Events.run(EventType.Trigger.draw) {
                Draw.drawRange(it.layzer,1f, {
                    Vars.renderer.effectBuffer.begin()
                }) {
                    Vars.renderer.effectBuffer.end()
                    Vars.renderer.effectBuffer.blit(it)
                }
            }
        }
    }
}