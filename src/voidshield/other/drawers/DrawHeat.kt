package voidshield.other.drawers

import arc.Core
import arc.graphics.*
import arc.graphics.g2d.*
import arc.util.*
import mindustry.entities.units.*
import mindustry.gen.*
import mindustry.world.*
import mindustry.world.draw.DrawBlock
import voidshield.world.blocks.HeatBlock

class DrawHeat : DrawBlock() {  // 建议改名，因为是热特效绘制
    var suffix: String = "-heat"
    var color: Color = Color.red
    var region: TextureRegion? = null

    override fun draw(build: Building) {
        // 安全类型转换
        val heatBuild = build as? HeatBlock.HeatBuild ?: return
        val heatBlock = build.block as? HeatBlock ?: return
        // 计算热透明度（归一化到 0-1）
        val alpha = (heatBuild.temperature / heatBlock.maxTemperature).coerceIn(0f, 1f)
        if (alpha <= 0.001f) return  // 温度太低不绘制

        Draw.color(color)
        Draw.alpha(alpha / 2f)
        Draw.rect(region, build.x, build.y, build.drawrot())
        Draw.color() // 重置颜色
    }

    override fun load(block: Block) {
        region = Core.atlas.find(block.name + suffix)
    }

    override fun drawPlan(block: Block, plan: BuildPlan, list: Eachable<BuildPlan>) {
        // 蓝图预览通常不显示热特效，或保持基础显示
    }

    override fun icons(block: Block): Array<TextureRegion> {
        return arrayOf(block.region)
    }
}
