package com.tanhua.model.vo;

import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.mongo.UserLike;
import com.tanhua.model.mongo.Visitors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-10 13:57
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorsInfoVo {

    private Long id; //用户id
    private String avatar;
    private String nickname;
    private String gender; //性别 man woman
    private Integer age;
    private String city;
    private String education;
    private Integer marriage;
    private Long matchRate; //缘分值
    private Boolean alreadyLove;

    public static VisitorsInfoVo init(UserInfo userInfo, Visitors visitors, UserLike userLike) {
        VisitorsInfoVo vo = new VisitorsInfoVo();
        BeanUtils.copyProperties(userInfo,vo);
        vo.setMatchRate(visitors.getScore().longValue());
        vo.setAlreadyLove(userLike!=null && userLike.getIsLike());
        return vo;
    }
}
