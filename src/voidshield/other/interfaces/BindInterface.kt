package voidshield.other.interfaces

import mindustry.gen.Building
import kotlin.collections.forEach

interface BindInterface<T> {
    var builds: MutableList<T>

    fun bind(building: T) {
        if (!builds.contains(building)) builds.add(building)
    }

    fun lifeCycle()
}

open class BindBuilding: Building(), BindInterface<Building> {
    override var builds: MutableList<Building> = mutableListOf()
    override fun updateTile() {
        super.updateTile()
        lifeCycle()
    }
    override fun lifeCycle() {
        val waitRemove: MutableList<Building> = mutableListOf()
        builds.forEach {
            if (!it.isAdded) {
                waitRemove += it
            }
        }
        waitRemove.forEach {
            builds.remove(it)
        }
    }
}