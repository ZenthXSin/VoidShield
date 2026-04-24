package voidshield.entities.skills

import arc.Events
import arc.util.Time
import mindustry.game.EventType

class BlackHole {
    val dataMap = HashMap<Int, BlackHoleData>()
    var idCounter = 0

    fun add(b: BlackHoleData): Int {
        b.startTime = Time.time
        val id = idCounter++
        dataMap[id] = b
        return id
    }

    fun remove(id: Int) {
        dataMap.remove(id)
    }

    init {
        Events.run(EventType.Trigger.update) {
            dataMap.entries.removeIf { (_, data) ->
                val expired = data.isDead()
                if (expired) data.onDeath?.invoke()
                expired
            }
        }
    }
}

class BlackHoleData(
    var x: Float = 0f,
    var y: Float = 0f,
    var r: Float = 100f,
    var lifeTime: Float = 60f,
    var startTime: Float = 0f,
    var onDeath: (() -> Unit)? = null
) {
    fun progress(): Float = ((Time.time - startTime) / lifeTime).coerceIn(0f, 1f)
    fun isDead(): Boolean = Time.time - startTime >= lifeTime
    fun remaining(): Float = (lifeTime - (Time.time - startTime)).coerceAtLeast(0f)
    fun update() {
    }
    fun load() {

    }
}