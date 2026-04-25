package voidshield.world.blocks

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.math.geom.Point2
import arc.struct.IntSeq
import arc.struct.Seq
import arc.util.Eachable
import arc.util.Time
import arc.util.Tmp
import arc.util.io.Reads
import arc.util.io.Writes
import mindustry.Vars.tilesize
import mindustry.Vars.world
import mindustry.core.Renderer
import mindustry.entities.units.BuildPlan
import mindustry.gen.Building
import mindustry.graphics.Drawf
import mindustry.graphics.Pal
import mindustry.input.Placement
import mindustry.world.Tile
import mindustry.world.meta.Stat
import mindustry.world.meta.StatCat
import mindustry.world.meta.StatUnit
import voidshield.world.HeatBlock
import voidshield.world.HeatStat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Heat node, link and configure behavior aligned with vanilla ItemBridge.
 */
open class HeatNode(name: String) : HeatBlock(name) {
    companion object {
        private var otherReq: BuildPlan? = null
    }

    val timerCheckMoved = timers++
    var range: Int = 5
    var conductivity: Float = 0.5f

    // for auto-link, mirrors ItemBridge.lastBuild behavior
    var lastBuild: HeatNodeBuild? = null

    init {
        update = true
        solid = true
        consumesPower = true
        configurable = true

        config(Point2::class.java) { build: HeatNodeBuild, point: Point2 ->
            build.link = Point2.pack(point.x + build.tileX(), point.y + build.tileY())
        }

        config(Int::class.javaPrimitiveType) { build: HeatNodeBuild, value: Int ->
            build.link = value
        }
    }

    override fun setStats() {
        super.setStats()
        stats.add(Stat("range", StatCat.general), range.toFloat(), StatUnit.blocks)
        stats.add(Stat("conductivity", HeatStat.catHeat), conductivity, StatUnit.none)
    }

    override fun drawPlanConfigTop(plan: BuildPlan, list: Eachable<BuildPlan>) {
        otherReq = null
        list.each { other ->
            if (other.block == this && plan != other && plan.config is Point2) {
                val p = plan.config as Point2
                if (p.x == other.x - plan.x && p.y == other.y - plan.y) {
                    otherReq = other
                }
            }
        }

        otherReq?.let {
            drawBridge(plan, it.drawx(), it.drawy(), 0f)
        }
    }

    fun drawBridge(req: BuildPlan, ox: Float, oy: Float, flip: Float) {
        if (Mathf.zero(Renderer.bridgeOpacity)) return
        Draw.alpha(Renderer.bridgeOpacity)

        Tmp.v1.set(ox, oy).sub(req.drawx(), req.drawy()).setLength(tilesize / 2f)

        Drawf.line(
            Pal.place,
            req.drawx() + Tmp.v1.x,
            req.drawy() + Tmp.v1.y,
            ox - Tmp.v1.x,
            oy - Tmp.v1.y
        )

        Draw.rect("bridge-arrow", (req.drawx() + ox) / 2f, (req.drawy() + oy) / 2f, flip)
        Draw.reset()
    }

    override fun init() {
        super.init()
        updateClipRadius((range + 0.5f) * tilesize)
    }

    open fun linkValid(tile: Tile?, other: Tile?): Boolean {
        return linkValid(tile, other, true)
    }

    open fun linkValid(tile: Tile?, other: Tile?, checkDouble: Boolean): Boolean {
        if (other == null || tile == null || !positionsValid(tile.x.toInt(), tile.y.toInt(), other.x.toInt(), other.y.toInt())) return false

        val build = other.build as? HeatNodeBuild ?: return false
        return ((other.block() == tile.block() && tile.block() == this) || (tile.block() !is HeatNode && other.block() == this))
            && (build.team == tile.team() || tile.block() != this)
            && (!checkDouble || build.link != tile.pos())
    }

    open fun positionsValid(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        return if (x1 == x2) {
            abs(y1 - y2) <= range
        } else if (y1 == y2) {
            abs(x1 - x2) <= range
        } else {
            false
        }
    }

    open fun findLink(x: Int, y: Int): Tile? {
        val tile = world.tile(x, y)
        val last = lastBuild
        if (tile != null && last != null && last.tile != tile && last.link == -1 && linkValid(tile, last.tile)) {
            return last.tile
        }

        if (tile == null) return null

        for (i in 1..range) {
            for (j in 0..3) {
                val dir = arc.math.geom.Geometry.d4[j]
                val nearby = tile.nearby(dir.x * i, dir.y * i) ?: continue
                val nearbyBuild = nearby.build as? HeatCatheterBuild ?: continue
                if (nearbyBuild.targetLink != -1) continue
                if (linkValid(tile, nearby)) return nearby
            }
        }
        return null
    }

