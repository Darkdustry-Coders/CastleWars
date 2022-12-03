package castle;

import arc.func.*;
import castle.components.CastleCosts;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.type.unit.ErekirUnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.environment.SpawnBlock;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.Rooms.*;
import static castle.CastleUtils.isSerpulo;
import static mindustry.Vars.*;

public class CastleGenerator {

    public static final int unitOffsetX = 5, unitOffsetY = 3, effectOffsetX = 3, effectOffsetY = 6;
    public static int offsetX, offsetY;

    public static void generate() {
        var saved = world.tiles;
        var tiles = world.resize(world.width(), world.height() * 2 + 65);

        for (int x = 0; x < saved.width; x++) {
            for (int y = 0; y < saved.height; y++) {
                var tile = saved.getc(x, y);

                var floor = tile.floor();
                var overlay = tile.overlay().needsSurface ? tile.overlay() : Blocks.air;
                var block = !tile.block().hasBuilding() && tile.isCenter() ? tile.block() : Blocks.air;

                tiles.set(x, y, new Tile(x, y, floor, overlay, block));
                tiles.set(x, tiles.height - y - 1, new Tile(x, tiles.height - y - 1, floor, overlay, block));
            }
        }

        for (int x = 0; x < saved.width; x++)
            for (int y = saved.height; y < tiles.height - saved.height; y++)
                tiles.set(x, y, new Tile(x, y, Blocks.space, Blocks.air, Blocks.air));

        for (int x = 0; x < saved.width; x++) {
            for (int y = 0; y < saved.height; y++) {
                var tile = saved.getc(x, y);
                if (!tile.isCenter()) continue;

                if (tile.block() instanceof CoreBlock core) {
                    var upgrade = isSerpulo() ? Blocks.coreNucleus : Blocks.coreAcropolis;

                    addRoom(x, y, upgrade.size, () -> new CoreRoom(core, upgrade, 5000));
                }

                if (tile.block() instanceof Turret turret) {
                    if (!turret.environmentBuildable() || !CastleCosts.turrets.containsKey(turret)) continue;

                    addRoom(x, y, turret.size, () -> new BlockRoom(turret, 0));
                }

                if (tile.build instanceof SorterBuild sorter) {
                    if (!CastleCosts.items.containsKey(sorter.config())) continue;

                    var drill = isSerpulo() ? Blocks.laserDrill : Blocks.impactDrill;

                    addRoom(x, y, drill.size, () -> new MinerRoom(drill, sorter.config()));
                }

                if (tile.overlay() instanceof SpawnBlock) {
                    //spawns.get(Team.sharded, Seq::new).add(tiles.getc(x, y2 + spawn.size % 2));
                    //spawns.get(Team.blue, Seq::new).add(tiles.getc(x, y));
                }
            }
        }

        generateShop(8, saved.height + 7);
    }

    public static void generateShop(int shopX, int shopY) {
        offsetX = offsetY = 0;

        CastleCosts.units.each((type, data) -> {
            boolean isErekir = type instanceof ErekirUnitType;
            if ((isErekir && isSerpulo()) || (!isErekir && !isSerpulo())) return;

            new UnitRoom(type, data.income(), true, shopX + offsetX * size, shopY + offsetY * size * 2, data.cost());
            new UnitRoom(type, -data.income(), false, shopX + offsetX * size, shopY + offsetY * size * 2 + size, data.cost());

            if (++offsetX % unitOffsetX != 0) return;
            if (++offsetY % unitOffsetY != 0) offsetX -= unitOffsetX;
            else offsetY -= unitOffsetY;
        });

        offsetY = 0;

        CastleCosts.effects.each((effect, data) -> {
            new EffectRoom(effect, data.duration(), data.ally(), shopX + offsetX * size, shopY + offsetY * size, data.cost());

            if (++offsetX % unitOffsetX % effectOffsetX != 0) return;
            if (++offsetY % effectOffsetY != 0) offsetX -= effectOffsetX;
            else offsetY -= effectOffsetY;
        });
    }

    public static void addRoom(int x, int y, int size, Prov<Room> create) {
        var first = create.get();
        first.set(x, y, size, Team.sharded);
        first.spawn();

        var second = create.get();
        second.set(x, world.tiles.height - y - 2 + size % 2, size, Team.blue);
        second.spawn();
    }
}