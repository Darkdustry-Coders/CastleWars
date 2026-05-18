package castle

import arc.func.Boolf
import arc.struct.Seq
import arc.util.Timer.schedule

import mindustry.Vars
import mindustry.game.Team
import mindustry.gen.Unit

import castle.CastleRooms.EffectRoom
import mindustry.Vars.tilesize

class TeamData(val team: Team) {
    val lockedRooms = Seq<EffectRoom?>()

    val unitCount: Int
        get() = team.data().units.count(Boolf { unit: Unit? -> !unit!!.spawnedByCore() && unit.type().useUnitCap })

    val unitCountAttack: Int
        get() {
            if (team === Team.sharded) {
                return team.data().units.count(Boolf { unit: Unit? -> !unit!!.spawnedByCore() && unit.type().useUnitCap && unit.y / 8 > Vars.world.height() / 2 })
            } else {
                return team.data().units.count(Boolf { unit: Unit? -> !unit!!.spawnedByCore() && unit.type().useUnitCap && unit.y / 8 < Vars.world.height() / 2 })
            }
        }

    val unitCountDefense: Int
        get() {
            if (team === Team.blue) {
                return team.data().units.count(Boolf { unit: Unit? -> !unit!!.spawnedByCore() && unit.type().useUnitCap && unit.y / tilesize > Vars.world.height() / 2 })
            } else {
                return team.data().units.count(Boolf { unit: Unit? -> !unit!!.spawnedByCore() && unit.type().useUnitCap && unit.y / tilesize < Vars.world.height() / 2 })
            }
        }

    fun locked(room: EffectRoom?): Boolean {
        return lockedRooms.contains { x: EffectRoom? -> x == room }
    }

    fun lock(room: EffectRoom): Boolean {
        if (lockedRooms.contains { x: EffectRoom? -> x == room }) return false
        lockedRooms.add(room)
        schedule({ lockedRooms.remove(room) }, room.delay)
        return true
    }

    companion object {
        val datas: Seq<TeamData?> = Seq<TeamData?>()

        fun getData(team: Team): TeamData {
            var dat: TeamData? = datas.find { data: TeamData? -> data!!.team == team }
            if (dat == null) {
                dat = TeamData(team)
                datas.add(dat)
            }
            return dat
        }
    }
}