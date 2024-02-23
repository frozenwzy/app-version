package com.ocan.app.vo;

import com.ocan.app.entity.App;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true) //用于在生成 equals 和 hashCode 方法时是否包含父类的字段
@Data
public class AppVO extends App {

    //磁盘路径
    private String file;
    //文件名
    private String name;
    //文件大小
    private Long size;
    //MD5值
    private String md5;
    //文件类型
    private String type;
    //下载后安装的目标位置
    private String dist;
    //判断是否是主文件
    private String isMain;
    //app的名字
    private String appName;
    //是否是最后一个文件
    private String isLast;

}







