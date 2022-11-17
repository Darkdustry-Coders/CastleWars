package castle;

import arc.struct.Seq;
import arc.util.Structs;
import castle.components.CastleCosts;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Team;
import mindustry.type.unit.ErekirUnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Sorter.SorterBuild;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleRooms.*;
import static castle.CastleUtils.*;
import static mindustry.Vars.world;

public class CastleGenerator {

    public static int offsetX, offsetY;

    public static void generate() {
        var saved = world.tiles;
        var tiles = world.resize(world.width(), world.height() * 2 + size * 13 / 2);

        for (int x = 0; x < saved.width; x++)
            for (int y = saved.height; y < tiles.height - saved.height; y++)
                tiles.set(x, y, new Tile(x, y, Blocks.space, Blocks.air, Blocks.air));

        for (int x = 0; x < saved.width; x++) {
            for (int y = 0; y < saved.height; y++) {
                var tile = saved.getc(x, y);

                var floor = tile.floor();
                var overlay = tile.overlay().needsSurface ? tile.overlay() : Blocks.air;
                var wall = !tile.block().hasBuilding() && tile.isCenter() ? tile.block() : Blocks.air;

                tiles.set(x, y, new Tile(x, y, floor, overlay, wall));
                tiles.set(x, tiles.height - y - 1, new Tile(x, tiles.height - y - 1, floor, overlay, wall));
            }
        }

        for (int x = 0; x < saved.width; x++) {
            for (int y = 0; y < saved.height; y++) {
                var tile = saved.getc(x, y);
                if (!tile.isCenter()) continue;

                int y2 = tiles.height - y - 2 + tile.block().size % 2;

                if (tile.block() instanceof CoreBlock) {
                    tiles.getc(x, y).setNet(CastleUtils.planet.defaultCore, Team.sharded, 0);
                    tiles.getc(x, y2).setNet(CastleUtils.planet.defaultCore, Team.blue, 0);

                    var core = isSerpulo() ? Blocks.coreNucleus : Blocks.coreAcropolis;

                    new BlockRoom(core, Team.sharded, x, y, 5000);
                    new BlockRoom(core, Team.blue, x, tiles.height - y - 2 + core.size % 2, 5000);
                }

                if (tile.block() instanceof Turret turret) {
                    boolean isErekirTurret = Structs.contains(turret.requirements, stack -> Planets.serpulo.hiddenItems.contains(stack.item));
                    if (isErekir() && !isErekirTurret || isSerpulo() && isErekirTurret) continue;

                    if (!CastleCosts.turrets.containsKey(turret)) continue;

                    new TurretRoom(turret, Team.sharded, x, y);
                    new TurretRoom(turret, Team.blue, x, y2);
                }

                if (tile.build instanceof SorterBuild sorter) {
                    var item = sorter.config();
                    if (!CastleCosts.items.containsKey(item)) continue;

                    var drill = isSerpulo() ? Blocks.laserDrill : Blocks.impactDrill;

                    new MinerRoom(drill, item, Team.sharded, x, y);
                    new MinerRoom(drill, item, Team.blue, x, tiles.height - y - 2 + drill.size % 2);
                }

                if (tile.overlay() == Blocks.spawn) {
                    spawns.get(Team.sharded, Seq::new).add(tiles.getc(x, y2));
                    spawns.get(Team.blue, Seq::new).add(tiles.getc(x, y));
                }
            }
        }

        generateShop(8, saved.height + 7);
    }

    public static void generateShop(int shopX, int shopY) {
        offsetX = offsetY = 0; // Сбрасываем offset, оставшийся от генерации старой карты

        CastleCosts.units.each((type, money) -> {
            boolean isErekirUnit = type instanceof ErekirUnitType;
            if (isErekir() && !isErekirUnit || isSerpulo() && isErekirUnit) return;

            new UnitRoom(type, UnitRoom.UnitRoomType.attack, money.income(), shopX + offsetX * size, shopY + offsetY * size * 2, money.cost());
            new UnitRoom(type, UnitRoom.UnitRoomType.defend, -money.income(), shopX + offsetX * size, shopY + offsetY * size * 2 + size, money.cost());

            if (++offsetX % 5 != 0) return;
            if (++offsetY % 3 != 0) offsetX -= 5;
            else offsetY -= 3;
        });

        CastleCosts.effects.each((effect, cost) -> {
            new EffectRoom(effect, shopX + offsetX * size, shopY + offsetY * size * 2, cost);

            if (++offsetX % 5 != 0) return;
            if (++offsetY % 3 != 0) offsetX -= 5;
            else offsetY -= 3;
        });
    }
}