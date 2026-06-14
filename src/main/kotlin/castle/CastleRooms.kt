package castle

import arc.func.Boolf
import arc.func.Cons
import arc.math.Mathf
import arc.math.geom.Point2
import arc.struct.Seq
import arc.util.Interval
import arc.util.Nullable
import arc.util.Structs
import arc.util.Time

import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Unit
import mindustry.gen.WorldLabel
import mindustry.type.Item
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.ConstructBlock
import mindustry.world.blocks.storage.CoreBlock
import mindustry.Vars.tilesize

import castle.CastleCosts.EffectData
import castle.CastleCosts.UnitData
import castle.CastleUtils.defenseCap
import castle.CastleUtils.attackCap
import castle.CastleUtils.worldLabel

import buj.tl.Tl
import castle.CastleUtils.mirrorY
import castle.CastleUtils.mirrored
import mindustry.world.blocks.defense.turrets.Turret
import mindustry.world.blocks.production.Drill

import kotlin.math.min

class CastleRooms {
    open class Room {
        var x: Int = 0
        var y: Int = 0
        var size: Int = 0
        var cost: Int = 0
        var offset: Int = 0

        var team: Team? = null
        var label: WorldLabel = WorldLabel.create()

        fun set(x: Int, y: Int, size: Int, team: Team) {
            this.x = x
            this.y = y

            this.size = size
            this.team = team
            this.offset = size / 2 - (1 - size % 2)
        }

        open fun spawn() {
            if (CastleUtils.generatePlatforms && !(this is UnitRoom || this is EffectRoom)) Vars.world.tile(x, y)
                .getLinkedTilesAs(
                    ConstructBlock.get(size)
                ) { tile: Tile? -> tile!!.setFloor(Blocks.metalFloor.asFloor()) }
            else if (this is UnitRoom || this is EffectRoom) {
                val source = CastleUtils.platformSource.random()
                Vars.world.tile(x, y)
                    .getLinkedTilesAs(ConstructBlock.get(size)) { tile: Tile? ->
                        tile!!.setOverlayNet(source!!.get(tile.y + offset - y, tile.x + offset - x).overlay())
                        tile.setFloor(source.get(tile.y + offset - y, tile.x + offset - x).floor())
                        tile.packedData = source.get(tile.y + offset - y, tile.x + offset - x).getPackedData()
                    }
            }

            label.set(drawX(), drawY())
            label.text = toString()
            label.fontSize = min(size.toFloat(), 2f)
            label.flags = WorldLabel.flagOutline
            label.add()

            Main.rooms.add(this)
        }

        open fun buy(data: PlayerData?) {
            data!!.money -= cost
        }

        open fun canBuy(data: PlayerData?): Boolean {
            return data!!.money >= cost
        }

        fun check(tile: Tile): Boolean {
            return Structs.inBounds(tile.x - this.x + offset, tile.y - this.y + offset, size, size)
        }

        fun drawX(): Float {
            return (x + (1 - size % 2) / 2f) * tilesize
        }

        open fun drawY(): Float {
            return (y + (0.5f - size % 2) / 2f) * tilesize + 2
        }

        open fun update() {}
    }

    open class BlockRoom : Room {

        private var amount: Int = 0
        private var interval: Int = 0
        private var invincible: Boolean = false

        @Nullable
        val starting: Block?
        val block: Block

        constructor(block: Block, cost: Int, @Nullable starting: Block?) {
            this.block = block
            this.cost = cost
            this.starting = starting
        }

        constructor(block: Block, cost: Int, invincible: Boolean, @Nullable starting: Block?) {
            this.block = block
            this.cost = cost
            this.invincible = invincible
            this.starting = starting
        }

        constructor(block: Block, cost: Int, @Nullable starting: Block?, amount: Int, interval: Int, invincible: Boolean) {
            this.block = block
            this.cost = cost
            this.starting = starting
            this.amount = amount
            this.interval = interval
            this.invincible = invincible
        }

