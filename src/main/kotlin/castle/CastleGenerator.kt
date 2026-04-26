package castle

import arc.func.Cons
import arc.func.Intc2
import arc.func.Prov
import arc.math.Mathf
import arc.math.geom.Point2
import arc.struct.Seq

import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.game.Team
import mindustry.game.Teams
import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.environment.SpawnBlock
import mindustry.world.blocks.logic.LogicBlock.LogicBuild
import mindustry.world.blocks.logic.LogicBlock.LogicLink
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.blocks.storage.CoreBlock.CoreBuild

import castle.CastleCosts.EffectData
import castle.CastleRooms.*
import castle.CastleUtils.castleBlocks
import castle.CastleUtils.castleMiners
import castle.CastleUtils.mirrored
import castle.CastleUtils.worldLabel
import mindustry.Vars.state
import mindustry.world.blocks.units.Reconstructor

import kotlin.experimental.inv

object CastleGenerator {
    const val unitLimitY: Int = 3
    var unitLimitX: Int = 5
    var offsetX: Int = 0
    var maxOffsetX: Int = 0
    var maxOffsetY: Int = 0
    var offsetXAlly: Int = 0
    var offsetXEnemy: Int = 0
    var offsetY: Int = 0

    fun generate() {
        CastleUtils.refreshMeta()

        val saved = Vars.world.tiles
        Vars.world.resize(Vars.world.width(), Vars.world.height() * 2 + 58)

        // Set half height for further use
        Main.halfHeight = saved.height

        saved.each { x: Int, y: Int ->
            val tile = saved.get(x, y)
            val floor = tile.floor()
            val block = if (!tile.block().hasBuilding() && tile.isCenter) tile.block() else Blocks.air
            val overlay = if (tile.overlay().needsSurface) tile.overlay() else Blocks.air
            val packetData = tile.packedData
            var data = tile.data


            if (block === Blocks.cliff) {
                for (i in 0..2) {
                    val ba = (1 shl (i + 1)).toByte()
                    val bb = (1 shl (7 - i)).toByte()
                    var addMask: Byte = 0
                    var remMask: Byte = 0
                    if ((tile.data.toInt() and ba.toInt()) != 0) {
                        addMask = (addMask + bb).toByte()
                        remMask = (remMask + ba).toByte()
                    }
                    if ((tile.data.toInt() and bb.toInt()) != 0) {
                        addMask = (addMask + ba).toByte()
                        remMask = (remMask + bb).toByte()
                    }
                    data = ((data.toInt() and remMask.inv().toInt()) or addMask.toInt()).toByte()
                }
            }
            addTile(x, y, floor, block, overlay, tile.data)
            addTile(
                x,
                if (mirrored) Vars.world.tiles.height - y - 1 else Main.halfHeight + y + Vars.world.tiles.height - saved.height * 2,
                floor,
                block,
                overlay,
                data
            )
            Vars.world.tile(x, y).packedData = packetData
            Vars.world.tile(
                x,
                if (mirrored) Vars.world.tiles.height - y - 1 else Main.halfHeight + y + Vars.world.tiles.height - saved.height * 2
            ).packedData = packetData
        }

        for (x in 0..<saved.width) for (y in saved.height..<Vars.world.tiles.height - saved.height) addTile(
            x,
            y,
            CastleUtils.shopFloor,
            Blocks.air,
            Blocks.air,
            0.toByte()
        )

        Main.spawns.clear()
        state.teams.getActive().each(Cons { data: Teams.TeamData? ->
            data!!.cores.each(Cons { entity: CoreBuild? ->
                state.teams.unregisterCore(entity)
            })
        })

        saved.each(Intc2 { x: Int, y: Int ->
            val tile = saved.get(x, y)
            if (!tile.isCenter) return@Intc2
            when {
                tile.block() is CoreBlock -> {
                    val upgrade = CastleUtils.upgradeBlock(tile.block()) ?: return@Intc2
                    addRoom(x, y, upgrade.size) { BlockRoom(upgrade, 5000, tile.block()) }
                }


                tile.build is LogicBuild -> {
                    val build = (tile.build as LogicBuild)
                    val code: String? = build.code
                    val linksProcessor: Seq<LogicLink?>? = build.links.copy()
                    val tileEdit = Vars.world.tile(x, y)
                    val tileNew = Vars.world.tile(x, if (mirrored) Vars.world.tiles.height - y - 1 else Vars.world.tiles.height + y)

                    tileEdit.setNet(tile.block(), tile.build.team(), 0)
                    tileNew.setNet(tile.block(), Team.blue, 180)
                    val buildNew = tileNew.build as LogicBuild
                    val yCoordinate = tile.build.getY().toInt() / 8
                    val mirroredLinks = Seq<LogicLink?>()

                    for (link in build.links) {
                        val xLink: Int = link.x
                        val yLink: Int = if (mirrored) Vars.world.height() - yCoordinate - (link.y - yCoordinate) else Main.halfHeight +link.y
                        val mirrored = LogicLink(xLink, yLink, link.name, link.valid)
                        mirroredLinks.add(mirrored)
                    }

                    if (tileEdit.build is LogicBuild) {
                        build.updateCode(code)
                        build.links.set(linksProcessor)
                        build.updateTile()
                    }
                    if (tileNew.build is LogicBuild) {
                        buildNew.updateCode(code)
                        buildNew.links.set(mirroredLinks)
                        buildNew.updateTile()
                    }
                }
                tile.overlay() is SpawnBlock -> Main.spawns.add (x, y)
            }

        })

        for (block in castleBlocks) {
            addRoom(
                block.x,
                block.y,
                block.block.size
            ) { BlockRoom(block.block, block.cost, block.invincible, null) }

        }
        for (miner in castleMiners) {
            addRoom(
                    miner.x, miner.y, miner.block.size
            ) { MinerRoom(miner.block, miner.item, miner.cost, miner.amount, miner.interval.toFloat()) }
        }

        generateShop(7, saved.height + 6)
    }

