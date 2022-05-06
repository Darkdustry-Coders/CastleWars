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
                UnitTypes.dagger,   new Moneys(70,    0,  20),
                UnitTypes.mace,     new Moneys(180,   1,  50),
                UnitTypes.fortress, new Moneys(650,   4,  200),
                UnitTypes.scepter,  new Moneys(3000,  20, 850),
                UnitTypes.reign,    new Moneys(10000, 60, 1500),

                UnitTypes.crawler,  new Moneys(50,    0,  15),
                UnitTypes.atrax,    new Moneys(200,   1,  60),
                UnitTypes.spiroct,  new Moneys(700,   4,  250),
                UnitTypes.arkyid,   new Moneys(3850,  20, 750),
                UnitTypes.toxopid,  new Moneys(14000, 50, 1750),

                UnitTypes.nova,     new Moneys(85,    0,  15),
                UnitTypes.pulsar,   new Moneys(150,   1,  50),
                UnitTypes.quasar,   new Moneys(750,   4,  300),
                UnitTypes.vela,     new Moneys(4000,  22, 750),
                UnitTypes.corvus,   new Moneys(15000, 70, 1500),

                UnitTypes.risso,    new Moneys(200,   0,  50),
                UnitTypes.minke,    new Moneys(250,   1,  75),
                UnitTypes.bryde,    new Moneys(1000  ,5,  200),
                UnitTypes.sei,      new Moneys(4250,  24, 900),
                UnitTypes.omura,    new Moneys(15000, 60, 2000),

                UnitTypes.retusa,   new Moneys(150,   0,  50),
                UnitTypes.oxynoe,   new Moneys(700,   4,  80),
                UnitTypes.cyerce,   new Moneys(1650,  6,  200),
                UnitTypes.aegires,  new Moneys(5250,  26, 1000),
                UnitTypes.navanax,  new Moneys(13500, 70, 2000)
        );

        turrets = OrderedMap.of(
                Blocks.duo,        50,
                Blocks.scatter,    150,
                Blocks.scorch,     200,
                Blocks.hail,       250,
                Blocks.wave,       250,
                Blocks.lancer,     300,
                Blocks.arc,        100,
                Blocks.swarmer,    1650,
                Blocks.salvo,      750,
                Blocks.tsunami,    800,
                Blocks.fuse,       1350,
                Blocks.ripple,     1400,
                Blocks.cyclone,    2000,
                Blocks.foreshadow, 6000,
                Blocks.spectre,    4000,
                Blocks.meltdown,   3500
        );

        items = OrderedMap.of(
                Items.copper,        250,
                Items.lead,          300,
                Items.metaglass,     550,
                Items.graphite,      400,
                Items.sand,          50,
                Items.coal,          50,
                Items.titanium,      850,
                Items.thorium,       1150,
                Items.scrap,         50,
                Items.silicon,       500,
                Items.plastanium,    1300,
                Items.phaseFabric,   1250,
                Items.surgeAlloy,    1850,
                Items.sporePod,      500,
                Items.blastCompound, 100,
                Items.pyratite,      100
        );

        effects = OrderedMap.of(
                StatusEffects.overclock, 4000,
                StatusEffects.overdrive, 12500,
                StatusEffects.boss,      20000
        );
    }

    public static int drop(UnitType type) {
        return units.containsKey(type) ? units.get(type).drop() : -1;
    }

    public record Moneys(int cost, int income, int drop) {}
}
