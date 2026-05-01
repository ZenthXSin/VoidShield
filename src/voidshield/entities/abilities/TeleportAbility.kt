package voidshield.entities.abilities

import arc.Core
import arc.audio.Sound
import arc.math.Angles
import arc.math.Mathf
import arc.math.geom.Vec2
import arc.scene.ui.layout.Table
import arc.struct.Seq
import arc.util.Time
import mindustry.Vars
import mindustry.content.Bullets
import mindustry.content.Fx
import mindustry.content.StatusEffects
import mindustry.entities.Effect
import mindustry.entities.abilities.Ability
import mindustry.entities.bullet.BulletType
import mindustry.gen.Bullet
import mindustry.gen.Groups
import mindustry.gen.Sounds
import mindustry.gen.Unit
import mindustry.graphics.Pal
import mindustry.type.StatusEffect
import mindustry.ui.Bar
import voidshield.world.effects.VSEffects
import voidshield.world.effects.effect.TeleportEffect

class TeleportData {
    var range: Float = 200f
    var cooldown: Float = 60f
    var readyTime: Float = 0f
    var endTime: Float = 0f
    var useDefaultEffect: Boolean = true

    //    var auto: Boolean = false
//    var autoConfig = AutoConfig()
    var effects: Seq<TeleportEffectData> = Seq()
    var sounds: Seq<TeleportSoundData> = Seq()
    var bullets: Seq<TeleportBulletData> = Seq()
    var statusEffect: StatusEffect = StatusEffects.none
    var statusTime = 0f
}

class TeleportEffectData {
    var effect: Effect = Fx.none
    var startTime: Float = 0f
}

class TeleportBulletData {
    var bullet: BulletType = Bullets.spaceLiquid
    var startTime: Float = 0f
}

class TeleportSoundData {
    var sound: Sound = Sounds.none
    var volume: Float = 1f
    var startTime: Float = 0f
}

//class AutoConfig {
//    var range: Float = 20f
//    var autoStop: Boolean = false
//    var autoStopRange: Float = 20f
//}

class TeleportAbility : Ability {
    // 配置参数
    var data: TeleportData = TeleportData().apply {
        readyTime = 290f
        endTime = 130f
        effects.add(TeleportEffectData().apply {
            effect = VSEffects.readyTeleportEffect
            startTime = 0f
        })
        effects.add(TeleportEffectData().apply {
            effect = VSEffects.endTeleportEffect
            startTime = 291f
        })
    }

    // 状态
    var charging: Boolean = false
    var isCooldown: Boolean = true
    var targetX: Float = 0f
    var targetY: Float = 0f
    var startTeleport = false
    var time: Float = 0f
    var runEffect: Seq<Effect> = Seq()
    var runBullet: Seq<BulletType> = Seq()

    var readyState: TeleportEffect.ReadyState = TeleportEffect.ReadyState()
    var endState: TeleportEffect.EndState = TeleportEffect.EndState()
    var ready: Boolean = false
    var touch: Boolean = false
    var coolDown: Float = 0f
    var startRotate = false

    constructor()

    override fun addStats(t: Table) {
        t.table {
            t.row()
            it.add("跃迁范围: ${data.range} 格").row()
            it.add("冷却: ${data.cooldown / 60} 秒").left().row()
        }
    }

    override fun displayBars(unit: Unit, bars: Table) {
        super.displayBars(unit, bars)
        bars.add(Bar("跃迁充能", Pal.accent) { coolDown / data.cooldown })
    }

    fun teleport(x: Float, y: Float, range: Float, unit: Unit) {
        if (!isCooldown && !charging) {
            val angleToMouse = Angles.angle(unit.x, unit.y, x, y)
            if (Angles.angle(unit.x, unit.y, x, y) <= data.range && Angles.within(unit.rotation, angleToMouse, 5f)) {
                val newVec2 = shortenLineVec(Vec2(unit.x, unit.y), Vec2(x, y), range)
                targetX = newVec2.x
                targetY = newVec2.y
                charging = true
                unit.apply(data.statusEffect, data.statusTime)
            }
        }
    }

