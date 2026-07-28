package voidshield.shader

import arc.util.Log
import arc.util.serialization.Json
import mindustry.Vars
import voidshield.core.VsVars
import voidshield.shader.v1.DefaultShader
import voidshield.shader.v2.V2Shaders

class ShaderConfig {
    var name: String = ""
    var frag: String = ""
    var vert: String = ""
    var version: Int = 1
    var layzer: Float = 1f
}

class ShaderConfigRoot {
    var shaders: MutableList<ShaderConfig> = mutableListOf()
}

object JsonShaderLoader {

    @JvmStatic
    fun loadJson(mod: String) {
        try {
            val modInfo = Vars.mods.locateMod(mod)
            if (modInfo == null) {
                Log.err("[JsonShaderLoader] 未找到mod: @", mod)
                return
            }

            val configFi = modInfo.root.child("content").child("config").child("shadersConfig.json")
            if (!configFi.exists()) {
                Log.info("[JsonShaderLoader] 未找到 shadersConfig.json: @", configFi.path())
                return
            }

            Log.info("[JsonShaderLoader] 加载自定义着色器配置: @", configFi.path())

            val text = configFi.readString()
            val root = Json().fromJson(ShaderConfigRoot::class.java, text)

            if (root == null || root.shaders.isEmpty()) {
                Log.info("[JsonShaderLoader] shadersConfig.json 为空或没有可用着色器配置")
                return
            }

            root.shaders.forEach { conf ->
                Log.info("[JsonShaderLoader] 着色器实现版本: v${conf.version}")
                if (conf.name.isBlank()) {
                    Log.warn("[JsonShaderLoader] 跳过一个着色器配置：name 为空")
                    return@forEach
                }
                if (conf.frag.isBlank()) {
                    Log.warn("[JsonShaderLoader] 跳过着色器 @：frag 为空", conf.name)
                    return@forEach
                }
                if (conf.vert.isBlank()) {
                    Log.warn("[JsonShaderLoader] 跳过着色器 @：vert 为空", conf.name)
                    return@forEach
                }

                try {
                    when (conf.version) {
                        1 -> {
                            val shader = object : DefaultShader() {
                                init {
                                    shader = ShaderManage.getShader(conf.vert, conf.frag, mod)
                                }
                            }

                            VsVars.v1Shaders.shaders[conf.name] = shader
                        }
                        2 -> {
                            V2Shaders.LoadShader(fragName = conf.frag + ".frag", layzer = conf.layzer)
                        }
                    }
                    Log.info("[JsonShaderLoader] 已注册着色器: @ (vert=@, frag=@)", conf.name, conf.vert, conf.frag)
                } catch (e: Exception) {
                    Log.err("[JsonShaderLoader] 加载着色器失败: @ (vert=@, frag=@)", conf.name, conf.vert, conf.frag)
                    Log.err(e)
                }
            }

        } catch (e: Exception) {
            Log.err("[JsonShaderLoader] 尝试加载自定义着色器失败:")
            Log.err(e)
        }
    }
}
