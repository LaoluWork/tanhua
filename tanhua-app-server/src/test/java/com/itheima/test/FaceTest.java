package com.itheima.test;

import com.baidu.aip.face.AipFace;
import com.tanhua.autoconfig.template.AipFaceTemplate;
import com.tanhua.server.AppServerApplication;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-14 11:22
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AppServerApplication.class)
public class FaceTest {

    @Autowired
    private AipFaceTemplate aipFaceTemplate;

    @Test
    public void testAiPFaceTemplate() {
        boolean detect = aipFaceTemplate.detect("https://tan--hua001.oss-cn-guangzhou.aliyuncs.com/2022/08/14/8c606cc3-a813-4024-8eb7-626695238f41.jpg");
        System.out.println(detect);
    }
    //设置APPID/AK/SK
    public static final String APP_ID = "...";
    public static final String API_KEY = "...";
    public static final String SECRET_KEY = "...";

    public static void main(String[] args) {
        // 初始化一个AipFace
        AipFace client = new AipFace(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);


        // 调用接口
        String image = "https://tan--hua001.oss-cn-guangzhou.aliyuncs.com/2022/08/14/8c606cc3-a813-4024-8eb7-626695238f41.jpg";
        String imageType = "URL";

        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("face_field", "age");
        options.put("max_face_num", "2");
        options.put("face_type", "LIVE");
        options.put("liveness_control", "LOW");

        // 人脸检测
        JSONObject res = client.detect(image, imageType, options);
        System.out.println(res.toString(2));

    }
}
