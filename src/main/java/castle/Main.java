package castle;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Interval;
import castle.components.*;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleRooms.*;
import static castle.CastleUtils.*;
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

        Bundle.load();
        CastleCosts.load();
        CastleIcons.load();

        world = new CastleWorld();

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

        Events.on(BlockDestroyEvent.class, event -> {
            if (event.tile.block() instanceof CoreBlock && event.tile.team().cores().size <= 1) {
                Events.fire(new GameOverEvent(event.tile.team() == Team.sharded ? Team.blue : Team.sharded));
            }
        });

        Events.on(UnitDestroyEvent.class, event -> {
            int income = CastleCosts.drop(event.unit.type);
            if (income <= 0 || event.unit.spawnedByCore) return;
            datas.each(data -> {
                if (data.player.team() != event.unit.team) {
                    data.money += income;
                    Call.label(data.player.con, "[lime]+[accent] " + income, 2f, event.unit.x, event.unit.y);
                }
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

        Events.run(Trigger.update, () -> {
            if (isBreak() || state.isPaused()) return;

            Groups.unit.each(unit -> unit.isFlying() && !unit.spawnedByCore && (unit.floorOn() == null || unit.floorOn() == Blocks.space), unit -> {
                Call.effect(Fx.unitEnvKill, unit.x, unit.y, 0f, Color.scarlet);
                Call.unitDespawn(unit);
            });

            datas.each(PlayerData::update);
            rooms.each(Room::update);

            if (interval.get(60f)) {
                datas.each(PlayerData::updateMoney);
                spawns.each((team, spawns) -> spawns.each(spawn -> {
                    for (int deg = 0; deg < 36; deg++) {
                        float x = spawn.worldx() + Mathf.cosDeg(deg * 10) * state.rules.dropZoneRadius;
                        float y = spawn.worldy() + Mathf.sinDeg(deg * 10) * state.rules.dropZoneRadius;
                        Call.effect(Fx.mineBig, x, y, 0f, team.color);
                    }
                }));

                if (--timer <= 0) Events.fire(new GameOverEvent(Team.derelict));
            }
        });
    }
}