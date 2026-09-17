package com.hongchu.qqrobotsign.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.pojo.DTO.UserDTO;
import com.hongchu.qqrobotsign.pojo.VO.UserVO;
import com.hongchu.qqrobotsign.pojo.entity.SignItem;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理
 *
 * @author hongchu
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final IAdminService adminService;

    /**
     * 分页查询用户列表
     * @param page 页码
     * @param size 每页数量
     * @param keyword 关键词（用户名、姓名、邮箱）
     * @param filter 筛选条件（autoSign: 自动签到开启, noJws: JWS失效）
     * @return 用户列表
     */
    @GetMapping("/users")
    public Result<IPage<UserVO>> getUsersByPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String filter) {
        log.info("controller层-分页查询用户列表-page: {}, size: {}, keyword: {}, filter: {}", page, size, keyword, filter);
        Page<User> pageParam = new Page<>(page, size);
        IPage<UserVO> userPage = adminService.getUsersByPage(pageParam, keyword, filter);
        return Result.success(userPage);
    }

    /**
     * 用户统计数据（全局，不分页）
     */
    @GetMapping("/users/stats")
    public Result<java.util.Map<String, Object>> getUserStats() {
        log.info("controller层-获取用户统计数据");
        return Result.success(adminService.getUserStats());
    }

    /**
     * 给指定用户续签JWS
     * @param username 用户名
     * @return 操作成功
     */
    @PostMapping("/refresh-jws/{username}")
    public Result<Void> refreshUserJws(@PathVariable String username) {
        log.info("controller层-给用户续签JWS-username: {}", username);
        adminService.refreshUserJws(username);
        return Result.success();
    }

    /**
     * 删除用户
     * @param username 用户名
     * @return 操作成功
     */
    @DeleteMapping("/users/{username}")
    public Result<Void> deleteUser(@PathVariable String username) {
        log.info("controller层-删除用户-username: {}", username);
        adminService.deleteUser(username);
        return Result.success();
    }

    /**
     * 修改用户信息
     * @param username 用户名
     * @param userDTO 修改信息
     * @return 操作成功
     */
    @PutMapping("/users/{username}")
    public Result<Void> updateUserInfo(@PathVariable String username, @RequestBody UserDTO userDTO) {
        log.info("controller层-修改用户信息-username: {}, UserDTO: {}", username, userDTO);
        adminService.updateUserInfo(username, userDTO);
        return Result.success();
    }

    /**
     * 获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/users/{username}")
    public Result<UserVO> getUserInfo(@PathVariable String username) {
        log.info("controller层-获取用户信息-username: {}", username);
        UserVO userVO = adminService.getUserInfo(username);
        if (userVO == null) throw new BusinessException("获取用户信息失败");
        return Result.success(userVO);
    }

    /**
     * 获取用户详细信息
     * @param username 用户名
     * @return 用户详细信息
     */
    @GetMapping("/users/{username}/detail")
    public Result<UserVO> getUserDetail(@PathVariable("username") String username) {
        log.info("controller层-获取用户详细信息-username: {}", username);
        UserVO userVO = adminService.getUserInfo(username);
        if (userVO == null) throw new BusinessException("获取用户信息失败");
        return Result.success(userVO);
    }

    /**
     * 切换用户自动签到状态
     * @param username 用户名
     * @param autoSign 自动签到状态
     * @return 操作成功
     */
    @PostMapping("/users/{username}/auto-sign")
    public Result<Void> toggleAutoSign(@PathVariable("username") String username,
                                       @RequestParam("autoSign") Boolean autoSign) {
        log.info("controller层-切换用户自动签到状态-username: {}, autoSign: {}", username, autoSign);
        adminService.toggleAutoSign(username, autoSign);
        return Result.success();
    }

    /**
     * 新增用户
     * @param userDTO 用户信息
     * @return 操作成功
     */
    @PostMapping("/users")
    public Result<Void> addUser(@RequestBody UserDTO userDTO) throws InterruptedException {
        log.info("controller层-新增用户-username: {}", userDTO.getUsername());
        adminService.addUser(userDTO);
        return Result.success();
    }

    /**
     * 获取用户最近的签到记录
     * @param username 用户名
     * @param limit 限制数量（默认10条）
     * @return 签到记录列表
     */
    @GetMapping("/users/{username}/signs")
    public Result<List<SignItem>> getUserSigns(@PathVariable String username, @RequestParam(defaultValue = "10") Integer limit) {
        log.info("controller层-获取用户签到记录-username: {}, limit: {}", username, limit);
        List<SignItem> signs = adminService.getUserSigns(username, limit);
        return Result.success(signs);
    }
}
