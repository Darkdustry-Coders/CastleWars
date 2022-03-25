package castle;

import arc.Events;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Timer;
import castle.components.Bundle;
import castle.components.CastleIcons;
import castle.components.CastleUnitDrops;
import castle.components.PlayerData;
import mindustry.ai.types.GroundAI;
import mindustry.content.Blocks;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.EnergyFieldAbility;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class Main extends Plugin {
//Hello darkdustry. 
    @Override
    public void init() {
        UnitTypes.flare.defaultController = GroundAI::new;
        UnitTypes.horizon.defaultController = GroundAI::new;
        UnitTypes.zenith.defaultController = GroundAI::new;
        UnitTypes.antumbra.defaultController = GroundAI::new;
        UnitTypes.eclipse.defaultController = GroundAI::new;
        UnitTypes.omura.abilities.clear();
        UnitTypes.corvus.abilities.clear();
        UnitTypes.arkyid.abilities.clear();
        UnitTypes.aegires.abilities.each(ability -> {
            if (ability instanceof EnergyFieldAbility fieldAbility) {
                fieldAbility.maxTargets = 3;
                fieldAbility.status = StatusEffects.freezing;
                fieldAbility.statusDuration = 24f;
                fieldAbility.damage = 12f;
            }
        });

        CastleIcons.load();
        CastleUnitDrops.load();
        CastleRooms.load();

        netServer.admins.addActionFilter(action -> {
            if ((action.type != ActionType.placeBlock && action.type != ActionType.breakBlock) || action.tile == null) return true;

            if (action.tile.block() == Blocks.itemSource || action.tile.block() == Blocks.liquidSource || action.tile.block() == Blocks.powerSource) return false;

            if (Seq.with(action.tile.nearby(0), action.tile.nearby(1), action.tile.nearby(2), action.tile.nearby(3)).contains(tile -> tile != null && (tile.block() == Blocks.itemSource || tile.block() == Blocks.itemSource))) return false;
            return !action.tile.getLinkedTilesAs(action.block, new Seq<>()).contains(tile -> tile.floor() == Blocks.metalFloor || tile.floor() == Blocks.metalFloor5 || tile.overlay() == Blocks.tendrils);
        });

        netServer.assigner = (player, players) -> {
            int sharded = Seq.with(players).count(p -> p != player && p.team() == Team.sharded);
            int blue = Seq.with(players).count(p -> p != player && p.team() == Team.blue);

            return sharded > blue ? Team.blue : Team.sharded;
        };

        Events.on(PlayerJoin.class, event -> PlayerData.datas.put(event.player.uuid(), new PlayerData(event.player)));

        Events.on(PlayerLeave.class, event -> PlayerData.datas.remove(event.player.uuid()));

        Events.on(BlockDestroyEvent.class, event -> {
            if (!world.isGenerating() && !state.gameOver) {
                if (event.tile.block() instanceof CoreBlock && event.tile.team().cores().size <= 1) {
                    if (event.tile.team() == Team.sharded) {
                        CastleLogic.gameOver(Team.blue);
                    } else if (event.tile.team() == Team.blue) {
                        CastleLogic.gameOver(Team.sharded);
                    }
                }
            }
        });

        Events.on(UnitDestroyEvent.class, event -> {
            int income = CastleUnitDrops.get(event.unit.type);
            if (income > 0 && !event.unit.spawnedByCore) {
                PlayerData.datas().each(data -> data.player.team() != event.unit.team, data -> {
                    data.money += income;
                    Call.label(data.player.con, "[lime] + [accent]" + income, 0.5f, event.unit.x, event.unit.y);
                });
            }
        });

        Timer.schedule(CastleLogic::update, 0f, 0.01f);

        CastleLogic.restart();
        netServer.openServer();
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("hud", "Toggle HUD.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            if (data.showHud) {
                data.showHud = false;
                Call.hideHudText(player.con);
                Bundle.bundled(player, "commands.hud.off");
            } else {
                data.showHud = true;
                Bundle.bundled(player, "commands.hud.on");
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("stop");
        handler.removeCommand("gameover");

        handler.register("gameover", "End the game.", args -> CastleLogic.gameOver(Team.derelict));
    }
}
