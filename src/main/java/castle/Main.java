package castle;

import arc.Events;
import arc.struct.Seq;
import arc.util.Timer;
import castle.CastleGenerator.Spawns;
import castle.CastleRooms.Room;
import mindustry.ai.ControlPathfinder;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;
import useful.Bundle;

import static castle.CastleCosts.*;
import static castle.CastleUtils.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final Seq<Room> rooms = new Seq<>();
    public static final Spawns spawns = new Spawns();

    public static int timer, halfHeight;

    @Override
    public void init() {
        content.statusEffects().each(effect -> effect.permanent = false);

        content.units().each(type -> type.playerControllable, type -> {
            type.payloadCapacity = 0f;
            type.controller = unit -> new CastleCommandAI();
        });

        UnitTypes.omura.abilities.clear();

        UnitTypes.renale.pathCost = ControlPathfinder.costLegs;
        UnitTypes.latum.pathCost = ControlPathfinder.costLegs;

        Bundle.load(getClass());
        CastleCosts.load();

        netServer.admins.addActionFilter(action -> {
            if (action.tile == null) return true;
            if (spawns.within(action.tile)) return false;

            return !(action.tile.block() instanceof Turret || action.tile.block() instanceof Drill);
        });

        Events.on(PlayerJoin.class, event -> {
            var data = PlayerData.getData(event.player);
            if (data == null) {
                PlayerData.datas.add(new PlayerData(event.player));
                return;
            }

            data.player = event.player;
        });

        Events.on(TapEvent.class, event -> {
            if (event.player.team().core() == null) return;
            var data = PlayerData.getData(event.player);
            if (data == null) return; // Why

            rooms.each(room -> room.check(event.tile) && room.canBuy(data), room -> room.buy(data));
        });

        Events.on(UnitDestroyEvent.class, event -> {
            if (!units.containsKey(event.unit.type)) return;

            int income = units.get(event.unit.type).drop();
            PlayerData.datas.each(data -> data.player.team() != event.unit.team && data.player.team().core() != null, data -> {
                data.money += income;
                Call.label(data.player.con, "[lime]+[accent] " + income, 1f, event.unit.x, event.unit.y);
            });
        });

        Events.on(PlayEvent.class, event -> CastleUtils.applyRules(state.rules));

        Events.on(ResetEvent.class, event -> {
            rooms.clear();
            PlayerData.datas.retainAll(data -> data.player.con.isConnected()).each(PlayerData::reset);
            TeamData.datas.clear();

            timer = 45 * 60;
        });

        Events.on(WorldLoadEndEvent.class, event -> CastleGenerator.generate(CastleUtils.isSerpulo()));

        Timer.schedule(() -> {
            if (isBreak()) return;

            PlayerData.datas.each(PlayerData::update);
            rooms.each(Room::update);

            Groups.unit.each(unit -> {
                if (unit.spawnedByCore)
                    return false;

                if (!world.tiles.in(unit.tileX(), unit.tileY()))
                    return true;

                return unit.tileY() >= halfHeight && unit.tileY() <= world.height() - halfHeight - 1;
            }, Call::unitEnvDeath);
        }, 0f, 0.1f);

        Timer.schedule(() -> {
            if (isBreak()) return;

            PlayerData.datas.each(PlayerData::updateMoney);
            spawns.draw();

            if (--timer == 0) Events.fire(new GameOverEvent(Team.derelict));
        }, 0f, 1f);
    }
}
