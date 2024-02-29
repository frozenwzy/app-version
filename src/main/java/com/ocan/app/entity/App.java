package com.ocan.app.entity;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Set;

@Data
//当设置 autoResultMap 为 true 时，MyBatis Plus 会自动生成结果映射，
// 省去了手动书写 resultMap 的步骤，

@TableName(autoResultMap = true)
public class App {

    @JsonSerialize(using = ToStringSerializer.class) //把long类型转为字符串类型
    private Long id;
    //名称
    private String name;
    //平台
    private String platform;
    //构造编号
    private Integer build;
    //版本号
    private String version;
    //更新说明
    private String releaseInfo;
    //发布时间
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss") //指定Date对象序列化为JSON对象时的日期格式
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") //指定接受到的JSON数据转化为Date对象时的日期格式
    private Date releaseTime;
    //全部文件大小
    private Long size;
    //应用文件
    @TableField(value = "files", typeHandler = JacksonTypeHandler.class)
    private JSONArray files;
    //图标路径
    private String icon;
    //应用编码
    private String appCode;
    //应用所有者
    private String appOwner;

    //存放文件
    @TableField(exist = false)
    private Set<byte[]> fileSet;

    //存放图片
    @TableField(exist = false)
    private byte[] pictureSet;

}