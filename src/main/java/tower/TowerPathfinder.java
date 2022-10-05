package tower;

import mindustry.ai.Pathfinder;
import mindustry.gen.PathTile;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.state;
import static mindustry.content.Blocks.darkPanel4;
import static tower.Main.isPath;

public class TowerPathfinder extends Pathfinder {

    @Override
    public int packTile(Tile tile) {
        boolean nearLiquid = false, nearSolid = false, nearGround = false, solid = tile.solid(), allDeep = tile.floor().isDeep(), isPath = isPath(tile) || tile.floor().isLiquid;

        for (int i = 0; i < 4; i++) {
            var other = tile.nearby(i);
            if (other == null) continue;

            if (other.floor().isLiquid) nearLiquid = true;
            if (other.solid() && !other.block().teamPassable || other.floor() == darkPanel4) nearSolid = true;
            if (!other.floor().isLiquid) nearGround = true;
            if (!other.floor().isDeep()) allDeep = false;
        }

        return PathTile.get(
                tile.build == null || !solid || tile.block() instanceof CoreBlock ? 0 : Math.min((int) (tile.build.health / 40), 80),
                tile.getTeamID() == 0 && tile.build != null && state.rules.coreCapture ? 255 : tile.getTeamID(),
                !isPath || solid,
                tile.floor().isLiquid,
                !isPath || tile.staticDarkness() > 1,
                nearLiquid,
                nearGround,
                nearSolid,
                tile.floor().isDeep(),
                tile.floor().damageTaken > 0f,
                allDeep,
                tile.block().teamPassable
        );
    }
}