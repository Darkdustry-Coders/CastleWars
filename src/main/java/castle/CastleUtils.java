package castle;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.game.*;
import mindustry.game.MapObjectives.FlagObjective;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.type.unit.ErekirUnitType;
import mindustry.type.unit.NeoplasmUnitType;
import mindustry.world.Block;
import mindustry.world.Tiles;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;

import static mindustry.Vars.*;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;

public class CastleUtils {
    public static Seq<UnitType> revealedUnits = new Seq<>();
    public static boolean generatePlatforms = true;
    public static Seq<Tiles> platformSource = new Seq<>();
    public static Floor shopFloor = Blocks.space.asFloor();

    public static short boatSpawnX = -1;
    public static short boatSpawnY = -1;
    public static short landSpawnX = -1;
    public static short landSpawnY = -1;
    public static short airSpawnX = -1;
    public static short airSpawnY = -1;

    public static boolean any(String[] array, String value) {
        for (var test : array)
            if (test.equals(value))
                return true;
        return false;
    }

    public static void refreshMeta() {
        revealedUnits.clear();
        if (isSerpulo()) revealedUnits.addAll(content.units()
            .select(unit -> !unit.internal
                && !(unit instanceof NeoplasmUnitType || unit instanceof ErekirUnitType)));
        if (isErekir()) revealedUnits.addAll(content.units()
            .select(unit -> !unit.internal
                && (unit instanceof NeoplasmUnitType || unit instanceof ErekirUnitType)));

        generatePlatforms = true;
        platformSource.clear();
        shopFloor = Blocks.space.asFloor();
        boatSpawnX = -1;
        boatSpawnY = -1;

        for (var objective : state.rules.objectives.all) {
            if (objective instanceof FlagObjective flag) {
                if (any((flag.details + "\n" + flag.text + "\n" + flag.flag).split("\n"), "noplatform"))
                    generatePlatforms = false;
                if (flag.flag.startsWith("platformsource ")) {
                    try {
                        String[] args = flag.flag.split(" ");
                        var x = Integer.valueOf(args[1]);
                        var y = Integer.valueOf(args[2]);
                        var replace = args.length < 4 ? null : content.block(args[3]).asFloor();

                        var newSource = new Tiles(6, 6);
                        newSource.fill();
                        newSource.eachTile(tile -> {
                            tile.setFloor(world.tile(tile.x + x, tile.y + y).floor());
                            if (replace != null)
                                world.tile(tile.x + x, tile.y + y).setFloor(replace);
                        });
                        platformSource.add(newSource);
                    } catch (Exception error) {
                        Log.warn("Failed to load custom platform!\n" + error);
                    }
                }
                if (flag.flag.startsWith("shopfloor ")) {
                    try {
                        String[] args = flag.flag.split(" ");
                        shopFloor = content.block(args[1]).asFloor();
                    } catch (Exception error) {
                        Log.warn("Failed to set custom shop floor!\n" + error);
                    }
                }
                if (flag.flag.startsWith("boatspawn ")) {
                    try {
                        String[] args = flag.flag.split(" ");
                        boatSpawnX = Short.valueOf(args[1]);
                        boatSpawnY = Short.valueOf(args[2]);
                    } catch (Exception error) {
                        Log.warn("Failed to set boat spawn!\n" + error);
                        boatSpawnX = -1;
                        boatSpawnY = -1;
                    }
                }
                if (flag.flag.startsWith("landspawn ")) {
                    try {
                        String[] args = flag.flag.split(" ");
                        landSpawnX = Short.valueOf(args[1]);
                        landSpawnY = Short.valueOf(args[2]);
                    } catch (Exception error) {
                        Log.warn("Failed to set land spawn!\n" + error);
                        landSpawnX = -1;
                        landSpawnY = -1;
                    }
                }
                if (flag.flag.startsWith("airspawn ")) {
                    try {
                        String[] args = flag.flag.split(" ");
                        airSpawnX = Short.valueOf(args[1]);
                        airSpawnY = Short.valueOf(args[2]);
                    } catch (Exception error) {
                        Log.warn("Failed to set air spawn!\n" + error);
                        airSpawnX = -1;
                        airSpawnY = -1;
                    }
                }
            }
        }

        if (platformSource.isEmpty()) {
            var newSource = new Tiles(6, 6);
            newSource.fill();
            newSource.eachTile(tile -> tile.setFloor(Blocks.metalFloor.asFloor()));
            platformSource.add(newSource);
        }
    }

    public static void applyRules(Rules rules) {
        rules.waveTimer = rules.waves = rules.waveSending = false;
        rules.pvp = rules.attackMode = rules.polygonCoreProtection = true;

        rules.unitCap = 500;
        rules.unitCapVariable = false;

        rules.dropZoneRadius = 48f;
        rules.buildSpeedMultiplier = 0.5f;

        rules.modeName = "Castle Wars";

        rules.teams.get(Team.sharded).cheat = true;
        rules.teams.get(Team.blue).cheat = true;

        rules.weather.clear();
        rules.bannedBlocks.addAll(content.blocks().select(block -> block instanceof Turret || block instanceof Drill
                || block instanceof UnitFactory || block instanceof CoreBlock || block.group == BlockGroup.logic));
    }

    public static boolean isSerpulo() {
        return state.rules.planet == Planets.serpulo
            || state.rules.planet == Planets.sun
            || state.rules.hiddenBuildItems.isEmpty()
            || !state.rules.hasEnv(Env.scorching);
    }

    public static boolean isErekir() {
        return state.rules.planet == Planets.erekir
            || state.rules.planet == Planets.sun
            || state.rules.hiddenBuildItems.isEmpty()
            || state.rules.hasEnv(Env.scorching);
    }

    public static Block drill(Item item) {
        if (item == Items.lead || item == Items.copper || item == Items.titanium
            || item == Items.metaglass || item == Items.coal || item == Items.scrap || item == Items.plastanium
            || item == Items.surgeAlloy || item == Items.pyratite || item == Items.blastCompound
            || item == Items.sporePod) return Blocks.laserDrill;
        if (item == Items.beryllium || item == Items.tungsten || item == Items.oxide
            || item == Items.carbide || item == Items.fissileMatter || item == Items.dormantCyst) return Blocks.impactDrill;

        return state.rules.hasEnv(Env.scorching) ? Blocks.impactDrill : Blocks.laserDrill;
    }

    public static @Nullable Block upgradeBlock(Block block) {
        if (block == Blocks.coreBastion) return Blocks.coreAcropolis;
        if (block == Blocks.coreShard) return Blocks.coreNucleus;

        return null;
    }

    public static boolean isBreak() {
        return state.gameOver || state.isPaused() || world.isGenerating();
    }

    public static boolean onEnemySide(Teamc teamc) {
        return (teamc.team() == Team.sharded && teamc.y() > world.unitHeight() / 2f)
            || (teamc.team() == Team.blue && teamc.y() < world.unitHeight() / 2f);
    }
}
