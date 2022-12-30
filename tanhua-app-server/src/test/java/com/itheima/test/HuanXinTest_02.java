package com.itheima.test;


import cn.hutool.core.collection.CollUtil;
import com.easemob.im.server.EMProperties;
import com.easemob.im.server.EMService;
import com.easemob.im.server.model.EMTextMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;


/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-03 20:54
 */


public class HuanXinTest_02 {

    private EMService service;

    @Before
    public void init(){
        EMProperties emProperties = EMProperties.builder()
                .setAppkey("1173220903163308#demo")
                .setClientId("YXA6vqqPGFa7RtKui3jh0K_GOg")
                .setClientSecret("YXA6ivCIHlQoVNvlT11seNhojrRFU8s")
                .build();
        service = new EMService(emProperties);
    }

    @Test
    public void test01() {
        service.user().create("test01", "123456").block();
        service.user().create("test02", "123456").block();

        service.contact().add("test01", "test02").block();

        //接收人用户列表
        Set<String> set = CollUtil.newHashSet("test02");
        //文本消息
        EMTextMessage message = new EMTextMessage().text("java美丽的");
        //发送消息  from：admin是管理员发送
        service.message().send("test01","users",
                set,message,null).block();

    }
}
