package castle

import arc.func.Boolf
import arc.util.Time
import castle.Main.Companion.rooms
import castle.Main.Companion.playerTasks
import mindustry.Vars
import mindustry.Vars.tilesize

class HoldTask(val player: mindustry.gen.Player, val timeStart: Long) : Runnable {
    @Volatile var cancelled = false

    override fun run() {
        if (cancelled || player.unit() == null || !player.unit().isShooting || !player.con.isConnected) {
            playerTasks.remove(player.uuid())
            return
        }
        if (Time.millis() - timeStart < 2500) {
            Time.runTask(5f, this)
            return
        }
        val shootX = player.mouseX.toInt() / tilesize
        val shootY = player.mouseY.toInt() / tilesize
        val data = PlayerData.of(player)
        if (shootX < 0 || shootY < 0 || shootX >= Vars.world.width() || shootY >= Vars.world.height()) return
        val tile = Vars.world.tile(shootX, shootY)
        rooms.each(
            { room -> room.check(tile) && room.canBuy(data) }
        ) { room: CastleRooms.Room -> room.buy(data) }
        Time.runTask(0.03f, this)
    }
}