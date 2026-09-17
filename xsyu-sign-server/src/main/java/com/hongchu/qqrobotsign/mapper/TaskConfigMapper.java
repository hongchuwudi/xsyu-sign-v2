package com.hongchu.qqrobotsign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hongchu.qqrobotsign.pojo.entity.TaskConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务配置 Mapper
 */
@Mapper
public interface TaskConfigMapper extends BaseMapper<TaskConfig> {
}