        override fun buy(data: PlayerData?) {
            super.buy(data)
            label.hide()

            val coreItems: Array<IntArray>?
            val tile = Vars.world.tile(x, y)

            if (block !is CoreBlock) {
                tile.setNet(block, team, 0)
                if(invincible || block is Turret || block is Drill) tile.build.health(Float.POSITIVE_INFINITY)
                coreItems = null
            } else {
                coreItems = arrayOf<IntArray?>(IntArray(Vars.content.items().size)) as Array<IntArray>?
                team!!.core().items.each { item: Item?, count: Int ->
                    val t = coreItems?.get(0)
                    t?.set(item!!.id.toInt(), count)
                }
                tile.setNet(block, team, 0)
            }

            worldLabel("rooms.block.bought",drawX(), drawY(),1f,Pair("player",data!!.player.coloredName()))


            if (coreItems != null) for (id in coreItems[0].indices) team!!.core().items
                .set(Vars.content.item(id), coreItems[0][id])
        }

        override fun canBuy(data: PlayerData?): Boolean {
            return super.canBuy(data) && data!!.player.team() === team && label.isAdded()
        }

        override fun toString(): String {
            return block.emoji() + " : " + cost
        }

        override fun drawY(): Float {
            return super.drawY() + 1
        }

        override fun spawn() {
            super.spawn()
            if (starting != null) Vars.world.tile(x, y).setBlock(starting, team, 0)
        }
    }

    class MinerRoom(drill: Block, val item: Item?, cost: Int, val amount: Int, val intervalGet: Float)
        : BlockRoom(drill, cost, null, amount, intervalGet.toInt(), true) {

        val interval: Interval = Interval()

        override fun update() {
            if (label.isAdded() || !interval.get(intervalGet)) return

            Call.effect(Fx.mineHuge, drawX(), drawY(), 0f, team!!.color)
            Call.transferItemTo(null, item, amount, drawX(), drawY(), team!!.core())
        }

        override fun toString(): String {
            return "[" + item?.emoji() + "]\n" + cost
        }
    }

    class UnitRoom(val type: UnitType?, data: UnitData?, attack: Boolean) : Room() {
        val attack: Boolean
        val income: Int

        init {
            this.cost = data!!.cost

            this.attack = attack
            this.income = if (attack) data.income else -data.income
        }

        private fun spawnUnit(data: PlayerData, spawns: Seq<Point2>, type: UnitType) {
            val pt = CastleUtils.randomSpawn(spawns)
            spawnUnitAt(data, pt.x, pt.y, type)
        }

        private fun spawnUnitAt(data: PlayerData, x: Int, y: Int, type: UnitType) {
            var x = x
            var startY = y
            var coordinate = Point2(0,0)
            if (x < 0) {
                x = data.player.core().x.toInt() + 6*tilesize
                startY = data.player.core().y.toInt()
            } else {
                x *= tilesize
                startY = if(data.player.team() == Team.blue)  mirrorY(y)*tilesize else y*tilesize
            }
            startY -= 6*tilesize
            for (y in startY..<startY+12*tilesize) {
                coordinate = Point2(x/tilesize, y/tilesize)
                coordinate = Point2(x/tilesize, y/tilesize)
                if(CastleUtils.validForSpawn(type, coordinate)) break
            }
            val unit = type.spawn(
                data.player.team(),
                coordinate.x.toFloat()*tilesize, coordinate.y.toFloat()*tilesize
            )
            worldLabel("rooms.unit.bought", unit.getX(), unit.getY(), 1f, Pair("player", data.player.coloredName()))
        }

