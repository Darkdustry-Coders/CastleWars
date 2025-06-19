package castle;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;
import arc.util.io.ReusableByteOutStream;
import arc.util.io.Writes;
import castle.CastleGenerator.Spawns;
import castle.CastleRooms.Room;
import mindustry.Vars;
import mindustry.ai.ControlPathfinder;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.defense.turrets.ContinuousLiquidTurret.ContinuousLiquidTurretBuild;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.production.Drill;
import useful.Bundle;

import static castle.CastleCosts.*;
import static castle.CastleUtils.*;
import static mindustry.Vars.*;

import java.io.DataOutputStream;
import java.io.IOException;

public class Main extends Plugin {

    public static final Seq<Room> rooms = new Seq<>();
    public static final Spawns spawns = new Spawns();

    public static int timer, halfHeight;

    @Override
    public void init() {
        try {
            netServer.getClass().getDeclaredField("syncStream").setAccessible(true);
            netServer.getClass().getDeclaredField("dataStream").setAccessible(true);
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        }

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
            if (action.tile == null)
                return true;
            if (spawns.within(action.tile))
                return false;

            return !((action.tile.block() instanceof Turret && action.type != ActionType.depositItem)
                    || action.tile.block() instanceof Drill);
        });

        Events.on(PlayerJoin.class, event -> {
            var data = PlayerData.getData(event.player);
            if (data == null) {
                PlayerData.datas.add(new PlayerData(event.player));
                return;
            }

            data.player = event.player;
        });

        Events.on(PlayerConnectionConfirmed.class, event -> {
            try {
                var netServerSyncStream = (ReusableByteOutStream) (netServer.getClass().getDeclaredField("syncStream").get(netServer));
                var netServerDataStream = (DataOutputStream) (netServer.getClass().getDeclaredField("dataStream").get(netServer));
                Groups.build.each(b -> {
                    if (b instanceof TurretBuild t) {
                        if (t.ammo.size > 1) {
                            Core.app.post(() -> {
                                try {
                                    netServerSyncStream.reset();
                                    netServerDataStream.writeInt(t.pos());
                                    netServerDataStream.writeShort(t.block().id);
                                    t.writeAll(Writes.get(netServerDataStream));
                                    netServerDataStream.close();
                                    Call.blockSnapshot((short) 1, netServerSyncStream.toByteArray());
                                    netServerSyncStream.reset();
                                } catch (IOException e) {
                                    Log.err(e);
                                }
                            });
                        }
                    }
                });
            } catch (Exception ohno) {
                throw new RuntimeException(ohno);
            }
        });

        Events.on(TapEvent.class, event -> {
            if (event.player.team().core() == null)
                return;
            var data = PlayerData.getData(event.player);
            if (data == null)
                return; // Why

            rooms.each(room -> room.check(event.tile) && room.canBuy(data), room -> room.buy(data));
        });

        Events.on(UnitDestroyEvent.class, event -> {
            if (!units.containsKey(event.unit.type))
                return;

            int income = units.get(event.unit.type).drop();
            PlayerData.datas.each(data -> data.player.team() != event.unit.team && data.player.team().core() != null,
                    data -> {
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
            if (isBreak())
                return;

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
            if (isBreak())
                return;

            Groups.build.each(build -> {
                try {
                    if (build.block != Blocks.sublimate) return;
                    if (build instanceof ContinuousLiquidTurretBuild subl) {
                        if (subl.liquids().current() != Liquids.ozone) return;
                        var hasCyan = false;
                        a: for (var dx = -2; dx <= 2; dx++) for (var dy = -2; dy <= 2; dy++) {
                            var build2 = Vars.world.build((int) (subl.x) + dx, (int) (subl.y) + dy);
                            if (build2 == null) continue;
                            if (!build2.block().hasLiquids) continue;
                            if (build2.liquids().current() != Liquids.cyanogen) continue;
                            hasCyan = true;
                            break a;
                        }
                        if (!hasCyan) return;
                        subl.liquids.set(Liquids.cyanogen, 1f);
                        var netServerSyncStream = (ReusableByteOutStream) (netServer.getClass().getDeclaredField("syncStream").get(netServer));
                        var netServerDataStream = (DataOutputStream) (netServer.getClass().getDeclaredField("dataStream").get(netServer));
                        Core.app.post(() -> {
                            try {
                                netServerSyncStream.reset();
                                netServerDataStream.writeInt(subl.pos());
                                netServerDataStream.writeShort(subl.block().id);
                                subl.writeAll(Writes.get(netServerDataStream));
                                netServerDataStream.close();
                                Call.blockSnapshot((short) 1, netServerSyncStream.toByteArray());
                                netServerSyncStream.reset();
                            } catch (IOException e) {
                                Log.err(e);
                            }
                        });
                    }
                } catch (Exception ohno) {
                    throw new RuntimeException(ohno);
                }
            });
        }, 0f, 0.1f);

        Timer.schedule(() -> {
            if (isBreak())
                return;

            PlayerData.datas.each(PlayerData::updateMoney);
            spawns.draw();

            if (--timer == 0)
                Events.fire(new GameOverEvent(Team.derelict));
        }, 0f, 1f);
    }
}
