package castle;

import arc.struct.Seq;
import mindustry.core.UI;
import mindustry.gen.Player;
import useful.Bundle;
import static castle.CastleUtils.defenseCap;
import static castle.CastleUtils.attackCap;
import static castle.Main.*;
import static mindustry.Vars.*;

public class PlayerData {

    public static final Seq<PlayerData> datas = new Seq<>();

    public Player player;
    public int money, income;

    public PlayerData(Player player) {
        this.player = player;
        this.reset();
    }

    public static PlayerData getData(Player player) {
        return datas.find(data -> data.player.uuid().equals(player.uuid()));
    }

    public void update() {
        if (player.con.isConnected()) Bundle.setHud(player, "ui.hud",
                money >= 0 ? "lime" : "scarlet", money,
                income >= 0 ? "lime" : "scarlet", income, 
                team().getUnitCountAttack(), team().getUnitCountAttack() < attackCap ? "lightgray" : "#be2537ff",
                team().getUnitCountDefense(),
                team().getUnitCountDefense() < defenseCap ? "lightgray" : "#1659a7ff",
            attackCap,defenseCap, UI.formatTime(timer * 60f));
    }

    public void updateMoney() {
        if (player.con.isConnected()) money += income;
    }

    public void reset() {
        this.money = 0;
        this.income = 15;
    }

    public TeamData team() {
        return TeamData.getData(this.player.team());
    }
}
