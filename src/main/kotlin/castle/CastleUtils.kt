package castle

import arc.func.Boolf
import arc.func.Cons
import arc.func.Cons2
import arc.math.Mathf
import arc.math.geom.Point2
import arc.struct.Seq
import arc.util.Log
import arc.util.Nullable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import buj.tl.Tl
import castle.CastleCosts.UnitData
import castle.CastleCosts.effects
import castle.CastleCosts.items
import castle.CastleCosts.turrets
import castle.CastleCosts.units
import castle.Main.Companion.halfHeight
import castle.Main.Companion.rooms
import kotlinx.serialization.json.boolean
import mindurka.util.ModifyWorld.syncBuild
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.Planets
import mindustry.game.Rules
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Teamc
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.type.unit.ErekirUnitType
import mindustry.type.unit.NeoplasmUnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.Tiles
import mindustry.world.blocks.defense.turrets.Turret
import mindustry.world.blocks.environment.Floor
import mindustry.world.blocks.production.Drill
import mindustry.world.blocks.storage.CoreBlock
import mindustry.world.blocks.units.UnitFactory
import mindustry.world.meta.BlockGroup
import mindustry.world.meta.Env

import mindurka.util.Schematic
import mindustry.Vars.tilesize
import mindustry.content.Liquids
import mindustry.content.UnitTypes
import mindustry.gen.Building
import mindustry.gen.Unit
import mindustry.world.blocks.defense.turrets.ContinuousLiquidTurret.ContinuousLiquidTurretBuild
import mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild
import mindustry.world.blocks.defense.turrets.LiquidTurret.LiquidTurretBuild

object CastleUtils {
    private var betterGroundValid = true

    var revealedUnits: Seq<UnitType?> = Seq<UnitType?>()
    var generatePlatforms: Boolean = true

    var platformSource: Seq<Tiles?> = Seq<Tiles?>()

    var shopFloor: Floor? = Blocks.empty.asFloor()

    var boatSpawns: Seq<Point2> = Seq<Point2>()
    var landSpawns: Seq<Point2> = Seq<Point2>()
    var airSpawns: Seq<Point2> = Seq<Point2>()

    var isDivideCap: Int = 1
    val capType: UnitCapType
        get() {
            if (isDivideCap == 0) {
                return UnitCapType.NONE
            }
            val hasAttack = attackCap > 0
            val hasDefense = defenseCap > 0
            return when {
                hasAttack && hasDefense -> UnitCapType.BOTH
                hasAttack -> UnitCapType.ATTACK_ONLY
                hasDefense -> UnitCapType.DEFENSE_ONLY
                else -> UnitCapType.NONE
            }
        }
    var defenseCap: Short = 0
    var attackCap: Short = 0
    var unitCap: Short = 500

    var castleBlocks: Seq<CastleBlock> = Seq<CastleBlock>()

    var castleMiners: Seq<CastleMiner> = Seq<CastleMiner>()

    var mirrored: Boolean = true

    data class CastleBlock(
        val block: Block,
        val x: Int,
        val y: Int,
        val cost: Int,
        val invincible: Boolean
    ) {
        fun contains(tx: Int, ty: Int): Boolean =
            tx >= x && tx < x + block.size && ty >= y && ty < y + block.size
    }

    data class CastleMiner(
        val block: Block,
        val x: Int,
        val y: Int,
        val cost: Int,
        val amount: Int,
        val interval: Int,
        val item: Item
    ) {
        fun contains(tx: Int, ty: Int): Boolean =
            tx >= x && tx < x + block.size && ty >= y && ty < y + block.size
    }


    fun randomSpawn(spawns: Seq<Point2>): Point2 =
        if (spawns.isEmpty) Point2(-1, -1) else spawns.get(Mathf.random(spawns.size - 1))

