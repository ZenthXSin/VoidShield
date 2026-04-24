package voidshield.world.effects

import voidshield.world.effects.effect.TeleportEffect.endTeleport
import voidshield.world.effects.effect.TeleportEffect.readyTeleport

/**
 * 跃迁特效 - 纯粒子效果（无着色器）
 */
object VSEffects {
    val readyTeleportEffect = readyTeleport()
    val endTeleportEffect = endTeleport()
}
