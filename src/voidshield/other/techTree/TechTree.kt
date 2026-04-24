package voidshield.other.techTree

import arc.struct.Seq
import mindustry.content.TechTree
import mindustry.content.TechTree.TechNode
import mindustry.ctype.UnlockableContent
import mindustry.game.Objectives.Objective
import mindustry.game.Objectives.Produce
import mindustry.type.ItemStack


/**
 * @author guiY
 * 感谢他老人家的代码，解决了很多麻烦
 */
object TechTree {
    var context: TechNode? = null
    fun addToNode(p: UnlockableContent, c: Runnable) {
        context = TechTree.all.find { t: TechNode -> t.content === p }
        c.run()
    }

    fun node(content: UnlockableContent, children: Runnable) {
        node(content, content.researchRequirements(), children)
    }

    fun node(content: UnlockableContent?, requirements: Array<ItemStack?>?, children: Runnable) {
        node(content, requirements, null, children)
    }

    fun node(
        content: UnlockableContent?,
        requirements: Array<ItemStack?>?,
        objectives: Seq<Objective?>?,
        children: Runnable
    ) {
        val node = TechNode(context, content, requirements)
        if (objectives != null) {
            node.objectives.addAll(objectives)
        }
        val prev = context
        context = node
        children.run()
        context = prev
    }

    fun node(content: UnlockableContent, objectives: Seq<Objective?>?, children: Runnable) {
        node(content, content.researchRequirements(), objectives, children)
    }

    fun node(block: UnlockableContent) {
        node(block) {}
    }

    fun nodeProduce(content: UnlockableContent, objectives: Seq<Objective?>, children: Runnable) {
        node(content, content.researchRequirements(), objectives.add(Produce(content)), children)
    }

    fun nodeProduce(content: UnlockableContent, children: Runnable) {
        nodeProduce(content, Seq(), children)
    }
}