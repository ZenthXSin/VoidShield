package voidshield.world.blocks

import arc.Events
import mindustry.game.EventType
import mindustry.world.Block
import voidshield.OtherMod

class OtherBlock(name: String) : Block(name) {
    init {
        Events.run(EventType.ClientLoadEvent::class.java) {
            OtherMod.load(name)
        }
    }
}