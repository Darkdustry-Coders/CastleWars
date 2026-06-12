package castle

import arc.Core
import arc.func.Boolf
import arc.func.Cons
import arc.struct.OrderedMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Time
import arc.util.io.Streams

import mindustry.Vars
import mindustry.ai.ControlPathfinder
import mindustry.content.UnitTypes
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.mod.Plugin
import mindustry.net.Administration.*
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.blocks.defense.turrets.Turret
import mindustry.world.blocks.production.Drill

import castle.CastleGenerator.Spawns
import castle.CastleUtils.isBreak
import castle.CastleUtils.replaceTurretBullets
import castle.CastleUtils.unitOnBorderKill

import mindurka.util.ModifyWorld.syncBuild
import mindurka.api.Gamemode
import mindurka.api.Lifetime
import mindurka.api.emit
import mindurka.api.interval
import mindurka.api.on
import mindurka.util.prefixed

class Main : Plugin() {
    override fun init() {
        Gamemode.init(javaClass.classLoader.prefixed("castle"))

        val patch = Streams.copyString(javaClass.classLoader.prefixed("castle").getResourceAsStream("patch.hjson"))

        Gamemode.defaultPatch = { patch }
        Gamemode.restoreTeams = true
        Gamemode.unlockSpecialBlocks = false

        Vars.content.statusEffects().each(Cons { effect: StatusEffect -> effect.permanent = false })

        Vars.content.units()
            .each({ type: UnitType -> type.playerControllable }) { type: UnitType ->
                type.payloadCapacity = 0f
                type.controller = { CastleCommandAI() }
            }

        UnitTypes.omura.abilities.clear()

        UnitTypes.renale.pathCost = ControlPathfinder.costLegs
        UnitTypes.latum.pathCost = ControlPathfinder.costLegs

        CastleCosts.load()

        Vars.netServer.admins.addActionFilter(ActionFilter { action: PlayerAction? ->
            if (action?.tile == null) return@ActionFilter true
            if (spawns.within(action.tile) ||
                CastleUtils.withinAnyPointDef(action.tile, CastleUtils.boatSpawns, 16) ||
                CastleUtils.withinAnyPointDef(action.tile, CastleUtils.landSpawns, 16) ||
                CastleUtils.withinAnyPointDef(action.tile, CastleUtils.airSpawns, 16)
            ) return@ActionFilter false
            !(undestroyableBlocks.contains(action.tile.build))
        })

        on { event: PlayerJoin ->
            PlayerData.of(event.player).player = event.player
        }

        on { event: PlayerConnectionConfirmed ->
            try {
                Groups.build.each { b: Building? ->
                    if (b is Building) {
                        syncBuild(b)
                    }
                }
            } catch (ohno: Exception) {
                Log.err("Error while syncing build for @, error: @",event.player.name,ohno)
            }
        }

        on { event: TapEvent ->
            val data = PlayerData.of(event.player)
            if (event.player.team().core() == null || event.player.unit() == null) return@on

            val tapped = event.tile
            rooms.each<CastleRooms.Room>(
                { room -> room.check(tapped) && room.canBuy(data) }
            ) { room -> room.buy(data) }

            val existing = playerTasks.get(event.player.uuid())
            if (existing != null) existing.cancelled = true

            val task = HoldTask(event.player, Time.millis())
            playerTasks.put(event.player.uuid(), task)
            Time.runTask(34f, task)
        }

        on { event: UnitDestroyEvent ->
            CastleCosts.units.containsKey(event.unit.type).let { if (!it) return@on }
            val income: Int = CastleCosts.units.get(event.unit.type).drop
            PlayerData.datas.each(
                { data: PlayerData ->
                    data.player.team() !== event.unit.team && data.player.team().core() != null
                }
            ) { data: PlayerData ->
                data.money += income
                Call.label(data.player.con, "[lime]+[accent] $income", 1f, event.unit.x, event.unit.y)
            }
        }

        on { _: PlayEvent ->
            CastleUtils.applyRules(Vars.state.rules)

            timer = 45 * 60

            undestroyableBlocks.clear()

            interval(1f, 1f, lifetime = Lifetime.Round) schedule@{
                if (isBreak) return@schedule
                PlayerData.datas.each { obj: PlayerData -> obj.updateMoney() }
                spawns.draw()
                if (--timer <= 0) emit(GameOverEvent(Team.derelict))
            }
        }

        on { _: ResetEvent ->
            rooms.clear()
            PlayerData.datas.retainAll { data: PlayerData? -> data!!.player.con.isConnected }
                .each(Cons { obj: PlayerData? -> obj!!.reset() })
            TeamData.datas.clear()
        }

        on { _: WorldLoadEndEvent -> CastleGenerator.generate() }

        interval(0.1f, 0f) schedule@{
            if (isBreak) return@schedule
            unitOnBorderKill()
        }

        interval(0.1f, 0f) schedule@{
            if (isBreak) return@schedule
            replaceTurretBullets()
        }
    }

    companion object {
        @JvmField
        val rooms = Seq<CastleRooms.Room>(CastleRooms.Room::class.java)
        @JvmField
        val spawns = Spawns()
        @JvmField
        var timer = 0
        @JvmField
        var halfHeight = 0

        val playerTasks = OrderedMap<String, HoldTask>()

        val undestroyableBlocks = Seq<Building>()
    }
}