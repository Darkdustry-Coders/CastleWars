package castle

import arc.Events
import arc.func.Boolf
import arc.func.Cons
import arc.func.Func
import arc.func.Prov
import arc.struct.OrderedMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Time
import arc.util.Timer

import mindustry.Vars
import mindustry.ai.ControlPathfinder
import mindustry.content.UnitTypes
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Unit
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
import mindurka.util.prefixed


class Main : Plugin() {
    override fun init() {
        val patch = StringBuilder()
        patch.append("block.malign.heatRequirement: 0\n")
        Gamemode.defaultPatch = Prov {patch.toString()}
        Gamemode.restoreTeams = true
        Gamemode.unlockSpecialBlocks = false
        Gamemode.hasStats = true
        Gamemode.init(javaClass.classLoader.prefixed("castle"))

        Vars.content.statusEffects().each(Cons { effect: StatusEffect? -> effect!!.permanent = false })

        Vars.content.units()
            .each(Boolf { type: UnitType? -> type!!.playerControllable }) { type: UnitType? ->
                type!!.payloadCapacity = 0f
                type.controller = Func { unit: Unit? -> CastleCommandAI() }
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
            !((action.tile.block() is Turret && action.type != ActionType.depositItem)
                    || action.tile.block() is Drill)
        })

        Events.on(PlayerJoin::class.java, Cons { event: PlayerJoin? ->
            val data = PlayerData.getData(event!!.player)
            if (data == null) {
                PlayerData.datas.add(PlayerData(event.player))
                return@Cons
            }
            data.player = event.player
        })

        Events.on(
            PlayerConnectionConfirmed::class.java
        ) { event: PlayerConnectionConfirmed ->
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

        Events.on(TapEvent::class.java, Cons { event: TapEvent? ->
            val data = PlayerData.getData(event!!.player)
            if (event.player.team().core() == null || event.player.unit() == null || data == null) return@Cons

            val tapped = event.tile
            rooms.each<CastleRooms.Room?>(
                Boolf { room -> room!!.check(tapped) && room.canBuy(data) }
            ) { room -> room!!.buy(data) }

            val existing = playerTasks.get(event.player.uuid())
            if (existing != null) existing.cancelled = true

            val task = HoldTask(event.player, Time.millis())
            playerTasks.put(event.player.uuid(), task)
            Time.runTask(34f, task)
        })


        Events.on(UnitDestroyEvent::class.java, Cons { event: UnitDestroyEvent? ->
            CastleCosts.units?.containsKey(event!!.unit.type)?.let { if (!it) return@Cons }
            val income: Int = CastleCosts.units!!.get(event!!.unit.type).drop
            PlayerData.datas.each(
                Boolf { data: PlayerData? ->
                    data!!.player.team() !== event.unit.team && data.player.team().core() != null
                }
            ) { data: PlayerData? ->
                data!!.money += income
                Call.label(data.player.con, "[lime]+[accent] " + income, 1f, event.unit.x, event.unit.y)
            }
        })

        Events.on(
            PlayEvent::class.java
        ) { event: PlayEvent? -> CastleUtils.applyRules(Vars.state.rules) }

        Events.on(ResetEvent::class.java) { event: ResetEvent? ->
            rooms.clear()
            PlayerData.datas.retainAll { data: PlayerData? -> data!!.player.con.isConnected }
                .each(Cons { obj: PlayerData? -> obj!!.reset() })
            TeamData.datas.clear()
            timer = 45 * 60
        }

        Events.on(
            WorldLoadEndEvent::class.java
        ) { event: WorldLoadEndEvent? -> CastleGenerator.generate() }

        Timer.schedule(Runnable schedule@{
            if (isBreak) return@schedule
            unitOnBorderKill()
        }, 0f, 0.1f)

        Timer.schedule( Runnable schedule@{
            if (isBreak) return@schedule
            replaceTurretBullets()
        }, 0f, 0.1f)

        Timer.schedule(Runnable schedule@ {
            if (isBreak) return@schedule
            PlayerData.datas.each(Cons { obj: PlayerData? -> obj!!.updateMoney() })
            spawns.draw()
            if (--timer == 0) Events.fire(GameOverEvent(Team.derelict))
        }, 0f, 1f)
    }

    companion object {
        @JvmField
        val rooms: Seq<CastleRooms.Room?> = Seq<CastleRooms.Room?>()
        @JvmField
        val spawns: Spawns = Spawns()
        @JvmField
        var timer: Int = 0
        @JvmField
        var halfHeight: Int = 0

        val playerTasks = OrderedMap<String, HoldTask>()
    }
}