    fun refreshMeta() {
        revealedUnits.clear()
        if (isSerpulo) revealedUnits.addAll(
            Vars.content.units()
                .select { unit: UnitType? ->
                    !unit!!.internal
                        && !(unit is NeoplasmUnitType || unit is ErekirUnitType)
                }
        )
        if (isErekir) revealedUnits.addAll(
            Vars.content.units()
                .select { unit: UnitType? ->
                    !unit!!.internal
                        && (unit is NeoplasmUnitType || unit is ErekirUnitType)
                }
        )

        generatePlatforms = true
        platformSource.clear()
        shopFloor = Blocks.space.asFloor()

        boatSpawns.clear()
        landSpawns.clear()
        airSpawns.clear()

        defenseCap = 100
        attackCap = 0
        unitCap = 500
        isDivideCap = 1
        betterGroundValid = true
        mirrored = true

        castleBlocks.clear()
        castleMiners.clear()

        CastleCosts.load()
        val rules = Vars.state.rules

        rules.tags.each(Cons2 { key: String?, value: String? ->
            if (!key!!.startsWith("mdrk.castle.")) return@Cons2
            var key = key.replace("mdrk.castle.", "")
            if (key.startsWith("utils")) {
                key = key.replace("utils.", "")
                when {
                    key.startsWith("noPlatform") -> generatePlatforms = false

                    key.startsWith("mirrored") -> mirrored = value.toBoolean()

                    key.startsWith("platformSource.") && !key.startsWith("platformSource.count") ->
                        try {
                            val idx = key.removePrefix("platformSource.").toInt()
                            val tiles = schematicToTiles(value!!,5,5)
                            if (tiles != null) {
                                while (platformSource.size <= idx) platformSource.add(null as Tiles?)
                                platformSource.set(idx, tiles)
                            }
                        } catch (error: Exception) {
                            Log.warn("Failed to load platform source #${key}: $error")
                        }

                    key.startsWith("shopFloor") ->
                        try {
                            shopFloor = Vars.content.block(value)?.asFloor()
                                ?: Blocks.space.asFloor()
                        } catch (error: Exception) {
                            Log.warn("Failed to set custom shop floor!\n$error")
                        }

                    key.startsWith("navalSpawn") ->
                        try {
                            parsePoints(value!!).forEach { boatSpawns.add(it) }
                        } catch (error: Exception) {
                            Log.warn("Failed to set boat spawns!\n$error")
                        }

                    key.startsWith("airSpawn") ->
                        try {
                            parsePoints(value!!).forEach { airSpawns.add(it) }
                        } catch (error: Exception) {
                            Log.warn("Failed to set air spawns!\n$error")
                        }

                    key.startsWith("groundSpawn") ->
                        try {
                            parsePoints(value!!).forEach { landSpawns.add(it) }
                        } catch (error: Exception) {
                            Log.warn("Failed to set ground spawns!\n$error")
                        }

                    key.startsWith("defenseUnitCap") ->
                        try {
                            defenseCap = value!!.toShort()
                        } catch (error: Exception) {
                            Log.warn("Failed to set defense cap!\n$error")
                        }

                    key.startsWith("attackUnitCap") ->
                        try {
                            attackCap = value!!.toShort()
                        } catch (error: Exception) {
                            Log.warn("Failed to set attack cap!\n$error")
                        }

                    key.startsWith("divideCap") ->
                        try {
                            isDivideCap = if (value.toBoolean()) 1 else 0
                        } catch (error: Exception) {
                            Log.warn("Failed to set divided cap state!\n$error")
                        }

                    key.startsWith("betterGroundValid") ->
                        try {
                            betterGroundValid = value.toBoolean()
                        } catch (error: Exception) {
                            Log.warn("Failed to set betterGroundValid state!\n$error")
                        }
                }
            }
            when {
                key.startsWith("unit.") && !key.endsWith("unit.") -> {
                    val parts = key.split(".")
                    val unit = Vars.content.unit(parts[1]) ?: return@Cons2
                    if (!units!!.containsKey(unit)) units!!.put(unit, UnitData(-1, -1, -1))
                    val data = units!!.get(unit)
                    when {
                        key.endsWith("cost") -> data.cost = value!!.toInt()
                        key.endsWith("income") -> data.income = value!!.toInt()
                        key.endsWith("drop") -> data.drop = value!!.toInt()
                    }
                }
                key.startsWith("turret.") && !key.endsWith("turret.") -> {
                    val parts = key.split(".")
                    val block = Vars.content.block(parts[1].lowercase()) as? Turret ?: return@Cons2
                    if (key.endsWith("cost")) turrets!!.put(block, value!!.toInt())
                }
                key.startsWith("status.") && !key.endsWith("status.") -> {
                    val parts = key.split(".")
                    val effect = Vars.content.statusEffect(parts[1].lowercase()) ?: return@Cons2
                    if (!effects!!.containsKey(effect)) effects!!.put(effect, CastleCosts.EffectData(0, 0, true, 0f))
                    val data = effects!!.get(effect)
                    when {
                        key.endsWith("cost") -> data.cost = value!!.toInt()
                        key.endsWith("delay") -> data.delay = value!!.toFloat()
                        key.endsWith("duration") -> data.duration = value!!.toInt()
                        key.endsWith("ally") -> data.ally = value!!.toBoolean()
                    }
                }
                key.startsWith("item.") && !key.endsWith("item.") -> {
                    val parts = key.split(".")
                    val item = Vars.content.item(parts[1].lowercase()) ?: return@Cons2
                    if (!items!!.containsKey(item)) items!!.put(item,
                        CastleCosts.ItemData(-1, -1f, -1, Blocks.laserDrill)
                    )
                    val data = items!!.get(item)
                    when {
                        key.endsWith("cost") -> data.cost = value!!.toInt()
                        key.endsWith("amount") -> data.amount = value!!.toInt()
                        key.endsWith("interval") -> data.interval = value!!.toFloat()
                        key.endsWith("drill") -> data.drill = Vars.content.block(value) ?: return@Cons2
                    }
                }
            }
        })

        platformSource.removeAll { it == null }

        if (platformSource.isEmpty) {
            // Default: metalFloor 6×6
            val newSource = Tiles(6, 6)
            newSource.fill()
            newSource.eachTile { tile: Tile? -> tile!!.setFloor(Blocks.metalFloor.asFloor()) }
            platformSource.add(newSource)
        }

        val blocksJson = rules.tags.get("mdrk.castle.blocks")
        if (!blocksJson.isNullOrBlank()) {
            try {
                val array = Json.parseToJsonElement(blocksJson) as? JsonArray
                    ?: throw IllegalStateException("blocks tag is not a JSON array")
                for ((i, element) in array.withIndex()) {
                    try {
                        val obj       = element as? JsonObject ?: continue
                        val x         = obj["x"]?.jsonPrimitive?.int ?: continue
                        val y         = obj["y"]?.jsonPrimitive?.int ?: continue
                        val cost      = obj["cost"]?.jsonPrimitive?.int ?: 0
                        val invincible      = obj["invincible"]?.jsonPrimitive?.boolean ?: false
                        val blockName = obj["block"]?.jsonPrimitive?.content ?: continue
                        val block     = Vars.content.block(blockName) ?: run {
                            Log.warn("CastleUtils: unknown block '$blockName' in blocks tag")
                            continue
                        }
                        castleBlocks.add(CastleBlock(block, x, y, cost,invincible))
                    } catch (e: Exception) {
                        Log.warn("CastleUtils: failed to parse castle block entry #$i: $e")
                    }
                }
            } catch (e: Exception) {
                Log.warn("CastleUtils: failed to parse 'mdrk.castle.blocks' tag: $e")
            }
        }

        val minersJson = rules.tags.get("mdrk.castle.miners")
        if (!minersJson.isNullOrBlank()) {
            try {
                val array = Json.parseToJsonElement(minersJson) as? JsonArray
                    ?: throw IllegalStateException("miners tag is not a JSON array")
                for ((i, element) in array.withIndex()) {
                    try {
                        val obj       = element as? JsonObject ?: continue
                        val x         = obj["x"]?.jsonPrimitive?.int ?: continue
                        val y         = obj["y"]?.jsonPrimitive?.int ?: continue
                        val cost      = obj["cost"]?.jsonPrimitive?.int ?: 0
                        val amount    = obj["amount"]?.jsonPrimitive?.int ?: 0
                        val interval  = obj["interval"]?.jsonPrimitive?.int ?: 0
                        val blockName = obj["block"]?.jsonPrimitive?.content ?: continue
                        val itemName  = obj["item"]?.jsonPrimitive?.content ?: continue
                        val block     = Vars.content.block(blockName.lowercase()) ?: run {
                            Log.warn("CastleUtils: unknown block '$blockName' in miners tag")
                            continue
                        }
                        val item      = Vars.content.item(itemName.lowercase()) ?: run {
                            Log.warn("CastleUtils: unknown item '$itemName' in miners tag")
                            continue
                        }
                        castleMiners.add(CastleMiner(block, x, y, cost, amount, interval, item))
                    } catch (e: Exception) {
                        Log.warn("CastleUtils: failed to parse castle miner entry #$i: $e")
                    }
                }
            } catch (e: Exception) {
                Log.warn("CastleUtils: failed to parse 'mdrk.castle.miners' tag: $e")
            }
        }
    }

