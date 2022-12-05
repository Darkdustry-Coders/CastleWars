package castle;

import arc.func.Prov;
import castle.components.CastleCosts;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.type.unit.ErekirUnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.environment.SpawnBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleRooms.*;
import static mindustry.Vars.*;

public class CastleGenerator {

    public static final int unitOffsetX = 5, unitOffsetY = 3, effectOffsetX = 3, effectOffsetY = 6;
    public static int offsetX, offsetY;

    public static void generate() {
        var saved = world.tiles;
        var tiles = world.resize(world.width(), world.height() * 2 + 58);

        // region tiles

        saved.each((x, y) -> {
            var tile = saved.get(x, y);

            var floor = tile.floor();
            var block = !tile.block().hasBuilding() && tile.isCenter() ? tile.block() : Blocks.air;
            var overlay = tile.overlay().needsSurface ? tile.overlay() : Blocks.air;

            addTile(x, y, floor, block, overlay);
            addTile(x, tiles.height - y - 1, floor, block, overlay);
        });

        for (int x = 0; x < saved.width; x++)
            for (int y = saved.height; y < tiles.height - saved.height; y++)
                addTile(x, y, Blocks.space, Blocks.air, Blocks.air);

        // endregion
        // region rooms

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

            if (tile.overlay() instanceof SpawnBlock) {
                // spawns.get(Team.sharded, Seq::new).add(tiles.getc(x, y2 + spawn.size % 2));
                // spawns.get(Team.blue, Seq::new).add(tiles.getc(x, y));
            }
        });

        // endregion

        generateShop(7, saved.height + 6);
    }

    public static void generateShop(int shopX, int shopY) {
        offsetX = offsetY = 0;

        CastleCosts.units.each((type, data) -> {
            boolean isErekir = type instanceof ErekirUnitType;
            if (isErekir == CastleUtils.isSerpulo()) return;

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
        room.set(x, y, 5, Team.derelict); // TODO replace 5 with 3 and fix room.spawn
        room.spawn();

        room.label.y += 12f;
        room.label.fontSize(2.25f);
    }
}