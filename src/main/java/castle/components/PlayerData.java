package castle.components;

import arc.struct.Seq;
import mindustry.entities.Units;
import mindustry.gen.Player;
import useful.Bundle;

import java.util.Locale;

import static castle.CastleUtils.countUnits;
import static castle.Main.timer;

public class PlayerData {

    public static final Seq<PlayerData> datas = new Seq<>();

    public Player player;
    public Locale locale;

    public int money, income;

    public PlayerData(Player player) {
        this.handlePlayerJoin(player);
        this.reset();
    }

    public static PlayerData getData(Player player) {
        return datas.find(data -> data.player.uuid().equals(player.uuid()));
    }

    public void update() {
        if (!player.con.isConnected()) return;

        int units = countUnits(player.team()), unitsLimit = Units.getCap(player.team());
        Bundle.setHud(player, "ui.hud",
                money >= 0 ? "lime" : "scarlet", money,
                income >= 0 ? "lime" : "scarlet", income,
                units < unitsLimit ? "lightgray" : "scarlet", units, unitsLimit, timer);
    }

    public void updateMoney() {
        if (!player.con.isConnected()) return;
        money += income;
    }

    public void handlePlayerJoin(Player player) {
        this.player = player;
        this.locale = Bundle.locale(player);
    }

    public void reset() {
        this.money = 0;
        this.income = 15;
    }
}