    fun replaceTurretBullets(){
        Groups.build.each(Cons each@{ build: Building? ->
            try {
                if (build!!.block !== Blocks.sublimate && build is ItemTurretBuild) {
                    for (i in 0..<build.ammo.size-1) {
                        if (build.ammo.size > 1) {
                            if (build.ammo.get(i).amount >= 25) build.ammo.remove(i)
                        } else {
                            if (build.ammo.get(i).amount > 25) {
                                build.update()
                                build.updateTile()
                                build.ammo.get(i).amount = 25
                                syncBuild(build)
                            }
                            build.totalAmmo = 1
                        }
                    }
                }
                if (build is LiquidTurretBuild) {
                    var hasLiq = false
                    var minX = -2
                    var minY = -2
                    if (build.hitSize() == 16f) {
                        minX = -1
                        minY = -1
                    }
                    a@ for (dx in minX..2) for (dy in minY..2) {
                        val build2 = Vars.world.build(build.tileX() + dx, build.tileY() + dy) ?: continue
                        if (!build2.block.hasLiquids || !build2.block.outputsLiquid) continue
                        if (build2.liquids.current() === build.liquids.current()) continue
                        hasLiq = true
                        break@a
                    }
                    if (!hasLiq) return@each
                    build.liquids.clear()
                    try {
                        syncBuild(build)
                    } catch (ohno: Exception) {
                        Log.err(ohno)
                    }
                }
                if (build.block !== Blocks.sublimate) return@each
                if (build is ContinuousLiquidTurretBuild) {
                    if (build.liquids.current() !== Liquids.ozone) return@each
                    var hasCyan = false
                    a@ for (dx in -2..2) for (dy in -2..2) {
                        val build2 = Vars.world.build(build.tileX() + dx, build.tileY() + dy) ?: continue
                        if (!build2.block.hasLiquids) continue
                        if (build2.liquids.current() !== Liquids.cyanogen) continue
                        hasCyan = true
                        break@a
                    }
                    if (!hasCyan) return@each
                    build.liquids.clear()
                    try {
                        syncBuild(build)
                    } catch (ohno: Exception) {
                        Log.err(ohno)
                    }
                }
            } catch (ohno: Exception) {
                throw RuntimeException(ohno)
            }
        })
    }

