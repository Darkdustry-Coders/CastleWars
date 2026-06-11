package castle

import arc.Core
import mindustry.Vars
import mindustry.core.UI
import mindustry.gen.Call
import mindustry.gen.Player

import buj.tl.Tl
import mindurka.api.on
import mindurka.util.newSeq
import mindustry.game.EventType
import mindustry.gen.Groups

class PlayerData(var player: Player) {
    var money: Int = 0
    var income: Int = 0

    init {
        this.reset()
    }

    fun update() {
        if (!player.con.isConnected || player.team() == null) return

        val economyText = Tl.fmt(player)
            .put("colorMoney", if (money >= 0) "lime" else "scarlet").put("money", money.toString())
            .put("colorIncome", if (income >= 0) "lime" else "scarlet").put("income", income.toString())
            .done("{ui.hud-part.economy}")

        val timeText = "\n" + Tl.fmt(player)
            .put("time", UI.formatTime(Main.timer * 60f))
            .done("{ui.hud-part.time}")

        fun capText() = "\n" + Tl.fmt(player)
            .put("color", if (team().unitCount < Vars.state.rules.unitCap) "lightgray" else "scarlet")
            .put("unitCount", team().unitCount.toString())
            .put("unitCap", Vars.state.rules.unitCap.toString())
            .done("{ui.hud-part.unitCap}")

        fun attackText() = "\n" + Tl.fmt(player)
            .put("color", if (team().unitCountAttack < CastleUtils.attackCap) "lightgray" else "#be2537ff")
            .put("unitCount", team().unitCountAttack.toString())
            .put("unitCap", CastleUtils.attackCap.toString())
            .done("{ui.hud-part.unitCapAttack}")

        fun defenseText() = "\n" + Tl.fmt(player)
            .put("color", if (team().unitCountDefense < CastleUtils.defenseCap) "lightgray" else "#1659a7ff")
            .put("unitCount", team().unitCountDefense.toString())
            .put("unitCap", CastleUtils.defenseCap.toString())
            .done("{ui.hud-part.unitCapDefense}")

        val capPart = when (CastleUtils.capType) {
            CastleUtils.UnitCapType.NONE         -> capText()
            CastleUtils.UnitCapType.DEFENSE_ONLY -> capText() + defenseText()
            CastleUtils.UnitCapType.ATTACK_ONLY  -> capText() + attackText()
            CastleUtils.UnitCapType.BOTH         -> capText() + defenseText() + attackText()
        }

        Call.setHudText(player.con, economyText + capPart + timeText)
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
        val datas = newSeq<PlayerData>()

        fun of(player: Player): PlayerData {
            val data = datas.find { data: PlayerData -> data.player.uuid() == player.uuid() }
            if (data == null) {
                val d2 = PlayerData(player)
                datas.add(d2)
                return d2
            }
            return data
        }

        init {
            on<EventType.PlayEvent> { Core.app.post { // You can never be sure with this codebase!
                datas.retainAll { it.player.con.isConnected }
                datas.each { it.reset() }
            } }
        }
    }
}
