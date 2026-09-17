package com.hongchu.qqrobotsign.controller;

import com.hongchu.qqrobotsign.pojo.DTO.EmailNotificationTaskDTO;
import com.hongchu.qqrobotsign.pojo.DTO.EmailTemplateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserGroupDTO;
import com.hongchu.qqrobotsign.pojo.VO.EmailNotificationTaskVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailTaskRecipientVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailUserVO;
import com.hongchu.qqrobotsign.pojo.VO.UserGroupVO;
import com.hongchu.qqrobotsign.pojo.entity.EmailTemplate;
import com.hongchu.qqrobotsign.result.Result;
import com.hongchu.qqrobotsign.service.IAdminEmailService;
import com.hongchu.qqrobotsign.task.EmailTaskDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/email")
@RequiredArgsConstructor
public class AdminEmailController {
    private final IAdminEmailService adminEmailService;
    private final EmailTaskDispatcher taskDispatcher;

    @GetMapping("/templates")
    public Result<List<EmailTemplate>> listTemplates() {
        return Result.success(adminEmailService.listTemplates());
    }

    @PostMapping("/templates")
    public Result<EmailTemplate> createTemplate(@RequestBody EmailTemplateDTO dto) {
        return Result.success(adminEmailService.createTemplate(dto));
    }

    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody EmailTemplateDTO dto) {
        adminEmailService.updateTemplate(id, dto);
        return Result.success();
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        adminEmailService.deleteTemplate(id);
        return Result.success();
    }

    @GetMapping("/groups")
    public Result<List<UserGroupVO>> listGroups() {
        return Result.success(adminEmailService.listGroups());
    }

    @PostMapping("/groups")
    public Result<UserGroupVO> createGroup(@RequestBody UserGroupDTO dto) {
        return Result.success(adminEmailService.createGroup(dto));
    }

    @PutMapping("/groups/{id}")
    public Result<Void> updateGroup(@PathVariable Long id, @RequestBody UserGroupDTO dto) {
        adminEmailService.updateGroup(id, dto);
        return Result.success();
    }

    @DeleteMapping("/groups/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        adminEmailService.deleteGroup(id);
        return Result.success();
    }

    @GetMapping("/users")
    public Result<List<EmailUserVO>> searchUsers(@RequestParam(required = false) String keyword) {
        return Result.success(adminEmailService.searchUsers(keyword));
    }

    @GetMapping("/tasks")
    public Result<List<EmailNotificationTaskVO>> listTasks() {
        return Result.success(adminEmailService.listTasks());
    }

    @GetMapping("/tasks/{id}/recipients")
    public Result<List<EmailTaskRecipientVO>> listRecipients(@PathVariable Long id) {
        return Result.success(adminEmailService.listRecipients(id));
    }

    @PostMapping("/tasks/send-now")
    public Result<EmailNotificationTaskVO> sendNow(@RequestBody EmailNotificationTaskDTO dto) {
        EmailNotificationTaskVO task = adminEmailService.createImmediateTask(dto);
        taskDispatcher.dispatchAsync(task.getId());
        return Result.success(task);
    }

    @PostMapping("/tasks/schedule")
    public Result<EmailNotificationTaskVO> schedule(@RequestBody EmailNotificationTaskDTO dto) {
        return Result.success(adminEmailService.createScheduledTask(dto));
    }

    @PostMapping("/tasks/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        adminEmailService.cancelTask(id);
        return Result.success();
    }
}
