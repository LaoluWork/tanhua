package com.tanhua.server.controller;

import com.tanhua.model.domain.UserInfo;
import com.tanhua.server.interceptor.UserHolder;
import com.tanhua.server.service.UserInfoService;
import com.tanhua.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-15 9:53
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserService userService;

    /**
     * 获取登录验证码
     * 请求参数：phone （Map）
     * 响应：void
     * ResponseEntity
     */
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody Map map) {
        String phone = (String) map.get("phone");
        userService.sendMsg(phone);
        //正常返回状态码200
        return ResponseEntity.ok(null);
    }

    /**
     * 检验登录
     */
    @PostMapping("/loginVerification")
    public ResponseEntity loginVerification(@RequestBody Map map) {
        //1、调用map集合获取请求参数
        String phone = (String) map.get("phone");
        String code = (String) map.get("verificationCode");
        //2、调用userService完成用户登录
        Map retMap = userService.loginVerification(phone, code);
        //3、构造返回
        return ResponseEntity.ok(retMap);

    }

    /**
     * 保存用户info表
     */
    @PostMapping("/loginReginfo")
    public ResponseEntity loginReginfo(@RequestBody UserInfo userInfo) {

        //1.向userinfo中设置用户id
        userInfo.setId(UserHolder.getUserId());
        //2.调用service
        userInfoService.save(userInfo);
        return ResponseEntity.ok(null);
    }

    /**
     * 上传头像
     */
    @PostMapping("/loginReginfo/head")
    public ResponseEntity head(MultipartFile headPhoto) throws IOException {
        userInfoService.updateHead(headPhoto, UserHolder.getUserId());
        return ResponseEntity.ok(null);
    }

}
