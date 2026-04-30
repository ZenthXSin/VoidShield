package voidshield.other

import arc.Core
import arc.Events
import arc.assets.loaders.TextureLoader.TextureParameter
import arc.func.Cons
import arc.graphics.Texture
import arc.graphics.Texture.TextureFilter
import arc.graphics.Texture.TextureWrap
import arc.graphics.g2d.Draw
import arc.math.geom.Rect
import arc.util.Log
import mindustry.Vars
import mindustry.game.EventType
import mindustry.graphics.Layer
import mindustry.graphics.Pal
import mindustry.logic.LCategory
import voidshield.entities.skills.BlackHole
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.extends.other.Settings
import voidshield.shader.v1.DefaultShader
import voidshield.shader.v1.JsonShaderLoader
import voidshield.shader.v1.TestShader
import voidshield.world.effects.effect.TeleportEffect.loadJson
import voidshield.world.shaders.BlackHoleShader
import voidshield.world.shaders.HeatShader

object VsVars {
    var world = World()

    var modName = "voidshield"

    var shaders = Shaders()

    var logicCategory: LogicCategory = LogicCategory()

    var settings: Settings = Settings()

    fun load() {
        Log.info("[VsVars] VsVars Loading")
        world.load()
        shaders.load()
        JsonShaderLoader.loadJson(modName)
        Events.run(EventType.ClientLoadEvent::class.java) {
            loadJson(modName)
            settings.init()
        }
    }

}

class Skills {
    var blackHole: BlackHole = BlackHole()
}

class Shaders {
    var shaders: MutableMap<String, DefaultShader> = mutableMapOf()
    var heatShader: HeatShader = addShader("HeatShader", HeatShader()) as HeatShader
    var blackHole: BlackHoleShader = addShader("BlackHole", BlackHoleShader()) as BlackHoleShader
    var spaceDistortion: DefaultShader = addShader("SpaceDistortion", DefaultShader(frag = "spaceDistortion"))
    var defaultShader: DefaultShader = addShader("DefaultShader", DefaultShader())
    fun addShader(name: String, shader: DefaultShader): DefaultShader {
        shaders[name] = shader
        return shader
    }

    fun load() {
        Events.run(EventType.Trigger.postDraw) {
            Draw.draw(Layer.effect) {
                shaders.forEach { (_, shader) ->
                    if (shader.meshMap.isNotEmpty()) {
                        shader.use()
                    }
                }
                TestShader.use()
            }
        }
    }

}

class World {
    var spaceDate: SpaceDate = SpaceDate()
    fun load() {
        Events.on(EventType.WorldLoadBeginEvent::class.java) {
            spaceDate.clear()
        }
        Events.on(EventType.WorldLoadEndEvent::class.java) {
            spaceDate.updateBounds(Rect(0f, 0f, Vars.world.width() * 8f, Vars.world.height() * 8f))
            Log.info("[World] SpaceDate bounds set to: ${spaceDate.bounds}")
        }
    }
}

class LogicCategory {
    var voidShield = LCategory("VoidShield", Pal.accent, Core.atlas.getDrawable("${VsVars.modName}-maxVoidShield"))
}