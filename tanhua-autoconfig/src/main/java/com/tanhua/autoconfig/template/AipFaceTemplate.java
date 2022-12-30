package com.tanhua.autoconfig.template;

import com.baidu.aip.face.AipFace;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-14 11:42
 */
public class AipFaceTemplate {
    @Autowired
    private AipFace client;

    /**
     * 检测图片中是否含有人脸，返回true包含，false不包含
     * @param imageUrl
     * @return
     */
    public boolean detect(String imageUrl) {
        // 调用接口
        String imageType = "URL";

        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("face_field", "age");
        options.put("max_face_num", "2");
        options.put("face_type", "LIVE");
        options.put("liveness_control", "LOW");

        // 人脸检测
        JSONObject res = client.detect(imageUrl, imageType, options);
        System.out.println(res.toString(2));

        Integer errorCode = (Integer) res.get("error_code");

        return errorCode == 0;
    }
}
