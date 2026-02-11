package castle;

import arc.struct.Seq;
import mindustry.core.UI;
import mindustry.gen.Call;
import mindustry.gen.Player;
import static mindustry.Vars.*;

import static castle.CastleUtils.*;
import static castle.Main.*;

import useful.Bundle;

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
        if (!player.con.isConnected() && player.team() == null) return;
        String hudText = "";
        String economyText = Bundle.format("ui.hudPart.economy",player,
                money >= 0 ? "lime" : "scarlet", money,
                income >= 0 ? "lime" : "scarlet", income);
        String timeText= Bundle.format("ui.hudPart.time",player,
                UI.formatTime(timer * 60f));
        hudText += economyText;
        String unitCapText = Bundle.format("ui.hudPart.unitCap", player,
                team().getUnitCount() < state.rules.unitCap ? "lightgray" : "scarlet",
                team().getUnitCount(), state.rules.unitCap
        );
        String unitCapAttack = Bundle.format("ui.hudPart.unitCapAttack", player,
                team().getUnitCountAttack() < attackCap ? "lightgray" : "#be2537ff",
                team().getUnitCountAttack(), attackCap
        );
        String unitCapDefense = Bundle.format("ui.hudPart.unitCapDefense", player,
                team().getUnitCountDefense() < defenseCap ? "lightgray" : "#1659a7ff",
                team().getUnitCountDefense(), defenseCap
        );
        if(isDivideCap==0) hudText += unitCapText;
        if(unitCapType==1) hudText +=  unitCapText + unitCapDefense;
        if(unitCapType==2) hudText +=  unitCapText + unitCapAttack;
        if(unitCapType==3) hudText += unitCapDefense + unitCapAttack;
        hudText += timeText;
        Call.setHudText(player.con, hudText);
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