    fun generateShop(shopX: Int, shopY: Int) {
        offsetY = 0
        offsetX = 0
        maxOffsetX = 0
        maxOffsetY = 0

        val upgradeMap = mutableMapOf<UnitType, UnitType>()
        Vars.content.blocks().each { block ->
            if (block is Reconstructor) {
                block.upgrades.each { pair ->
                    upgradeMap[pair[0]] = pair[1]
                }
            }
        }

        val erekirChains = mapOf(
            "precept" to "vanquish",
            "vanquish" to "conquer",
            "anthicus" to "tecta",
            "tecta" to "collaris",
            "obviate" to "quell",
            "quell" to "disrupt",
            "latum" to "renale"
        )

        erekirChains.forEach { (fromName, toName) ->
            val from = Vars.content.units().find { it.name == fromName }
            val to = Vars.content.units().find { it.name == toName }
            if (from != null && to != null &&
                CastleCosts.units!!.containsKey(from) &&
                CastleCosts.units!!.containsKey(to) &&
                !upgradeMap.containsKey(from)) {
                upgradeMap[from] = to
            }
        }


        val allUpgraded = upgradeMap.values.toSet()

        val branches = mutableListOf<MutableList<UnitType>>()
        val visited = mutableSetOf<UnitType>()


        CastleCosts.units!!.keys().toSeq().forEach { type ->
            if (type !in visited && type !in allUpgraded) {
                val branch = mutableListOf<UnitType>()
                var current: UnitType? = type
                while (current != null && CastleCosts.units!!.containsKey(current)) {
                    if (current in visited) break
                    branch.add(current)
                    visited.add(current)
                    current = upgradeMap[current]
                }
                if (branch.isNotEmpty()) branches.add(branch)
            }
        }

        var branchOffsetY = 0
        var branchOffsetX = 0

        for (branch in branches) {
            if (CastleUtils.revealedUnits.none { it in branch }) continue

            var tierX = 0
            for (type in branch) {
                if (!CastleUtils.revealedUnits.contains(type)) { tierX++; continue }
                if (!state.rules.bannedUnits.contains(type)) {
                    val data = CastleCosts.units!!.get(type)
                    addShopRoom(shopX + (branchOffsetX + tierX) * 9, shopY + branchOffsetY * 18, UnitRoom(type, data, true))
                    addShopRoom(shopX + (branchOffsetX + tierX) * 9, shopY + branchOffsetY * 18 + 9, UnitRoom(type, data, false))
                }
                tierX++
            }

            branchOffsetY += 1
            if(branchOffsetX>maxOffsetX) maxOffsetX = branchOffsetX
            if(branchOffsetY>maxOffsetY) maxOffsetY = branchOffsetY
            if(branchOffsetY >= unitLimitY){
                unitLimitX += 6
                branchOffsetX += 5
                branchOffsetY = 0

            }
            if (branchOffsetX - 1 >= unitLimitX) {
                branchOffsetX = 0
                branchOffsetY++
            }
        }

        val unitEndX = branchOffsetX
        val unitEndY = branchOffsetY

        offsetY = unitEndY
        offsetXAlly = unitEndX
        offsetXEnemy = unitEndX

        CastleCosts.effects!!.each { effect: StatusEffect?, data: EffectData? ->
            offsetY = if (data!!.ally) 0 else 1
            offsetX = if (data.ally) offsetXAlly else offsetXEnemy
            addShopRoom(shopX + offsetX * 9, shopY + unitEndY * 18 + offsetY * 9, EffectRoom(effect, data))
            if (data.ally) offsetXAlly += 1 else offsetXEnemy += 1
        }
    }