        override fun buy(data: PlayerData?) {
            val td = data!!.team()
            var selfDefenseCap = defenseCap
            var selfAttackCap = attackCap
            if (defenseCap.toInt() == 0) selfDefenseCap = Vars.state.rules.unitCap.toShort()
            if (attackCap.toInt() == 0) selfAttackCap = Vars.state.rules.unitCap.toShort()
            if (attack && (td.unitCountAttack >= selfAttackCap || td.unitCount >= Vars.state.rules.unitCap)) return
            if (!attack && (td.unitCountDefense >= selfDefenseCap || td.unitCount >= Vars.state.rules.unitCap)) return
            super.buy(data)
            data.income += income
            val prevLimit = Vars.state.rules.unitCap
            Vars.state.rules.unitCap = Integer.MAX_VALUE
            if (attack) Main.spawns.spawn(data.player, data.player.team(), type)
            else if (type!!.naval) {
                spawnUnit(data, CastleUtils.boatSpawns, type)
            } else if (!type.naval && !type.flying || type.canBoost) {
                spawnUnit(data, CastleUtils.landSpawns, type)
            } else if (type.flying) {
                spawnUnit(data, CastleUtils.airSpawns, type)
            }
            Vars.state.rules.unitCap = prevLimit
        }



        override fun canBuy(data: PlayerData?): Boolean {
            var selfDefenseCap = defenseCap
            var selfAttackCap = attackCap
            if (!super.canBuy(data)) return false
            if (defenseCap.toInt() == 0) selfDefenseCap = Vars.state.rules.unitCap.toShort()
            if (attackCap.toInt() == 0) selfAttackCap = Vars.state.rules.unitCap.toShort()
            if (attack) {
                if (data!!.team().unitCountAttack >= selfAttackCap ||
                    data.team().unitCount >= Vars.state.rules.unitCap
                ) {
                    Call.announce(data.player.con, Tl.fmt(data.player).done("{rooms.unit.limit}"))
                    return false
                }
            } else {
                if (data!!.team().unitCountDefense >= selfDefenseCap ||
                    data.team().unitCount >= Vars.state.rules.unitCap
                ) {
                    Call.announce(data.player.con, Tl.fmt(data.player).done("{rooms.unit.limit}"))
                    return false
                }
            }
            return true
        }

        override fun toString(): String {
            return type?.emoji() + " " + (if (attack) "[accent]\uE865" else "[scarlet]\uE84D") +
                    "\n[gray]" + cost +
                    "\n[white]\uF8BA : " + (if (income > 0) "[lime]+" else if (income == 0) "[gray]" else "[scarlet]") + income
        }
    }

    class EffectRoom(val effect: StatusEffect?, data: EffectData?) : Room() {
        @JvmField
        val delay: Float
        val duration: Int
        val ally: Boolean

        init {
            this.cost = data!!.cost
            this.duration = data.duration
            this.ally = data.ally
            this.delay = data.delay
        }

        override fun canBuy(data: PlayerData?): Boolean {
            if (!super.canBuy(data)) return false
            if (data!!.team().locked(this)) {
                Call.announce(data.player.con, Tl.fmt(data.player).done("{rooms.effect.limit}"))
                return false
            }
            return true
        }

        override fun buy(data: PlayerData?) {
            if (!data!!.team().lock(this)) return

            super.buy(data)
            Groups.unit.each(
                Boolf { unit: Unit? -> ally == (unit!!.team === data.player.team()) },
                Cons { unit: Unit? -> unit!!.apply(effect, duration * 60f) })

            // Visual things
            for (rotation in 0..35) Time.run(
                rotation.toFloat()
            ) {
                Call.effect(
                    Fx.coreLandDust,
                    data.player.x,
                    data.player.y,
                    Mathf.random(360f),
                    effect?.color
                )
            }
            worldLabel("rooms.effect.bought", drawX(), drawY(),1f,Pair("player",data.player.coloredName()))
        }

        override fun toString(): String {
            return effect?.emoji() +
                    "\n[gray]" + cost +
                    "\n" + (if (ally) "[stat]\uE804" else "[negstat]\uE805") + duration + "s"
        }
    }
}