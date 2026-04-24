package voidshield.shader.v1

import arc.util.Log
import arc.util.serialization.Json
import mindustry.Vars
import voidshield.other.VsVars
import voidshield.shader.ShaderManage

class ShaderConfig {
    var name: String = ""
    var frag: String = ""
    var vert: String = ""
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
                Log.err("[JsonShader] 未找到mod: @", mod)
                return
            }

            val configFi = modInfo.root.child("content").child("config").child("shadersConfig.json")
            if (!configFi.exists()) {
                Log.info("[JsonShader] 未找到 shadersConfig.json: @", configFi.path())
                return
            }

            Log.info("[JsonShader] 加载自定义着色器配置: @", configFi.path())

            val text = configFi.readString()
            val root = Json().fromJson(ShaderConfigRoot::class.java, text)

            if (root == null || root.shaders.isEmpty()) {
                Log.info("[JsonShader] shadersConfig.json 为空或没有可用着色器配置")
                return
            }

            root.shaders.forEach { conf ->
                if (conf.name.isBlank()) {
                    Log.warn("[JsonShader] 跳过一个着色器配置：name 为空")
                    return@forEach
                }
                if (conf.frag.isBlank()) {
                    Log.warn("[JsonShader] 跳过着色器 @：frag 为空", conf.name)
                    return@forEach
                }
                if (conf.vert.isBlank()) {
                    Log.warn("[JsonShader] 跳过着色器 @：vert 为空", conf.name)
                    return@forEach
                }

                try {
                    val shader = object : DefaultShader() {
                        init {
                            shader = ShaderManage.getShader(conf.vert, conf.frag, mod)
                        }
                    }

                    VsVars.shaders.shaders[conf.name] = shader
                    Log.info("[JsonShader] 已注册着色器: @ (vert=@, frag=@)", conf.name, conf.vert, conf.frag)
                } catch (e: Exception) {
                    Log.err("[JsonShader] 加载着色器失败: @ (vert=@, frag=@)", conf.name, conf.vert, conf.frag)
                    Log.err(e)
                }
            }

        } catch (e: Exception) {
            Log.err("[JsonShader] 尝试加载自定义着色器失败")
            Log.err(e)
        }
    }
}
