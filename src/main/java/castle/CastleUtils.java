package castle;

import arc.Core;
import arc.util.io.Writes;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.math.geom.Point2;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.game.*;
import mindustry.game.MapObjectives.FlagObjective;
import mindustry.gen.Teamc;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.type.unit.ErekirUnitType;
import mindustry.type.unit.NeoplasmUnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import mindustry.gen.Call;
import static mindustry.Vars.*;

import static castle.Main.syncStream;
import static castle.Main.dataStream;
public class CastleUtils {

    private static int betterGroundValid = 0;

    public static Seq<UnitType> revealedUnits = new Seq<>();
    public static boolean generatePlatforms = true;
    public static Seq<Tiles> platformSource = new Seq<>();
    public static Floor shopFloor = Blocks.space.asFloor();

    public static Point2 boatSpawn = new Point2(-1, -1);
    public static Point2 landSpawn = new Point2(-1, -1);
    public static Point2 airSpawn = new Point2(-1, -1);

    public enum unitCapType {
        NONE,
        ATTACK_ONLY,
        DEFENSE_ONLY,
        BOTH
    }

    public static int isDivideCap = 1;
    public static unitCapType capType = unitCapType.DEFENSE_ONLY;
    public static short defenseCap = 0;
    public static short attackCap = 0;
    public static short unitCap = 500;

    public static boolean any(String[] array, String value) {
        for (var test : array)
            if (test.equals(value))
                return true;
        return false;
    }

    private static unitCapType getCapType() {
        if (isDivideCap == 0) {
            return unitCapType.NONE;
        }
        boolean hasAttack = attackCap > 0;
        boolean hasDefense = defenseCap > 0;
        if (hasAttack && hasDefense) {
            return unitCapType.BOTH;
        } else if (hasAttack) {
            return unitCapType.ATTACK_ONLY;
        } else if (hasDefense) {
            return unitCapType.DEFENSE_ONLY;
        } else {
            return unitCapType.NONE;
        }
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

        boatSpawn = new Point2(-1, -1);
        landSpawn = new Point2(-1, -1);
        airSpawn = new Point2(-1, -1);

        defenseCap = 100;
        attackCap = 0;
        unitCap = 500;
        isDivideCap = 1;
        betterGroundValid = 0;

        for (var objective : state.rules.objectives.all) {
            if (objective instanceof FlagObjective flag) {
                String flagName = flag.flag.toLowerCase();
                if (any((flag.details + "\n" + flag.text + "\n" + flagName).split("\n"), "noplatform"))
                    generatePlatforms = false;
                if (flagName.startsWith("platformsource ")) {
                    try {
                        String[] args = flagName.split(" ");
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
                if (flagName.startsWith("shopfloor ")) {
                    try {
                        String[] args = flagName.split(" ");
                        shopFloor = content.block(args[1]).asFloor();
                    } catch (Exception error) {
                        Log.warn("Failed to set custom shop floor!\n" + error);
                    }
                }
                if (flagName.startsWith("boatspawn ")) {
                    try {
                        String[] args = flagName.split(" ");
                        boatSpawn = new Point2(Short.parseShort(args[1]), Short.parseShort(args[2]));
                    } catch (Exception error) {
                        Log.warn("Failed to set boat spawn!\n" + error);
                    }
                }
                if (flagName.startsWith("landspawn ")) {
                    try {
                        String[] args = flagName.split(" ");
                        landSpawn = new Point2(Short.parseShort(args[1]), Short.parseShort(args[2]));
                    } catch (Exception error) {
                        Log.warn("Failed to set land spawn!\n" + error);
                    }
                }
                if (flagName.startsWith("airspawn ")) {
                    try {
                        String[] args = flagName.split(" ");
                        airSpawn = new Point2(Short.parseShort(args[1]), Short.parseShort(args[2]));
                    } catch (Exception error) {
                        Log.warn("Failed to set air spawn!\n" + error);
                    }
                }
                if (flagName.startsWith("defensecap ")) {
                    try {
                        String[] args = flagName.split(" ");
                        defenseCap = Short.valueOf(args[1]);
                    } catch (Exception error) {
                        Log.warn("Failed to set Defense Cap!\n" + error);
                        defenseCap = 150;
                    }
                }
                if (flagName.startsWith("attackcap ")) {
                    try {
                        String[] args = flagName.split(" ");
                        attackCap = Short.valueOf(args[1]);
                    } catch (Exception error) {
                        Log.warn("Failed to set Attack Cap!\n" + error);
                        attackCap = 350;
                    }
                }
                if (flagName.startsWith("unitcap ")) {
                    try {
                        String[] args = flagName.split(" ");
                        unitCap = Short.valueOf(args[1]);
                        if(unitCap > 500) unitCap = 500;
                    } catch (Exception error) {
                        Log.warn("Failed to set Unit Cap!\n" + error);
                        unitCap = 500;
                    }
                }
                if (flagName.startsWith("isdividecap ")) {
                    try {
                        String[] args = flagName.split(" ");
                        isDivideCap = Integer.parseInt(args[1]);
                    } catch (Exception error) {
                        Log.warn("Failed to set is divided Cap!\n" + error);
                        isDivideCap = 1;
                    }
                }
                if (flagName.startsWith("bettergroundvalid ")) {
                    try {
                        String[] args = flagName.split(" ");
                        betterGroundValid = Integer.parseInt(args[1]);
                    } catch (Exception error) {
                        Log.warn("Failed to set state of valid to spawn check!\n" + error);
                        betterGroundValid = 0;
                    }
                }
            }
        }
        capType = getCapType();

        if (platformSource.isEmpty()) {
            var newSource = new Tiles(6, 6);
            newSource.fill();
            newSource.eachTile(tile -> tile.setFloor(Blocks.metalFloor.asFloor()));
            platformSource.add(newSource);
        }
    }

    public static void syncBlock(Building block_sync){
        try{
            final Building block = block_sync;
            Core.app.post(() -> {
                try {
                    syncStream.reset();
                    dataStream.writeInt(block.pos());
                    dataStream.writeShort(block.block().id);
                    block.writeAll(Writes.get(dataStream));
                    dataStream.close();
                    Call.blockSnapshot((short) 1, syncStream.toByteArray());
                    syncStream.reset();
                } catch (Exception ohshit) {
                    throw new RuntimeException(ohshit);
            }});
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static boolean validForSpawn(UnitType type, Point2 pos) {
        var tile = world.tile(pos.x, pos.y);
        // TODO: Check if tile is in death zone.
        return tile != null &&
            (type.flying || tile.block().isAir()) &&
            (!type.naval || tile.floor().isLiquid) &&
            ((type.naval || type.flying) || tile.floor().drownTime == 0.0 || betterGroundValid != 1);
    }

    public static boolean withinPointDef(Tile tile, Point2 point, int distance) {
        int ySecondPos = (Vars.world.height() - point.y);
        return (tile.within(point.x * tilesize, point.y * tilesize, distance) || tile.within(point.x * tilesize, ySecondPos * tilesize, distance));
    }

    public static void applyRules(Rules rules) {
        rules.waveTimer = rules.waves = rules.waveSending = false;
        rules.pvp = rules.attackMode = rules.polygonCoreProtection = true;

        rules.unitCap = unitCap;
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