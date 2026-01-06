package castle;

import mindustry.ai.Pathfinder;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Teamc;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;
import mindustry.gen.Unit;
import mindustry.entities.Units;

import static castle.CastleUtils.*;
import static mindustry.entities.Units.*;

public class CastleCommandAI extends CommandAI {

    @Override
    public void updateUnit() {
        if (!hasCommand() && onEnemySide(unit)) {
             target = attackTarget = unit.closestEnemyCore();
            if (unit.type.flying){
                if (unit.health>unit.type.health*0.6) moveTo(target, unit.type.range * 0.8f);
                else moveTo(target, 0f);
            }
            else pathfind(Pathfinder.fieldCore);

            Unit enemy = Units.closestEnemy(
                unit.team,
                unit.x,
                unit.y,
                unit.type.range,
                u -> u.checkTarget(unit.type.targetAir, unit.type.targetGround)
            );
            if (enemy != null) {
                target = attackTarget = enemy;
            }

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