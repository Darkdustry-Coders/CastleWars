package castle;

import arc.util.Time;
import arc.util.Tmp;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;

import static castle.CastleUtils.onEnemySide;
import static mindustry.entities.Units.closestTarget;

public class CastleCommandAI extends CommandAI {

    public static final long inactivityInterval = 10000;

    public long lastCommandTime = -1;

    @Override
    public void updateUnit() {
        if (!hasCommand() && Time.timeSinceMillis(lastCommandTime) > inactivityInterval && onEnemySide(unit) && unit.closestEnemyCore() != null) {
            var core = unit.closestEnemyCore();
            attackTarget = core;
            targetPos = Tmp.v1.set(core);
        } else {
            super.updateUnit();
            if (hasCommand()) {
                lastCommandTime = Time.millis();
            }
        }
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return nearAttackTarget(x, y, range) ? attackTarget : target(x, y, range, air, ground);
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground) {
         return closestTarget(unit.team, x, y, range, unit -> unit.checkTarget(air, ground), building -> ground && building.health < 999999999f);
    }
}