    override fun update(unit: Unit) {
        super.update(unit)
        if (startRotate) {
            val mouseX = unit.player.mouseX
                val mouseY = unit.player.mouseY
                unit.lookAt(Angles.angle(unit.x, unit.y, mouseX, mouseY))
                val angleToMouse = Angles.angle(unit.x, unit.y, mouseX, mouseY)
                if (Angles.within(unit.rotation, angleToMouse, 5f)) {
                    targetX = mouseX
                    targetY = mouseY
                    charging = true
                    unit.apply(data.statusEffect, data.statusTime)
                    startRotate = false
                }
            }
        if (charging) {
            time += Time.delta
            if (!startTeleport) {
                if (data.useDefaultEffect) {
                    // 创建新的状态对象
                    readyState = TeleportEffect.ReadyState()
                    endState = TeleportEffect.EndState()
                }
                if (data.useDefaultEffect) {
                    VSEffects.readyTeleportEffect.at(unit.x, unit.y, unit.rotation, readyState)
                    Time.run(291f) {
                        VSEffects.endTeleportEffect.at(unit.x, unit.y, unit.rotation, endState)
                    }
                }
                startTeleport = true
                Time.run(data.readyTime) {
                    unit.x(targetX)
                    unit.y(targetY)
                    if (unit.isPlayer) Core.camera.position.set(unit)
                }
                Time.run(data.readyTime + data.endTime) {
                    isCooldown = true
                    charging = false
                    startTeleport = false
                    coolDown = 0f
                    time = 0f
                    runEffect = Seq()
                }
            }
            if (!data.useDefaultEffect) {
                data.effects.forEach { i ->
                    if (runEffect.contains(i.effect)) return
                    if (i.startTime <= time) {
                        runEffect.add(i.effect)
                        i.effect.at(unit, unit.rotation)
                    }
                }
                data.bullets.forEach { i ->
                    if (runBullet.contains(i.bullet)) return
                    if (i.startTime <= time) {
                        runBullet.add(i.bullet)
                        i.bullet.create(unit,unit.team(),unit.x,unit.y,unit.rotation)
                    }
                }
                data.sounds.forEach { i ->
                    if (i.startTime <= time && (time - i.startTime) <= 5) {
                        Vars.control.sound.loop(i.sound, unit, i.volume)
                    }
                }
            }
        }
        if (isCooldown && coolDown >= data.cooldown) {
            isCooldown = false
            coolDown = data.cooldown
        }
        if (isCooldown) {
            coolDown += Time.delta
        }
        if (!charging && unit.isPlayer && unit.isShooting && !isCooldown && (Mathf.dst(
                unit.x,
                unit.y,
                targetX,
                targetY
            ) <= data.range * 8)
        ) {
            if (ready && !touch) {
                ready = false
                startRotate = true
            }
            if (!touch) ready = true
            touch = true
            Time.run(120f) {
                ready = false
            }
        } else {
            touch = false
        }
//        if (data.auto) {
//            if (!charging && unit.isShooting && !unit.isPlayer && !isCooldown) {
//                if (Groups.unit.none {
//                        Angles.angle(
//                            it.x,
//                            it.y,
//                            unit.x,
//                            unit.y
//                        ) <= data.autoConfig.autoStopRange * 8 && it.team != unit.team && data.autoConfig.autoStop
//                    }) {
//                    val x = unit.aimX
//                    val y = unit.aimY
//                    if (Angles.angle(unit.x, unit.y, x, y) - data.autoConfig.range <= data.range) {
//                        val newVec2 = shortenLineVec(Vec2(unit.x, unit.y), Vec2(x,y),data.autoConfig.range * 8)
//                        targetX = newVec2.x
//                        targetY = newVec2.y
//                        charging = true
//                        unit.apply(data.statusEffect,data.statusTime)
//                    }
//                }
//            }
//        }
    }
}

fun shortenLineVec(start: Vec2, end: Vec2, n: Float): Vec2 {
    val length = start.dst(end)

    if (n >= length) {
        return start.cpy()  // 返回起点副本
    }

    // 从 end 向 start 方向移动 n 距离
    return end.cpy().sub(start).setLength(length - n).add(start)
}