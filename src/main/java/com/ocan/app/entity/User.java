package com.ocan.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(autoResultMap = true)
public class User {

    //用户id
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    //姓名
    private String name;
    //用户名
    private String username;
    //密码
    private String password;
    //md5密码盐
    private String salt;
    //手机号
    private String phone;
    //性别
    private String sex;
    //身份证号
    private String idNumber;
    //状态
    private Integer status;
}
