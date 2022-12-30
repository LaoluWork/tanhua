package com.tanhua.server.interceptor;

import com.tanhua.model.domain.User;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-17 15:29
 */
public class UserHolder {
    private static ThreadLocal<User> tl = new ThreadLocal<>();

    // 将用户对象存入ThreadLocal
    public static void set(User user) {
        tl.set(user);
    }

    // 从当前线程获取用户对象
    public static User get(){
        return tl.get();
    }

    // 从当前线程获取对象的id
    public static Long getUserId() {
        return  tl.get().getId();
    }

    // 从当前线程获取用户对象的手机号码
    public static String getMobile() {
        return tl.get().getMobile();
    }

    // 将当前线程的user对象删除
    public static void remove() {
        tl.remove();
    }
}
