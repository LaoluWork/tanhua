package com.tanhua.server.exception;

import com.tanhua.model.vo.ErrorResult;
import lombok.Data;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-20 15:04
 */
@Data
public class BusinessException extends RuntimeException{

    static final long serialVersionUID = -70348971932469390L;//提供一个和Exception源码差不多数量级的数

    private ErrorResult errorResult;

    public BusinessException(ErrorResult errorResult) {
        super(errorResult.getErrMessage());
        this.errorResult = errorResult;
    }
}
