package voidshield.shader.v2

import arc.Core
import arc.assets.loaders.TextureLoader.TextureParameter
import arc.func.Cons
import arc.graphics.Texture
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.gl.Shader
import arc.scene.ui.layout.Scl
import arc.util.Time
import voidshield.shader.ShaderManage

open class TestV2 : Shader(
    ShaderManage.getShaderFi("screenspace.vert")
    , ShaderManage.getShaderFi("v2Test.frag")
) {
    override fun apply() {
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

        Core.assets.load("sprites/space.png", Texture::class.java, object : TextureParameter() {
            init {
                this.magFilter = TextureFilter.linear
                this.minFilter = TextureFilter.mipMapLinearLinear
                this.wrapV = TextureWrap.mirroredRepeat
                this.wrapU = this.wrapV
                this.genMipMaps = true
            }
        }).loaded = Cons { t: Texture ->
            t.bind(5)
            this.setUniformi("u_background", 5)
        }
    }
}