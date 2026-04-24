package voidshield.shader.v2

import arc.Core
import arc.graphics.gl.Shader
import arc.util.Time
import voidshield.shader.ShaderManage.getShaderFi

object DefaultShader: Shader(getShaderFi("screenspace.vert"), getShaderFi("v2Test.frag")) {
    override fun apply() {
        setUniformf("u_time", Time.time)
        setUniformf("u_resolution", Core.graphics.width.toFloat(), Core.graphics.height.toFloat())
        setUniformf("u_campos", Core.camera.position.x, Core.camera.position.y)
    }
}