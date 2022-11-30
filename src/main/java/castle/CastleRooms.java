package castle;

import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import castle.components.*;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.storage.CoreBlock;
import useful.Bundle;

import static castle.CastleUtils.countUnits;
import static mindustry.Vars.*;

public class CastleRooms {

    public static final int size = 10;
    public static final Seq<Room> rooms = new Seq<>();
    public static final ObjectMap<Team, Seq<Tile>> spawns = new ObjectMap<>();

    public static class Room implements Position {
        public int x;
        public int y;

        public int startx;
        public int starty;
        public int endx;
        public int endy;

        public int cost;
        public int size;

        public float offset;
        public Tile tile;

        public WorldLabel label = WorldLabel.create();

        public Room(int x, int y, int cost, int size) {
            this.x = x;
            this.y = y;

            this.startx = x - size / 2;
            this.starty = y - size / 2;
            this.endx = x + size / 2 + size % 2;
            this.endy = y + size / 2 + size % 2;

            this.cost = cost;
            this.size = size;
            this.offset = (size % 2) * tilesize / 2f;
            this.tile = world.tile(x, y);

            this.label.set(getX(), getY());
            this.label.fontSize(1.75f);
            this.label.flags(WorldLabel.flagOutline);
            this.label.add();

            this.spawn();
            rooms.add(this);
        }

        public void update() {}

        public void buy(PlayerData data) {
            data.money -= cost;
        }

        public boolean canBuy(PlayerData data) {
            return data.money >= cost;
        }

        public boolean check(float x, float y) {
            return x > startx * tilesize && y > starty * tilesize && x < endx * tilesize && y < endy * tilesize;
        }

        public float getX() {
            return x * tilesize + offset;
        }

        public float getY() {
            return y * tilesize + offset;
        }

        public void spawn() {
            for (int x = startx; x <= endx; x++)
                for (int y = starty; y <= endy; y++) {
                    var floor = x == startx || y == starty || x == endx || y == endy ? Blocks.metalFloor5 : Blocks.metalFloor;
                    world.tiles.getc(x, y).setFloor(floor.asFloor());
                }
        }
    }

    public static class BlockRoom extends Room {
        public final Block block;
        public final Team team;

        public BlockRoom(Block block, Team team, int x, int y, int cost, int size) {
            super(x, y, cost, size);

            this.block = block;
            this.team = team;
            this.label.text(CastleUtils.getIcon(block) + " :[white] " + cost);
        }

        public BlockRoom(Block block, Team team, int x, int y, int cost) {
            this(block, team, x, y, cost, block.size + 1);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            label.hide();

            tile.setNet(block, team, 0);
            if (!(block instanceof CoreBlock)) tile.build.health(Float.MAX_VALUE);

            Groups.player.each(player -> Call.label(player.con, Bundle.format("events.buy", player, data.player.coloredName()), 1f, getX(), getY()));
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && label.isAdded();
        }
    }

    public static class TurretRoom extends BlockRoom {
        public TurretRoom(Turret block, Team team, int x, int y) {
            super(block, team, x, y, CastleCosts.turrets.get(block));
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);

            var source = world.tile(startx, y);
            int timeOffset = 0;

            var item = content.items().find(block::consumesItem);
            if (item != null) {
                Timer.schedule(() -> {
                    source.setNet(Blocks.itemSource, team, 0);
                    source.build.configure(item);
                    source.build.health(Float.MAX_VALUE);
                }, timeOffset++);
            }

            var liquid = content.liquids().find(block::consumesLiquid);
            if (liquid != null) {
                Timer.schedule(() -> {
                    source.setNet(Blocks.liquidSource, team, 0);
                    source.build.configure(liquid);
                    source.build.health(Float.MAX_VALUE);
                }, timeOffset++);
            }

            if (item != null || liquid != null) {
                Timer.schedule(() -> {
                    Call.effect(Fx.mineHuge, source.worldx(), source.worldy(), 0, team.color);
                    source.removeNet();
                }, timeOffset);
            }
        }
    }

    public static class MinerRoom extends BlockRoom {
        public final Interval interval = new Interval();

        public final Item item;
        public final int amount;

        public MinerRoom(Block drill, Item item, Team team, int x, int y) {
            super(drill, team, x, y, CastleCosts.items.get(item));

            this.item = item;
            this.amount = (int) (300f - item.cost * 150f);

            this.label.text("[" + CastleUtils.getIcon(item) + "] : " + cost);
        }

        @Override
        public void update() {
            if (!label.isAdded() && interval.get(300f)) {
                Call.effect(Fx.mineHuge, getX(), getY(), 0f, team.color);
                Call.transferItemTo(null, item, amount, getX(), getY(), team.core());
            }
        }
    }

    public static class UnitRoom extends Room {
        public final UnitType type;
        public final int income;
        public final boolean attack;

        public UnitRoom(UnitType type, int income, boolean attack, int x, int y, int cost) {
            super(x, y, cost, 4);

            this.type = type;
            this.income = income;
            this.attack = attack;

            this.label.set(getX(), getY() + 12f);
            this.label.fontSize(2.25f);

            this.label.text(CastleUtils.getIcon(type) + " " + (attack ? "[accent]" + Iconc.modeAttack : "[scarlet]" + Iconc.defense) + "\n[gray]" + cost + "\n[white]" + Iconc.blockPlastaniumCompressor + " : " + (income > 0 ? "[lime]+" : income == 0 ? "[gray]" : "[crimson]") + income);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;

            Tmp.v1.rnd(Math.min(type.hitSize, 48f));

            if (attack) {
                var spawn = spawns.get(data.player.team()).random();
                type.spawn(data.player.team(), spawn.worldx() + Tmp.v1.x, spawn.worldy() + Tmp.v1.y);
            } else if (data.player.core() != null) {
                var core = data.player.core();
                type.spawn(data.player.team(), core.x + 48f, core.y + Tmp.v1.y);
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            if (!super.canBuy(data)) return false;

            if (countUnits(data.player.team()) >= Units.getCap(data.player.team())) {
                Bundle.announce(data.player, "rooms.unit.limit");
                return false;
            }

            return true;
        }
    }

    public static class EffectRoom extends Room {
        public final StatusEffect effect;
        public final int duration;
        public final boolean ally;

        public EffectRoom(StatusEffect effect, int duration, boolean ally, int x, int y, int cost) {
            super(x, y, cost, 4);

            this.effect = effect;
            this.duration = duration;
            this.ally = ally;

            this.label.set(getX(), getY() + 12f);
            this.label.fontSize(2.25f);

            this.label.text(CastleUtils.getIcon(effect) + "\n[gray]" + cost);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            Groups.unit.each(unit -> !unit.spawnedByCore && ((ally && unit.team == data.player.team()) || (!ally && unit.team != data.player.team())), unit -> unit.apply(effect, duration));
        }
    }
}