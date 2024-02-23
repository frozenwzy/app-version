package com.ocan.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocan.app.entity.App;
import com.ocan.app.mapper.AppMapper;
import com.ocan.app.service.AppService;
import org.springframework.stereotype.Service;

@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

}