    override fun drawPlace(x: Int, y: Int, rotation: Int, valid: Boolean) {
        super.drawPlace(x, y, rotation, valid)

        val link = findLink(x, y)
        for (i in 0..3) {
            val dir = arc.math.geom.Geometry.d4[i]
            Drawf.dashLine(
                Pal.placing,
                x * tilesize + dir.x * (tilesize / 2f + 2f),
                y * tilesize + dir.y * (tilesize / 2f + 2f),
                x * tilesize + dir.x * range * tilesize.toFloat(),
                y * tilesize + dir.y * range * tilesize.toFloat()
            )
        }

        Draw.reset()
        Draw.color(Pal.placing)
        Lines.stroke(1f)
        if (link != null && abs(link.x.toInt() - x) + abs(link.y.toInt() - y) > 1) {
            val rot = link.absoluteRelativeTo(x, y)
            val w = if (link.x.toInt() == x) tilesize.toFloat() else abs(link.x.toInt() - x) * tilesize - tilesize.toFloat()
            val h = if (link.y.toInt() == y) tilesize.toFloat() else abs(link.y.toInt() - y) * tilesize - tilesize.toFloat()
            Lines.rect((x + link.x.toInt()) / 2f * tilesize - w / 2f, (y + link.y.toInt()) / 2f * tilesize - h / 2f, w, h)
            Draw.rect(
                "bridge-arrow",
                link.x.toInt() * tilesize + arc.math.geom.Geometry.d4(rot.toInt()).x * tilesize.toFloat(),
                link.y.toInt() * tilesize + arc.math.geom.Geometry.d4(rot.toInt()).y * tilesize.toFloat(),
                link.absoluteRelativeTo(x, y) * 90f
            )
        }
        Draw.reset()
    }

    override fun handlePlacementLine(plans: Seq<BuildPlan>) {
        for (i in 0 until plans.size - 1) {
            val cur = plans[i]
            val next = plans[i + 1]
            if (positionsValid(cur.x, cur.y, next.x, next.y)) {
                cur.config = Point2(next.x - cur.x, next.y - cur.y)
            }
        }
    }

    override fun changePlacementPath(points: Seq<Point2>, rotation: Int) {
        Placement.calculateNodes(points, this, rotation) { point, other ->
            maxOf(abs(point.x - other.x), abs(point.y - other.y)) <= range
        }
    }

