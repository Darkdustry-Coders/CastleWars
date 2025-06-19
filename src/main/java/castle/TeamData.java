package castle;

import arc.struct.Seq;
import arc.util.Timer;
import mindustry.game.Team;

public class TeamData {
    public static final Seq<TeamData> datas = new Seq<>();

    private Team team;
    private Seq<CastleRooms.EffectRoom> lockedRooms = new Seq<>();

    public static TeamData getData(Team team) {
        var dat = datas.find(data -> data.team.equals(team));
        if (dat == null) {
            dat = new TeamData();
            dat.team = team;
            datas.add(dat);
        }
        return dat;
    }

    public int getUnitCount() {
        return team.data().unitCount - team.data().players.count(player -> player.unit() != null && player.unit().spawnedByCore());
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
