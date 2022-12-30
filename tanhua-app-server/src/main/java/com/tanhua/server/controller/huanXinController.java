package com.tanhua.server.controller;

import com.tanhua.model.vo.HuanXinUserVo;
import com.tanhua.server.service.HuanXinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 14:55
 */
@RestController
@RequestMapping("/huanxin")
public class huanXinController {

    @Autowired
    private HuanXinService huanXinService;

    /**
     * 查询环信的账号和密码
     */
    @GetMapping("/user")
    public ResponseEntity user() {
        HuanXinUserVo vo = huanXinService.findHuanXinUser();
        return ResponseEntity.ok(vo);
    }
}
