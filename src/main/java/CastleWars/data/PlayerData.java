package CastleWars.data;

import CastleWars.Main;
import CastleWars.logic.Room;
import CastleWars.logic.TurretRoom;
import arc.Events;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.gen.*;

import static CastleWars.Bundle.findLocale;
import static CastleWars.Bundle.format;

public class PlayerData {

    public static IntMap<PlayerData> datas = new IntMap<>();
    public static float MoneyInterval = 60f;
    public static float LabelInterval = 60f * 30f;

    public Player player;
    public boolean disabledHud = false;
    public int money, income = 15;
    Interval interval;

    public PlayerData(Player player) {
        this.player = player;
        interval = new Interval(2);
    }

    public void update() {
        if (interval.get(0, MoneyInterval))
            money += income * (Groups.player.copy(new Seq<>()).select(p -> p.team().equals(this.player.team())).size / (Groups.player.size() - Groups.player.copy(new Seq<Player>()).select(p -> p.team().equals(this.player.team())).size));
        updateLabels();

        if (player.shooting && player.unit() != null) {
            Room.rooms.each(room -> {
                if (room instanceof TurretRoom && !(((TurretRoom)room).team == player.team())) return;
                if (room.check(player.unit().aimX, player.unit().aimY) && room.canBuy(this)) room.buy(this);
            });
        }
        // Set Unit to risso
        if (player.unit().spawnedByCore && !(player.unit() instanceof WaterMovec)) {
            if (player.team().core() != null) {
                Unit unit = UnitTypes.risso.spawn(player.team(), player.team().core().x + 30, player.team().core().y + Mathf.random(-40, 40));
                unit.spawnedByCore = true;
                player.unit(unit);
            }
        }

        // Set Hud Text
        if (!disabledHud) {
            StringBuilder text = new StringBuilder(format("commands.hud.display", findLocale(player), money, income));
            if (player.unit() != null && player.unit().isFlying() && !Main.logic.placeCheck(player.team(), player.tileOn())) text.append(format("commands.hud.fly-warning", findLocale(player)));
            Call.setHudText(player.con, text.toString());
        }
    }

    public void updateLabels() {
        if (interval.get(1, LabelInterval)) labels(player);
    }

    public static void init() {
        Events.on(EventType.PlayerJoin.class, event -> {
            datas.put(event.player.id, new PlayerData(event.player));
            Vars.netServer.assignTeam(event.player, Groups.player);
            Timer.schedule(() -> labels(event.player), 1);
            Groups.player.each(PlayerData::labels);
        });

        Events.on(EventType.PlayerLeave.class, event -> datas.remove(event.player.id));
    }

    public static void labels(Player player) {
        Room.rooms.each(room -> {
            if (room instanceof TurretRoom && ((TurretRoom)room).team != player.team()) return;
            if (room.labelVisible) {
                Call.label(player.con, room.label, LabelInterval / 60f, room.centreDrawx, room.centreDrawy - room.size * 2);
            }
        });
    }

    public void reset() {
        this.income = 15;
        this.money = 0;
    }
}