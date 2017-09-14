package org.black_ixx.playerpoints;

import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerPointPlaceholder extends EZPlaceholderHook {

    public PlayerPointPlaceholder(Plugin plugin) {
        super(plugin, "playerpoint");
    }

    @Override
    public String onPlaceholderRequest(Player player, String s) {
        if(s.contains("time")){
            String[] strings = s.split("_");
            String begin = strings[2];
            System.out.print(begin + "开始时间");
            String end = strings[3];
            System.out.print(begin + "结束时间");
            int i = PlayerPoints.getConsuM().getPlayerConsumen(player , begin , end);
            return i + "";
        }
        return "0";
    }
}
