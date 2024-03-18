package com.ocan.app.controller;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocan.app.entity.App;
import com.ocan.app.mapper.AppMapper;
import com.ocan.app.mode.Result;
import com.ocan.app.service.AppService;
import com.ocan.app.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("appUpdate")
public class AppUpdateController {

    @Autowired
    private AppService appService;

    @Autowired
    private AppMapper appMapper;

    //获取文件的存储根路径
    @Value("${file.uploadFolder}")
    private  String rootPath;

    //临时存放文件的MD5
    private Set<String> md5List = new HashSet<>();


    //单文件上传
    @PostMapping("upload")
    public Result<?> upload(MultipartFile file, HttpServletRequest request) throws IOException {

        //判断是否接收到文件
        if (null == file) {
            return Result.error("上传失败，文件为空！");
        }

        //获取请求体中的参数
        Map<String, Object> map = parameterToMap(request);

        boolean isMain = Boolean.parseBoolean(map.get("isMain").toString());

        //上传文件
        JSONObject jsonObject = transformToJSON(file, isMain);

        return Result.OK("上传成功！", jsonObject);

    }


    //检查新版本
    @GetMapping("checkNewVersion")
    public Result<?> checkNewVersion(@RequestParam String appCode, @RequestParam String platform, @RequestParam String appOwner) {

        //拼接所需的SQL语句
        Page<App> appPage = new Page<>(0,1);

        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_Code", appCode).eq("platform", platform).
                eq("app_Owner", appOwner).orderByDesc("version");

        //获取查询结果
        Page<App> page = appService.page(appPage, queryWrapper);
        List<App> appList = page.getRecords();

        if (appList.isEmpty()) {
            return Result.error("检查失败！所查询的软件不存在。");
        }

        App one = appList.get(0);

        //装载查询结果的值
        Map<String, Object> map = new HashMap<>();
        map.put("id", one.getId());
        map.put("build", one.getBuild());
        map.put("version", one.getVersion());
        map.put("releaseInfo", one.getReleaseInfo());
        map.put("mainFileUrl", getMainFileUrl(one));

        return Result.OK("检查成功！", map);
    }


    //下载主文件
    @GetMapping("downloadMainVersionFile")
    public void downloadMainVersionFile(@RequestParam Long id, @RequestParam String transformInfo, HttpServletResponse response) throws IOException {
        App app = appService.getById(id);
        JSONArray files = app.getFiles();
        //主文件的下载路径
        String filePath = "";
        JSONObject jsonObject = new JSONObject();
        //设置下载文件的大小
        long size = 0;

        for (int i = 0; i < files.size(); i++) {
            if (files.getJSONObject(i).getBoolean("isMain")) {
                //获取JSON对象
                jsonObject = files.getJSONObject(i);
                //返回文件下载地址
                filePath = jsonObject.getString("file");
                //设置文件下载大小
                size = jsonObject.getLong("size");
                break;
            }
        }

        //设置文件的下载名
        String name = jsonObject.getString("name");
        //传输文件
        transferFile(response, name, filePath, size);
    }


    //下载指定文件
    @GetMapping("downloadVersionFile")
    public void downloadApp(@RequestParam Long id, @RequestParam String md5, HttpServletResponse response) throws IOException {
        App app = appService.getById(id);
        JSONArray files = app.getFiles();
        //指定文件的下载路径
        String filePath = "";
        JSONObject jsonObject = new JSONObject();
        //设置下载文件的大小
        long size = 0;

        for (int i = 0; i < files.size(); i++) {
            if ( md5.equals( files.getJSONObject(i).get("md5") ) ) {
                //获取JSON对象
                jsonObject = files.getJSONObject(i);
                //返回文件下载地址
                filePath = rootPath + jsonObject.getString("file");
                //设置文件下载大小
                size = jsonObject.getLong("size");
                break;
            }
        }

        //设置文件的下载名
        String name = jsonObject.getString("name");
        //传输文件
        transferFile(response, name, filePath, size);
    }


