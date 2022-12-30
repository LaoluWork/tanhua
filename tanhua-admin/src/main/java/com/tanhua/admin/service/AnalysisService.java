package com.tanhua.admin.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.admin.mapper.AnalysisMapper;
import com.tanhua.admin.mapper.LogMapper;
import com.tanhua.model.domain.Analysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-16 14:35
 */
@Service
public class AnalysisService {

    @Autowired
    private AnalysisMapper analysisMapper;

    @Autowired
    private LogMapper logMapper;

    /**
     * 定时统计tb_log表中的数据，保存或更新tb_analysis表
     * 1、查询tb_log表数据（当天注册人数，登录人数， 活跃用户数， 次日留存）
     * 2、构造analysis对象
     * 3、保存或更新
     */
    public void analysis() throws ParseException {
        //1.定义出当前日期
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String yesterdayStr = DateUtil.yesterday().toString();
        //2.查询tb_log中的数据--注册数量，登录数量， 活跃数量， 次日留存数量
        Integer registerCount = logMapper.queryByTypeAndLogTime("0102", todayStr);
        Integer loginCount = logMapper.queryByTypeAndLogTime("0101", todayStr);
        Integer activeCount = logMapper.queryByLogTime(todayStr);
        Integer numRetention1d = logMapper.queryNumRetention1d(todayStr, yesterdayStr);
        //3.查询tb_analysis表中的当天数据
        QueryWrapper<Analysis> qw = new QueryWrapper<>();
        qw.eq("record_date", new SimpleDateFormat("yyyy-MM-dd").parse(todayStr));
        Analysis analysis = analysisMapper.selectOne(qw);
        //4.如果存在就更新，否则就保存
        if (analysis != null) {
            analysis.setNumActive(activeCount);
            analysis.setNumLogin(loginCount);
            analysis.setNumRegistered(registerCount);
            analysis.setNumRetention1d(numRetention1d);
            analysisMapper.updateById(analysis);
        }else {
            analysis = new Analysis();
            analysis.setNumLogin(loginCount);
            analysis.setNumRegistered(registerCount);
            analysis.setNumRetention1d(numRetention1d);
            analysis.setNumActive(activeCount);
            analysis.setRecordDate(new SimpleDateFormat("yyyy-MM-dd").parse(todayStr));
            analysis.setCreated(new Date());
            analysisMapper.insert(analysis);
        }

    }
}
