package castle;

import arc.func.Prov;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import castle.components.CastleCosts;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.type.unit.ErekirUnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.environment.SpawnBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleRooms.*;
import static castle.Main.spawns;
import static mindustry.Vars.*;

public class CastleGenerator {

    public static final int unitOffsetX = 5, unitOffsetY = 3, effectOffsetX = 3, effectOffsetY = 6;
    public static int offsetX, offsetY;

    public static void generate() {
        var saved = world.tiles;
        world.resize(world.width(), world.height() * 2 + 58);

        // region tiles

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

        // endregion
        // region rooms

        spawns.clear();

        saved.each((x, y) -> {
            var tile = saved.get(x, y);
            if (!tile.isCenter()) return;

            if (tile.block() instanceof CoreBlock core) {
                var upgrade = CastleUtils.isSerpulo() ? Blocks.coreNucleus : Blocks.coreAcropolis;
                addRoom(x, y, upgrade.size, () -> new CoreRoom(core, upgrade, 5000));
            }

            if (tile.block() instanceof Turret turret && turret.environmentBuildable() && CastleCosts.turrets.containsKey(turret))
                addRoom(x, y, turret.size, () -> new BlockRoom(turret, 0));

            if (tile.build instanceof SorterBuild sorter) {
                if (!CastleCosts.items.containsKey(sorter.config())) return;

                var drill = CastleUtils.isSerpulo() ? Blocks.laserDrill : Blocks.impactDrill;
                addRoom(x, y, drill.size, () -> new MinerRoom(drill, sorter.config()));
            }

            if (tile.overlay() instanceof SpawnBlock) spawns.add(x, y);
        });

        // endregion

        generateShop(7, saved.height + 6);
    }

    public static void generateShop(int shopX, int shopY) {
        offsetX = offsetY = 0;

        CastleCosts.units.each((type, data) -> {
            if (type instanceof ErekirUnitType == CastleUtils.isSerpulo()) return;

            addShopRoom(shopX + offsetX * 9, shopY + offsetY * 18,     new UnitRoom(type, data.income(), true, data.cost()));
            addShopRoom(shopX + offsetX * 9, shopY + offsetY * 18 + 9, new UnitRoom(type, -data.income(), false, data.cost()));

            if (++offsetX % unitOffsetX != 0) return;
            if (++offsetY % unitOffsetY != 0) offsetX -= unitOffsetX;
            else offsetY -= unitOffsetY;
        });

        offsetY = 0;

        CastleCosts.effects.each((effect, data) -> {
            addShopRoom(shopX + offsetX * 9, shopY + offsetY * 9, new EffectRoom(effect, data.duration(), data.ally(), data.cost()));

            if (++offsetX % unitOffsetX % effectOffsetX != 0) return;
            if (++offsetY % effectOffsetY != 0) offsetX -= effectOffsetX;
            else offsetY -= effectOffsetY;
        });
    }

    private static void addTile(int x, int y, Block floor, Block block, Block overlay) {
        world.tiles.set(x, y, new Tile(x, y, floor, overlay, block));
    }

    private static void addRoom(int x, int y, int size, Prov<Room> create) {
        var first = create.get();
        first.set(x, y, size, Team.sharded);
        first.spawn();

        var second = create.get();
        second.set(x, world.tiles.height - y - 2 + size % 2, size, Team.blue);
        second.spawn();
    }

    private static void addShopRoom(int x, int y, Room room) {
        room.set(x, y, 3, Team.derelict);
        room.spawn();

        room.label.y += 12f;
        room.label.fontSize(2.25f);
    }

    public static class Spawns {

        public Seq<Point2> sharded = new Seq<>();
        public Seq<Point2> blue = new Seq<>();

        public void add(int x, int y) {
            sharded.add(new Point2(x, world.height() - y - 2));
            blue.add(new Point2(x, y));
        }

        public void clear() {
            sharded.clear();
            blue.clear();
        }

        public boolean within(Tile tile) {
            for (Point2 spawn : sharded) if (within(tile, spawn)) return true;
            for (Point2 spawn : blue) if (within(tile, spawn)) return true;

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
            for (int deg = 0; deg < 360; deg += 10) Call.effect(Fx.mineBig,
                    spawn.x * tilesize + Mathf.cosDeg(deg) * state.rules.dropZoneRadius,
                    spawn.y * tilesize + Mathf.sinDeg(deg) * state.rules.dropZoneRadius, 0f, team.color);
        }
    }
}