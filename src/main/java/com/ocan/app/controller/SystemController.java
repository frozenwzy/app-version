package com.ocan.app.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ocan.app.dto.SystemLogin;
import com.ocan.app.entity.User;
import com.ocan.app.mode.Result;
import com.ocan.app.service.UserService;
import com.ocan.app.utils.Constants;
import com.ocan.app.utils.JwtUtil;
import com.ocan.app.utils.RandImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController()
@RequestMapping("system")
public class SystemController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserController userController;

    //生成验证码
    @PostMapping("randomImage")
    public void randomImage(HttpServletRequest request,
                            HttpServletResponse response) {

        Result<String> res = new Result<>();
        try {
            //生成验证码
            String code = RandomUtil.randomString(Constants.BASE_CHECK_CODES, 4);

            //存到Session里面,前端生成的随机数作为key
            HttpSession session = request.getSession();
            session.setAttribute(Constants.VERIFICATION_CODE, code);
            session.setMaxInactiveInterval(60 * 5);
            log.info("获取验证码,Session checkCode = {},", code);

            //生成验证码图片,将生成的数据流返回
            ByteArrayOutputStream os = RandImageUtil.generate(code);
            byte[] bytes = os.toByteArray();
            StreamUtils.copy(bytes, response.getOutputStream());
            response.setContentType("image/jpg");

        } catch (Exception e) {
            res.error500("获取验证码出错" + e.getMessage());
            e.printStackTrace();
        }
    }


    //用户登录接口
    @PostMapping("login")
    public Result<JSONObject> login(HttpServletRequest request,
                                    @RequestBody SystemLogin systemLogin) {

        Result<JSONObject> result = new Result<>();

        //获取用户名
        String username = systemLogin.getUsername();
        //获取密码
        String password = systemLogin.getPassword();
        //获取验证码
        String smsCode = systemLogin.getSmsCode();


        //检查验证码是否过期和正确
        Object checkCode = request.getSession().getAttribute(Constants.VERIFICATION_CODE);
        if (checkCode == null || !checkCode.toString().equalsIgnoreCase(smsCode)) {
            log.warn("验证码错误, Ui checkCode= {}, Session checkCode = {}", smsCode, checkCode);
            return result.error500("验证码错误！");
        }

        //根据用户名获取数据库中的数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User one = userService.getOne(queryWrapper);

        //判断用户是否存在
        if (one == null) {
            return result.error500("用户名错误或该用户不存在！");
        }
        //判断用户是否被禁用

        if (one.getStatus().equals(Constants.SYS_USER_DISABLE)) {
            return result.error500("该用户已被禁用！");
        }

        //判断用户的密码是否正确
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((password + one.getSalt()).getBytes(StandardCharsets.UTF_8));
        String sysPassword = one.getPassword();
        if (!sysPassword.equals(md5DigestAsHex)) {
            return result.error500("密码错误！");
        }

        //登录成功，生成并返回token以及返回用户信息
        String token = JwtUtil.getToken(one.getId());
        log.info("生成token:{}", token);

        //封装返回的信息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", token);
        one.setSalt(null);
        one.setPassword(null);
        jsonObject.put("sysUser", one);
        result.success("登录成功！");
        result.setResult(jsonObject);
        return result;

    }

    //用户注册接口
    @PostMapping("register")
    public Result<?> addUser(@RequestBody User user) {

        //判断注册的用户是否存在
        if (userController.isExist(user)) {
            return Result.error("注册失败！该用户已存在");
        }

        //获取md5密码盐
        String salt = RandomUtil.randomString(Constants.BASE_CHECK_CODES, 8);
        //获取加密后的密码
        String password = DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes(StandardCharsets.UTF_8));

        //设置用户的值
        user.setSalt(salt);
        user.setPassword(password);

        //注册
        boolean whetherSuccess = userService.save(user);
        if (whetherSuccess) {
            return Result.OK("注册成功");
        } else {
            return Result.error("注册失败！未知原因。");
        }
    }


    //冻结后台账户
    @GetMapping ("frozen")
    public Result<?> frozen(@RequestParam Long id) {

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(User::getId, id);
        updateWrapper.set(User::getStatus, Constants.SYS_USER_DISABLE);
        boolean whetherSuccess = userService.update(updateWrapper);
        if (whetherSuccess) {
            return Result.ok("冻结成功！");
        } else {
            return Result.error("冻结失败！该用户不存在。");
        }

    }


}
