package voidshield.core.drawers

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.util.Time
import mindustry.gen.Building
import mindustry.world.Block
import mindustry.world.draw.DrawBlock

/**
 * 能量球/护盾场绘制器
 * 用于绘制单位或建筑周围的能量场效果
 */
class DrawEnergyField : DrawBlock() {

    // ========== 配置参数 ==========

    /** 能量球半径（像素） */
    var radius: Float = 80f

    /** 基础颜色 */
    var color: Color = Color.valueOf("4fc3f7")

    /** 发光颜色（外层光环） */
    var glowColor: Color = Color.valueOf("81d4fa")

    /** 是否启用脉冲动画 */
    var pulseEnabled: Boolean = true

    /** 脉冲速度（越大越快） */
    var pulseSpeed: Float = 3f

    /** 脉冲强度（0-1之间，控制振幅） */
    var pulseIntensity: Float = 0.15f

    /** 基础透明度 */
    var baseAlpha: Float = 0.6f

    /** 填充层透明度系数 */
    var fillAlphaMultiplier: Float = 0.4f

    /** 边框线条粗细 */
    var strokeWidth: Float = 2.5f

    /** 是否绘制多层光环 */
    var multiLayerEnabled: Boolean = true

    /** 光环层数 */
    var layerCount: Int = 3

    /** 层间距 */
    var layerSpacing: Float = 15f

    /** 是否只在高效工作时显示 */
    var requireEfficiency: Boolean = true

    /** 最小效率阈值 */
    var minEfficiency: Float = 0.01f

    // ========== 核心绘制方法 ==========

    override fun draw(build: Building) {
        // 效率检查
        if (requireEfficiency && build.efficiency < minEfficiency) return

        // 计算基础透明度
        val efficiencyAlpha = if (requireEfficiency) build.efficiency else 1f

        // 计算脉冲效果
        val pulse = if (pulseEnabled) {
            Mathf.sin(Time.time * pulseSpeed) * pulseIntensity + (1f - pulseIntensity)
        } else {
            1f
        }

        val alpha = (efficiencyAlpha * baseAlpha * pulse).coerceIn(0f, 1f)

        // 如果透明度过低则不绘制
        if (alpha < 0.01f) return

        // 绘制能量球
        if (multiLayerEnabled) {
            drawMultiLayer(build.x, build.y, alpha)
        } else {
            drawSingleLayer(build.x, build.y, alpha)
        }

        // 重置绘图状态
        Draw.reset()
    }

    /**
     * 绘制单层能量球
     */
    private fun drawSingleLayer(x: Float, y: Float, alpha: Float) {
        // 填充层
        Draw.color(color)
        Draw.alpha(alpha * fillAlphaMultiplier)
        Fill.circle(x, y, radius)

        // 边框层
        Draw.color(glowColor)
        Draw.alpha(alpha)
        Lines.stroke(strokeWidth)
        Lines.circle(x, y, radius)
    }

    /**
     * 绘制多层能量球（更炫酷的效果）
     */
    private fun drawMultiLayer(x: Float, y: Float, baseAlpha: Float) {
        for (i in 0 until layerCount) {
            val layerRadius = radius + i * layerSpacing
            val layerAlpha = (baseAlpha - i * 0.15f).coerceAtLeast(0f)

            if (layerAlpha < 0.01f) break

            Draw.color(if (i == 0) color else glowColor)
            Draw.alpha(layerAlpha * if (i == 0) fillAlphaMultiplier else 0.7f)

            if (i == 0) {
                // 最内层：填充圆
                Fill.circle(x, y, layerRadius)
            } else {
                // 外层：边框圆
                Lines.stroke(strokeWidth * (1f - i * 0.2f))
                Lines.circle(x, y, layerRadius)
            }
        }
    }

    // ========== 资源加载 ==========

    override fun load(block: Block) {
        // 能量球使用程序化绘制，无需加载贴图
    }

    // ========== 蓝图预览 ==========

    override fun drawPlan(block: Block, plan: mindustry.entities.units.BuildPlan, list: arc.util.Eachable<mindustry.entities.units.BuildPlan>) {
        // 蓝图预览时显示半透明的能量球
        Draw.color(color)
        Draw.alpha(0.3f)
        Lines.stroke(1f)
        Lines.circle(plan.drawx(), plan.drawy(), radius)
        Draw.reset()
    }

    // ========== 图标 ==========

    override fun icons(block: Block): Array<arc.graphics.g2d.TextureRegion> {
        return arrayOf(block.region)
    }
}
