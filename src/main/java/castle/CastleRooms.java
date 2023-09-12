package castle;

import arc.math.Mathf;
import arc.util.*;
import castle.components.CastleCosts.*;
import castle.components.PlayerData;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.storage.CoreBlock;
import useful.Bundle;

import static castle.Main.*;
import static mindustry.Vars.*;

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
            world.tile(x, y).getLinkedTilesAs(ConstructBlock.get(size), tile -> tile.setFloor(Blocks.metalFloor.asFloor()));

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
            return (x + (1 - size % 2) / 2f) * tilesize;
        }

        public float drawY() {
            return (y + (1 - size % 2) / 2f) * tilesize;
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

            if (!(block instanceof CoreBlock)) tile.build.health(Float.MAX_VALUE);

            Bundle.label(1f, drawX(), drawY(), "rooms.block.bought", data.player.coloredName());
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && data.player.team() == team && label.isAdded();
        }

        @Override
        public String toString() {
            return block.emoji() + " : " + cost;
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
            world.tile(x, y).setBlock(core, team);
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

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;

            if (attack) spawns.spawn(data.player.team(), type);
            else if (data.player.core() != null) {
                var core = data.player.core();
                type.spawn(data.player.team(), core.x + 48f, core.y + Mathf.range(48f));
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            if (!super.canBuy(data)) return false;

            if (data.player.team().data().unitCount >= state.rules.unitCap) {
                Bundle.announce(data.player, "rooms.unit.limit");
                return false;
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

        public final int duration;
        public final boolean ally;

        public EffectRoom(StatusEffect effect, EffectData data) {
            this.effect = effect;

            this.cost = data.cost();
            this.duration = data.duration();
            this.ally = data.ally();
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            Groups.unit.each(unit -> ally == (unit.team == data.player.team()), unit -> unit.apply(effect, duration * 60f));

            // Visual things
            for (int rotation = 0; rotation < 36; rotation++)
                Time.run(rotation, () -> Call.effect(Fx.coreLandDust, data.player.x, data.player.y, Mathf.random(360f), effect.color));
        }

        @Override
        public String toString() {
            return effect.emoji() +
                    "\n[gray]" + cost +
                    "\n" + (ally ? "[stat]\uE804" : "[negstat]\uE805") + duration + "s";
        }
    }
}