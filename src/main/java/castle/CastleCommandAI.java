package castle;

import arc.util.Time;
import arc.util.Tmp;
import mindustry.ai.types.CommandAI;

import static castle.CastleUtils.onEnemySide;

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
}
