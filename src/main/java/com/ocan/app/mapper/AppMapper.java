package com.ocan.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocan.app.entity.App;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppMapper extends BaseMapper<App> {
}
