package com.ocan.app.dto;

import lombok.Data;

@Data
public class SystemLogin {
    //用户名
    private String username;
    //密码
    private String password;
    //验证码
    private String smsCode;
    //验证码校验码Key
    private String Key;
}
