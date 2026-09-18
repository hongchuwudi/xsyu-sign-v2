package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.EmailNotificationTaskMapper;
import com.hongchu.qqrobotsign.mapper.EmailTaskRecipientMapper;
import com.hongchu.qqrobotsign.mapper.EmailTemplateMapper;
import com.hongchu.qqrobotsign.mapper.UserGroupMapper;
import com.hongchu.qqrobotsign.mapper.UserGroupMemberMapper;
import com.hongchu.qqrobotsign.mapper.UserMapper;
import com.hongchu.qqrobotsign.pojo.DTO.EmailNotificationTaskDTO;
import com.hongchu.qqrobotsign.pojo.DTO.EmailTemplateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserGroupDTO;
import com.hongchu.qqrobotsign.pojo.VO.EmailNotificationTaskVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailTaskRecipientVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailUserVO;
import com.hongchu.qqrobotsign.pojo.VO.UserGroupVO;
import com.hongchu.qqrobotsign.pojo.entity.EmailNotificationTask;
import com.hongchu.qqrobotsign.pojo.entity.EmailTaskRecipient;
import com.hongchu.qqrobotsign.pojo.entity.EmailTemplate;
import com.hongchu.qqrobotsign.pojo.entity.User;
import com.hongchu.qqrobotsign.pojo.entity.UserGroup;
import com.hongchu.qqrobotsign.pojo.entity.UserGroupMember;
import com.hongchu.qqrobotsign.service.IAdminEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminEmailServiceImpl implements IAdminEmailService {
    private final EmailTemplateMapper templateMapper;
    private final UserGroupMapper groupMapper;
    private final UserGroupMemberMapper groupMemberMapper;
    private final EmailNotificationTaskMapper taskMapper;
    private final EmailTaskRecipientMapper recipientMapper;
    private final UserMapper userMapper;

    @Override
    public List<EmailTemplate> listTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<EmailTemplate>()
                .orderByDesc(EmailTemplate::getUpdatedAt));
    }

    @Override
    public EmailTemplate createTemplate(EmailTemplateDTO dto) {
        validateTemplate(dto);
        ensureTemplateNameAvailable(dto.getName().trim(), null);
        LocalDateTime now = LocalDateTime.now();
        EmailTemplate template = new EmailTemplate();
        template.setName(dto.getName().trim());
        template.setSubject(dto.getSubject().trim());
        template.setContent(dto.getContent());
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        templateMapper.insert(template);
        return template;
    }

    @Override
    public void updateTemplate(Long id, EmailTemplateDTO dto) {
        validateTemplate(dto);
        if (templateMapper.selectById(id) == null) throw new BusinessException("邮件模板不存在");
        ensureTemplateNameAvailable(dto.getName().trim(), id);
        EmailTemplate template = new EmailTemplate();
        template.setId(id);
        template.setName(dto.getName().trim());
        template.setSubject(dto.getSubject().trim());
        template.setContent(dto.getContent());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        if (templateMapper.deleteById(id) == 0) throw new BusinessException("邮件模板不存在");
    }

    @Override
    public List<UserGroupVO> listGroups() {
        return groupMapper.selectList(new LambdaQueryWrapper<UserGroup>()
                        .orderByDesc(UserGroup::getUpdatedAt))
                .stream().map(this::toGroupVO).toList();
    }

    @Override
    @Transactional
    public UserGroupVO createGroup(UserGroupDTO dto) {
        validateGroup(dto);
        ensureGroupNameAvailable(dto.getName().trim(), null);
        List<User> users = loadEmailUsers(dto.getUserIds(), true);
        LocalDateTime now = LocalDateTime.now();
        UserGroup group = new UserGroup();
        group.setName(dto.getName().trim());
        group.setDescription(trimToNull(dto.getDescription()));
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        groupMapper.insert(group);
        replaceGroupMembers(group.getId(), users, now);
        return toGroupVO(group);
    }

    @Override
    @Transactional
    public void updateGroup(Long id, UserGroupDTO dto) {
        validateGroup(dto);
        UserGroup group = groupMapper.selectById(id);
        if (group == null) throw new BusinessException("用户组不存在");
        ensureGroupNameAvailable(dto.getName().trim(), id);
        List<User> users = loadEmailUsers(dto.getUserIds(), true);
        group.setName(dto.getName().trim());
        group.setDescription(trimToNull(dto.getDescription()));
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.updateById(group);
        groupMemberMapper.delete(new LambdaQueryWrapper<UserGroupMember>().eq(UserGroupMember::getGroupId, id));
        replaceGroupMembers(id, users, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        if (groupMapper.selectById(id) == null) throw new BusinessException("用户组不存在");
        groupMemberMapper.delete(new LambdaQueryWrapper<UserGroupMember>().eq(UserGroupMember::getGroupId, id));
        groupMapper.deleteById(id);
    }

    @Override
    public List<EmailUserVO> searchUsers(String keyword) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getName, User::getEmail)
                .isNotNull(User::getEmail)
                .ne(User::getEmail, "");
        if (StringUtils.isNotBlank(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper.like(User::getUsername, value)
                    .or().like(User::getName, value)
                    .or().like(User::getEmail, value));
        }
        query.orderByAsc(User::getUsername).last("LIMIT 100");
        return userMapper.selectList(query).stream().map(this::toEmailUserVO).toList();
    }

    @Override
    public List<EmailNotificationTaskVO> listTasks() {
        return taskMapper.selectList(new LambdaQueryWrapper<EmailNotificationTask>()
                        .orderByDesc(EmailNotificationTask::getCreatedAt)
                        .last("LIMIT 50"))
                .stream().map(this::toTaskVO).toList();
    }

    @Override
    public List<EmailTaskRecipientVO> listRecipients(Long taskId) {
        if (taskMapper.selectById(taskId) == null) throw new BusinessException("邮件任务不存在");
        return recipientMapper.selectList(new LambdaQueryWrapper<EmailTaskRecipient>()
                        .eq(EmailTaskRecipient::getTaskId, taskId)
                        .orderByAsc(EmailTaskRecipient::getId))
                .stream().map(this::toRecipientVO).toList();
    }

    @Override
    @Transactional
    public EmailNotificationTaskVO createImmediateTask(EmailNotificationTaskDTO dto) {
        return toTaskVO(createTask(dto));
    }

    @Override
    public void cancelTask(Long id) {
        LocalDateTime now = LocalDateTime.now();
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<EmailNotificationTask>()
                .eq(EmailNotificationTask::getId, id)
                .eq(EmailNotificationTask::getStatus, "PENDING")
                .eq(EmailNotificationTask::getEnabled, true)
                .set(EmailNotificationTask::getStatus, "CANCELLED")
                .set(EmailNotificationTask::getEnabled, false)
                .set(EmailNotificationTask::getUpdatedAt, now));
        if (updated == 0) {
            if (taskMapper.selectById(id) == null) throw new BusinessException("邮件任务不存在");
            throw new BusinessException("只有待发送任务可以取消");
        }
    }

    private EmailNotificationTask createTask(EmailNotificationTaskDTO dto) {
        validateTask(dto);
        Set<Long> userIds = new LinkedHashSet<>();
        if (dto.getUserIds() != null) userIds.addAll(dto.getUserIds());
        if (dto.getGroupIds() != null && !dto.getGroupIds().isEmpty()) {
            Set<Long> groupIds = new LinkedHashSet<>(dto.getGroupIds());
            if (groupMapper.selectBatchIds(groupIds).size() != groupIds.size()) throw new BusinessException("选择的用户组不存在");
            groupMemberMapper.selectList(new LambdaQueryWrapper<UserGroupMember>()
                            .in(UserGroupMember::getGroupId, groupIds))
                    .forEach(member -> userIds.add(member.getUserId()));
        }
        if (userIds.isEmpty()) throw new BusinessException("请至少选择一个收件人或用户组");
        List<User> users = loadEmailUsers(new ArrayList<>(userIds), false);
        if (users.isEmpty()) throw new BusinessException("选中的用户均未设置邮箱");

        LocalDateTime now = LocalDateTime.now();
        EmailNotificationTask task = new EmailNotificationTask();
        task.setSubject(dto.getSubject().trim());
        task.setContent(dto.getContent());
        task.setStatus("PENDING");
        task.setEnabled(true);
        task.setScheduledAt(null);
        task.setTotalCount(users.size());
        task.setSuccessCount(0);
        task.setFailureCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);

        for (User user : users) {
            EmailTaskRecipient recipient = new EmailTaskRecipient();
            recipient.setTaskId(task.getId());
            recipient.setUserId(user.getId());
            recipient.setUsername(user.getUsername());
            recipient.setName(user.getName());
            recipient.setEmail(user.getEmail().trim());
            recipient.setStatus("PENDING");
            recipient.setCreatedAt(now);
            recipientMapper.insert(recipient);
        }
        return task;
    }

    private void validateTemplate(EmailTemplateDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getName())) throw new BusinessException("请输入模板名称");
        if (StringUtils.isBlank(dto.getSubject())) throw new BusinessException("请输入邮件主题");
        if (StringUtils.isBlank(dto.getContent())) throw new BusinessException("请输入邮件内容");
        if (dto.getName().trim().length() > 100) throw new BusinessException("模板名称不能超过 100 个字符");
        if (dto.getSubject().trim().length() > 255) throw new BusinessException("邮件主题不能超过 255 个字符");
    }

    private void validateGroup(UserGroupDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getName())) throw new BusinessException("请输入用户组名称");
        if (dto.getName().trim().length() > 100) throw new BusinessException("用户组名称不能超过 100 个字符");
        if (dto.getDescription() != null && dto.getDescription().trim().length() > 500) throw new BusinessException("用户组说明不能超过 500 个字符");
    }

    private void validateTask(EmailNotificationTaskDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getSubject())) throw new BusinessException("请输入邮件主题");
        if (StringUtils.isBlank(dto.getContent())) throw new BusinessException("请输入邮件内容");
        if (dto.getSubject().trim().length() > 255) throw new BusinessException("邮件主题不能超过 255 个字符");
    }

    private void ensureTemplateNameAvailable(String name, Long ignoredId) {
        Long count = templateMapper.selectCount(new LambdaQueryWrapper<EmailTemplate>()
                .eq(EmailTemplate::getName, name)
                .ne(ignoredId != null, EmailTemplate::getId, ignoredId));
        if (count > 0) throw new BusinessException("模板名称已存在");
    }

    private void ensureGroupNameAvailable(String name, Long ignoredId) {
        Long count = groupMapper.selectCount(new LambdaQueryWrapper<UserGroup>()
                .eq(UserGroup::getName, name)
                .ne(ignoredId != null, UserGroup::getId, ignoredId));
        if (count > 0) throw new BusinessException("用户组名称已存在");
    }

    private List<User> loadEmailUsers(List<Long> ids, boolean requireAll) {
        Set<Long> uniqueIds = ids == null ? Set.of() : new LinkedHashSet<>(ids);
        if (uniqueIds.isEmpty()) return List.of();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getName, User::getEmail)
                .in(User::getId, uniqueIds)
                .isNotNull(User::getEmail)
                .ne(User::getEmail, "")
                .orderByAsc(User::getUsername));
        if (requireAll && users.size() != uniqueIds.size()) throw new BusinessException("用户不存在或尚未设置邮箱");
        return users;
    }

    private void replaceGroupMembers(Long groupId, List<User> users, LocalDateTime now) {
        for (User user : users) {
            UserGroupMember member = new UserGroupMember();
            member.setGroupId(groupId);
            member.setUserId(user.getId());
            member.setCreatedAt(now);
            groupMemberMapper.insert(member);
        }
    }

    private UserGroupVO toGroupVO(UserGroup group) {
        List<Long> userIds = groupMemberMapper.selectList(new LambdaQueryWrapper<UserGroupMember>()
                        .eq(UserGroupMember::getGroupId, group.getId())
                        .orderByAsc(UserGroupMember::getId))
                .stream().map(UserGroupMember::getUserId).toList();
        List<EmailUserVO> members = userIds.isEmpty() ? List.of() : userMapper.selectBatchIds(userIds)
                .stream().map(this::toEmailUserVO).toList();
        return UserGroupVO.builder()
                .id(group.getId()).name(group.getName()).description(group.getDescription())
                .memberCount(members.size()).members(members)
                .createdAt(group.getCreatedAt()).updatedAt(group.getUpdatedAt()).build();
    }

    private EmailUserVO toEmailUserVO(User user) {
        return EmailUserVO.builder().id(user.getId()).username(user.getUsername())
                .name(user.getName()).email(user.getEmail()).build();
    }

    private EmailNotificationTaskVO toTaskVO(EmailNotificationTask task) {
        return EmailNotificationTaskVO.builder()
                .id(task.getId()).subject(task.getSubject()).content(task.getContent())
                .status(task.getStatus()).enabled(task.getEnabled()).scheduledAt(task.getScheduledAt())
                .startedAt(task.getStartedAt()).completedAt(task.getCompletedAt())
                .totalCount(task.getTotalCount()).successCount(task.getSuccessCount())
                .failureCount(task.getFailureCount()).createdAt(task.getCreatedAt()).build();
    }

    private EmailTaskRecipientVO toRecipientVO(EmailTaskRecipient recipient) {
        return EmailTaskRecipientVO.builder()
                .id(recipient.getId()).userId(recipient.getUserId()).username(recipient.getUsername())
                .name(recipient.getName()).email(recipient.getEmail()).status(recipient.getStatus())
                .errorMessage(recipient.getErrorMessage()).sentAt(recipient.getSentAt()).build();
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}
