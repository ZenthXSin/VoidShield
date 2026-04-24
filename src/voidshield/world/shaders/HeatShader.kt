package voidshield.world.shaders

import voidshield.other.VsVars
import voidshield.shader.v1.ScreenShader
import voidshield.shader.ShaderManage

class HeatShader: ScreenShader() {
    init {
        shader = ShaderManage.getShader("default", "heat", VsVars.modName)
    }
}