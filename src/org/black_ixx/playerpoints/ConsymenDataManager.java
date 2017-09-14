package org.black_ixx.playerpoints;

import org.bukkit.entity.Player;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsymenDataManager {

    public Map<Integer , ConsumeData> consumeDatas = new ConcurrentHashMap<Integer, ConsumeData>();
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public int getPlayerConsumen(Player player , String beginTime , String endTime){
        int point = 0;
        for(ConsumeData consumeData : consumeDatas.values()){
            List<ConsumeData> consumeDataList = new ArrayList<ConsumeData>();
            if(consumeData.getPlayerName().equalsIgnoreCase(player.getName())){
                consumeDataList.add(consumeData);
            }
            for(ConsumeData consume : consumeDataList){
                try {
                    Date begindate = df.parse(beginTime);
                    Date enddate = df.parse(endTime);
                    Date date = df.parse(consume.getTime());
                    if(date.getTime() >= enddate.getTime() && date.getTime() <= begindate.getTime()){
                        point = point + consume.getPoint();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return point;
    }
}