    private fun addTile(x: Int, y: Int, floor: Block?, block: Block, overlay: Block?, data: Byte) {
        val tile = Tile(x, y, floor, overlay, block)
        tile.data = data
        Vars.world.tiles.set(x, y, tile)
    }

    private fun addRoom(x: Int, y: Int, size: Int, create: Prov<Room>) {
        val sharded = create.get()
        sharded.set(x, y, size + 2, Team.sharded)
        sharded.spawn()

        val blue = create.get()
        blue.set(x, if (mirrored) Vars.world.height() - y - 2 + size % 2 else y + Vars.world.tiles.height - Main.halfHeight, size + 2, Team.blue)
        blue.spawn()
    }

    private fun addShopRoom(x: Int, y: Int, room: Room) {
        room.set(x, y, 5, Team.derelict)
        room.spawn()

        room.label.fontSize = 2.25f
    }

    class Spawns {
        var sharded: Seq<Point2> = Seq<Point2>()
        var blue: Seq<Point2> = Seq<Point2>()

        fun get(team: Team?): Point2? {
            return if (team === Team.sharded) sharded.random() else blue.random()
        }

        fun add(x: Int, y: Int) {
            sharded.add(Point2(x, Vars.world.height() - y - 1))
            blue.add(Point2(x, y))
        }

        fun clear() {
            sharded.clear()
            blue.clear()
        }

        fun spawn(summonner: Player, team: Team?, type: UnitType?) {
            var spawn: Point2
            do {
                spawn = get(team)!!.cpy().add(Mathf.range(Vars.tilesize), Mathf.range(Vars.tilesize))
            } while (!CastleUtils.validForSpawn(type, spawn))
            val prevLimit = state.rules.unitCap
            state.rules.unitCap = Integer.MAX_VALUE
            val unit = type!!.spawn(team, (spawn.x * Vars.tilesize).toFloat(), (spawn.y * Vars.tilesize).toFloat())
            state.rules.unitCap = prevLimit
            worldLabel("rooms.unit.bought",unit.getX(), unit.getY(),1f,Pair("player",summonner.coloredName()))
        }

        fun within(tile: Tile): Boolean {
            for (spawn in sharded) if (within(tile, spawn)) return true
            for (spawn in blue) if (within(tile, spawn)) return true

            return false
        }

        fun within(tile: Tile, point: Point2): Boolean {
            return tile.within(
                (point.x * Vars.tilesize).toFloat(),
                (point.y * Vars.tilesize).toFloat(),
                state.rules.dropZoneRadius
            )
        }

        fun draw() {
            sharded.each(Cons { spawn: Point2? -> draw(spawn!!, Team.sharded) })
            blue.each(Cons { spawn: Point2? -> draw(spawn!!, Team.blue) })
        }

        fun draw(spawn: Point2, team: Team) {
            var deg = 0
            while (deg < 360) {
                Call.effect(
                    Fx.mineBig,
                    spawn.x * Vars.tilesize + Mathf.cosDeg(deg.toFloat()) * state.rules.dropZoneRadius,
                    spawn.y * Vars.tilesize + Mathf.sinDeg(deg.toFloat()) * state.rules.dropZoneRadius,
                    0f,
                    team.color
                )
                deg += 10
            }
        }
    }
}
