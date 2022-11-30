package castle;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Interval;
import castle.components.CastleCosts;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;
import useful.Bundle;

import static castle.CastleRooms.*;
import static castle.CastleUtils.*;
import static castle.components.CastleCosts.units;
import static castle.components.PlayerData.datas;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final int roundTime = 45 * 60;

    public static final Interval interval = new Interval();

    @Override
    public void init() {
        content.units().each(unit -> unit.playerControllable, unit -> {
            unit.payloadCapacity = 0f;
            unit.controller = u -> new CastleCommandAI();
        });

        content.statusEffects().each(statusEffect -> statusEffect.permanent = false);

        Bundle.load(Main.class);
        CastleCosts.load();

        netServer.admins.addActionFilter(action -> {
            if (action.tile == null) return true;

            for (var entry : spawns)
                for (var tile : entry.value)
                    if (tile.dst(action.tile) <= state.rules.dropZoneRadius) return false;

            return !(action.tile.block() instanceof Turret) && !(action.tile.block() instanceof Drill) && action.tile.block() != Blocks.itemSource && action.tile.block() != Blocks.liquidSource;
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
            spawns.clear();
            datas.filter(data -> data.player.con.isConnected());
            datas.each(PlayerData::reset);
        });

        Events.on(PlayEvent.class, event -> CastleUtils.applyRules(state.rules));

        Events.on(WorldLoadEvent.class, event -> CastleUtils.timer = roundTime);
        Events.on(WorldLoadEndEvent.class, event -> {
            CastleUtils.checkPlanet();
            CastleGenerator.generate();
        });

        Events.run(Trigger.update, () -> {
            if (isBreak() || state.isPaused()) return;

            Groups.unit.each(unit -> !unit.spawnedByCore && (unit.floorOn() == null || unit.floorOn().solid), unit -> {
                Call.effect(Fx.unitEnvKill, unit.x, unit.y, 0f, Color.scarlet);
                Call.unitDespawn(unit);
            });

            datas.each(PlayerData::update);
            rooms.each(Room::update);

            if (interval.get(60f)) {
                datas.each(PlayerData::updateMoney);
                spawns.each((team, spawns) -> spawns.each(spawn -> {
                    for (int deg = 0; deg < 36; deg++)
                        Call.effect(Fx.mineBig, spawn.worldx() + Mathf.cosDeg(deg * 10) * state.rules.dropZoneRadius, spawn.worldy() + Mathf.sinDeg(deg * 10) * state.rules.dropZoneRadius, 0f, team.color);
                }));

                if (--timer <= 0) Events.fire(new GameOverEvent(Team.derelict));
            }
        });
    }
}