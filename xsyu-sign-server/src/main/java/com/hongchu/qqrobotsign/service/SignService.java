package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.DTO.SignDTO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;

// 签到服务
public interface SignService {
    // 一键签到所有未签到的签到
    String signAll(String username);

    // 处理单个签到
    String processSingleSign(String username, SignItem signItem, SignDTO signDTO);

    // 为所有用户一键签到
    void AllUserSignAll();
}
