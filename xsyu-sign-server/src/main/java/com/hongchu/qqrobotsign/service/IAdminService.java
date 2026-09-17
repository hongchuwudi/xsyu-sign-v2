package com.hongchu.qqrobotsign.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * <p>
 * 管理员服务类
 * </p>
 *
 * @author hongchu
 * @since 2025-11-23
 */
public interface IAdminService extends IService<User> {
    // 分页查询用户列表
    IPage<UserVO> getUsersByPage(Page<User> page, String keyword, String filter);

    // 给指定用户续签JWS
    void refreshUserJws(String username);

    // 删除用户
    void deleteUser(String username);

    // 修改用户信息
    void updateUserInfo(String username, UserDTO userDTO);

    // 获取用户信息
    UserVO getUserInfo(String username);

    // 切换用户自动签到状态
    void toggleAutoSign(String username, Boolean autoSign);

    // 新增用户
    void addUser(UserDTO userDTO) throws InterruptedException;

    // 获取用户签到记录
    List<SignItem> getUserSigns(String username, Integer limit);

    // 获取用户统计数据（全局，不分页）
    java.util.Map<String, Object> getUserStats();
}
