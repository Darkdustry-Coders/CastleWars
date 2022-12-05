package castle;

import arc.Events;
import arc.struct.Seq;
import arc.util.Interval;
import castle.CastleRooms.Room;
import castle.CastleGenerator.Spawns;
import castle.components.CastleCosts;
import castle.components.PlayerData;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import useful.Bundle;

import static castle.CastleUtils.*;
import static castle.components.CastleCosts.units;
import static castle.components.PlayerData.datas;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final Interval interval = new Interval();

    public static final Seq<Room> rooms = new Seq<>();
    public static final Spawns spawns = new Spawns();

    public static int timer = 0;

    @Override
    public void init() {
        content.units().each(unit -> unit.playerControllable, type -> {
            type.payloadCapacity = 0f;
            type.controller = unit -> new CastleCommandAI();
        });

        content.statusEffects().each(effect -> effect.permanent = false);

        Bundle.load(Main.class);
        CastleCosts.load();

        netServer.admins.addActionFilter(action -> {
            if (action.tile == null) return true;
            if (spawns.within(action.tile)) return false;

            return action.tile.build == null || action.tile.build.health != Float.POSITIVE_INFINITY;
        });

        Events.on(PlayerJoin.class, event -> {
            var data = PlayerData.getData(event.player.uuid());
            if (data != null) data.handlePlayerJoin(event.player);
            else datas.add(new PlayerData(event.player));
        });

        Events.on(UnitDestroyEvent.class, event -> {
            if (!units.containsKey(event.unit.type) || event.unit.spawnedByCore) return;

            int income = units.get(event.unit.type).drop();
            datas.each(data -> data.player.team() != event.unit.team, data -> {
                data.money += income;
                Call.label(data.player.con, "[lime]+[accent] " + income, 2f, event.unit.x, event.unit.y);
            });
        });

        Events.on(ResetEvent.class, event -> {
            rooms.clear();
            datas.filter(data -> data.player.con.isConnected());
            datas.each(PlayerData::reset);
        });

        Events.on(PlayEvent.class, event -> CastleUtils.applyRules(state.rules));

        Events.on(WorldLoadEndEvent.class, event -> {
            CastleUtils.checkPlanet();
            CastleGenerator.generate();

            timer = 45 * 60;
        });

        Events.run(Trigger.update, () -> {
            if (isBreak() || state.isPaused()) return;

            Groups.unit.each(unit -> !unit.spawnedByCore && (unit.floorOn() == null || unit.floorOn().solid), Call::unitEnvDeath);

            datas.each(PlayerData::update);
            rooms.each(Room::update);

            if (!interval.get(60f)) return;

            datas.each(PlayerData::updateMoney);
            spawns.draw();

            if (--timer <= 0) Events.fire(new GameOverEvent(Team.derelict));
        });
    }
}