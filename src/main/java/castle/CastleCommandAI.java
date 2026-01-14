package castle;

import mindustry.ai.Pathfinder;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;

import static castle.CastleUtils.*;
import static mindustry.entities.Units.*;

public class CastleCommandAI extends CommandAI {

    @Override
    public void updateUnit() {
        if (!hasCommand() && onEnemySide(unit)) {
             target = attackTarget = unit.closestEnemyCore();

            if (unit.type.flying) moveTo(target, unit.type.range * 0.8f);
            else pathfind(Pathfinder.fieldCore);

            if (!invalid(target) && unit.type.circleTarget)
                circleAttack(80f);

            updateWeapons();
            faceTarget();
        } else {
            super.updateUnit();
        }
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground) {
        return nearAttackTarget(x, y, range) ? attackTarget : target(x, y, range, air, ground);
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground) {
        return closestTarget(unit.team, x, y, range, unit -> unit.checkTarget(air, ground), build -> ground && !(build.block instanceof Turret || build.block instanceof Drill));
    }
}