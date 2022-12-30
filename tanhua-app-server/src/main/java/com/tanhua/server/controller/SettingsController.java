package com.tanhua.server.controller;

import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.SettingsVo;
import com.tanhua.server.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-28 11:08
 */
@RestController
@RequestMapping("/users")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    /**
     * 查询通用设置
     */
    @GetMapping("/settings")
    public ResponseEntity settings(){
       SettingsVo vo = settingsService.settings();
       return ResponseEntity.ok(vo);
    }

    /**
     * 设置陌生人问题
     */
    @PostMapping("/questions")
    public ResponseEntity questions(@RequestBody Map map) {
        //获取参数
        String content = (String)map.get("content");
        settingsService.saveQuestion(content);
        return ResponseEntity.ok(null);
    }
    /**
     * 设置通知设置
     */
    @PostMapping("/notifications/setting")
    public ResponseEntity notifications(@RequestBody Map map) {
        //获取参数
        settingsService.saveSettings(map);
        return ResponseEntity.ok(null);
    }
    /**
     * 分页查询黑名单列表
     */
    @GetMapping("/blacklist")
    public ResponseEntity blacklist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){
        //1.调用service查询
        PageResult pr = settingsService.blacklist(page, size);
        //2.构造返回
        return ResponseEntity.ok(pr);
    }

    /**
     * 取消黑名单
     */
    @DeleteMapping("/blacklist/{uid}")
    public ResponseEntity deleteBlackList(@PathVariable("uid") Long blackUserId) {
        settingsService.deleteBlackList(blackUserId);
        return ResponseEntity.ok(null);
    }



    /**
     * 修改绑定手机号第一步：发送验证码
     */
    @PostMapping("/phone/sendVerificationCode")
    public ResponseEntity sendVerificationCode(){
        settingsService.sendCode();
        return ResponseEntity.ok(null);
    }


    /**
     * 修改绑定手机号第二步：校验验证码
     */
    @PostMapping("/phone/checkVerificationCode")
    public ResponseEntity checkVerificationCode(@RequestBody Map map) {

        String verificationCode = (String) map.get("verificationCode");
        boolean isCheck = settingsService.checkCode(verificationCode);


        Map res = new HashMap();
        res.put("verification", isCheck);

        return ResponseEntity.ok(res);

    }


    /**
     * 修改绑定手机号第三步：保存新手机号
     */
    @PostMapping("/phone")
    public ResponseEntity savePhone(@RequestBody Map map) {

        String phone = (String) map.get("phone");
        settingsService.saveNewPhone(phone);

        return ResponseEntity.ok(null);

    }

}
