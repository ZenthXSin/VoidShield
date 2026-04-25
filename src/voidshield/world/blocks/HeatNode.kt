package voidshield.world.blocks

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Lines
import arc.math.Mathf
import arc.math.geom.Geometry
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

open class HeatNode(name: String) : HeatBlock(name) {
    companion object {
        private var otherReq: BuildPlan? = null
    }

    val timerCheckMoved = timers++
    var range = 5
    var conductivity = 100f

    @Transient
    var lastBuild: HeatNodeBuild? = null

    init {
        update = true
        solid = true
        configurable = true
        consumesPower = true

        config(Point2::class.java) { build: HeatNodeBuild, point: Point2 ->
            build.link = Point2.pack(point.x + build.tileX(), point.y + build.tileY())
        }
    }

    override fun setStats() {
        super.setStats()
        stats.add(Stat("range", StatCat.general), range.toFloat(), StatUnit.blocks)
        stats.add(Stat("conductivity", HeatStat.catHeat), conductivity, StatUnit.none)
    }

    override fun init() {
        super.init()
        updateClipRadius((range + 0.5f) * tilesize)
    }

    override fun drawPlanConfigTop(plan: BuildPlan, list: Eachable<BuildPlan>) {
        otherReq = null
        list.each { other ->
            if (other.block == this && plan != other && plan.config is Point2) {
                val p = plan.config as Point2
                if (p.equals(other.x - plan.x, other.y - plan.y)) {
                    otherReq = other
                }
            }
        }

        otherReq?.let { drawBridge(plan, it.drawx(), it.drawy(), 0f) }
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

    open fun positionsValid(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        if (x1 == x2) return abs(y1 - y2) <= range
        if (y1 == y2) return abs(x1 - x2) <= range
        return false
    }

    open fun linkValid(tile: Tile?, other: Tile?): Boolean = linkValid(tile, other, true)

    open fun linkValid(tile: Tile?, other: Tile?, checkDouble: Boolean): Boolean {
        if (tile == null || other == null) return false
        if (!positionsValid(tile.x.toInt(), tile.y.toInt(), other.x.toInt(), other.y.toInt())) return false

        val otherBuild = other.build as? HeatNodeBuild ?: return false
        if (other.block() != this) return false
        if (other.team() != tile.team()) return false
        if (checkDouble && otherBuild.link == tile.pos()) return false

        return true
    }

    open fun findLink(x: Int, y: Int): Tile? {
        val tile = world.tile(x, y)
        val last = lastBuild
        if (tile != null && last != null && last.tile != tile && last.link == -1 && linkValid(tile, last.tile)) {
            return last.tile
        }
        return null
    }

    override fun drawPlace(x: Int, y: Int, rotation: Int, valid: Boolean) {
        super.drawPlace(x, y, rotation, valid)

        val link = findLink(x, y)

        for (i in 0..3) {
            Drawf.dashLine(
                Pal.placing,
                x * tilesize + Geometry.d4[i].x * (tilesize / 2f + 2f),
                y * tilesize + Geometry.d4[i].y * (tilesize / 2f + 2f),
                x * tilesize + Geometry.d4[i].x * range * tilesize.toFloat(),
                y * tilesize + Geometry.d4[i].y * range * tilesize.toFloat()
            )
        }

        Draw.reset()
        Draw.color(Pal.placing)
        Lines.stroke(1f)
        if (link != null && abs(link.x.toInt() - x) + abs(link.y.toInt() - y) > 1) {
            val rot = link.absoluteRelativeTo(x, y)
            val w = if (link.x.toInt() == x) tilesize.toFloat() else abs(link.x.toInt() - x) * tilesize - tilesize
            val h = if (link.y.toInt() == y) tilesize.toFloat() else abs(link.y.toInt() - y) * tilesize - tilesize

            Lines.rect((x + link.x.toInt()) / 2f * tilesize - w.toFloat() / 2f, (y + link.y.toInt()) / 2f * tilesize - h.toFloat() / 2f, w.toFloat(), h.toFloat())
            Lines.square(
                (link.x.toInt() * tilesize + Geometry.d4(rot.toInt()).x * tilesize).toFloat(),
                (link.y.toInt() * tilesize + Geometry.d4(rot.toInt()).y * tilesize).toFloat(),
                link.absoluteRelativeTo(x, y) * 90f)
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
        var link = -1
        var incoming = IntSeq(false, 4)
        var time = -8f
        var timeSpeed = 0f
        var wasMoved = false
        var moved = false

        override fun pickedUp() {
            link = -1
        }

        override fun playerPlaced(config: Any?) {
            super.playerPlaced(config)

            val found = findLink(tile.x.toInt(), tile.y.toInt())
            if (linkValid(tile, found) && link != found!!.pos() && !proximity.contains(found.build)) {
                found.build.configure(tile.pos())
            }

            lastBuild = this
        }

        override fun onConfigureBuildTapped(other: Building): Boolean {
            if (other is HeatNodeBuild && other.link == pos()) {
                configure(other.pos())
                other.configure(-1)
                return false
            }

            if (linkValid(tile, other.tile)) {
                if (link == other.pos()) configure(-1) else configure(other.pos())
                return false
            }

            return true
        }

        fun checkIncoming() {
            var idx = 0
            while (idx < incoming.size) {
                val i = incoming.items[idx]
                val other = world.tile(i)
                val otherBuild = other?.build as? HeatNodeBuild
                if (!linkValid(tile, other, false) || otherBuild?.link != tile.pos()) {
                    incoming.removeIndex(idx)
                } else {
                    idx++
                }
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
            checkIncoming()

            val other = world.tile(link)?.build as? HeatNodeBuild
            if (other != null && linkValid(tile, other.tile)) {
                other.incoming.addUnique(pos())
                warmup = Mathf.approachDelta(warmup, efficiency, 1f / 30f)
                exchangeHeat(other, bridgeRate(other))
            } else {
                warmup = Mathf.approachDelta(warmup, 0f, 1f / 30f)
            }

            exchangeHeatAdjacent()
            temperature = clampTemperature(temperature)
        }

        fun bridgeRate(other: HeatNodeBuild): Float {
            val otherNode = other.block as HeatNode
            return Mathf.clamp(minOf(conductivity, otherNode.conductivity), 0f, 5f) * 0.18f
        }

        fun exchangeHeat(other: HeatBuild, rawRate: Float) {
            if (pos() >= other.pos()) return
            val rate = Mathf.clamp(rawRate * Time.delta, 0f, 0.45f)
            if (rate <= 0f) return

            val delta = other.temperature - temperature
            if (Mathf.zero(delta)) return

            val change = delta * rate
            temperature = clampTemperature(temperature + change)
            other.temperature = clampTemperature(other.temperature - change)
            moved = true
        }

        fun exchangeHeatAdjacent() {
            for (i in 0 until proximity.size) {
                val other = proximity[i] as? HeatBuild ?: continue
                if (other === this) continue
                if (other is HeatNodeBuild && (other.pos() == link || other.link == pos())) continue
                exchangeHeat(other, conductivity * 0.08f)
            }
        }

        fun clampTemperature(value: Float): Float {
            return Mathf.clamp(value, 0f, (block as HeatBlock).maxTemperature)
        }

        override fun drawConfigure() {
            Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, Pal.accent)

            for (i in 1..range) {
                for (j in 0..3) {
                    val other = tile.nearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i)
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
            if (linkValid(tile, world.tile(link))) drawInput(world.tile(link)!!)
            incoming.each { pos -> world.tile(pos)?.let { drawInput(it) } }
            Draw.reset()
        }

        private fun drawInput(other: Tile) {
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

        override fun draw() {
            super.draw()

            val target = world.tile(link)?.build as? HeatNodeBuild ?: return
            if (!linkValid(tile, target.tile, false) || Mathf.zero(Renderer.bridgeOpacity)) return

            Draw.alpha(maxOf(warmup, 0.25f) * Renderer.bridgeOpacity)
            drawLink(getTemperatureColor(this), target)
            Draw.reset()
        }

        private fun drawLink(color: Color, other: HeatNodeBuild) {
            val from = getEdgePosition(tile, other.tile)
            val to = getEdgePosition(other.tile, tile)
            Drawf.line(color, from[0], from[1], to[0], to[1])
        }

        private fun getEdgePosition(from: Tile, to: Tile): FloatArray {
            val dx = to.x - from.x
            val dy = to.y - from.y
            val offsetX = if (dx > 0) tilesize / 2f else if (dx < 0) -tilesize / 2f else 0f
            val offsetY = if (dy > 0) tilesize / 2f else if (dy < 0) -tilesize / 2f else 0f
            return floatArrayOf(from.drawx() + offsetX, from.drawy() + offsetY)
        }

        override fun shouldConsume(): Boolean {
            return linkValid(tile, world.tile(link)) && enabled
        }

        override fun config(): Point2 {
            return Point2.unpack(link).sub(tile.x.toInt(), tile.y.toInt())
        }

        override fun version(): Byte = 1

        override fun write(w: Writes) {
            super.write(w)
            w.i(link)
            w.f(warmup)
            w.b(incoming.size)
            for (i in 0 until incoming.size) w.i(incoming.items[i])
            w.bool(wasMoved || moved)
        }

        override fun read(r: Reads, revision: Byte) {
            super.read(r, revision)
            link = r.i()
            warmup = r.f()
            val links = r.b().toInt().coerceAtLeast(0)
            incoming.clear()
            for (i in 0 until links) incoming.add(r.i())

            if (revision >= 1) {
                wasMoved = r.bool()
                moved = wasMoved
            }
        }
    }
}