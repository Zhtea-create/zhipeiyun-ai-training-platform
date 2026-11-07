package com.itheima.utils;

/**
 * 当前请求的登录用户上下文。
 * TokenInterceptor 在 preHandle 中写入，在 afterCompletion 中清理，
 * 供操作日志切面等组件获取"谁在执行操作"。
 */
public class UserContext {

    public record CurrentUser(Integer id, String name) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(Integer id, String name) {
        HOLDER.set(new CurrentUser(id, name));
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
