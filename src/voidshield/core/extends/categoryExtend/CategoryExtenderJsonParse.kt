package voidshield.core.extends.categoryExtend

import arc.struct.Seq
import arc.util.Log
import arc.util.serialization.Json
import mindustry.Vars
import mindustry.type.Category
import mindustry.world.Block

class CategoryExtenderConfig {
    var catList: Seq<CatConfig> = Seq()
}

class CatConfig {
    var name: String = ""
    var block: Seq<Block> = Seq()
}

object CategoryExtenderJsonParse {
    var config = CategoryExtenderConfig()
    var newCat: MutableMap<String, Category> = mutableMapOf()

    fun load() {
        //追加Category
        config.catList.forEach {
            newCat[it.name] = registerCategory(it.name)
        }
        //注册Category
        newCat.forEach {
            applyCategory(it.value)
        }
        //修改Block的Category
        config.catList.forEach { config ->
            config.block.forEach {
                Vars.content.block(it.name).category = newCat[config.name]
            }
        }
    }

    fun loadJson(mod: String) {
        try {
            val modInfo = Vars.mods.locateMod(mod)
            if (modInfo == null) {
                Log.err("[CategoryExtenderJsonParse] 未找到mod: @", mod)
                return
            }

            val configFi = modInfo.root.child("content").child("config").child("CategoryExtenderConfig.json")
            if (!configFi.exists()) {
                Log.info("[CategoryExtenderJsonParse] 未找到 CategoryExtenderConfig.json: @", configFi.path())
                return
            }

            Log.info("[CategoryExtenderJsonParse] 加载自定义建筑分类: @", configFi.path())

            val text = configFi.readString()
            config = Json().fromJson(CategoryExtenderConfig::class.java, text)

            load()

        } catch (e:Exception) {
            Log.err("[CategoryExtenderJsonParse] 尝试加载自定义建筑分类失败:")
            Log.err(e)
        }
    }
}