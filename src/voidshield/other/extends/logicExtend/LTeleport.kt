package voidshield.other.extends.logicExtend

import arc.math.Angles
import arc.scene.ui.layout.Table
import mindustry.core.World
import mindustry.gen.LogicIO
import mindustry.gen.Unit
import mindustry.logic.*
import voidshield.entities.abilities.TeleportAbility

class LTeleport {
    class TeleportStatement : LStatement() {
        var x = "0"
        var y = "0"
        var range = "0"

        override fun build(table: Table) {
            table.add("x ")
            field(table, x) { str -> x = str }.width(100f)
            table.add("y ")
            field(table, y) { str -> y = str }.width(100f)
            row(table)
            table.add("range ")
            field(table, range) { str -> range = str }.width(100f)
        }

        override fun build(builder: LAssembler): LExecutor.LInstruction {
            return TeleportI(
                builder.`var`(x),
                builder.`var`(y),
                builder.`var`(range)
            )
        }

        override fun category(): LCategory {
            return LCategory.unit
        }

        override fun write(builder: StringBuilder) {
            LEExtend.appendLStmt(builder, "teleport", x, y, range)
        }

        override fun copy(): LStatement? {
            val build = StringBuilder()
            write(build)
            val read = LAssembler.read(build.toString(), true)
            return if (read.size == 0) null else read.first() as TeleportStatement
        }

        companion object {
            @JvmStatic
            fun create() {
                LAssembler.customParsers.put("teleport") { params ->
                    val stmt = TeleportStatement()
                    if (params.size >= 2) stmt.x = params[1]
                    if (params.size >= 3) stmt.y = params[2]
                    if (params.size >= 4) stmt.range = params[3]
                    stmt.afterRead()  // 调用父类的后处理
                    stmt
                }

                LogicIO.allStatements.add { TeleportStatement() }
            }
        }
    }

    class TeleportI(
        var p1: LVar,
        var p2: LVar,
        var range: LVar
    ) : LExecutor.LInstruction {
        override fun run(exec: LExecutor) {
            val unit = exec.unit.obj() as? Unit ?: return
            if (unit.isPlayer) return

            val x = LEExtend.safeToString(p1).toFloatOrNull() ?: World.unconv(p1.numf())
            val y = LEExtend.safeToString(p2).toFloatOrNull() ?: World.unconv(p2.numf())
            if (x == 0f && y == 0f) return

            val worldX = x * 8f
            val worldY = y * 8f

            // 传送能力
            unit.abilities.forEach {
                if (it is TeleportAbility) {
                    if (it.isCooldown) return@forEach
                    it.teleport(worldX, worldY, LEExtend.safeToString(range).toFloat() * 8f, unit)
                    val targetAngle = Angles.angle(unit.x, unit.y, worldX, worldY)
                    unit.lookAt(targetAngle)
                }
            }
        }
    }
}
