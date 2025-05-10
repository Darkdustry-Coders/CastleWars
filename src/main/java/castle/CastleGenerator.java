package castle;

import arc.func.Prov;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import castle.CastleCosts;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.type.UnitType;
import mindustry.type.unit.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.environment.SpawnBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleRooms.*;
import static castle.Main.*;
import static mindustry.Vars.*;

public class CastleGenerator {

    public static final int unitLimitX = 5, unitLimitY = 3, effectLimitX = 4;
    public static int offsetX, offsetY;

    public static void generate(boolean serpulo) {
        var saved = world.tiles;
        world.resize(world.width(), world.height() * 2 + 58);

        // Set half height for further use
        halfHeight = saved.height;

        saved.each((x, y) -> {
            var tile = saved.get(x, y);

            var floor = tile.floor();
            var block = !tile.block().hasBuilding() && tile.isCenter() ? tile.block() : Blocks.air;
            var overlay = tile.overlay().needsSurface ? tile.overlay() : Blocks.air;

            addTile(x, y, floor, block, overlay);
            addTile(x, world.tiles.height - y - 1, floor, block, overlay);
        });

        for (int x = 0; x < saved.width; x++)
            for (int y = saved.height; y < world.tiles.height - saved.height; y++)
                addTile(x, y, Blocks.space, Blocks.air, Blocks.air);

        spawns.clear();
        state.teams.getActive().each(data -> data.cores.each(state.teams::unregisterCore));

        saved.each((x, y) -> {
            var tile = saved.get(x, y);
            if (!tile.isCenter())
                return;

            if (tile.block() instanceof CoreBlock core) {
                var upgrade = serpulo ? Blocks.coreNucleus : Blocks.coreAcropolis;
                addRoom(x, y, upgrade.size, () -> new CoreRoom(core, upgrade, 5000));
            }

            if (tile.block() instanceof Turret turret && CastleCosts.turrets.containsKey(turret))
                addRoom(x, y, turret.size, () -> new BlockRoom(turret, CastleCosts.turrets.get(turret)));

            if (tile.build instanceof SorterBuild sorter && CastleCosts.items.containsKey(sorter.config())) {
                var drill = serpulo ? Blocks.laserDrill : Blocks.impactDrill;
                addRoom(x, y, drill.size,
                        () -> new MinerRoom(drill, sorter.config(), CastleCosts.items.get(sorter.config())));
            }

            if (tile.overlay() instanceof SpawnBlock)
                spawns.add(x, y);
        });

        generateShop(7, saved.height + 6, serpulo);
    }

    public static void generateShop(int shopX, int shopY, boolean serpulo) {
        offsetX = offsetY = 0;

        // Spawn unit rooms
        CastleCosts.units.each((type, data) -> {
            if ((type instanceof ErekirUnitType || type instanceof NeoplasmUnitType) == serpulo)
                return;

            addShopRoom(shopX + offsetX * 9, shopY + offsetY * 18, new UnitRoom(type, data, true));
            addShopRoom(shopX + offsetX * 9, shopY + offsetY * 18 + 9, new UnitRoom(type, data, false));

            if (++offsetX % unitLimitX > 0)
                return;
            if (++offsetY % unitLimitY > 0)
                offsetX -= unitLimitX;
            else
                offsetY -= unitLimitY;
        });

        if (offsetX % unitLimitX > 0) {
            offsetX += unitLimitX;
            offsetX -= offsetX % unitLimitX;

            if (++offsetY % unitLimitY > 0)
                offsetX -= unitLimitX;
            else
                offsetY -= unitLimitY;
        }

        int unitOffsetX = offsetX, unitOffsetY = offsetY;
        offsetY = 0;

        // Spawn effect rooms
        CastleCosts.effects.each((effect, data) -> {
            addShopRoom(shopX + offsetX * 9, shopY + unitOffsetY * 18 + offsetY * 9, new EffectRoom(effect, data));

            if ((++offsetX - unitOffsetX) % effectLimitX == 0) {
                offsetX -= effectLimitX;
                offsetY++;
            }
        });
    }

    private static void addTile(int x, int y, Block floor, Block block, Block overlay) {
        world.tiles.set(x, y, new Tile(x, y, floor, overlay, block));
    }

    private static void addRoom(int x, int y, int size, Prov<Room> create) {
        var sharded = create.get();
        sharded.set(x, y, size + 2, Team.sharded);
        sharded.spawn();

        var blue = create.get();
        blue.set(x, world.height() - y - 2 + size % 2, size + 2, Team.blue);
        blue.spawn();
    }

    private static void addShopRoom(int x, int y, Room room) {
        room.set(x, y, 5, Team.derelict);
        room.spawn();

        room.label.y += 12f;
        room.label.fontSize(2.25f);
    }

    public static class Spawns {
        public Seq<Point2> sharded = new Seq<>();
        public Seq<Point2> blue = new Seq<>();

        public Point2 get(Team team) {
            return team == Team.sharded ? sharded.random() : blue.random();
        }

        public void add(int x, int y) {
            sharded.add(new Point2(x, world.height() - y - 1));
            blue.add(new Point2(x, y));
        }

        public void clear() {
            sharded.clear();
            blue.clear();
        }

        private boolean validFor(UnitType type, Point2 pos) {
            int x = (int) pos.x;
            int y = (int) pos.y;

            var tile = world.tile(x, y);
            if (tile == null)
                return false;
            // TODO: Check if tile is in death zone.
            if (!type.flying && !tile.block().isAir())
                return false;
            if (type.naval && !tile.floor().isLiquid)
                return false;

            return true;
        }

        public void spawn(Team team, UnitType type) {
            Point2 spawn;
            do {
                spawn = get(team).cpy().add(Mathf.range(tilesize), Mathf.range(tilesize));
            } while (!validFor(type, spawn));
            type.spawn(team, spawn.x * tilesize, spawn.y * tilesize);
        }

        public boolean within(Tile tile) {
            for (var spawn : sharded)
                if (within(tile, spawn))
                    return true;
            for (var spawn : blue)
                if (within(tile, spawn))
                    return true;

            return false;
        }

        public boolean within(Tile tile, Point2 point) {
            return tile.within(point.x * tilesize, point.y * tilesize, state.rules.dropZoneRadius);
        }

        public void draw() {
            sharded.each(spawn -> draw(spawn, Team.sharded));
            blue.each(spawn -> draw(spawn, Team.blue));
        }

        public void draw(Point2 spawn, Team team) {
            for (int deg = 0; deg < 360; deg += 10)
                Call.effect(Fx.mineBig,
                        spawn.x * tilesize + Mathf.cosDeg(deg) * state.rules.dropZoneRadius,
                        spawn.y * tilesize + Mathf.sinDeg(deg) * state.rules.dropZoneRadius, 0f, team.color);
        }
    }
}
