package voidshield.other.extends.categoryExtend

import arc.Core
import arc.scene.style.TextureRegionDrawable
import arc.util.Log
import arc.util.OS.isAndroid
import mindustry.Vars
import mindustry.gen.Icon
import mindustry.type.Category
import mindustry.ui.fragments.PlacementFragment
import universe.util.reflect.Enums.accessEnum0
import universe.util.reflect.Reflection.accessField
import voidshield.other.VsVars
import java.lang.reflect.Field

/**
 * 第一阶段：仅向 Category 枚举追加新枚举值，并同步更新 Category.all。
 * 在 Mod.loadContent() 中（通过 object 属性初始化）调用。
 */
 
fun registerCategory(name: String): Category {
    val newCat = Category::class.accessEnum0().appendEnumInstance(name)
    if (isAndroid) {
        val allField = Category::class.java.getDeclaredField("all")
        allField.isAccessible = true
        allField.set(null, Category.values())
        Log.info("[CategoryExtender] Updated Category.all on Android")
    } else {
        try {
            val allAccessor = Category::class.accessField<Array<Category>>("all")
            allAccessor.set(Category.values())
            Log.info("[CategoryExtender] Updated Category.all, new size: ${Category.entries.size}")
        } catch (e: Exception) {
            Log.err("[CategoryExtender] Failed to update Category.all: ${e.message}")
        }
        Log.info("[CategoryExtender] Updated Category.all on Desktop")
    }
    return newCat
}

/**
 * 第二阶段：注册图标并刷新建筑选择栏 UI。
 * 在 Mod.init() 中调用，此时 UI 已构建完成。
 */
fun applyCategory(cat: Category) {
    val name = "${VsVars.modName}-${cat.name}"

    // 注册图标：若图集中找不到对应贴图则回退到 error 图标
    if (!Icon.icons.containsKey(name)) {
        val drawable: TextureRegionDrawable = if (Core.atlas.has(name)) {
        Core.atlas.getDrawable(name) 
        } else if (Core.atlas.has(cat.name)) {
        Core.atlas.getDrawable(cat.name)
        } else {
        Core.atlas.getDrawable("error")
        }
        Icon.icons.put(cat.name, drawable)
    }

    // 扩展 PlacementFragment.categoryEmpty 数组
    val blockfrag = Vars.ui.hudfrag.blockfrag
    val categoryEmptyAccessor = accessField<PlacementFragment, BooleanArray>("categoryEmpty")
    val oldArray: BooleanArray = categoryEmptyAccessor.get(blockfrag)
    val newArray = BooleanArray(Category.all.size) { i -> if (i < oldArray.size) oldArray[i] else true }
    categoryEmptyAccessor.set(blockfrag, newArray)

    // 刷新建筑选择栏 UI
    blockfrag.rebuild()
}