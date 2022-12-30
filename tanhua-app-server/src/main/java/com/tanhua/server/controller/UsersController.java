package com.tanhua.server.controller;

import com.aliyun.oss.internal.ResponseParsers;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.UserInfoVo;
import com.tanhua.model.vo.VisitorsInfoVo;
import com.tanhua.server.interceptor.UserHolder;
import com.tanhua.server.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-17 13:39
 */
@RestController
@RequestMapping("/users")
public class UsersController {
    @Autowired
    private UserInfoService userInfoService;

    /**
     * 查询用户资料
     * 1.请求头： token
     * 2.请求参数： userID
     */
    @GetMapping
    public ResponseEntity users(Long userID) {

        //判断userID
        if (userID == null) {
            userID = UserHolder.getUserId();
        }
        //查询结果
        UserInfoVo userInfoVo = userInfoService.findById(userID);
        return ResponseEntity.ok(userInfoVo);
    }

    /**
     * 更新用户资料
     */
    @PutMapping
    public ResponseEntity updateUserInfo(@RequestBody UserInfo userinfo) {

        //3.设置用户id
        userinfo.setId(UserHolder.getUserId());
        //4.更新数据库资料
        userInfoService.update(userinfo);
        return ResponseEntity.ok(null);
    }

    /**
     * 修改头像
     */
    @PostMapping("/header")
    public ResponseEntity updateUserInfoHead(MultipartFile headPhoto) throws IOException {
        userInfoService.updateHead(headPhoto, UserHolder.getUserId());
        return ResponseEntity.ok(null);
    }

    /**
     * 查询访客的详情列表
     */
    @GetMapping("/friends/4")
    public ResponseEntity queryVisitorsInfo(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pagesize) {
        PageResult pr = userInfoService.queryVisitorsInfo(page, pagesize);

        return ResponseEntity.ok(pr);

    }
}
