package org.black_ixx.playerpoints.listeners;

import org.black_ixx.playerpoints.BungeeUtil;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Created by SkySslience on 2016.12.15.
 */
public class PlayerJoin implements Listener{
    public PlayerJoin(PlayerPoints playerPoints) {

    }

    @EventHandler(priority = EventPriority.LOW)
    public void playerJoin(PlayerJoinEvent event){
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(PlayerPoints.instance, new Runnable() {
            @Override
            public void run() {
                if(PlayerPoints.getServerName() == null || PlayerPoints.getServerName().equals("")){
                    BungeeUtil.sendMessage(player);
                }
            }
        },20l);
    }
}