    fun unitOnBorderKill(){
        PlayerData.datas.each(Cons { obj: PlayerData? -> obj!!.update() })
        rooms.each(Cons { obj: CastleRooms.Room? -> obj!!.update() })
        Groups.unit.each(Boolf { unit: Unit? ->
            if (unit!!.spawnedByCore) return@Boolf false
            if (!Vars.world.tiles.`in`(unit.tileX(), unit.tileY())) return@Boolf true

            if (unit.tileY() >= halfHeight && unit.tileY() <= Vars.world.height() - halfHeight - 1) {
                if (!onEnemySide(unit) && (unit.type === UnitTypes.poly || unit.type === UnitTypes.mega)) {
                    unit.set(unit.team().core().x, unit.team().core().y)
                    return@Boolf false
                }
                return@Boolf true
            }
            false
        }, Cons { unit: Unit? -> Call.unitEnvDeath(unit) })
    }

    private fun parsePoints(value: String): List<Point2> {
        return value.split(",").mapNotNull { entry ->
            val parts = entry.trim().split(" ")
            if (parts.size < 2) null
            else Point2(parts[0].toInt(), parts[1].toInt())
        }
    }

    private fun schematicToTiles(schematic: String,w: Int,h: Int): Tiles {
        val tiles = Tiles(w,h)
        tiles.fill()
        val schematic = Schematic.of(schematic)
        val packetData = schematic.data
        val floors = schematic.floors
        val overlays = schematic.overlays
        for (x in 0 until w) {
            for (y in 0 until h) {
                val idx = x + y * w
                val tile: Tile = tiles.get(x, y) ?: continue

                floors[idx]?.let   { tile.setFloor(it) }
                overlays[idx]?.let { tile.setOverlay(it) }

                if (packetData[idx] != 0L) {
                    tile.setPackedData(packetData[idx])
                }
            }
        }
        return tiles
    }

