package voidshield.shader

import arc.Core
import arc.files.Fi
import arc.graphics.gl.Shader
import arc.util.Log
import mindustry.Vars
import voidshield.other.VsVars

object ShaderManage {

    /**
     * 获取shader文件，优先从mod内查找，其次尝试原版/internal路径。
     *
     * 查找顺序：
     * 1. modRoot/shaders/name
     * 2. internal shaders/name
     * 3. Vars.tree.get("shaders/name")（如可用）
     */
    @JvmStatic
    fun getShaderFi(fileName: String, modName: String = VsVars.modName): Fi? {
        // 1. 优先查找mod目录
        val mod = Vars.mods.locateMod(modName)
        if (mod == null) {
            Log.err("ShaderManage: mod not found: @", modName)
        } else {
            val modFi = mod.root.child("shaders").child(fileName)
            if (modFi.exists()) {
                return modFi
            } else {
                Log.debug("ShaderManage: shader file not found in mod '@': @", modName, modFi.path())
            }
        }

        // 2. 尝试 internal/shaders
        val internalFi = Core.files.internal("shaders/$fileName")
        if (internalFi.exists()) {
            return internalFi
        }

        // 3. 尝试 Vars.tree（兼容Mindustry资源树）
        return try {
            val treeFi = Vars.tree.get("shaders/$fileName")
            if (treeFi != null && treeFi.exists()) treeFi else null
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * 读取shader源码文件内容
     */
    @JvmStatic
    fun readShaderSource(fileName: String, modName: String = VsVars.modName): String {
        val fi = getShaderFi(fileName, modName)
            ?: throw IllegalArgumentException("Shader source file not found: $fileName, mod=$modName")
        return fi.readString()
    }

    /**
     * 获取shader。
     *
     * @param vertName 顶点着色器名，不带 .vert
     * @param fragName 片元着色器名，不带 .frag
     * @param modName mod名
     */
    @JvmStatic
    fun getShader(vertName: String, fragName: String, modName: String = VsVars.modName): Shader {
        val vertFile = "$vertName.vert"
        val fragFile = "$fragName.frag"

        val vertFi = getShaderFi(vertFile, modName)
        val fragFi = getShaderFi(fragFile, modName)

        require(vertFi != null) {
            "Vertex shader not found: $vertFile (mod=$modName)"
        }
        require(fragFi != null) {
            "Fragment shader not found: $fragFile (mod=$modName)"
        }

        return try {
            val shader = Shader(vertFi, fragFi)
            Log.info("Shader loaded: vert=@, frag=@, mod=@", vertFi.path(), fragFi.path(), modName ?: "internal")
            shader
        } catch (e: Throwable) {
            Log.err(e)
            throw RuntimeException(
                "Failed to compile shader. vert=${vertFi.path()}, frag=${fragFi.path()}, mod=$modName",
                e
            )
        }
    }
}
