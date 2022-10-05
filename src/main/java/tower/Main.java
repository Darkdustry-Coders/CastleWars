package tower;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.ai.types.GroundAI;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.defense.ShockMine;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BlockFlag;

import java.awt.Color;

import static arc.util.Strings.kebabToCamel;
import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.Fx.*;
import static mindustry.content.Items.*;
import static mindustry.content.StatusEffects.disarmed;
import static mindustry.content.UnitTypes.*;
import static mindustry.type.ItemStack.with;

public class Main extends Plugin {

    public static ObjectMap<UnitType, ItemStack[]> drops;

    public static float multiplier = 1f;

    public static char getIcon(Item item) {
        try {
            return Reflect.get(Iconc.class, kebabToCamel("item-" + item.name));
        } catch (Exception e) {
            return '?';
        }
    }

    public static String trafficLightColor(float value) {
        return "[#" + Integer.toHexString(Color.HSBtoRGB(value / 3f, 1f, 1f)).substring(2) + "]";
    }

    public static boolean isPath(Tile tile) {
        return tile.floor() == darkPanel4 || tile.floor() == darkPanel5;
    }

    public static boolean proximityCheck(Tile tile, Block block) {
        return !tile.getLinkedTilesAs(block, new Seq<>()).contains(Main::isPath);
    }

