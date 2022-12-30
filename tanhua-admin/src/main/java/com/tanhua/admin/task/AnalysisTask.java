package com.tanhua.admin.task;

import com.tanhua.admin.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-16 14:20
 */
@Component
public class AnalysisTask {

    @Autowired
    private AnalysisService analysisService;

    // 每5分钟自动统计一次tb_log表
    @Scheduled(cron = "0 0/5 * * * ?")
    public void analysis(){
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("当前时间是" + time);

        try {
            analysisService.analysis();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
