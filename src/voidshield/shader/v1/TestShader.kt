package voidshield.shader.v1

import arc.Core
import arc.assets.loaders.TextureLoader.TextureParameter
import arc.func.Cons
import arc.graphics.Texture
import voidshield.other.VsVars
import voidshield.shader.ShaderManage

object TestShader : DefaultShader() {
    init {
        shader = ShaderManage.getShader("default", "blackHole", VsVars.modName)
    }

    override fun use() {
        //screenBuffer = Vars.renderer.effectBuffer
        shader.bind()
        setUniformf()
        Core.assets.load("sprites/space.png", Texture::class.java, object : TextureParameter() {
            init {
                magFilter = Texture.TextureFilter.linear
                minFilter = Texture.TextureFilter.mipMapLinearLinear
                wrapU = Texture.TextureWrap.mirroredRepeat
                wrapV = Texture.TextureWrap.mirroredRepeat
                genMipMaps = true
            }
        }).loaded = Cons {
            bindTexture(it)
        }
        captureScreen()
        bindScreenTexture()
        draw()
    }
}
