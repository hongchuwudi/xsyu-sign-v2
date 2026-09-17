package com.hongchu.qqrobotsign.service;

import com.hongchu.qqrobotsign.pojo.DTO.EmailNotificationTaskDTO;
import com.hongchu.qqrobotsign.pojo.DTO.EmailTemplateDTO;
import com.hongchu.qqrobotsign.pojo.DTO.UserGroupDTO;
import com.hongchu.qqrobotsign.pojo.VO.EmailNotificationTaskVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailTaskRecipientVO;
import com.hongchu.qqrobotsign.pojo.VO.EmailUserVO;
import com.hongchu.qqrobotsign.pojo.VO.UserGroupVO;
import com.hongchu.qqrobotsign.pojo.entity.EmailTemplate;

import java.util.List;

public interface IAdminEmailService {
    List<EmailTemplate> listTemplates();
    EmailTemplate createTemplate(EmailTemplateDTO dto);
    void updateTemplate(Long id, EmailTemplateDTO dto);
    void deleteTemplate(Long id);

    List<UserGroupVO> listGroups();
    UserGroupVO createGroup(UserGroupDTO dto);
    void updateGroup(Long id, UserGroupDTO dto);
    void deleteGroup(Long id);

    List<EmailUserVO> searchUsers(String keyword);
    List<EmailNotificationTaskVO> listTasks();
    List<EmailTaskRecipientVO> listRecipients(Long taskId);
    EmailNotificationTaskVO createImmediateTask(EmailNotificationTaskDTO dto);
    EmailNotificationTaskVO createScheduledTask(EmailNotificationTaskDTO dto);
    void cancelTask(Long id);
}
