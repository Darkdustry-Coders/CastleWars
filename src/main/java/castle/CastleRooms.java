package castle;

import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Structs;
import arc.util.Tmp;
import castle.components.CastleCosts;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.core.World;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.storage.CoreBlock;
import useful.Bundle;

import static castle.CastleUtils.countUnits;
import static mindustry.Vars.*;

public class CastleRooms {

    public static final Seq<Room> rooms = new Seq<>();

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
            world.tile(x, y).getLinkedTilesAs(ConstructBlock.get(size + 2), tile -> {
                var floor = check(tile.x, tile.y) ? Blocks.metalFloor : Blocks.metalFloor5;
                tile.setFloor(floor.asFloor());
            });

            label.set(x * tilesize, y * tilesize);
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

        public boolean check(float x, float y) {
            return Structs.inBounds(World.toTile(x) - this.x + offset, World.toTile(y) - this.y + offset, size, size);
        }

        public boolean check(int x, int y) {
            return Structs.inBounds(x - this.x + offset, y - this.y + offset, size, size);
        }

        public void update() {}
    }

    public static class BlockRoom extends Room {

        public final Block block;

        public BlockRoom(Block block, int cost) {
            this.block = block;
            this.cost = cost;
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            label.hide();

            var tile = world.tile(x, y);

            tile.setNet(block, team, 0);
            if (block instanceof CoreBlock == false) tile.build.health(Float.MAX_VALUE);

            Bundle.label(1f, x * tilesize, y * tilesize, "events.buy", data.player.coloredName());
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && label.isAdded();
        }

        @Override
        public String toString() {
            return CastleUtils.getIcon(block) + " : " + cost;
        }
    }

    public static class CoreRoom extends BlockRoom {

        public final Block core;

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
        }

        @Override
        public void update() {
            if (label.isAdded() || !interval.get(300f)) return;

            Call.effect(Fx.mineHuge, x * tilesize, y * tilesize, 0f, team.color);
            Call.transferItemTo(null, item, amount, x * tilesize, y * tilesize, team.core());
        }

        @Override
        public String toString() {
            return "[" + CastleUtils.getIcon(item) + "] : " + cost;
        }
    }

    public static class UnitRoom extends Room {

        public final UnitType type;
        public final int income;
        public final boolean attack;

        public UnitRoom(UnitType type, int income, boolean attack, int cost) {
            this.cost = cost;

            this.type = type;
            this.income = income;
            this.attack = attack;
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

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(CastleUtils.getIcon(type) + " ").append(attack ? "[accent]" + Iconc.modeAttack : "[scarlet]" + Iconc.defense)
                    .append("\n[gray]" + cost + "\n[white]")
                    .append(Iconc.blockPlastaniumCompressor + " : ").append(income > 0 ? "[lime]+" : income == 0 ? "[gray]" : "[crimson]").append(income)
                    .toString();
        }
    }

    public static class EffectRoom extends Room {

        public final StatusEffect effect;
        public final int duration;
        public final boolean ally;

        public EffectRoom(StatusEffect effect, int duration, boolean ally, int cost) {
            this.cost = cost;

            this.effect = effect;
            this.duration = duration;
            this.ally = ally;
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            Groups.unit.each(unit -> !unit.spawnedByCore && ((ally && unit.team == data.player.team()) || (!ally && unit.team != data.player.team())), unit -> unit.apply(effect, duration));
        }

        @Override
        public String toString() {
            return CastleUtils.getIcon(effect) + "\n[gray]" + cost;
        }
    }
}