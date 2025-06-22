package castle;

import arc.struct.Seq;
import arc.util.Timer;
import mindustry.game.Team;

public class TeamData {
    public static final Seq<TeamData> datas = new Seq<>();

    public final Team team;
    private Seq<CastleRooms.EffectRoom> lockedRooms = new Seq<>();

    private TeamData(Team team) {
        this.team = team;
    }

    public static TeamData getData(Team team) {
        var dat = datas.find(data -> data.team.equals(team));
        if (dat == null) {
            dat = new TeamData(team);
            datas.add(dat);
        }
        return dat;
    }

    public int getUnitCount() {
        return team.data().units.count(unit -> !unit.spawnedByCore() && unit.type().useUnitCap);
    }

    public boolean locked(CastleRooms.EffectRoom room) {
        return lockedRooms.contains(x -> x == room);
    }

    public boolean lock(CastleRooms.EffectRoom room) {
        if (lockedRooms.contains(x -> x == room)) return false;
        lockedRooms.add(room);
        Timer.schedule(() -> lockedRooms.remove(room), room.delay);
        return true;
    }
}
