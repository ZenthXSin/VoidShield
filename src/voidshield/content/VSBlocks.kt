package voidshield.content

import mindustry.content.Items
import mindustry.type.Category
import mindustry.type.ItemStack
import voidshield.world.blocks.HeaterBlock
import voidshield.other.extends.categoryExtend.registerCategory
import voidshield.world.blocks.CorVacuum
import voidshield.world.blocks.HeatCatheter
import voidshield.world.blocks.MicroVoid
import voidshield.world.blocks.VelumSolvent


object VSBlocks {
    var voidShield: Category = registerCategory("voidShield")
    var heat: Category = registerCategory("heat-catheter")
    var heatSink: HeaterBlock? = null

    var evaporativeCooler: HeaterBlock? = null

    var airCooler: HeaterBlock? = null

    var heatCatheter: HeatCatheter? = null

    var velumSolvent: VelumSolvent? = null

    var microVoid: MicroVoid? = null

    var corVacuum: CorVacuum? = null

    fun load() {
        heatSink = HeaterBlock("heatSink").apply {
            size = 2
            hasPower = false
            maxTemperature = 2000f
            heatingRate = -0.5f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        airCooler = HeaterBlock("airCooler").apply {
            size = 3
            consumePower(300f)
            maxTemperature = 2000f
            heatingRate = -2f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        evaporativeCooler = HeaterBlock("evaporative-cooler").apply {
            size = 4
            //consumePower(300f)
            maxTemperature = 2000f
            heatingRate = -5f
            overheatDamage = 1f
            //specificHeat = 5f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        heatCatheter = HeatCatheter("heat-catheter").apply {
            size = 1
            maxTemperature = 2000f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            specificHeat = 1f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        velumSolvent = VelumSolvent("velumSolvent").apply {
            size = 5
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            consumePower(50f)
        }

        microVoid = MicroVoid("microVoid").apply {
            size = 4
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            consumePower(50f)
        }

        corVacuum = CorVacuum("corVacuum").apply {
            size = 8
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            consumePower(50f)
        }

        HeaterBlock("test").apply {
            size = 2
            consumePower(50f)
            maxTemperature = 2000f
            heatingRate = 5f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

    }
}