    fun worldLabel(key: String, x: Float, y: Float, duration: Float, vararg args: Pair<String, String>) {
        Groups.player.each { player ->
            val fmt = Tl.fmt(player)
            args.forEach { (k, v) -> fmt.put(k, v) }
            Call.label(player.con, fmt.done("{${key}}"), duration, x, y)
        }
    }

    fun validForSpawn(type: UnitType?, pos: Point2): Boolean {
        val tile = Vars.world.tile(pos.x, pos.y)
        // TODO: Check if tile is in death zone.
        return tile != null &&
            ((type!!.flying && !type.canBoost) || !tile.block().solid) &&
            (!type.naval || tile.floor().isLiquid) &&
            ((type.naval || type.flying) || tile.floor().drownTime.toDouble() == 0.0 || !betterGroundValid)
    }

    fun withinPointDef(tile: Tile, point: Point2, distance: Int): Boolean {
        val ySecondPos = (Vars.world.height() - point.y)
        return (tile.within(
            (point.x * Vars.tilesize).toFloat(),
            (point.y * Vars.tilesize).toFloat(),
            distance.toFloat()
        ) || tile.within(
            (point.x * Vars.tilesize).toFloat(),
            (ySecondPos * Vars.tilesize).toFloat(),
            distance.toFloat()
        ))
    }

    fun withinAnyPointDef(tile: Tile, points: Seq<Point2>, distance: Int): Boolean {
        for (p in points) if (withinPointDef(tile, p, distance)) return true
        return false
    }

    fun applyRules(rules: Rules) {
        rules.polygonCoreProtection = true
        rules.pvp = true
        rules.attackMode = true

        rules.unitCap = unitCap.toInt()
        rules.unitCapVariable = false

        rules.modeName = "Castle Wars"

        rules.teams.get(Team.sharded).cheat = true
        rules.teams.get(Team.blue).cheat = true

        rules.weather.clear()
        rules.bannedBlocks.addAll(Vars.content.blocks().select { block: Block? ->
            block is Turret || block is Drill
                || block is UnitFactory || block is CoreBlock || block!!.group == BlockGroup.logic
        })
        rules.canGameOver = true
    }

    val isSerpulo: Boolean
        get() = Vars.state.rules.planet == Planets.serpulo ||
            Vars.state.rules.planet == Planets.sun ||
            !Vars.state.rules.hasEnv(Env.scorching)

    val isErekir: Boolean
        get() = Vars.state.rules.planet === Planets.erekir ||
            Vars.state.rules.planet == Planets.sun ||
            Vars.state.rules.hasEnv(Env.scorching)

    fun drill(item: Item?): Block? {
        if (item === Items.lead || item === Items.copper || item === Items.titanium || item === Items.metaglass || item === Items.coal || item === Items.scrap || item === Items.plastanium || item === Items.surgeAlloy || item === Items.pyratite || item === Items.blastCompound || item === Items.sporePod) return Blocks.laserDrill
        if (item === Items.beryllium || item === Items.tungsten || item === Items.oxide || item === Items.carbide || item === Items.fissileMatter || item === Items.dormantCyst) return Blocks.impactDrill

        return if (Vars.state.rules.hasEnv(Env.scorching)) Blocks.impactDrill else Blocks.laserDrill
    }

    @Nullable
    fun upgradeBlock(block: Block?): Block? {
        if (block === Blocks.coreBastion) return Blocks.coreAcropolis
        if (block === Blocks.coreShard) return Blocks.coreNucleus

        return null
    }

    val isBreak: Boolean
        get() = Vars.state.gameOver || Vars.state.isPaused || Vars.world.isGenerating

    fun onEnemySide(teamc: Teamc): Boolean {
        return (teamc.team() === Team.sharded && teamc.y() > Vars.world.unitHeight() / 2f)
            || (teamc.team() === Team.blue && teamc.y() < Vars.world.unitHeight() / 2f)
    }

    fun mirrorY(y: Int): Int {
        when {
            y <= halfHeight -> {
                return if (mirrored) Vars.world.height() - y - 1
                else y+Vars.world.tiles.height-halfHeight
            }
            y > halfHeight -> {
                return if (mirrored) y-Vars.world.height() - halfHeight*2
                else halfHeight-(y-Vars.world.height()+halfHeight)
            }
        }
        return 0 //how
    }

    enum class UnitCapType {
        NONE,
        ATTACK_ONLY,
        DEFENSE_ONLY,
        BOTH
    }
}
