package org.black_ixx.playerpoints;

import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerPointPlaceholder extends EZPlaceholderHook {

    public PlayerPointPlaceholder(Plugin plugin) {
        super(plugin, "myplayerpoint");
    }

    @Override
    public String onPlaceholderRequest(Player player, String s) {
        if(s.contains("time")){
            String[] strings = s.split("_");
            String begin = strings[1];
            String end = strings[2];
            for(String sss : strings) {
                System.out.print(sss);
            }
            int i = PlayerPoints.getConsuM().getPlayerConsumen(player , begin , end , strings[3]);
            return i + "";
        }
        return "0";
    }
}
