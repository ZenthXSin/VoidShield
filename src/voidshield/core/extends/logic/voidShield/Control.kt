package voidshield.core.extends.logic.voidShield

import arc.math.geom.Rect
import arc.scene.ui.Button
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import mindustry.gen.Groups
import mindustry.gen.LogicIO
import mindustry.logic.LAssembler
import mindustry.logic.LCanvas
import mindustry.logic.LCategory
import mindustry.logic.LExecutor
import mindustry.logic.LStatement
import mindustry.logic.LVar
import mindustry.ui.Styles
import voidshield.core.VsVars
import voidshield.core.dateTypes.SpaceDate
import voidshield.core.interfaces.BindBuilding
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

    fun row(table: Table?, row: Boolean = false, run: (table: Table) -> Unit) {
        table ?: return
        if (LCanvas.useRows() || row) {
            table.row()
            table.table { i ->
                i.color.set(table.color)
                run(i.left())
            }.left()
        } else {
            run(table)
        }
    }

    fun rebuild(table: Table) {
        table.clearChildren()
        table.table { i ->
            i.left()
            i.color.set(table.color)

            i.clearChildren()
            i.defaults() // Reset any defaults that might have been set
            row(i, true) { i1 ->
                i1.button({ b: Button? ->
                    b!!.label { type.name }
                    b.clicked {
                        showSelect(b, ControlMode.all, type, { t: ControlMode ->
                            type = t
                            vars = mutableListOf()
                            type.params.forEach { vars += it }
                            rebuild(table)
                        }, 2, { cell: Cell<*>? -> cell!!.size(100f, 50f) })
                    }
                }, Styles.logict, {}).size(120f, 40f).color(i1.color).padLeft(2f)
            }
        }.left()
        table.row()
        table.table { i ->
            i.left()
            i.color.set(table.color)

            when (type) {
                ControlMode.Default -> {
                    row(i, true) { i ->
                        i.left()
                        i.add(" MicroVoid ")
                        field(i, vars[0]) { str -> vars[0] = str }.width(80f)
                        i.add(" CorVacuum ")
                        field(i, vars[1]) { str -> vars[1] = str }.width(80f)
                    }

                    row(i, true) { table ->
                        table.add(" x ")
                        field(table, vars[2]) { str -> vars[2] = str }.width(80f)
                        table.add(" y ")
                        field(table, vars[3]) { str -> vars[3] = str }.width(80f)
                        table.add(" range ")
                        field(table, vars[4]) { str -> vars[4] = str }.width(80f)
                    }

                }

                ControlMode.AutoBlock -> {
                    row(i, true) { table ->
                        table.add(" MicroVoid ")
                        field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                        table.add(" CorVacuum ")
                        field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                    }
                }

                ControlMode.ClearZone -> {
                    i.left()
                    i.add(" VelumSolvent ")
                    field(i, vars[0]) { str -> vars[0] = str }.width(80f)

                }

                ControlMode.Bind -> {
                    row(i)
                    field(i, vars[0]) { str -> vars[0] = str }.width(80f)
                    i.add(" to ")
                    field(i, vars[1]) { str -> vars[1] = str }.width(80f)
                }

                ControlMode.CircleZone -> {
                    row(i, true) { table ->
                        table.add(" x ")
                        field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                        table.add(" y ")
                        field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                        table.add(" range ")
                        field(table, vars[2]) { str -> vars[2] = str }.width(80f)
                    }
                }

                ControlMode.PolygonZone -> {
                    row(i, true) { table ->
                        table.add(" x ")
                        field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                        table.add(" y ")
                        field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                    }
                }

                ControlMode.UseZone -> {
                    row(i, true) { table ->
                        table.add(" VelumSolvent ")
                        field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                    }
                }

                ControlMode.UpdateZone -> {
                    row(i, true) { table ->
                        table.add(" x ")
                        field(table, vars[0]) { str -> vars[0] = str }.width(80f)
                        table.add(" y ")
                        field(table, vars[1]) { str -> vars[1] = str }.width(80f)
                        row(table) {
                            table.add(" tx ")
                            field(table, vars[2]) { str -> vars[2] = str }.width(80f)
                            table.add(" ty ")
                            field(table, vars[3]) { str -> vars[3] = str }.width(80f)
                            table.add(" VelumSolvent ")
                            field(table, vars[4]) { str -> vars[4] = str }.width(80f)
                        }
                    }
                }
            }
            i.left()
        }.growX().left().row()
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
        return if (read.size == 0) null else read.first() as? VSControl
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

    /** UseZone 命令缓冲项;比字符串栈快,避免 `$x | $y` 拼接 + split 解析 */
    sealed class ZoneCmd {
        class Circle(val x: Float, val y: Float, val r: Float) : ZoneCmd()
        class Polygon(val x: Float, val y: Float) : ZoneCmd()
    }

    companion object {
        val tmp: MutableMap<Int, MutableList<ZoneCmd>> = mutableMapOf()
        // 共享的 hitbox 暂存,run 单线程内连续覆写无冲突
        private val rectBuf = Rect()
    }

    override fun run(exec: LExecutor) {
        if (tmp[exec.build.id] == null) tmp[exec.build.id] = mutableListOf()

        when (mode) {
            ControlMode.Default -> {
                val build1 = lVars[0].building() as? MicroVoid.MicroVoidBuild ?: return
                val build3 = lVars[1].building() as? CorVacuum.CorVacuumBuild ?: return
                val x = lVars[2].numfWorld()
                val y = lVars[3].numfWorld()
                val radius = lVars[4].numfWorld()

                build1.builds.forEach { build ->
                    val build2 = build as? VelumSolventBuild ?: return@forEach
                    if (!(build1.efficiency == 0f || build2.efficiency == 0f || build3.efficiency == 0f)) {
                        build2.spaces.forEach { (_, zone) ->
                            zone.hitbox(rectBuf)
                            Groups.bullet.intersect(rectBuf.x, rectBuf.y, rectBuf.width, rectBuf.height).each { b ->
                                if (b.team == build2.team()) return@each
                                if (!zone.contains(b.x, b.y)) return@each
                                build1.addCircle(b.x, b.y, 8f, 5f, 0.3f)
                                build2.triggerHitEffect(zone.id)
                            }
                        }

                        if (build2.spaces.isEmpty()) {
                            build2.addCircle(x, y, radius)
                        }

                        build2.update {
                            if (it !is SpaceDate.CircleZone) return@update
                            VsVars.world.spaceDate.updateCirclePosition(it, x, y)
                        }
                    }
                }

            }

            ControlMode.AutoBlock -> {
                val build1 = lVars[0].building() as? MicroVoid.MicroVoidBuild ?: return
                val build3 = lVars[1].building() as? CorVacuum.CorVacuumBuild ?: return

                build1.builds.forEach { build ->
                    val build2 = build as? VelumSolventBuild ?: return@forEach
                    if (!(build1.efficiency == 0f || build2.efficiency == 0f || build3.efficiency == 0f)) {
                        build2.spaces.forEach { (_, zone) ->
                            zone.hitbox(rectBuf)
                            Groups.bullet.intersect(rectBuf.x, rectBuf.y, rectBuf.width, rectBuf.height).each { b ->
                                if (b.team == build2.team()) return@each
                                if (!zone.contains(b.x, b.y)) return@each
                                build1.addCircle(b.x, b.y, 8f, 5f, 0.3f)
                                build2.triggerHitEffect(zone.id)
                            }
                        }
                    }
                }
            }

            ControlMode.ClearZone -> {
                val build = lVars[0].building() as? VelumSolventBuild ?: return

                if (build.efficiency == 0f) return

                build.clearZone()
            }

            ControlMode.Bind -> {
                val build1 = lVars[0].building() ?: return
                val build2 = lVars[1].building() as? BindBuilding ?: return

                build2.bind(build1)
            }

            ControlMode.CircleZone -> {
                val x = lVars[0].numfWorld()
                val y = lVars[1].numfWorld()
                val radius = lVars[2].numfWorld()

                tmp[exec.build.id]?.add(ZoneCmd.Circle(x, y, radius))
            }

            ControlMode.PolygonZone -> {
                val x = lVars[0].numfWorld()
                val y = lVars[1].numfWorld()

                tmp[exec.build.id]?.add(ZoneCmd.Polygon(x, y))
            }

            ControlMode.UseZone -> {
                val build = lVars[0].building() as? VelumSolventBuild ?: return

                // FloatSeq 走原生 float,避免 MutableList<Float> 的自动装箱
                val polygonTmp = arc.struct.FloatSeq()

                tmp[exec.build.id]?.forEach { cmd ->
                    when (cmd) {
                        is ZoneCmd.Circle -> {
                            if (build.efficiency == 0f) return
                            build.addCircle(cmd.x, cmd.y, cmd.r)
                        }
                        is ZoneCmd.Polygon -> {
                            polygonTmp.add(cmd.x)
                            polygonTmp.add(cmd.y)
                        }
                    }
                }

                if (polygonTmp.size >= 6) {
                    if (build.efficiency == 0f) return
                    build.addPolygon(polygonTmp.toArray())
                }

                tmp.remove(exec.build.id)
            }

            ControlMode.UpdateZone -> {
                val x = lVars[0].numfWorld()
                val y = lVars[1].numfWorld()
                val tx = lVars[2].numfWorld()
                val ty = lVars[3].numfWorld()
                val build = lVars[4].building() as? VelumSolventBuild ?: return

                if (build.efficiency == 0f) return
                when (val zone = build.getZoneByPos(x,y)) {
                    is SpaceDate.CircleZone -> {
                        VsVars.world.spaceDate.updateCirclePosition(zone, tx, ty)
                    }
                    is SpaceDate.PolygonZone -> {
                        VsVars.world.spaceDate.updatePolygonPosition(zone, tx, ty)
                    }
                }
            }
        }
    }
}