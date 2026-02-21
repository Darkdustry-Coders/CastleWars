package castle;


import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;
import arc.util.io.ReusableByteOutStream;
import arc.util.Time;

import mindustry.Vars;
import mindustry.world.Tile;
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
import mindustry.world.blocks.defense.turrets.LiquidTurret.LiquidTurretBuild;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.gen.Call;
import static mindustry.Vars.*;

import static castle.CastleCosts.*;
import static castle.CastleUtils.*;
import static castle.CastleUtils.syncBlock;
import static castle.CastleUtils.withinPointDef;
import castle.CastleGenerator.Spawns;
import castle.CastleRooms.Room;

import java.io.DataOutputStream;

import useful.Bundle;

public class Main extends Plugin {

    public static final Seq<Room> rooms = new Seq<>();
    public static final Spawns spawns = new Spawns();

    public static ReusableByteOutStream syncStream = new ReusableByteOutStream(512);
    public static DataOutputStream dataStream = new DataOutputStream(syncStream);

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
            if(spawns.within(action.tile) ||
                withinPointDef(action.tile,boatSpawn,16) ||
                withinPointDef(action.tile,landSpawn,16))
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
                Groups.build.each(b -> {
                    if(b instanceof Building){
                        syncBlock(b);
                    }
                });
            } catch (Exception ohno) {
                throw new RuntimeException(ohno);
            }
        });

        // unit cant buy anything while building cuz dont shoot
        Events.on(TapEvent.class, event -> {
            var data = PlayerData.getData(event.player);
            if (event.player.team().core() == null || event.player.unit() == null || data == null) return;
            Tile tapped = event.tile;
            rooms.each(room -> room.check(tapped) && room.canBuy(data), room -> room.buy(data));
            Time.runTask(120, new Runnable() {
                @Override
                public void run() {
                    if (event.player.unit().isShooting) {
                        int shootX = (int) event.player.unit().aimX()/8;
                        int shootY = (int) event.player.unit().aimY()/8;
                        var data = PlayerData.getData(event.player);
                        if (shootX < 0 || shootY < 0 || shootX >= Vars.world.width() || shootY >= Vars.world.height()) return;
                        Tile tile = Vars.world.tile(shootX, shootY);
                        rooms.each(room -> room.check(tile) && room.canBuy(data), room -> room.buy(data));
                        Time.runTask(0.03f, this);
                    }
                }
            });
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

        Events.on(WorldLoadEndEvent.class, event -> CastleGenerator.generate());

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

                if (unit.tileY() >= halfHeight && unit.tileY() <= world.height() - halfHeight - 1 ){
                    if(!onEnemySide(unit) && unit.type == UnitTypes.poly || unit.type == UnitTypes.mega){
                        unit.set(unit.team().core().x, unit.team().core().y);
                        return false;
                    }
                    return true;
                }
                return false;
            }, Call::unitEnvDeath);
        }, 0f, 0.1f);

        Timer.schedule(() -> {
            if (isBreak())
                return;
            
            Groups.build.each(build -> {
                try {   
                    if (build.block != Blocks.sublimate && build instanceof ItemTurret.ItemTurretBuild turret) {
                        // BulletType active = turret.peekAmmo();
                        for(int i = 0; i < turret.ammo.size; i++){
                            if(i == 0 && turret.ammo.size > 1){
                                turret.ammo.remove(i);
                            }
                            else {
                                if (turret.ammo.get(i).amount >25) {
                                    turret.update();
                                    turret.updateTile();
                                    turret.ammo.get(i).amount = 25;
                                    syncBlock(turret);
                                }
                                turret.totalAmmo = 1;
                            }
                        }             
                    }  
                    if (build instanceof LiquidTurretBuild LiqTurret) {
                        var hasLiq = false;
                        int minX = -2;
                        int minY = -2;
                        if(LiqTurret.hitSize() == 16){
                            minX = -1;
                            minY = -1;
                        }
                        a: for (var dx = minX; dx <= 2; dx++) for (var dy = minY; dy <= 2; dy++) {
                            var build2 = Vars.world.build(LiqTurret.tileX() + dx, LiqTurret.tileY() + dy);
                            if (build2 == null) continue;
                            if (!build2.block().hasLiquids || !build2.block().outputsLiquid) continue;
                            if (build2.liquids().current() == LiqTurret.liquids().current()) continue;
                            hasLiq = true;
                            break a;
                        }
                        if (!hasLiq) return;
                        LiqTurret.liquids.clear();
                        try{
                            syncBlock(LiqTurret);
                        }catch (Exception ohno) {
                            Log.err(ohno);
                        }
                    }
                    if (build.block != Blocks.sublimate) return;
                    if (build instanceof ContinuousLiquidTurretBuild subl) {
                        if (subl.liquids().current() != Liquids.ozone) return;
                        var hasCyan = false;
                        a: for (var dx = -2; dx <= 2; dx++) for (var dy = -2; dy <= 2; dy++) {
                            var build2 = Vars.world.build(subl.tileX() + dx, subl.tileY() + dy);
                            if (build2 == null) continue;
                            if (!build2.block().hasLiquids) continue;
                            if (build2.liquids().current() != Liquids.cyanogen) continue;
                            hasCyan = true;
                            break a;
                        }
                        if (!hasCyan) return;
                        subl.liquids.clear();
                        try{
                            syncBlock(subl);
                        }catch (Exception ohno) {
                            Log.err(ohno);
                        }
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