    //下载所有文件
    @GetMapping("downloadAll")
    public void downloadAll(@RequestParam Long id, HttpServletResponse response) throws IOException {

        //记录下载的app的id和下载时间
        log.info("time = {},开始下载app,appId = {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), id);
        //获取指定id的app对象
        App app = appService.getById(id);
        JSONArray jsonArray = app.getFiles();
        JSONObject jsonObject = jsonArray.getJSONObject(0);

        //文件的下载路径
        String filePath = rootPath +  jsonObject.getString("file");
        //设置下载的文件名
        String name = jsonObject.getString("name");
        //文件的大小
        Long size = app.getSize();


        //如果是多个文件
        if (app.getFiles().size() != 1) {
            //获取文件的存放位置
            File file = new File(rootPath + filePath);
            String parentFile = file.getParent();
            //压缩文件，该工具类可能会被攻击
            ZipUtil.zip(parentFile);
            //对应磁盘上的压缩文件
            File zipFile = new File(parentFile + ".zip");
            size = zipFile.length();
            name = zipFile.getName();
            filePath = zipFile.getAbsolutePath();

        }

        //传输文件
        transferFile(response, name, filePath, size);

    }


    //传输单一文件
    private void transferFile(HttpServletResponse response, String name, String filePath, long size) throws IOException {
        //传输文件

        //设置文件名，文件类型
        response.setHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(name, "utf-8"));
        response.setContentType("multipart/form-data"); //这样设置，会自动判断下载文件类型

        //把磁盘中的文件读取到appFile数组中
        byte[] appFile = FileUtils.readFileToByteArray(new File(filePath));
        response.setContentLength((int) size);
        StreamUtils.copy(appFile, response.getOutputStream());

    }


    //获取App主文件的下载地址
    private String getMainFileUrl(App app) {

        JSONArray files = app.getFiles();

        //当有多个文件时
        for (int i = 0; i < files.size(); i++) {
            if (files.getJSONObject(i).getBoolean("isMain")) {
                //获取JSON对象
                JSONObject jsonObject = files.getJSONObject(i);
                //返回文件下载地址
                return jsonObject.getString("file");
            }
        }

        return "该App没有主文件！";
    }

    //初始化文件信息，把文件对象转为JSON对象，并保存在磁盘上
    private JSONObject transformToJSON(MultipartFile file, boolean isMain) throws IOException {

        //获取当前年月
        String format = new SimpleDateFormat("yyyyMM").format(new Date());
        //创建用于保存文件信息的JSON对象
        JSONObject jsonFile = new JSONObject();

        //获取文件的MD5值
        String md5 = MD5.bufferMD5(file.getBytes());
        //获取保存文件的名字
        String fileName = MD5.stringMD5(md5 + file.getOriginalFilename());

        //获取文件的后缀名
        String fileSuffixName = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        //设置文件保存在数据库中的路径
        String saveFileName = format + File.separator + fileName + fileSuffixName;


        //创建文件对象
        File saveFile = new File(rootPath + saveFileName);

        //判断磁盘上是否存在文件
        if (saveFile.createNewFile()) {
            //拷贝文件
            FileCopyUtils.copy(file.getBytes(), saveFile);
            //设置文件的属性
            //设置文件的磁盘路径
            jsonFile.put("file", saveFileName);
            //设置文件名
            jsonFile.put("name", file.getOriginalFilename());
            //设置文件大小
            jsonFile.put("size", file.getSize());
            //设置文件的MD5值
            jsonFile.put("md5", md5);
            //设置文件的类型
            jsonFile.put("type", file.getContentType());
            //设置是否是主文件
            jsonFile.put("isMain", isMain);
        } else {
            throw new FileExistsException();
        }
        return jsonFile;
    }

    //初始化文件信息，把图片保存在磁盘上，并返回路径
    public String saveUploadedPicture(MultipartFile file) throws IOException {


        //获取图片的名字
        String filename = file.getOriginalFilename();
        //图片的完整存储路径
        String fileFullPath = rootPath + "picture" + File.separator + filename;

        //创建文件对象
        File saveFile = new File(fileFullPath);
        //判断图片是否存在
        if (saveFile.createNewFile()) {
            //拷贝文件
            FileCopyUtils.copy(file.getBytes(), saveFile);
            //设置图片的访问路径
            return "http://192.168.0.126:9090/meeting/picture/" + filename;
        } else {
            throw new FileExistsException();
        }
    }

    //把前端表单参数转化为键值对存到map中
    public static Map<String, Object> parameterToMap(HttpServletRequest request) throws FileNotFoundException {

        //从请求体中获取所有键值对数据
        Map<String, String[]> parameterMap = request.getParameterMap();

        HashMap<String, Object> map = new HashMap<>();
        JSONArray jsonArray = new JSONArray();

        //遍历parameterMap集合，把v[0]赋值给map集合
        parameterMap.forEach((k, v) -> {
            //找出前端发送的JSON字符串
            if ("files[]".equals(k)) {
                if (v != null) {
                    //把JSON字符串转为JSON对象
                    for (String string : v) {
                        jsonArray.add(JSONObject.parseObject(string));
                    }
                    map.put("files", jsonArray);
                }
            } else {
                if (v.length > 0 && v[0] != null) {
                    map.put(k, v[0]);
                }

            }
        });

        return map;
    }


}
