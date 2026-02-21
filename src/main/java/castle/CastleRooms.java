package castle;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.util.*;

import mindustry.Vars;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.type.UnitType;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.storage.CoreBlock;

import castle.CastleCosts.*;
import static castle.CastleUtils.*;
import static castle.Main.*;

import useful.Bundle;

public class CastleRooms {
    public static class Room {
        public int x, y;
        public int size, cost, offset;

        public Team team;
        public WorldLabel label = WorldLabel.create();

        public void set(int x, int y, int size, Team team) {
            this.x = x;
            this.y = y;

            this.size = size;
            this.team = team;
            this.offset = size / 2 - (1 - size % 2);
        }

        public void spawn() {
            if (generatePlatforms && !(this instanceof UnitRoom || this instanceof EffectRoom))
                Vars.world.tile(x, y).getLinkedTilesAs(ConstructBlock.get(size), tile -> tile.setFloor(Blocks.metalFloor.asFloor()));
            else if (this instanceof UnitRoom || this instanceof EffectRoom)
                Vars.world.tile(x, y).getLinkedTilesAs(ConstructBlock.get(size), tile -> {
                    var source = platformSource.random();
                    tile.setFloor(source.get(tile.x + offset - x, tile.y + offset - y).floor());
                });

            label.set(drawX(), drawY());
            label.text(toString());
            label.fontSize(Math.min(size, 2f));
            label.flags(WorldLabel.flagOutline);
            label.add();

            rooms.add(this);
        }

        public void buy(PlayerData data) {
            data.money -= cost;
        }

        public boolean canBuy(PlayerData data) {
            return data.money >= cost;
        }

        public boolean check(Tile tile) {
            return Structs.inBounds(tile.x - this.x + offset, tile.y - this.y + offset, size, size);
        }

        public float drawX() {
            return (x + (1 - size % 2) / 2f) * Vars.tilesize;
        }

        public float drawY() {
            return (y + (1 - size % 2) / 2f) * Vars.tilesize + 2;
        }

        public void update() {}
    }

    public static class BlockRoom extends Room {
        public final @Nullable Block starting;
        public final Block block;

        public BlockRoom(Block block, int cost) {
            this.block = block;
            this.cost = cost;
            this.starting = null;
        }

        public BlockRoom(Block block, int cost, @Nullable Block starting) {
            this.block = block;
            this.cost = cost;
            this.starting = starting;
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            label.hide();

            final int[][] coreItems;
            var tile = Vars.world.tile(x, y);
            tile.setNet(block, team, 0);

            if (!(block instanceof CoreBlock)) {
                tile.build.health(Float.MAX_VALUE);
                coreItems = null;
            }
            else {
                coreItems = new int[][] { new int[Vars.content.items().size] };
                team.core().items().each((item, count) -> {
                    var t = coreItems[0];
                    t[item.id] = count;
                });
            }

            Bundle.label(1f, drawX(), drawY(), "rooms.block.bought", data.player.coloredName());

            if (coreItems != null)
                for (int id = 0; id < coreItems[0].length; id++)
                    team.core().items().set(Vars.content.item(id), coreItems[0][id]);
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && label.isAdded();
        }

        @Override
        public String toString() {
            return block.emoji() + " : " + cost;
        }

        @Override
        public float drawY() {
            return super.drawY() + 1;
        }

        @Override
        public void spawn() {
            super.spawn();
            if (starting != null) 
                Vars.world.tile(x, y).setBlock(starting, team, 0);
        }
    }

    public static class MinerRoom extends BlockRoom {
        public final Interval interval = new Interval();
        public final Item item;

        public MinerRoom(Block drill, Item item, int cost) {
            super(drill, cost);
            this.item = item;
        }

        @Override
        public void update() {
            if (label.isAdded() || !interval.get(300f)) return;

            Call.effect(Fx.mineHuge, drawX(), drawY(), 0f, team.color);
            Call.transferItemTo(null, item, 48, drawX(), drawY(), team.core());
        }

        @Override
        public String toString() {
            return "[" + item.emoji() + "]\n" + cost;
        }
    }

