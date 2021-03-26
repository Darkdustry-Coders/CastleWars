package CastleWars;

import CastleWars.game.Logic;
import CastleWars.logic.PlayerData;
import CastleWars.logic.UnitCost;
import arc.Events;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Nulls;
import mindustry.gen.Unit;
import mindustry.mod.Plugin;

public class Main extends Plugin {

    Logic logic;

    @Override
    public void init() {
        logic = new Logic();
        UnitCost.init(logic);

        Events.run(EventType.Trigger.update, () -> {
            Groups.unit.each(unit -> {
                if ((unit.team == Team.sharded && unit.tileY() > Vars.world.height() / 2) || (unit.team == Team.blue && unit.tileY() < Vars.world.height() / 2)) {
                    unit.set(unit.team().core().x, unit.team().core().y + 3 *Vars.tilesize);
                    if (unit.isPlayer()) unit.getPlayer().unit(Nulls.unit);
                }
            });
            Groups.player.each(player -> {
                if (player.unit().spawnedByCore && player.unit().type != UnitTypes.dagger) {
                    if (player.team().core() != null) {
                        Unit unit = UnitTypes.dagger.create(Team.crux);
                        unit.set(player.team().core().x, player.team().core().y + 3 *Vars.tilesize);
                        unit.add();
                        unit.team(player.team());
                        unit.spawnedByCore = true;
                        player.unit(unit);
                    }
                }
            });
            logic.update();
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            logic.datas.add(new PlayerData(event.player));
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            logic.datas.remove(data -> data.player.equals(event.player));
        });
        
        Events.on(EventType.ServerLoadEvent.class, event -> {
            logic.reset();
            Vars.netServer.openServer();

            Blocks.coreShard.unitCapModifier = 999999;
            Blocks.coreNucleus.unitCapModifier = 999999;
            Blocks.coreFoundation.unitCapModifier = 999999;
        });
    }
}