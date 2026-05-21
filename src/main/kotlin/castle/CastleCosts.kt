package castle

import arc.struct.OrderedMap

import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.type.Item
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.blocks.defense.turrets.Turret
import mindustry.world.Block
import castle.CastleUtils.drill
import mindurka.util.UnsafeNull
import mindurka.util.nodecl

object CastleCosts {
    lateinit var units: OrderedMap<UnitType, UnitData>
    lateinit var effects: OrderedMap<StatusEffect, EffectData>

    lateinit var turrets: OrderedMap<Turret, Int>
    lateinit var items: OrderedMap<Item, ItemData>

    fun load() {
        units = OrderedMap.of(
            UnitTypes.dagger, UnitData(60, 0, 15),
            UnitTypes.mace, UnitData(170, 1, 50),
            UnitTypes.fortress, UnitData(550, 4, 200),
            UnitTypes.scepter, UnitData(3000, 20, 750),
            UnitTypes.reign, UnitData(10000, 60, 1500),

            UnitTypes.crawler, UnitData(50, 0, 10),
            UnitTypes.atrax, UnitData(180, 1, 60),
            UnitTypes.spiroct, UnitData(600, 4, 200),
            UnitTypes.arkyid, UnitData(4300, 20, 1000),
            UnitTypes.toxopid, UnitData(13000, 50, 1750),

            UnitTypes.nova, UnitData(75, 0, 15),
            UnitTypes.pulsar, UnitData(180, 1, 50),
            UnitTypes.quasar, UnitData(600, 4, 200),
            UnitTypes.vela, UnitData(3800, 22, 750),
            UnitTypes.corvus, UnitData(15000, 70, 1500),

            UnitTypes.risso, UnitData(175, 1, 24),
            UnitTypes.minke, UnitData(250, 1, 70),
            UnitTypes.bryde, UnitData(1000, 5, 200),
            UnitTypes.sei, UnitData(5500, 24, 900),
            UnitTypes.omura, UnitData(15000, 65, 2000),

            UnitTypes.retusa, UnitData(130, 0, 50),
            UnitTypes.oxynoe, UnitData(625, 3, 150),
            UnitTypes.cyerce, UnitData(1400, 6, 200),
            UnitTypes.aegires, UnitData(7000, 16, 3000),
            UnitTypes.navanax, UnitData(13500, 70, 1350),

            UnitTypes.flare, UnitData(80, 0, 20),
            UnitTypes.horizon, UnitData(200, 1, 70),
            UnitTypes.zenith, UnitData(700, 4, 150),
            UnitTypes.antumbra, UnitData(4100, 23, 850),
            UnitTypes.eclipse, UnitData(12000, 60, 1250),

            UnitTypes.poly, UnitData(350, 1, 90),
            UnitTypes.mega, UnitData(900, 5, 200),
            UnitTypes.quad, UnitData(5250, 27, 900),
            UnitTypes.oct, UnitData(13000, 65, 1300),

            UnitTypes.stell, UnitData(260, 2, 100),
            UnitTypes.locus, UnitData(800, 4, 250),
            UnitTypes.precept, UnitData(2000, 12, 600),
            UnitTypes.vanquish, UnitData(5000, 26, 1000),
            UnitTypes.conquer, UnitData(10000, 60, 1700),

            UnitTypes.merui, UnitData(280, 2, 100),
            UnitTypes.cleroi, UnitData(900, 4, 400),
            UnitTypes.anthicus, UnitData(2450, 14, 750),
            UnitTypes.tecta, UnitData(5500, 27, 1100),
            UnitTypes.collaris, UnitData(11000, 55, 1900),

            UnitTypes.elude, UnitData(300, 2, 110),
            UnitTypes.avert, UnitData(900, 4, 300),
            UnitTypes.obviate, UnitData(2200, 13, 750),
            UnitTypes.quell, UnitData(4750, 25, 1500),
            UnitTypes.disrupt, UnitData(11500, 45, 2300),

            UnitTypes.renale, UnitData(1500, 6, 500),
            UnitTypes.latum, UnitData(20000, 80, 5000)
        )

        effects = OrderedMap.of(
            StatusEffects.overclock, EffectData(4000, 20, true, 20f),
            StatusEffects.overdrive, EffectData(12000, 30, true, 30f),
            StatusEffects.boss, EffectData(36000, 40, true, 40f),
            StatusEffects.shielded, EffectData(72000, 10, true, 10f),

            StatusEffects.sporeSlowed, EffectData(12000, 25, false, 25f),
            StatusEffects.electrified, EffectData(24000, 20, false, 20f),
            StatusEffects.sapped, EffectData(36000, 15, false, 15f),
            StatusEffects.unmoving, EffectData(96000, 5, false, 25f)
        )

        turrets = OrderedMap.of(
            Blocks.duo, 50,
            Blocks.scatter, 150,
            Blocks.scorch, 200,
            Blocks.hail, 250,
            Blocks.wave, 250,
            Blocks.lancer, 250,
            Blocks.arc, 100,
            Blocks.swarmer, 1450,
            Blocks.salvo, 600,
            Blocks.fuse, 1350,
            Blocks.ripple, 1400,
            Blocks.cyclone, 2000,
            Blocks.foreshadow, 4500,
            Blocks.spectre, 4000,
            Blocks.meltdown, 3500,
            Blocks.segment, 1000,
            Blocks.parallax, 500,
            Blocks.tsunami, 800,

            Blocks.breach, 500,
            Blocks.diffuse, 800,
            Blocks.sublimate, 2500,
            Blocks.titan, 2000,
            Blocks.disperse, 3200,
            Blocks.afflict, 2250,
            Blocks.lustre, 4500,
            Blocks.scathe, 4250,
            Blocks.smite, 5000,
            Blocks.malign, 12500
        )

        items = OrderedMap.of(
            Items.copper, ItemData(250, 300f, 48, drill(Items.copper) ?: Blocks.laserDrill),
            Items.lead, ItemData(300, 300f, 48, drill(Items.lead) ?: Blocks.laserDrill),
            Items.metaglass, ItemData(500, 300f, 48, drill(Items.metaglass) ?: Blocks.laserDrill),
            Items.graphite, ItemData(400, 300f, 48, drill(Items.graphite) ?: Blocks.laserDrill),
            Items.titanium, ItemData(750, 300f, 48, drill(Items.titanium) ?: Blocks.laserDrill),
            Items.thorium, ItemData(1000, 300f, 48, drill(Items.thorium) ?: Blocks.laserDrill),
            Items.silicon, ItemData(500, 300f, 48, drill(Items.silicon) ?: Blocks.laserDrill),
            Items.plastanium, ItemData(1200, 300f, 48, drill(Items.plastanium) ?: Blocks.laserDrill),
            Items.phaseFabric, ItemData(1500, 300f, 48, drill(Items.phaseFabric) ?: Blocks.laserDrill),
            Items.surgeAlloy, ItemData(1800, 300f, 48, drill(Items.surgeAlloy) ?: Blocks.laserDrill),

            Items.beryllium, ItemData(500, 300f, 48, drill(Items.beryllium) ?: Blocks.impactDrill),
            Items.tungsten, ItemData(1000, 300f, 48, drill(Items.tungsten) ?: Blocks.impactDrill),
            Items.oxide, ItemData(1500, 300f, 48, drill(Items.oxide) ?: Blocks.impactDrill),
            Items.carbide, ItemData(1800, 300f, 48, drill(Items.carbide) ?: Blocks.impactDrill)
        )
    }


    data class UnitData(@JvmField var cost: Int, @JvmField var income: Int, var drop: Int)


    data class EffectData(@JvmField var cost: Int, @JvmField var duration: Int, @JvmField var ally: Boolean, @JvmField var delay: Float)


    data class ItemData(@JvmField var cost: Int,@JvmField var interval: Float,@JvmField var amount: Int, var drill: Block)
}