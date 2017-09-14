package org.black_ixx.playerpoints;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

/**
 * Created by SkySslience on 2016.12.2.
 */
public class BungeeUtil {

    public static void sendMessage(Player player){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        player.sendPluginMessage(PlayerPoints.instance, "BungeeCord", out.toByteArray());
    }
}
