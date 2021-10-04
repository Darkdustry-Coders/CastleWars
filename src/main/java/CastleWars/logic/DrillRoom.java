package CastleWars.logic;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.game.Team;

public class DrillRoom extends TurretRoom {

    public DrillRoom(Team team, int x, int y) { super(team, Blocks.laserDrill, x, y, 1000, 4); }

    @Override
    public void update() {
        if (bought && interval.get(0, 240f) && team.core() != null) {
            team.core().items.add(Items.thorium, 24);
        }
    }
}