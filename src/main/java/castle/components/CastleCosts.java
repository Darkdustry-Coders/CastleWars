package castle.components;

import arc.struct.OrderedMap;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.blocks.defense.turrets.Turret;

public class CastleCosts {

    public static OrderedMap<UnitType, Moneys> units;
    public static OrderedMap<Turret, Integer> turrets;
    public static OrderedMap<Item, Integer> items;
    public static OrderedMap<StatusEffect, Integer> effects;

    public static void load() {
        units = OrderedMap.of(
                UnitTypes.dagger,   new Moneys(50,    0,  10),
                UnitTypes.mace,     new Moneys(150,   1,  50),
                UnitTypes.fortress, new Moneys(650,   4,  200),
                UnitTypes.scepter,  new Moneys(3000,  20, 800),
                UnitTypes.reign,    new Moneys(10000, 45, 1500),

                UnitTypes.crawler,  new Moneys(30,    0,  15),
                UnitTypes.atrax,    new Moneys(180,   1,  60),
                UnitTypes.spiroct,  new Moneys(700,   4,  150),
                UnitTypes.arkyid,   new Moneys(4600,  24, 750),
                UnitTypes.toxopid,  new Moneys(12500, 50, 1750),

                UnitTypes.nova,     new Moneys(70,    0,  15),
                UnitTypes.pulsar,   new Moneys(180,   1,  50),
                UnitTypes.quasar,   new Moneys(650,   4,  175),
                UnitTypes.vela,     new Moneys(3750,  25, 750),
                UnitTypes.corvus,   new Moneys(13500, 60, 1500),

                UnitTypes.risso,    new Moneys(200,   0,  50),
                UnitTypes.minke,    new Moneys(400,   2,  75),
                UnitTypes.bryde,    new Moneys(1000,  5,  250),
                UnitTypes.sei,      new Moneys(3800,  24, 800),
                UnitTypes.omura,    new Moneys(17500, 50, 1750),

                UnitTypes.retusa,   new Moneys(160,   0,  25),
                UnitTypes.oxynoe,   new Moneys(650,   3,  80),
                UnitTypes.cyerce,   new Moneys(1300,  6,  200),
                UnitTypes.aegires,  new Moneys(5300,  24, 1000),
                UnitTypes.navanax,  new Moneys(11000, 70, 2000)
        );

        turrets = OrderedMap.of(
                Blocks.duo,        50,
                Blocks.scatter,    150,
                Blocks.scorch,     100,
                Blocks.hail,       150,
                Blocks.wave,       250,
                Blocks.lancer,     500,
                Blocks.arc,        80,
                Blocks.swarmer,    1850,
                Blocks.salvo,      800,
                Blocks.tsunami,    800,
                Blocks.fuse,       1250,
                Blocks.ripple,     1200,
                Blocks.cyclone,    2650,
                Blocks.foreshadow, 6000,
                Blocks.spectre,    4000,
                Blocks.meltdown,   3500
        );

        items = OrderedMap.of(
                Items.copper,        250,
                Items.lead,          250,
                Items.metaglass,     600,
                Items.graphite,      400,
                Items.sand,          50,
        );
                Items.coal,          50,
                Items.titanium,      800,
                Items.thorium,       1250,
                Items.scrap,         50,
                Items.silicon,       500,
                Items.plastanium,    1500,
                Items.phaseFabric,   1600,
                Items.surgeAlloy,    1800,
                Items.sporePod,      500,
                Items.blastCompound, 750,
                Items.pyratite,      750
        );

        effects = OrderedMap.of(
                StatusEffects.overclock, 5000,
                StatusEffects.overdrive, 16500,
                StatusEffects.boss,      28000
        );
    }

    public static int drop(UnitType type) {
        return units.containsKey(type) ? units.get(type).drop() : -1;
    }

    public record Moneys(int cost, int income, int drop) {}
}