    open inner class HeatNodeBuild : HeatBuild() {
        var link: Int = -1
        val incoming = IntSeq(false, 4)
        var time = -8f
        var timeSpeed = 0f
        var wasMoved = false
        var moved = false

        override fun pickedUp() {
            link = -1
        }

        override fun playerPlaced(config: Any?) {
            super.playerPlaced(config)

            val found = findLink(tileX(), tileY())
            if (linkValid(tile, found) && link != found!!.pos() && !proximity.contains(found.build)) {
                found.build.configure(tile.pos())
            }

            lastBuild = this
        }

        override fun onConfigureBuildTapped(other: Building): Boolean {
            // reverse connection
            if (other is HeatNodeBuild && other.link == pos()) {
                configure(other.pos())
                other.configure(-1)
                return true
            }

            if (linkValid(tile, other.tile)) {
                configure(if (link == other.pos()) -1 else other.pos())
                return false
            }

            return true
        }

        fun checkLink() {
            if (link == -1) return
            val target = world.tile(link)
            if (!linkValid(tile, target, false)) {
                link = -1
            }
        }

        fun checkIncoming() {
            var idx = 0
            while (idx < incoming.size) {
                val pos = incoming.items[idx]
                val other = world.tile(pos)
                val otherBuild = other?.build as? HeatNodeBuild
                if (!linkValid(tile, other, false) || otherBuild?.link != tile.pos()) {
                    incoming.removeIndex(idx)
                    idx--
                }
                idx++
            }
        }

        override fun updateTile() {
            if (timer(timerCheckMoved, 30f)) {
                wasMoved = moved
                moved = false
            }

            timeSpeed = Mathf.approachDelta(timeSpeed, if (wasMoved) 1f else 0f, 1f / 60f)
            time += timeSpeed * delta()

            setGradient()
            checkLink()
            checkIncoming()

<<<<<<< HEAD:src/voidshield/world/blocks/HeatNode.kt
            val remote = world.tile(link)?.build as? HeatNodeBuild
            if (remote == null || !linkValid(tile, remote.tile, false)) {
                warmup = 0f
            } else {
                remote.incoming.addUnique(pos())
                warmup = Mathf.approachDelta(warmup, efficiency, 1f / 30f)

=======
            val remote = world.tile(targetLink)?.build as? HeatCatheterBuild
            if (remote == null || !linkValid(tile, remote.tile, false)) {
                warmup = Mathf.approachDelta(warmup, 0f, 1f / 30f)
            } else {
                val remoteIncoming = remote.incoming
                remoteIncoming.addUnique(pos())

                warmup = Mathf.approachDelta(warmup, efficiency, 1f / 30f)
>>>>>>> master:src/voidshield/world/blocks/HeatCatheter.kt
                if (pos() < remote.pos()) {
                    exchangeHeatBridge(remote)
                }
            }

            for (i in 0 until proximity.size) {
                val other = proximity[i] as? HeatBuild ?: continue
                if (other === this) continue
<<<<<<< HEAD:src/voidshield/world/blocks/HeatNode.kt
                if (other is HeatNodeBuild && (link == other.pos() || other.link == pos())) continue
=======
                if (other is HeatCatheterBuild) {
                    val linkedByBridge = targetLink == other.pos() || other.targetLink == pos()
                    if (linkedByBridge) continue
                }
>>>>>>> master:src/voidshield/world/blocks/HeatCatheter.kt
                if (pos() < other.pos()) {
                    exchangeHeatAdjacent(other)
                }
            }

            temperature = clampTemperature(this, temperature)
        }

<<<<<<< HEAD:src/voidshield/world/blocks/HeatNode.kt
        fun exchangeHeatBridge(other: HeatNodeBuild) {
=======
        override fun playerPlaced(config: Any?) {
            super.playerPlaced(config)

            val link = findLink(tileX(), tileY())
            if (linkValid(tile, link) && targetLink != link!!.pos() && !proximity.contains(link.build)) {
                link.build.configure(tile.pos())
            }

            lastBuildPos = pos()
        }

        override fun pickedUp() {
            targetLink = -1
        }

        override fun onConfigureBuildTapped(other: Building): Boolean {
            if (other is HeatCatheterBuild && other.targetLink == pos()) {
                configure(other.pos())
                other.configure(-1)
                return false
            }

            if (linkValid(tile, other.tile)) {
                configure(if (targetLink == other.pos()) -1 else other.pos())
                return false
            }

            return true
        }

        fun checkLink() {
            if (targetLink == -1) return
            val target = world.tile(targetLink)
            if (!linkValid(tile, target, false)) {
                targetLink = -1
            }
        }

        fun checkIncoming() {
            var idx = 0
            while (idx < incoming.size) {
                val pos = incoming.items[idx]
                val other = world.tile(pos)
                val otherBuild = other?.build as? HeatCatheterBuild
                if (!linkValid(tile, other, false) || otherBuild?.targetLink != tile.pos()) {
                    incoming.removeIndex(idx)
                    idx--
                }
                idx++
            }
        }

        fun exchangeHeatBridge(other: HeatCatheterBuild) {
>>>>>>> master:src/voidshield/world/blocks/HeatCatheter.kt
            val delta = other.temperature - temperature
            if (Mathf.zero(delta)) return

            val bridgeEfficiency = min(efficiency, other.efficiency)
            if (bridgeEfficiency <= 0f) return

<<<<<<< HEAD:src/voidshield/world/blocks/HeatNode.kt
            val otherConductivity = (other.block as HeatNode).conductivity
=======
            val otherConductivity = (other.block as HeatCatheter).conductivity
>>>>>>> master:src/voidshield/world/blocks/HeatCatheter.kt
            val transferRate = Mathf.clamp(min(conductivity, otherConductivity) * 0.18f * Time.delta * bridgeEfficiency, 0f, 0.45f)
            transferHeat(other, transferRate)
        }

        fun exchangeHeatAdjacent(other: HeatBuild) {
            val delta = other.temperature - temperature
            if (Mathf.zero(delta)) return

            val transferRate = Mathf.clamp(conductivity * 0.08f * Time.delta, 0f, 0.25f)
            transferHeat(other, transferRate)
        }

        fun transferHeat(other: HeatBuild, rate: Float) {
            if (rate <= 0f) return

            val delta = other.temperature - temperature
            if (Mathf.zero(delta)) return

            val selfSpecificHeat = max((block as HeatBlock).specificHeat, 0.001f)
            val otherSpecificHeat = max((other.block as HeatBlock).specificHeat, 0.001f)

            val energyTransfer = delta * rate
            temperature = clampTemperature(this, temperature + energyTransfer / selfSpecificHeat)
            other.temperature = clampTemperature(other, other.temperature - energyTransfer / otherSpecificHeat)
            moved = true
        }

        fun clampTemperature(build: HeatBuild, value: Float): Float {
            return Mathf.clamp(value, 0f, (build.block as HeatBlock).maxTemperature)
        }

        override fun drawConfigure() {
            Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, Pal.accent)

            for (i in 1..range) {
                for (j in 0..3) {
                    val dir = arc.math.geom.Geometry.d4[j]
                    val other = tile.nearby(dir.x * i, dir.y * i)
                    if (linkValid(tile, other)) {
                        val linked = other!!.pos() == link
                        Drawf.select(
                            other.drawx(),
                            other.drawy(),
                            other.block().size * tilesize / 2f + 2f + if (linked) 0f else Mathf.absin(Time.time, 4f, 1f),
                            if (linked) Pal.place else Pal.breakInvalid
                        )
                    }
                }
            }
        }

        override fun drawSelect() {
            val target = world.tile(link)
            if (linkValid(tile, target, false)) {
                drawInput(target!!)
            }
            incoming.each { pos ->
                val other = world.tile(pos)
                if (other != null) drawInput(other)
            }
            Draw.reset()
        }

        override fun draw() {
            super.draw()
            val target = world.tile(link)
            if (!linkValid(tile, target, false)) return

            Draw.alpha(maxOf(warmup, 0.25f) * Renderer.bridgeOpacity)
            drawLink(getTemperatureColor(this), target!!.build as HeatNodeBuild)
            Draw.reset()
        }

        fun drawLink(color: Color, other: HeatNodeBuild) {
            val from = getEdgePosition(tile, other.tile)
            val to = getEdgePosition(other.tile, tile)
            Drawf.line(color, from[0], from[1], to[0], to[1])
        }

        fun drawInput(other: Tile) {
            if (!linkValid(tile, other, false)) return
            val linked = other.pos() == link

            Tmp.v2.trns(tile.angleTo(other), 2f)
            val tx = tile.drawx()
            val ty = tile.drawy()
            val ox = other.drawx()
            val oy = other.drawy()
            val alpha = abs((if (linked) 100f else 0f) - (Time.time * 2f) % 100f) / 100f
            val x = Mathf.lerp(ox, tx, alpha)
            val y = Mathf.lerp(oy, ty, alpha)

            val otherLink = if (linked) other else tile
            val rel = (if (linked) tile else other).absoluteRelativeTo(otherLink.x.toInt(), otherLink.y.toInt())

            Draw.color(Pal.gray)
            Lines.stroke(2.5f)
            Lines.square(ox, oy, 2f, 45f)
            Lines.stroke(2.5f)
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y)

            val color = (if (linked) Pal.place else Pal.accent).toFloatBits()

            Draw.color(color)
            Lines.stroke(1f)
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y)
            Lines.square(ox, oy, 2f, 45f)
            Draw.mixcol(color)
            Draw.color()
            Draw.rect("bridge-arrow", x, y, rel * 90f)
            Draw.mixcol()
        }

