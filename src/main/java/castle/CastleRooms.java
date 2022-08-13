package castle;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.Tmp;
import castle.components.CastleCosts;
import castle.components.CastleIcons;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleUtils.countUnits;
import static castle.Main.findLocale;
import static castle.components.Bundle.format;
import static castle.components.Bundle.get;
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
            this.label.flags(WorldLabel.flagBackground);
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
                    Block floor = x == startx || y == starty || x == endx || y == endy ? Blocks.metalFloor5 : Blocks.metalFloor;
                    world.tiles.getc(x, y).setFloor(floor.asFloor());
                }
        }
    }

    public static class BlockRoom extends Room {
        public Block block;
        public Team team;

        public boolean bought;

        public BlockRoom(Block block, Team team, int x, int y, int cost, int size) {
            super(x, y, cost, size);

            this.block = block;
            this.team = team;
            this.label.text(CastleIcons.get(block.name) + " :[white] " + cost);
        }

        public BlockRoom(Block block, Team team, int x, int y, int cost) {
            this(block, team, x, y, cost, block.size + 1);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            label.hide();
            bought = true;

            tile.setNet(block, team, 0);
            if (!(block instanceof CoreBlock)) tile.build.health(Float.MAX_VALUE);

            Groups.player.each(p -> Call.label(p.con, format("events.buy", findLocale(p), data.player.coloredName()), 1f, getX(), getY()));
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && !bought;
        }
    }

    public static class TurretRoom extends BlockRoom {
        public TurretRoom(Turret block, Team team, int x, int y) {
            super(block, team, x, y, CastleCosts.turrets.get(block));
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);

            Tile source = world.tile(startx, y);
            int timeOffset = 0;

            Item item = content.items().find(i -> block.consumesItem(i));
            if (item != null) {
                Timer.schedule(() -> {
                    source.setNet(Blocks.itemSource, team, 0);
                    source.build.configure(item);
                    source.build.health(Float.MAX_VALUE);
                }, timeOffset++);
            }

            Liquid liquid = content.liquids().find(l -> block.consumesLiquid(l));
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
        public Item item;
        public int amount;
        public Interval interval = new Interval();

        public MinerRoom(Block drill, Item item, Team team, int x, int y) {
            super(drill, team, x, y, CastleCosts.items.get(item));

            this.item = item;
            this.amount = (int) (300f - item.cost * 150f);

            this.label.text("[" + CastleIcons.get(item.name) + "] : " + cost);
        }

        @Override
        public void update() {
            if (bought && interval.get(300f)) {
                float randX = getX() + Mathf.range(12f), randY = getY() + Mathf.range(12f);
                Call.effect(Fx.mineHuge, randX, randY, 0f, team.color);
                Call.transferItemTo(null, item, amount, randX, randY, team.core());
            }
        }
    }

    public static class UnitRoom extends Room {
        public UnitType type;
        public UnitRoomType roomType;
        public int income;

        public UnitRoom(UnitType type, UnitRoomType roomType, int income, int x, int y, int cost) {
            super(x, y, cost, 4);
            if (true) {
                this.type = type;
                this.roomType = roomType;
                this.income = income;
            }

            this.label.set(getX(), getY() + 12f);
            this.label.fontSize(2.25f);
            this.label.text(" ".repeat((String.valueOf(cost).length() + String.valueOf(income).length() + 2) / 2) +
                    CastleIcons.get(type.name) + " " + roomType.icon +
                    "\n[gray]" + cost +
                    "\n[white]" + Iconc.blockPlastaniumCompressor + " : " + (income > 0 ? "[lime]+" : income == 0 ? "[gray]" : "[crimson]") + income);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;

            Tmp.v1.rnd(Math.min(type.hitSize, 48f));

            if (roomType == UnitRoomType.attack) {
                Tile spawn = spawns.get(data.player.team()).random();
                type.spawn(data.player.team(), spawn.worldx() + Tmp.v1.x, spawn.worldy() + Tmp.v1.y);
            } else if (data.player.core() != null) {
                Building core = data.player.core();
                type.spawn(data.player.team(), core.x + 48f, core.y + Tmp.v1.y);
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            if (!super.canBuy(data)) return false;

            if (income < 0 && data.income + income < 0) {
                Call.announce(data.player.con, get("rooms.unit.too-low-income", data.locale));
                return false;
            }

            if (countUnits(data.player.team()) >= Units.getCap(data.player.team())) {
                Call.announce(data.player.con, get("rooms.unit.limit", data.locale));
                return false;
            }

            return true;
        }

        public enum UnitRoomType {
            attack("[accent]" + Iconc.modeAttack), defend("[scarlet]" + Iconc.defense);

            public String icon;

            UnitRoomType(String icon) {
                this.icon = icon;
            }
        }
    }

    public static class EffectRoom extends Room {
        public StatusEffect effect;
        public Interval interval = new Interval();

        public EffectRoom(StatusEffect effect, int x, int y, int cost) {
            super(x, y, cost, 4);

            this.effect = effect;

            this.label.set(getX(), getY() + 12f);
            this.label.fontSize(2.25f);
            this.label.text("[accent]" + Strings.capitalize(effect.name) + "\n" + "effect" + "\n[white]" + CastleIcons.get(effect.name) + " : [gray]" + cost);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.player.team().data().units.each(unit -> unit.apply(effect, Float.POSITIVE_INFINITY));
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && interval.get(60f);
        }
    }
}
