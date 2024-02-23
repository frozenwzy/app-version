package com.ocan.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ocan.app.mapper.AppVOMapper;
import com.ocan.app.service.AppVOService;
import com.ocan.app.vo.AppVO;
import org.springframework.stereotype.Service;

@Service
public class AppVOServiceImpl extends ServiceImpl<AppVOMapper, AppVO> implements AppVOService {
}
