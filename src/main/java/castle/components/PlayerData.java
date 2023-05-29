package castle.components;

import arc.struct.Seq;
import mindustry.gen.Player;
import useful.Bundle;

import static castle.Main.*;
import static mindustry.Vars.*;

public class PlayerData {

    public static final Seq<PlayerData> datas = new Seq<>();

    public Player player;
    public int money, income;

    public PlayerData(Player player) {
        this.player(player);
        this.reset();
    }

    public static PlayerData getData(Player player) {
        return datas.find(data -> data.player.uuid().equals(player.uuid()));
    }

    public void update() {
        if (player.con.isConnected()) Bundle.setHud(player, "ui.hud",
                money >= 0 ? "lime" : "scarlet", money,
                income >= 0 ? "lime" : "scarlet", income,
                player.team().data().unitCount < state.rules.unitCap ? "lightgray" : "scarlet",
                player.team().data().unitCount, state.rules.unitCap, timer);
    }

    public void updateMoney() {
        if (player.con.isConnected()) money += income;
    }

    public void player(Player player) {
        this.player = player;
    }

    public void reset() {
        this.money = 0;
        this.income = 15;
    }
}