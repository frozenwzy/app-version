package com.ocan.app.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocan.app.entity.App;
import com.ocan.app.mode.Result;
import com.ocan.app.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RestController //这个注解可以将一个Java类标记为控制器，使其能够处理HTTP请求，并将方法的返回值直接写入HTTP响应体中
@RequestMapping("appVersion")
@Slf4j
public class AppController {

    @Autowired //警告说是不建议现在注入
    private AppService appService;


    //增加操作
    @PostMapping("add")
    public Result<?> add(HttpServletRequest request, MultipartFile pictureFile) {

        //使用日志记录器打印消息，表示开始添加app版本信息。
        log.info("开始添加app版本信息");

        //获取请求体中的数据
        Map<String, Object> map = AppUpdateController.parameterToMap(request);

        //把JSON数据映射到实体类
        App app = JSONObject.parseObject(JSONObject.toJSONString(map), App.class);
        //把图片保存到磁盘上，获取图片的存储路径
        app.setIcon(AppUpdateController.transformToPicture(pictureFile));


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
    public Result<?> edit(HttpServletRequest request) {

        //使用日志记录器打印消息，表示开始修改app版本信息。
        log.info("开始修改app版本信息");

        //获取请求体中的数据
        Map<String, Object> map = AppUpdateController.parameterToMap(request);

        //把JSON数据映射到实体类
        App app = JSONObject.parseObject(JSONObject.toJSONString(map), App.class);

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
        List<App> apps = modifyFiles(appList);
        appList.setRecords(apps);

        return Result.OK("查询成功", appList);
    }


    //添加新版本
    @PostMapping("addNewVersion")
    public Result<?> addNewVersion(HttpServletRequest request) {

        //获取请求体中的数据
        Map<String, Object> map = AppUpdateController.parameterToMap(request);

        //把JSON数据映射到实体类
        App app = JSONObject.parseObject(JSONObject.toJSONString(map), App.class);

        //设置size值
        app.setSize(totalSize(app.getFiles()));

        //生成信息的记录
        boolean whetherSuccess = appService.save(app);
        if (whetherSuccess) {
            return Result.ok("添加成功！");
        } else {
            return Result.error("添加失败！请联系软件开发工程师。");
        }
    }


    //通过app的id获取到文件属性
    @GetMapping("getById")
    public Result<?> getById(@RequestParam Long id) {
        App app = appService.getById(id);
        return Result.OK(app.getFiles());
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


    //修改查询返回的数据
    private List<App> modifyFiles(IPage<App> appIPage) {

        List<App> records = appIPage.getRecords();
        Set<byte[]> set = new HashSet<>();

        for (App app : records) {
            //给app的fileSet赋值
            app.setFileSet(getFileData(app));
        }

        return records;
    }


    //返回App的fileSet属性的值
    private Set<byte[]> getFileData(App app) {

        Set<byte[]> set = new HashSet<>();
        //获取JSON数组
        JSONArray files = app.getFiles();

        //遍历JSON数组
        for (int i = 0; i < files.size(); i++) {
            //获取文件的路径
            String filePath = files.getJSONObject(i).getString("file");
            try {
                //获取文件的字符格式
                byte[] fileData = FileUtils.readFileToByteArray(new File(filePath));
                set.add(fileData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return set;
    }


}
