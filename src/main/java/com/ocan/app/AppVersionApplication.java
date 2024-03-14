package com.ocan.app;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ServletComponentScan
public class AppVersionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppVersionApplication.class, args);
    }

    @Bean
    public MybatisPlusInterceptor plusInterceptor() {
        //mybatisPlusInterceptor相当于一个插件集合，把要使用的插件放入该集合就可以了
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        /*
        加入分页插件，指定使用的数据库类型，使用DbType枚举，该类型的包是mybaits-plus中的，不是
        druid中的
         */
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return mybatisPlusInterceptor;
    }





}
