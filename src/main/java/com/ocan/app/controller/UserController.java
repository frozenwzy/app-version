package com.ocan.app.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocan.app.entity.User;
import com.ocan.app.mode.Result;
import com.ocan.app.service.UserService;
import com.ocan.app.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    //新增用户
    @PostMapping("add")
    public Result<?> addUser(@RequestBody User user) {

        if (isExist(user)) {
            return Result.error("添加失败，该用户已存在！");
        }

        //获取md5密码盐
        String salt = RandomUtil.randomString(Constants.BASE_CHECK_CODES, 8);
        //获取加密后的密码
        String password = DigestUtils.md5DigestAsHex((user.getPassword() + salt).getBytes(StandardCharsets.UTF_8));

        //设置用户的值
        user.setSalt(salt);
        user.setPassword(password);

        //新增
        boolean whetherSuccess = userService.save(user);
        if (whetherSuccess) {
            return Result.OK("新增成功！");
        } else {
            return Result.error("添加失败！原因未知。");
        }

    }


    //删除用户
    @GetMapping("delete")
    public Result<String> deleteUser(@RequestParam("id") Long id) {

        boolean whetherSuccess = userService.removeById(id);
        if (whetherSuccess) {
            return Result.OK("删除成功！");
        } else {
            return Result.error("删除失败！该用户不存在。");
        }

    }


    //批量删除用户
    @GetMapping("deleteBatch")
    public Result<String> deleteBatch(@RequestParam("list") List<Long> list) {

        boolean whetherSuccess = userService.removeByIds(list);
        if (whetherSuccess) {
            return Result.OK("批量删除成功！");
        } else {
            return Result.error("批量删除失败！有用户不存在。");
        }


    }


    //修改用户
    @PostMapping("edit")
    public Result<String> exitUser(@RequestBody User user) {


        //获取加密后的修改密码
        String password = DigestUtils.md5DigestAsHex((user.getSalt() + user.getPassword()).getBytes(StandardCharsets.UTF_8));
        //获取md5密码盐
        String salt = RandomUtil.randomString(Constants.BASE_CHECK_CODES, 8);
        //设置修改后的
        user.setPassword(password);
        user.setSalt(salt);

        boolean whetherSuccess = userService.updateById(user);
        if (whetherSuccess) {
            return Result.OK("修改成功！");
        } else {
            return Result.error("修改失败！该用户不存在。");
        }

    }


    //查询用户
    @GetMapping("list")
    public Result<?> selectUser(
                                @RequestParam(name = "current", defaultValue = "1") Integer pageNo,
                                @RequestParam(name = "size", defaultValue = "10") Integer pageSize,
                                HttpServletRequest req) {

        Page<User> page = new Page<>(pageNo, pageSize);

        //拼接模糊查询的条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(req.getParameter("username"))) {
            queryWrapper.eq( "username", req.getParameter("username") );
        }
        if (!StringUtils.isEmpty(req.getParameter("phone"))) {
            queryWrapper.like( "phone", req.getParameter("phone") );
        }
        if (!StringUtils.isEmpty(req.getParameter("idNumber"))) {
            queryWrapper.like( "id_Number", req.getParameter("idNumber") );
        }

        Page<User> pageData = userService.page(page, queryWrapper);

        return Result.OK(pageData);


    }

    //测试异常
    @GetMapping("test")
    public Result<?> test() {

        String str = null;
        str.toString();

        return Result.ok("成功");
    }



    //根据username来判断数据库中是否存在重复数据
    public boolean isExist(User user) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        User one = userService.getOne(queryWrapper);

        return one != null;
    }




}
