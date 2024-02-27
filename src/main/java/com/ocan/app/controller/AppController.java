package com.ocan.app.controller;


import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocan.app.entity.App;
import com.ocan.app.mode.Result;
import com.ocan.app.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController //这个注解可以将一个Java类标记为控制器，使其能够处理HTTP请求，并将方法的返回值直接写入HTTP响应体中
@RequestMapping("appVersion")
@Slf4j
public class AppController {

    @Autowired //警告说是不建议现在注入
    private AppService appService;


    //增加操作
    @PostMapping("add")
    public Result<?> add(@RequestBody App app) {

        //使用日志记录器打印消息，表示开始添加app版本信息。
        log.info("开始添加app版本信息");

//        //获取请求体中的数据
//        Map<String, String> map = AppUpdateController.parameterToMap(request);
//        //把JSON数据映射到实体类
//        App app = JSONObject.parseObject(JSONObject.toJSONString(map), App.class);

        //判断添加的应用是否已存在
        if (isAppExist("name", app.getName())) {
            return Result.error("添加失败，该应用已存在!");
        } else if (isAppExist("app_code", app.getAppCode())) {
            return Result.error("添加失败，该应用的应用编码已存在！");
        }

        //设置size值大小
        app.setSize(totalSize(app.getFiles()));

        boolean whetherSuccess = appService.save(app);
        if (whetherSuccess) {
            return Result.ok("添加成功！");
        } else {
            return Result.error("添加失败！未知错误。");
        }
    }


    //删除操作
    @GetMapping("delete")
    public Result<String> delete(@RequestParam("id") Long id) {

        boolean whetherSuccess = appService.removeById(id);
        if (whetherSuccess) {
            return Result.ok("删除成功！");
        } else {
            return Result.error("删除失败，要删除的id不存在！");
        }
    }


    //批量删除操作
    @GetMapping("deleteBatch")
    public Result<String> deleteBatch(@RequestParam("list") List<Long> list) {

        boolean whetherSuccess = appService.removeByIds(list);
        if (whetherSuccess) {
            return Result.ok("批量删除成功！");
        } else {
            return Result.error("批量删除失败，要删除的id不存在！");
        }
    }


    //修改操作
    @PostMapping("edit")
    public Result<?> edit(@RequestBody App app) {

        //使用日志记录器打印消息，表示开始修改app版本信息。
        log.info("开始修改app版本信息");

        //设置size值大小
        app.setSize(totalSize(app.getFiles()));

        boolean whetherSuccess = appService.updateById(app);
        if (whetherSuccess) {
            return Result.ok("修改成功！");
        } else {
            return Result.error("修改失败！");
        }
    }


    //查询操作
    @GetMapping("list")
    public Result<?> list(@RequestParam(name = "current", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "size", defaultValue = "10") Integer pageSize,
                          HttpServletRequest request) {

        if (pageNo < 0) {
            pageNo = 1;
        }
        if (pageSize < 0 || pageSize > 200) {
            pageSize = 10;
        }
        Page<App> page = new Page<>(pageNo, pageSize);

        String platform = request.getParameter("platform");
        String name = request.getParameter("name");
        LambdaQueryWrapper<App> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        //构造查询的条件
        lambdaQueryWrapper.eq(!StringUtils.isEmpty(platform), App::getPlatform, platform);
        lambdaQueryWrapper.like(!StringUtils.isEmpty(name), App::getName, name);

        IPage<App> appList = appService.page(page, lambdaQueryWrapper);

        return Result.OK("查询成功", appList);
    }


    //添加新版本
    @PostMapping("addNewVersion")
    public Result<?> addNewVersion(@RequestBody App app) {

        //设置size值
        app.setSize(totalSize(app.getFiles()));

        //生成信息的记录
        boolean whetherSuccess = appService.save(app);
        if (whetherSuccess) {
            return Result.ok("添加成功！");
        } else {
            return Result.error("添加失败！未知错误。");
        }
    }


    //判断数据库中是否已经存在要添加的数据
    private boolean isAppExist(String column, Object val) {

        //拼接SQL语句
        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(column, val);

        //查询
        App one = appService.getOne(queryWrapper);

        return null != one;

    }


    //计算文件的总大小
    private Long totalSize(JSONArray jsonArray) {
        //临时保存文件总大小
        long sum = 0;

        //循环获取JSON数组中键为size的值
        for (int i = 0; i < jsonArray.size(); i++) {
            sum += jsonArray.getJSONObject(i).getLong("size");
        }

        return sum;
    }

}
