package com.ocan.app.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ocan.app.entity.App;
import com.ocan.app.mode.Result;
import com.ocan.app.service.AppService;
import com.ocan.app.utils.MD5;
import com.ocan.app.vo.AppVO;
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

    //获取文件的存储根路径
    @Value("${file.uploadFolder}")
    private String rootPath;

    //临时存放文件的MD5
    private Set<String> md5List = new HashSet<>();


    //上传文件
//    @PostMapping(value = "/upload")
//    public Result<JSONArray> upload(MultipartFile[] files, @RequestBody AppVO appVO) {
//
//        try {
//            JSONArray jsonArray = convert(files, appVO);
//            return Result.OK("上传成功", jsonArray);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return Result.error("文件上传失败，上传的文件中存在相同的文件！");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error(e.getMessage());
//        }
//    }



    //单文件上传
    @PostMapping("upload")
    public Result<?> upload(MultipartFile file, HttpServletRequest request) {

        //判断是否接收到文件
        if (null == file) {
            return Result.error("上传失败，文件为空！");
        }

        //获取请求体中的参数
        Map<String, String> map = parameterToMap(request);
        //把JSON数据映射到java实体类
        AppVO appVO = JSONObject.parseObject(JSONObject.toJSONString(map), AppVO.class);

        try {
            JSONObject jsonObject = transformToJSON(file, appVO);
            return Result.OK("上传成功", jsonObject);
        } catch (FileExistsException e) {
            return Result.error("上传失败，上传的文件中存在相同的文件！");
        } catch (IOException e) {
            return Result.error("上传失败，文件拷贝失败！");
        }


    }


    //检查新版本
    @GetMapping("checkNewVersion")
    public Result<?> checkNewVersion(@RequestParam String appCode, @RequestParam String platform, @RequestParam String appOwner) {
        //拼接所需的SQL语句
        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_Code", appCode).eq("platform", platform).
                eq("app_Owner", appOwner);
        //获取查询结果
        App one = appService.getOne(queryWrapper);

        if (one == null) {
            return Result.error("检查失败！所查询的软件不存在。");
        }

        //装载查询结果的值
        Map<String, String> map = new HashMap<>();
        map.put("id", one.getId().toString());
        map.put("build", one.getBuild().toString());
        map.put("version", one.getVersion());
        map.put("releaseInfo", one.getReleaseInfo());
        map.put("mainFileUrl", getMainFileUrl(one));

        return Result.OK("检查成功！", map);
    }


    //下载主文件
    @GetMapping("downloadMainVersionFile")
    public void downloadMainVersionFile(@RequestParam Long id, @RequestParam String transformInfo, HttpServletResponse response) {
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
    public void downloadApp(@RequestParam Long id, @RequestParam String md5, HttpServletResponse response) {
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


    //传输单一文件
    private void transferFile(HttpServletResponse response, String name, String filePath, long size) {
        //传输文件
        try {
            //设置文件名，文件类型
            response.setHeader("Content-disposition", "attachment; filename=" + URLEncoder.encode(name, "utf-8"));
            response.setContentType("multipart/form-data"); //这样设置，会自动判断下载文件类型

            //把磁盘中的文件读取到appFile数组中
            byte[] appFile = FileUtils.readFileToByteArray(new File(filePath));
            response.setContentLength((int) size);
            StreamUtils.copy(appFile, response.getOutputStream());


        } catch (IOException e) {
            e.printStackTrace();
        }
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


    //初始化文件信息，把MultipartFile对象转为JSONArray对象，并保存在磁盘上
    private JSONArray convert(MultipartFile[] files, List<AppVO> appVOList) throws IOException {

        //获取当前年月
        String format = new SimpleDateFormat("yyyy-MM").format(new Date());
        JSONArray jsonArray = new JSONArray();

        //获取每个appVO对象
        for (AppVO appVO : appVOList) {
            //获取每个文件
            for (MultipartFile file : files) {
                //创建用于保存文件信息的JSON对象
                JSONObject jsonFile = new JSONObject();

                //获取文件的MD5值
                String md5 = null;
                try {
                    md5 = MD5.bufferMD5(file.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                //创建文件对象
                File saveFile = new File(rootPath + format + File.separator
                        + appVO.getAppName() + File.separator + appVO.getName());
                //判断磁盘上是否存在对应的位置，如果不存在，则创建
                if (!saveFile.exists()) {
                    saveFile.mkdirs();
                }
                //拷贝文件
                FileCopyUtils.copy(file.getBytes(), saveFile);

                //设置文件的磁盘路径
                jsonFile.put("file", saveFile);
                //设置文件名
                jsonFile.put("name", appVO.getName());
                //设置文件大小
                jsonFile.put("size", file.getSize());
                //设置文件的MD5值
                jsonFile.put("md5", md5);
                //设置文件的类型
                jsonFile.put("type", file.getContentType());
                //设置是否是主文件
                jsonFile.put("isMain", appVO.getIsMain());

                //把JSON对象添加到JSON数组中
                jsonArray.add(jsonFile);
            }
        }



        return jsonArray;

    }


    //初始化文件信息，把MultipartFile对象转为JSON对象，并保存在磁盘上
    private JSONObject transformToJSON(MultipartFile file, AppVO appVO) throws IOException {

        //获取当前年月
        String format = new SimpleDateFormat("yyyy-MM").format(new Date());
        //创建用于保存文件信息的JSON对象
        JSONObject jsonFile = new JSONObject();


        //获取文件的MD5值
        String md5 = null;
        try {
            md5 = MD5.bufferMD5(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //把文件的MD5放到集合中
        boolean isSuccess = md5List.add(md5);
        if (!isSuccess) {
            throw new FileExistsException();
        }
        //如果是最后一个文件，重置set集合
        if ("true".equals(appVO.getIsLast())) {
            md5List.clear();
        }


        //创建文件对象
        File saveFile = new File(rootPath + format + File.separator
                + appVO.getAppName() + File.separator + appVO.getName());
        //判断磁盘上是否存在对应的位置，如果不存在，则创建
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        //拷贝文件
        FileCopyUtils.copy(file.getBytes(), saveFile);

        //设置文件的磁盘路径
        jsonFile.put("file", saveFile);
        //设置文件名
        jsonFile.put("name", appVO.getName());
        //设置文件大小
        jsonFile.put("size", file.getSize());
        //设置文件的MD5值
        jsonFile.put("md5", md5);
        //设置文件的类型
        jsonFile.put("type", file.getContentType());
        //设置是否是主文件
        jsonFile.put("isMain", appVO.getIsMain());


        return jsonFile;
    }


    //把前端表单参数转化为键值对存到map中
    public static Map<String, String> parameterToMap(HttpServletRequest request) {

        //从请求体中获取所有键值对数据
        Map<String, String[]> parameterMap = request.getParameterMap();
        log.info("parameterMap:{}", parameterMap);

        HashMap<String, String> map = new HashMap<>();

        //遍历parameterMap集合，把v[0]赋值给map集合
        parameterMap.forEach((k, v) -> {
            map.put(k, v[0]);
        });
        log.info("map:{}", map);

        return map;
    }



}
