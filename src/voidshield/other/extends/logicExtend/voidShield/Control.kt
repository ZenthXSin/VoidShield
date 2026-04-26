package voidshield.other.extends.logicExtend.voidShield

import arc.scene.ui.Button
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import mindustry.gen.Groups
import mindustry.gen.LogicIO
import mindustry.logic.LAssembler
import mindustry.logic.LCategory
import mindustry.logic.LExecutor
import mindustry.logic.LStatement
import mindustry.logic.LVar
import mindustry.ui.Styles
import voidshield.other.VsVars
import voidshield.other.dateTypes.SpaceDate
import voidshield.other.interfaces.BindBuilding
import voidshield.world.blocks.voidshield.CorVacuum
import voidshield.world.blocks.voidshield.MicroVoid
import voidshield.world.blocks.voidshield.VelumSolvent.VelumSolventBuild

class VSControl : LStatement() {

    var vars: MutableList<String> = mutableListOf()
    var type: ControlMode = ControlMode.Default

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
                showSelect(b, ControlMode.all, type, { t: ControlMode ->
                    type = t
                    vars = mutableListOf()
                    type.params.forEach { vars += it }
                    rebuild(table)
                }, 2, { cell: Cell<*>? -> cell!!.size(100f, 50f) })
            }
        }, Styles.logict, {}).size(120f, 40f).color(table.color).left().padLeft(2f)

        when (type) {
            ControlMode.Default -> {
                table.add(" MicroVoid ").left()
                field(table, vars[0]) { str -> vars[0] = str }.width(80f).left()
                row(table)
                table.add(" CorVacuum ").left()
                field(table, vars[1]) { str -> vars[1] = str }.width(80f).left()
                table.row()
                table.add(" x ").left()
                field(table, vars[2]) { str -> vars[2] = str }.width(80f).left()
                table.add(" y ").left()
                field(table, vars[3]) { str -> vars[3] = str }.width(80f).left()
                table.add(" range ").left()
                field(table, vars[4]) { str -> vars[4] = str }.width(80f).left()
            }
            ControlMode.Clear -> {
                table.add(" VelumSolvent ").left()
                field(table, vars[0]) { str -> vars[0] = str }.width(80f).left()
            }
            ControlMode.Bind -> {
                row(table)
                field(table, vars[0]) { str -> vars[0] = str }.width(80f).left()
                table.add(" to ").left()
                field(table, vars[1]) { str -> vars[1] = str }.width(80f).left()
            }
        }
        table.left()


    }

    override fun build(builder: LAssembler): LExecutor.LInstruction {
        val ret: MutableList<LVar> = mutableListOf()
        for (i in vars) {
            ret += builder.`var`(i)
        }
        return ControlI(ret, type)
    }

    override fun category(): LCategory {
        return VsVars.logicCategory.voidShield
    }

    override fun write(builder: StringBuilder) {
        builder.append("VSControl")
        builder.append(" ").append(type.name)
        for (s in vars) {
            builder.append(" ").append(s)
        }

    }

    override fun copy(): LStatement? {
        val build = StringBuilder()
        write(build)
        val read = LAssembler.read(build.toString(), true)
        return if (read.size == 0) null else read.first() as VSControl
    }

    companion object {
        @JvmStatic
        fun create() {
            LAssembler.customParsers.put("VSControl") { params ->
                val stmt = VSControl()
                stmt.type = ControlMode.valueOf(params[1])

                // 关键：根据实际 type 重新初始化 vars
                stmt.vars = mutableListOf()
                stmt.type.params.forEach { stmt.vars += it }

                // 安全赋值
                for ((i, element) in params.withIndex()) {
                    if (i >= 2 && element != null) {
                        val varIndex = i - 2
                        if (varIndex < stmt.vars.size) {
                            stmt.vars[varIndex] = element
                        }
                    }
                }

                stmt.afterRead()
                stmt
            }

            LogicIO.allStatements.add { VSControl() }
        }
    }
}

class ControlI(
    var lVars: MutableList<LVar>, var mode: ControlMode
) : LExecutor.LInstruction {
    override fun run(exec: LExecutor) {
        when (mode) {
            ControlMode.Default -> {
                val build1 = lVars[0].building() as? MicroVoid.MicroVoidBuild ?: return
                val build3 = lVars[1].building() as? CorVacuum.CorVacuumBuild ?: return
                val x = lVars[2].numval.toFloat() * 8
                val y = lVars[3].numval.toFloat() * 8
                val radius = lVars[4].numval.toFloat() * 8

                build1.builds.forEach { build ->
                    val build2 = build as VelumSolventBuild
                    if (!(build1.efficiency == 0f || build2.efficiency == 0f || build3.efficiency == 0f)) {
                        build2.spaces.forEach { (_, zone) ->
                            Groups.bullet.filterIndexed { _, bullet ->
                                zone.contains(
                                    bullet.x,
                                    bullet.y
                                ) && bullet.team != build2.team()
                            }.forEach { b ->
                                build1.addCircle(b.x, b.y, 8f, 5f, 0.3f)
                            }
                        }

                        if (build2.spaces.isEmpty()) {
                            build2.addCircle(0f, 0f, radius)
                        }

                        build2.update {
                            if (it !is SpaceDate.CircleZone) return@update
                            VsVars.world.spaceDate.updateCirclePosition(it, x, y)
                        }
                    }
                }

            }

            ControlMode.Clear -> {
                val build = lVars[0].building() as? VelumSolventBuild ?: return

                if (build.efficiency == 0f) return

                build.clear()
            }

            ControlMode.Bind -> {
                val build1 = lVars[0].building() ?: return
                val build2 = lVars[1].building() as? BindBuilding ?: return

                build2.bind(build1)
            }
        }
    }
}