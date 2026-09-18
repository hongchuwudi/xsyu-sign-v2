# 停用定时邮件设计

## 目标

停止邮件模块每 30 秒查询 `email_notification_task` 的后台轮询，只保留管理员主动触发的立即发送能力。邮件模板、用户组、发送记录和收件人明细继续保留。

## 后端调整

- 删除 `EmailTaskScheduler`，不再注册邮件数据库轮询任务。
- 保留 `EmailTaskDispatcher`。`POST /admin/email/tasks/send-now` 创建任务后继续直接调用 `dispatchAsync`，因此立即发送不依赖轮询。
- 删除 `POST /admin/email/tasks/schedule` Controller 方法。
- 从 `IAdminEmailService` 和 `AdminEmailServiceImpl` 删除创建定时任务的方法，只保留立即任务创建。
- 保留任务取消接口和状态展示，便于处理历史记录；新代码不再产生定时任务。

## 前端调整

- 从 `EmailNotificationModal` 删除“定时发送”开关和时间选择控件。
- 提交邮件时始终调用立即发送接口。
- 删除 `api.js` 中不再使用的定时发送 API 方法。
- 按钮和错误信息统一使用“立即发送/发送邮件失败”，不再显示定时任务文案。

## 现有任务处理

新增 SQL 迁移，将 `scheduled_at IS NOT NULL`、`status = 'PENDING'` 且仍启用的历史定时任务更新为：

- `status = 'CANCELLED'`
- `enabled = 0`
- 更新 `updated_at`

迁移不修改正在发送、已完成、失败或已经取消的任务，也不删除发送历史和收件人数据。

## 验证

1. 后端搜索不到 `@Scheduled` 邮件轮询和 `/tasks/schedule` 路由。
2. 前端搜索不到定时邮件开关、时间字段和定时发送 API 调用。
3. 前端构建成功。
4. 后端 Maven 打包成功。
5. 部署后日志不再每 30 秒查询 `email_notification_task`。
6. 管理员立即发送仍能创建任务并由异步 Dispatcher 执行。

## 部署顺序

1. 备份生产数据库。
2. 执行新增迁移，取消历史待发送定时任务。
3. 部署新镜像并执行健康检查。
4. 观察邮件轮询日志消失，并人工验证一次立即发送。

## 回滚

应用容器可通过现有部署脚本回滚。数据库迁移只把历史待发送任务改为已取消，不自动恢复，以避免回滚后意外补发过期邮件。
