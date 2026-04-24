package voidshield.world.shaders

import voidshield.other.VsVars
import voidshield.shader.v1.DefaultShader
import voidshield.shader.ShaderManage

class BlackHoleShader : DefaultShader() {
    init {
        shader = ShaderManage.getShader("default", "blackHole", VsVars.modName)
    }
}
