package voidshield.content

import arc.graphics.Blending
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.math.Interp
import arc.struct.Seq
import mindustry.content.Items
import mindustry.content.Liquids
import mindustry.type.Category
import mindustry.type.ItemStack
import mindustry.world.draw.*
import mindustry.world.draw.DrawDefault
import voidshield.core.drawers.DrawHeat
import voidshield.core.extends.category.registerCategory
import voidshield.world.blocks.heat.HeatCatheter
import voidshield.world.blocks.heat.HeatCrossover
import voidshield.world.blocks.heat.HeatRouter
import voidshield.world.blocks.heat.HeaterBlock
import voidshield.world.blocks.voidshield.CorVacuum
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
        heatSink = HeaterBlock("heat-sink").apply {
            size = 2
            hasPower = false
            maxTemperature = 2000f
            heatingRate = -0.5f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            drawer = DrawMulti(Seq.with(
                DrawRegion("-bottom"),
                DrawDefault(),
                DrawRegion("-top"),
                DrawHeat()
            ))
        }

        airCooler = HeaterBlock("air-cooler").apply {
            size = 3
            consumePower(300f)
            maxTemperature = 2000f
            heatingRate = -2f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            drawer = DrawMulti(Seq.with(
                DrawRegion("-bottom"),
                DrawRegion("-rotate").apply { spinSprite = true; rotateSpeed = 10f },
                DrawDefault(),
                DrawHeat()
            ))
        }

        evaporativeCooler = HeaterBlock("evaporative-cooler").apply {
            size = 4
            maxTemperature = 2000f
            heatingRate = -5f
            overheatDamage = 1f
            overheatThreshold = 0.9f
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            drawer = DrawMulti(Seq.with(
                DrawRegion("-bottom"),
                DrawRegion("-rotate").apply { spinSprite = true; rotateSpeed = 10f },
                DrawDefault(),
                DrawHeat()
            ))
            consumePower(8f)
            consumeLiquid(Liquids.water, 2f)
            consumeLiquid(Liquids.cryofluid, 0.8f)
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

        velumSolvent = VelumSolvent("velum-solvent").apply {
            size = 5
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            consumePower(50f)
            drawer = DrawMulti(Seq.with(
                DrawRegion("-bottom"),
                DrawDefault(),
                DrawRegion("-top"),
                DrawHeat()
            ))
        }

        microVoid = MicroVoid("micro-void").apply {
            size = 5
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            hasPower = true
            drawer = DrawMulti(Seq.with(DrawRegion("-bottom"),DrawGlowRegion().apply {
                suffix = "-glow1"
                blending = Blending.additive
                glowScale = 40f
                glowIntensity = 0.8f
                alpha = 0.7f
                color = Color.valueOf("fff3d6")
            }, DrawGlowRegion().apply {
                suffix = "-glow2"
                blending = Blending.additive
                glowScale = 20f
                alpha = 0.6f
                glowIntensity = 0.7f
                color = Color.valueOf("fff3d6")
            }, DrawFade().apply {
                Draw.color(Color.valueOf("fff3d6"))
                suffix = "-glow1"
                alpha = 0.3f
                scale = 8f
            } , DrawDefault(), DrawGlowRegion().apply {
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
            }, DrawHeat()))
            consumePower(50f)
        }

        corVacuum = CorVacuum("cor-vacuum").apply {
            size = 8
            requirements(voidShield, ItemStack.with(Items.copper, 150, Items.lead, 100))
            drawer = DrawMulti(Seq.with(DrawRegion("-bottom"), DrawPlasma().apply {
                plasmas = 5
                plasma1 = Color.valueOf("fff3d6")
                plasma2 = Color.valueOf("f69583")
            },DrawGlowRegion().apply {
                suffix = "-glow1"
                alpha = 0.2f
                glowScale = 120f
                color = Color.valueOf("d888cf")
            }, DrawGlowRegion().apply {
                suffix = "-glow2"
                alpha = 0.5f
                color = Color.valueOf("fff3d6")
                glowScale = 40f
                alpha = 0.4f
            }, DrawDefault(), DrawHeat("-glow2").apply { alpha = 0.5f }, DrawRegion("-top")))
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
