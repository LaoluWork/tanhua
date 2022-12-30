package com.tanhua.dubbo.api;

import com.tanhua.model.domain.Question;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-28 11:02
 */
public interface QuestionApi {
    Question findByUserId(Long userId);

    void save(Question question);

    void update(Question question);

}
