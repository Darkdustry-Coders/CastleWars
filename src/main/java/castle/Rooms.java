package castle;

import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Tmp;
import castle.components.CastleCosts;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import useful.Bundle;

import static castle.CastleUtils.countUnits;
import static mindustry.Vars.*;
import static mindustry.world.blocks.ConstructBlock.get;

public class Rooms {

    public static final Seq<Room> rooms = new Seq<>();

    public static class Room {
        public int x;
        public int y;

        public int size;
        public int cost;

        public Team team;
        public WorldLabel label;

        public Room() {
            rooms.add(this);
        }

        public void set(int x, int y, int size) {
            this.x = x;
            this.y = y;

            this.size = size;
        }

        public void buy(PlayerData data) {
            data.money -= cost;
        }

        public boolean canBuy(PlayerData data) {
            return data.money >= cost;
        }

        public void update() {}

        public void spawn() {
            world.tile(x, y).getLinkedTilesAs(get(size), tile -> tile.setFloor(Blocks.metalFloor.asFloor()));
        }
    }

    public static class BlockRoom extends Room {
        public final Block block;

        public BlockRoom(Block block, int cost) {
            this.block = block;
            this.cost = cost;

            this.label.text(CastleUtils.getIcon(block) + " :[white] " + cost);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            label.hide();

            var tile = world.tile(x, y);

            tile.setNet(block, team, 0);
            if (!(block instanceof CoreBlock)) tile.build.health(Float.MAX_VALUE);

            Groups.player.each(player -> Call.label(player.con, Bundle.format("events.buy", player, data.player.coloredName()), 1f, x * tilesize, y * tilesize));
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && label.isAdded();
        }
    }

    public static class CoreRoom extends BlockRoom {
        public Block core;

        public CoreRoom(Block core, Block upgrade, int cost) {
            super(upgrade, cost);

            this.core = core;
        }

        @Override
        public void spawn() {
            super.spawn();
            world.tile(x, y).setNet(core, team, 0);
        }
    }

    public static class MinerRoom extends BlockRoom {
        public final Interval interval = new Interval();

        public final Item item;
        public final int amount;

        public MinerRoom(Block drill, Item item) {
            super(drill, CastleCosts.items.get(item));

            this.item = item;
            this.amount = (int) (300f - item.cost * 150f);

            this.label.text("[" + CastleUtils.getIcon(item) + "] : " + cost);
        }

        @Override
        public void update() {
            if (!label.isAdded() && interval.get(300f)) {
                Call.effect(Fx.mineHuge, x * tilesize, y * tilesize, 0f, team.color);
                Call.transferItemTo(null, item, amount, x * tilesize, y * tilesize, team.core());
            }
        }
    }

    public static class UnitRoom extends Room {
        public final UnitType type;
        public final int income;
        public final boolean attack;

        public UnitRoom(UnitType type, int income, boolean attack, int x, int y, int cost) {
            this.cost = cost;

            this.type = type;
            this.income = income;
            this.attack = attack;

            this.label.set(x * tilesize, y * tilesize + 12f);
            this.label.fontSize(2.25f);

            this.label.text(CastleUtils.getIcon(type) + " " + (attack ? "[accent]" + Iconc.modeAttack : "[scarlet]" + Iconc.defense) + "\n[gray]" + cost + "\n[white]" + Iconc.blockPlastaniumCompressor + " : " + (income > 0 ? "[lime]+" : income == 0 ? "[gray]" : "[crimson]") + income);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;

            Tmp.v1.rnd(Math.min(type.hitSize, 48f));

            if (attack) {
                //var spawn = spawns.get(data.player.team()).random();
                //type.spawn(data.player.team(), spawn.worldx() + Tmp.v1.x, spawn.worldy() + Tmp.v1.y);
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
            this.cost = cost;

            this.effect = effect;
            this.duration = duration;
            this.ally = ally;

            this.label.set(x, y + 12f);
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