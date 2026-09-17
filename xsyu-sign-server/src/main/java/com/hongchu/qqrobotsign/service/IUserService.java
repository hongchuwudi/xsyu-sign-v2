package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.QrPollVO;
import com.hongchu.qqrobotsign.pojo.VO.QrSessionVO;
import com.hongchu.qqrobotsign.pojo.VO.UserLoginVO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author hongchu
 * @since 2025-11-17
 */
public interface IUserService extends IService<User> {
    // 学号+学校密码登录（管理员走本地密码校验；触发验证码抛 1004；无账号自动创建）
    UserLoginVO xsyPasswordLogin(String username, String casPsd, String captchaSessionId, String captchaCode);

    // 移除登录信息（删除数据库）
    String removeLoginInfo();

    // 短信登录：发送验证码
    String smsSendCode(String phone);

    // 短信登录：校验验证码并按学号匹配（无账号自动创建）
    UserLoginVO smsLogin(String smsSessionId, String username, String phone, String smsCode);

    // 扫码登录：创建二维码会话（需先输入学号）
    QrSessionVO qrCreate(String username);

    // 扫码登录：轮询扫码状态（校验学号归属）
    QrPollVO qrPoll(String qrSessionId);

    // 更新学校密码（用 CAS 密码验证当前账号；可能触发验证码）
    UserLoginVO bindByPassword(String casPsd, String captchaSessionId, String captchaCode);

    // 续签JWS
    void refreshJws(String username);

    // 修改信息
    void setInfo(UserDTO userDTO);

    // 设置自动登录
    void setAutoSign(Boolean isAuto);

    // 获取用户信息
    UserVO getMyInfo();

    // 分页条件查询获取自己的签到信息

    // 根据签到id手动签到
}
