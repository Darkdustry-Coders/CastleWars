package castle

import arc.struct.Seq

import mindustry.Vars
import mindustry.core.UI
import mindustry.gen.Call
import mindustry.gen.Player

import castle.CastleUtils.unitCapType

import buj.tl.Tl

class PlayerData(var player: Player) {
    var money: Int = 0
    var income: Int = 0

    init {
        this.reset()
    }

    fun update() {
        if (!player.con.isConnected || player.team() == null) return
        val economyText = Tl.fmt(player)
            .put("colorMoney",if (money >= 0) "lime" else "scarlet").put("money",money.toString())
            .put("colorIncome",if (income >= 0) "lime" else "scarlet").put("income",income.toString())
            .done("{ui.hud-part.economy}")
        var timeText = Tl.fmt(player)
            .put("time",UI.formatTime(Main.timer * 60f))
            .done("{ui.hud-part.time}")
        var unitCapText = Tl.fmt(player)
            .put("color",if (team().unitCount < Vars.state.rules.unitCap) "lightgray" else "scarlet").put("unitCount",team().unitCount.toString())
            .put("unitCap",Vars.state.rules.unitCap.toString())
            .done("{ui.hud-part.unitCap}")
        var unitCapAttack = Tl.fmt(player)
            .put("color",if (team().unitCountAttack < CastleUtils.attackCap) "lightgray" else "#be2537ff").put("unitCount",team().unitCountAttack.toString())
            .put("unitCap",CastleUtils.attackCap.toString())
            .done("{ui.hud-part.unitCapAttack}")
        var unitCapDefense = Tl.fmt(player)
            .put("color",if (team().unitCountDefense < CastleUtils.defenseCap) "lightgray" else "#1659a7ff").put("unitCount",team().unitCountDefense.toString())
            .put("unitCap",CastleUtils.defenseCap.toString())
            .done("{ui.hud-part.unitCapDefense}")
        unitCapDefense = "\n" + unitCapDefense
        unitCapAttack = "\n" + unitCapAttack
        unitCapText = "\n" + unitCapText
        timeText = "\n" + timeText
        var hudText: String? = ""
        hudText += economyText
        if (CastleUtils.capType == unitCapType.NONE) hudText += unitCapText
        if (CastleUtils.capType == unitCapType.DEFENSE_ONLY) hudText += unitCapText + unitCapDefense
        if (CastleUtils.capType == unitCapType.ATTACK_ONLY) hudText += unitCapText + unitCapAttack
        if (CastleUtils.capType == unitCapType.BOTH) hudText += unitCapDefense + unitCapAttack
        hudText += timeText
        Call.setHudText(player.con, hudText)
    }

    fun updateMoney() {
        if (player.con.isConnected) money += income
    }

    fun reset() {
        this.money = 0
        this.income = 15
    }

    fun team(): TeamData {
        return TeamData.getData(this.player.team())
    }

    companion object {
        val datas: Seq<PlayerData?> = Seq<PlayerData?>()

        fun getData(player: Player): PlayerData? {
            return datas.find { data: PlayerData? -> data!!.player.uuid() == player.uuid() }
        }
    }
}
