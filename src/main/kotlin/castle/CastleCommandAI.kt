package castle

import arc.func.Boolf
import mindustry.Vars.tilesize

import mindustry.ai.Pathfinder
import mindustry.ai.types.CommandAI
import mindustry.entities.Units
import mindustry.gen.Building
import mindustry.gen.Teamc
import mindustry.gen.Unit
import mindustry.world.blocks.defense.turrets.Turret
import mindustry.world.blocks.production.Drill

class CastleCommandAI : CommandAI() {
    override fun updateUnit() {
        if (!hasCommand() && CastleUtils.onEnemySide(unit)) {
            attackTarget = unit.closestEnemyCore()

            if (unit.type.flying) {
                if (unit.health > unit.type.health * 0.5) moveTo(attackTarget, unit.type.range * 0.8f)
                else moveTo(attackTarget, 0.5f*tilesize)
            } else pathfind(Pathfinder.fieldCore)

            if (!invalid(attackTarget) && unit.type.circleTarget) circleAttack(10f*tilesize)

            updateWeapons()
            faceTarget()
        } else {
            super.updateUnit()
        }
    }

    override fun findTarget(x: Float, y: Float, range: Float, air: Boolean, ground: Boolean): Teamc? {
        return if (nearAttackTarget(x, y, range)) attackTarget else target(x, y, range, air, ground)
    }

    override fun target(x: Float, y: Float, range: Float, air: Boolean, ground: Boolean): Teamc? {
        return Units.closestTarget(
            unit.team,
            x,
            y,
            range,
            Boolf { unit: Unit? -> unit!!.checkTarget(air, ground) },
            Boolf { build: Building? -> ground && !(build!!.block is Turret || build.block is Drill) })
    }
}