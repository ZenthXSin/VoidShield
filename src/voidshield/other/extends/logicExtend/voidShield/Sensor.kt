package voidshield.other.extends.logicExtend.voidShield

import arc.scene.ui.Button
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import mindustry.gen.LogicIO
import mindustry.logic.*
import mindustry.ui.Styles
import voidshield.other.VsVars
import voidshield.other.extends.logicExtend.*
import voidshield.world.blocks.VelumSolvent.VelumSolventBuild
import mindustry.core.World

class VSSensor : LStatement() {

    var vars: MutableList<String> = mutableListOf()
    var type: SensorMode = SensorMode.Effect

    init {
        type.params.forEach {
            vars += it
        }
    }

    override fun build(table: Table) {
        rebuild(table)
    }

    fun rebuild(table: Table) {
        table.clearChildren()

        table.button({ b: Button? ->
            b!!.label { type.name }
            b.clicked {
                showSelect(b, SensorMode.all, type, { t: SensorMode ->
                    type = t
                    rebuild(table)
                }, 2, { cell: Cell<*>? -> cell!!.size(100f, 50f) })
            }
        }, Styles.logict, {}).size(120f, 40f).color(table.color).left().padLeft(2f)

        when (type) {
            SensorMode.Effect -> {
                table.add(" x ")
                field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                table.add(" y ")
                row(table)
                field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                table.add(" sensors ")
                field(table, vars[2]) { str -> vars[2] = str }.width(80f)
                table.add(" out ")
                field(table, vars[3]) { str -> vars[3] = str }.width(80f)
            }

            SensorMode.Contains -> {
                table.add(" x ")
                field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                table.add(" y ")
                row(table)
                field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                table.add(" sensors ")
                field(table, vars[2]) { str -> vars[2] = str }.width(80f)
                table.add(" out ")
                field(table, vars[3]) { str -> vars[3] = str }.width(80f)
            }
        }
        table.left()


    }

    override fun build(builder: LAssembler): LExecutor.LInstruction {
        val ret: MutableList<LVar> = mutableListOf()
        for (i in vars) {
            ret += builder.`var`(i)
        }
        return SensorI(ret, type)
    }

    override fun category(): LCategory {
        return VsVars.logicCategory.voidShield
    }

    override fun write(builder: StringBuilder) {
        builder.append("VSSensor")
        builder.append(" ").append(type.name)
        for (s in vars) {
            builder.append(" ").append(s)
        }

    }

    override fun copy(): LStatement? {
        val build = StringBuilder()
        write(build)
        val read = LAssembler.read(build.toString(), true)
        return if (read.size == 0) null else read.first() as VSSensor
    }

    companion object {
        @JvmStatic
        fun create() {
            LAssembler.customParsers.put("VSSensor") { params ->
                val stmt = VSSensor()
                stmt.type = SensorMode.valueOf(params[1])


                for ((i, element) in params.withIndex()) {

                    //Log.info("$i = ${params[i]}")

                    if (i >= 2 && element != null) {
                        stmt.vars[i - 2] = element
                    }

                }

                stmt.afterRead()  // 调用父类的后处理
                stmt
            }

            LogicIO.allStatements.add { VSSensor() }
        }
    }
}

class SensorI(
    var lVars: MutableList<LVar>, var mode: SensorMode
) : LExecutor.LInstruction {
    override fun run(exec: LExecutor) {
        when (mode) {
            SensorMode.Effect -> {
                var p1 = lVars[0]
                val p2 = lVars[1]
                val build1 = lVars[2]
                val out = lVars[3]
                val build = build1.building() as? VelumSolventBuild ?: return

                if (build.efficiency == 0f) {
                    out.setobj("方块未启动")
                    return
                }

                val x = World.unconv(LEExtend.safeToString(p1).toFloatOrNull() ?: 0f)
                val y = World.unconv(LEExtend.safeToString(p2).toFloatOrNull() ?: 0f)

                //Log.info(VsVars.world.spaceDate.circles)
                out.setnum(VsVars.world.spaceDate.getTotalEffect(x, y).toDouble())

            }

            SensorMode.Contains -> {
                var p1 = lVars[0]
                val p2 = lVars[1]
                val build1 = lVars[2]
                val out = lVars[3]
                val build = build1.building() as? VelumSolventBuild ?: return

                if (build.efficiency == 0f) {
                    out.setobj("方块未启动")
                    return
                }

                val x = World.unconv(LEExtend.safeToString(p1).toFloatOrNull() ?: 0f)
                val y = World.unconv(LEExtend.safeToString(p2).toFloatOrNull() ?: 0f)
                out.setbool(VsVars.world.spaceDate.contains(x, y))

            }
        }
    }
}