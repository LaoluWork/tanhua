package com.tanhua.dubbo;

import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.dubbo.api.UserInfoApiImpl;
import com.tanhua.model.domain.UserInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-12-29 15:03
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DubboDBApplication.class)
public class WrapperTest {

    @Autowired
    private UserInfoApi api;

    @Test
    public void test01() {
        ArrayList<Long> list = new ArrayList<>();
        list.add(106L);
        Map<Long, UserInfo> map = api.findByIds(list, null);
        map.forEach((id, info) -> System.out.println(id + "--" + info));
    }
}