    @Override
    public void init() {
        drops = ObjectMap.of(
                crawler, with(copper, 20, lead, 10, silicon, 3),
                atrax, with(copper, 30, lead, 40, graphite, 10, titanium, 5),
                spiroct, with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
                arkyid, with(copper, 300, graphite, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
                toxopid, with(copper, 400, lead, 400, silicon, 120, graphite, 120, thorium, 40, plastanium, 40, phaseFabric, 5, surgeAlloy, 15),

                dagger, with(copper, 20, lead, 10, silicon, 3),
                mace, with(copper, 30, lead, 40, silicon, 10, titanium, 5),
                fortress, with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
                scepter, with(copper, 300, silicon, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
                reign, with(copper, 400, lead, 400, silicon, 120, graphite, 120, thorium, 40, plastanium, 40, phaseFabric, 5, surgeAlloy, 15),

                nova, with(copper, 20, lead, 10, metaglass, 3),
                pulsar, with(copper, 30, lead, 40, metaglass, 10),
                quasar, with(lead, 100, metaglass, 40, silicon, 40, titanium, 80, thorium, 10),
                vela, with(copper, 300, metaglass, 80, graphite, 80, titanium, 60, plastanium, 20, surgeAlloy, 5),
                corvus, with(copper, 400, lead, 400, silicon, 100, metaglass, 120, graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10),

                flare, with(copper, 20, lead, 10, graphite, 3, scrap, 1),
                horizon, with(copper, 30, lead, 40, graphite, 10, scrap, 2),
                zenith, with(lead, 100, silicon, 40, graphite, 40, titanium, 30, plastanium, 10, scrap, 3),
                antumbra, with(copper, 300, graphite, 80, metaglass, 80, titanium, 60, surgeAlloy, 15, scrap, 4),
                eclipse, with(copper, 400, lead, 400, silicon, 120, graphite, 120, titanium, 120, thorium, 40, plastanium, 40, phaseFabric, 10, surgeAlloy, 5, scrap, 5),

                mono, with(copper, 20, lead, 10, silicon, 3),
                poly, with(copper, 30, lead, 40, silicon, 10, titanium, 5),
                mega, with(lead, 100, silicon, 40, graphite, 40, thorium, 10),
                quad, with(copper, 300, silicon, 80, metaglass, 80, titanium, 80, thorium, 20, phaseFabric, 10),
                oct, with(copper, 400, lead, 400, silicon, 120, graphite, 120, thorium, 40, plastanium, 40, phaseFabric, 5, surgeAlloy, 15),

                risso, with(copper, 20, lead, 10, metaglass, 3),
                minke, with(copper, 30, lead, 40, metaglass, 10),
                bryde, with(lead, 100, metaglass, 40, silicon, 40, titanium, 80, thorium, 10),
                sei, with(copper, 300, metaglass, 80, graphite, 80, titanium, 60, plastanium, 20, surgeAlloy, 5),
                omura, with(copper, 400, lead, 400, silicon, 100, metaglass, 120, graphite, 100, titanium, 120, thorium, 60, phaseFabric, 10, surgeAlloy, 10),

                retusa, with(copper, 8, lead, 2, scrap, 8),
                oxynoe, with(copper, 12, lead, 4, scrap, 16, silicon, 8, plastanium, 2),
                cyerce, with(lead, 23, metaglass, 27, scrap, 86, phaseFabric, 2, thorium, 4),
                aegires, with(silicon, 47, phaseFabric, 8, surgeAlloy, 4, plastanium, 18, thorium, 18),
                navanax, with(surgeAlloy, 50, phaseFabric, 50),

                alpha, with(copper, 30, lead, 30, silicon, 20, graphite, 20, metaglass, 20),
                beta, with(titanium, 40, thorium, 20),
                gamma, with(plastanium, 20, phaseFabric, 10, surgeAlloy, 10),

                stell, with(beryllium, 10, graphite, 7, silicon, 5),
                locus, with(beryllium, 20, graphite, 10, silicon, 15, tungsten, 15, oxide, 10),
                precept, with(beryllium, 20, graphite, 15, silicon, 20, tungsten, 20, oxide, 20, thorium, 15),
                vanquish, with(beryllium, 45, graphite, 25, silicon, 35, tungsten, 35, oxide, 25, thorium, 25, carbide, 10, surgeAlloy, 10),
                conquer, with(beryllium, 50, graphite, 40, silicon, 50, tungsten, 50, oxide, 45, thorium, 35, carbide, 20, surgeAlloy, 20),

                merui, with(beryllium, 5, graphite, 3, silicon, 2),
                cleroi, with(beryllium, 15, graphite, 7, silicon, 5, tungsten, 7, oxide, 7),
                anthicus, with(beryllium, 20, graphite, 15, silicon, 10, tungsten, 17, oxide, 12, thorium, 8),
                tecta, with(beryllium, 30, graphite, 20, silicon, 30, tungsten, 30, oxide, 25, thorium, 8, carbide, 7, surgeAlloy, 7),
                collaris, with(beryllium, 80, graphite, 60, silicon, 80, tungsten, 80, oxide, 50, thorium, 50, carbide, 30, surgeAlloy, 30),

                elude, with(beryllium, 15, graphite, 12, silicon, 8),
                avert, with(beryllium, 30, graphite, 15, silicon, 23, tungsten, 23, oxide, 10),
                obviate, with(beryllium, 45, graphite, 23, silicon, 30, tungsten, 30, oxide, 17, thorium, 23),
                quell, with(beryllium, 72, graphite, 40, silicon, 56, tungsten, 56, oxide, 25, thorium, 40, carbide, 16, surgeAlloy, 16),
                disrupt, with(beryllium, 55, graphite, 45, silicon, 55, tungsten, 55, oxide, 30, thorium, 40, carbide, 25, surgeAlloy, 25)
        );

        pathfinder = new TowerPathfinder();

        content.units().each(type -> {
            type.payloadCapacity = 0f;

            type.legSplashDamage = 0f;
            type.mineWalls = false;
            type.mineFloor = false;

            type.targetAir = false;
            type.targetGround = false;

            type.targetFlags = new BlockFlag[] {BlockFlag.core};
        });

        crawler.aiController = GroundAI::new;

        netServer.admins.addActionFilter(action -> {
            if (action.type == ActionType.placeBlock || action.type == ActionType.breakBlock) {
                if (!(proximityCheck(action.tile, action.block) || action.block instanceof ShockMine || action.block instanceof CoreBlock || action.block instanceof ConstructBlock)) {
                    Call.label(action.player.con, "[scarlet]\uE868", 4f, action.tile.drawx(), action.tile.drawy());
                    return false;
                }
            }

            if ((action.type == ActionType.depositItem || action.type == ActionType.withdrawItem) && action.tile.block() != null && action.tile.block() instanceof CoreBlock) {
                Call.label(action.player.con, "[scarlet]\uE868", 4f, action.tile.drawx(), action.tile.drawy());
                return false;
            }

            return true;
        });

        Events.run(Trigger.update, () -> Groups.unit.each(unit -> unit.team == state.rules.waveTeam, unit -> {
            var core = unit.closestEnemyCore();
            if (core == null || unit.dst(core) > 80f) return;

            float damage = unit.health / Mathf.sqrt(multiplier / 16);
            core.damage(damage, true);

            Call.effect(damage > 50000f ? massiveExplosion : damage > 20000f ? reactorExplosion : blastExplosion, core.x(), core.y(), Math.max(1f, damage / 500f), state.rules.waveTeam.color);
            unit.kill();
        }));

        Events.on(WorldLoadEvent.class, event -> multiplier = 1f);

        Events.on(WaveEvent.class, event -> multiplier = Mathf.clamp(((state.wave * state.wave / 3175f) + 0.5f), multiplier, 100f));

        Events.on(UnitDestroyEvent.class, event -> {
            if (event.unit.team != state.rules.waveTeam) return;

            var core = state.rules.defaultTeam.core();
            var stacks = drops.get(event.unit.type);

            if (core == null || stacks == null || state.gameOver) return;

            var builder = new StringBuilder();

            for (var stack : stacks) {
                int amount = Mathf.random(stack.amount - stack.amount / 2, stack.amount + stack.amount / 2);

                builder.append("[accent]+").append(amount).append(" [white]").append(getIcon(stack.item)).append("  ");

                Call.transferItemTo(event.unit, stack.item, core.tile.build.acceptStack(stack.item, amount, core), event.unit.x + Mathf.range(8f), event.unit.y + Mathf.range(8f), core);
            }

            Call.label(builder.toString(), 1f, event.unit.x + Mathf.range(4f), event.unit.y + Mathf.range(4f));
        });

        Events.on(UnitSpawnEvent.class, event -> {
            if (event.unit.team() == state.rules.waveTeam) {
                event.unit.maxHealth = event.unit.maxHealth * multiplier;
                event.unit.health = event.unit.maxHealth;
                event.unit.damageMultiplier = 0f;
                event.unit.apply(disarmed, Float.MAX_VALUE);
            }
        });

        Timer.schedule(() -> Groups.player.each(player -> Call.infoPopup(player.con, Strings.format("[yellow]\uE86D[accent] Units health multiplier: @@x", trafficLightColor(multiplier), Strings.autoFixed(multiplier, 3)), 1f, 20, 50, 20, 450, 0)), 0f, 1f);
    }
}