        fun getEdgePosition(from: Tile, to: Tile): FloatArray {
            val dx = to.x - from.x
            val dy = to.y - from.y
            val offsetX = if (dx > 0) tilesize / 2f else if (dx < 0) -tilesize / 2f else 0f
            val offsetY = if (dy > 0) tilesize / 2f else if (dy < 0) -tilesize / 2f else 0f
            return floatArrayOf(from.drawx() + offsetX, from.drawy() + offsetY)
        }

        override fun config(): Any? {
            return Point2.unpack(link).sub(tileX(), tileY())
        }

        override fun version(): Byte = 3

        override fun write(w: Writes) {
            super.write(w)
            w.i(link)
            w.f(warmup)
            w.b(incoming.size)
            for (i in 0 until incoming.size) {
                w.i(incoming.items[i])
            }
            w.bool(wasMoved || moved)
        }

        override fun read(r: Reads, revision: Byte) {
            super.read(r, revision)
            link = when {
                revision >= 3 -> r.i()
                revision == 1.toByte() -> {
                    val amount = r.b().toInt().coerceAtLeast(0)
                    var first = -1
                    for (i in 0 until amount) {
                        val saved = r.i()
                        if (i == 0) first = saved
                    }
                    first
                }
                else -> r.i()
            }

            if (revision >= 3) {
                warmup = r.f()
                val amount = r.b().toInt().coerceAtLeast(0)
                incoming.clear()
                for (i in 0 until amount) {
                    incoming.add(r.i())
                }
                val movedNow = r.bool()
                moved = movedNow
                wasMoved = movedNow
            }
        }
    }
}
