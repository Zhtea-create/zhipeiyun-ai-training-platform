package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    private Integer code; // 1:成功 0:失败
    private String msg;   // 提示信息
    private Object data;  // 返回数据

    // 成功（无数据）
    public static Result success() {
        return new Result(1, "success", null);
    }

    // 成功（携带数据）
    public static Result success(Object data) {
        return new Result(1, "success", data);
    }

    // 失败
    public static Result error(String msg) {
        return new Result(0, msg, null);
    }
}
