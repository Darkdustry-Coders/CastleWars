package castle;

import mindustry.ai.Pathfinder;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;

import static castle.CastleUtils.onEnemySide;
import static mindustry.entities.Units.closestTarget;

public class CastleCommandAI extends CommandAI {

    @Override
    public void updateUnit() {
        // TODO доделать эту шизу
        if (!hasCommand() && onEnemySide(unit)) {
            target = attackTarget = unit.closestEnemyCore();
            pathfind(Pathfinder.fieldCore);

            updateWeapons();
        } else {
            super.updateUnit();
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