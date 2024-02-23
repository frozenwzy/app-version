package com.ocan.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocan.app.entity.User;
import com.ocan.app.mapper.UserMapper;
import com.ocan.app.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
