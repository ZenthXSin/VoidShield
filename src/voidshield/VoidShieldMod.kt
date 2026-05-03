package voidshield

import arc.Events
import arc.func.Prov
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.gl.Shader
import arc.util.Log
import mindustry.Vars
import mindustry.content.Planets
import mindustry.game.EventType
import mindustry.graphics.Layer
import mindustry.graphics.Shaders
import mindustry.graphics.g3d.HexMesh
import mindustry.graphics.g3d.SunMesh
import mindustry.mod.Mod
import mindustry.type.Planet
import voidshield.content.VSBlocks
import voidshield.other.VsVars
import voidshield.other.extends.categoryExtend.applyCategory
import voidshield.other.extends.logicExtend.LTeleport
import voidshield.other.extends.logicExtend.voidShield.VSControl
import voidshield.other.extends.logicExtend.voidShield.VSSensor
import voidshield.shader.ShaderManage
import voidshield.shader.v2.TestV2

class VoidShieldMod : Mod() {

    override fun init() {
        super.init()
        VsVars.load()
        VsVars.modName = Vars.mods.getMod(this::class.java).name
        LTeleport.TeleportStatement.create()
        VSSensor.create()
        VSControl.create()
        applyCategory(VSBlocks.heat)
        applyCategory(VSBlocks.voidShield)
        Log.info("[Void Shield] Mod initialized")

        val shader = TestV2()

        //着色器测试

        Events.run(EventType.Trigger.draw) {
            Draw.drawRange(Layer.shields + 5f,0.01f, {
                Vars.renderer.effectBuffer.begin()
            }) {
                Vars.renderer.effectBuffer.end()
                Vars.renderer.effectBuffer.blit(shader)
            }
        }
    }

    override fun loadContent() {
        VSBlocks.load()

        Log.info("[Void Shield] Content loaded successfully")
    }
}
