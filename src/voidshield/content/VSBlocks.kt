package voidshield.content

import arc.graphics.Blending
import arc.graphics.Color
import arc.math.Interp
import arc.struct.Seq
import mindustry.content.Items
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.world.draw.DrawBlock
import mindustry.world.draw.DrawDefault
import mindustry.world.draw.DrawGlowRegion
import mindustry.world.draw.DrawMulti
import mindustry.world.draw.DrawParticles
import mindustry.world.draw.DrawPulseShape
import mindustry.world.draw.DrawRegion
import voidshield.world.blocks.heat.HeaterBlock
import voidshield.other.extends.categoryExtend.registerCategory
import voidshield.world.blocks.voidshield.CorVacuum
import voidshield.world.blocks.heat.HeatCatheter
import voidshield.world.blocks.heat.HeatCrossover
import voidshield.world.blocks.heat.HeatRouter
import voidshield.world.blocks.voidshield.MicroVoid
import voidshield.world.blocks.voidshield.VelumSolvent


object VSBlocks {
    var voidShield: Category = registerCategory("voidShield")
    var heat: Category = registerCategory("heat-catheter")
    var heatSink: HeaterBlock? = null

    var evaporativeCooler: HeaterBlock? = null

    var airCooler: HeaterBlock? = null

    var heatCatheter: HeatCatheter? = null

    var heatCrossover: HeatCrossover? = null

    var heatRouter: HeatRouter? = null

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
            hasPower = false
            maxTemperature = 2000f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            specificHeat = 1f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        heatCrossover = HeatCrossover("heat-crossover").apply {
            size = 1
            health = 100
            hasPower = false
            specificHeat = 1f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        heatRouter = HeatRouter("heat-router").apply {
            size = 1
            health = 100
            specificHeat = 1f
            hasPower = false
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
        }

        velumSolvent = VelumSolvent("velumSolvent").apply {
            size = 5
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            consumePower(50f)
        }

        microVoid = MicroVoid("microVoid").apply {
            size = 5
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            drawer = DrawMulti(Seq.with(DrawRegion("-bottom"), DrawDefault(), DrawGlowRegion().apply {
                suffix = "-light"
            }, DrawParticles().apply {
                color = Color.valueOf("fff3d6")
                particles = 15
                particleRad = size * 9f
                fadeMargin = 1f
                particleLife = 180f
                rotateScl = 7f
                alpha = 0.7f
                particleInterp = Interp.PowIn(1.5f)
                blending = Blending.additive
            }))
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