    public static class UnitRoom extends Room {
        public final UnitType type;
        public final boolean attack;
        public final int income;

        public UnitRoom(UnitType type, UnitData data, boolean attack) {
            this.type = type;
            this.cost = data.cost();

            this.attack = attack;
            this.income = attack ? data.income() : -data.income();
        }

        private void spawnUnit(PlayerData data, int x, int y, UnitType type) {
            int yPosition = -10;
            int iterations = 0;
            if(x<0) {
                x = (int) data.player.core().x + 48;
                y = (int) data.player.core().y;
            }
            else {
                x = x*8;
                y = (data.team().team == Team.blue ? Vars.world.height() - y : y) * 8;
            }
            Point2 coordinate = new Point2(x, yPosition);
            while(!validForSpawn(type,coordinate) && iterations < 48){
                yPosition = y + iterations - 24;
                coordinate = new Point2(x, yPosition);
                iterations++;
            }
            Unit unit = type.spawn(
                data.player.team(),
                x,coordinate.y);
            Bundle.label(1f, unit.getX(), unit.getY(), "rooms.unit.bought", data.player.coloredName());
        }


        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;
            if (attack) spawns.spawn(data.player, data.player.team(), type);
            else if (type.naval) {
                spawnUnit(data, boatSpawn.x, boatSpawn.y, type);
            }
            else if (!type.naval && !type.flying) {
                spawnUnit(data, airSpawn.x, airSpawn.y, type);
            }
            else if (type.flying) {
                spawnUnit(data, landSpawn.x, landSpawn.y, type);
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            if (!super.canBuy(data)) return false;
            if(defenseCap == 0) defenseCap = (short) Vars.state.rules.unitCap;
            if(attackCap == 0) attackCap = (short) Vars.state.rules.unitCap;
            if (attack){
                if(data.team().getUnitCountAttack()>=attackCap ||
                        data.team().getUnitCount()>=Vars.state.rules.unitCap) {
                    Bundle.announce(data.player, "rooms.unit.limit");
                    return false;
                }
            }
            else{
                if(data.team().getUnitCountDefense()>=defenseCap ||
                        data.team().getUnitCount()>=Vars.state.rules.unitCap){
                    Bundle.announce(data.player, "rooms.unit.limit");
                    return false;
                }
            }
            return true;
        }
        @Override
        public String toString() {
            return type.emoji() + " " + (attack ? "[accent]\uE865" : "[scarlet]\uE84D") +
                "\n[gray]" + cost +
                "\n[white]\uF8BA : " + (income > 0 ? "[lime]+" : income == 0 ? "[gray]" : "[scarlet]") + income;
        }
    }

    public static class EffectRoom extends Room {
        public final StatusEffect effect;

        public final float delay;
        public final int duration;
        public final boolean ally;

        public EffectRoom(StatusEffect effect, EffectData data) {
            this.effect = effect;
            this.cost = data.cost();
            this.duration = data.duration();
            this.ally = data.ally();
            this.delay = data.delay();
        }

        @Override
        public boolean canBuy(PlayerData data) {
            if (!super.canBuy(data)) return false;
            if (data.team().locked(this)) {
                Bundle.announce(data.player, "rooms.effect.limit");
                return false;
            }
            return true;
        }

        @Override
        public void buy(PlayerData data) {
            if (!data.team().lock(this)) return;

            super.buy(data);
            Groups.unit.each(unit -> ally == (unit.team == data.player.team()), unit -> unit.apply(effect, duration * 60f));

            // Visual things
            for (int rotation = 0; rotation < 36; rotation++)
                Time.run(rotation, () -> Call.effect(Fx.coreLandDust, data.player.x, data.player.y, Mathf.random(360f), effect.color));

            Bundle.label(1f, drawX(), drawY(), "rooms.effect.bought", data.player.coloredName());
        }

        @Override
        public String toString() {
            return effect.emoji() +
                "\n[gray]" + cost +
                "\n" + (ally ? "[stat]\uE804" : "[negstat]\uE805") + duration + "s";
        }
    }
}
