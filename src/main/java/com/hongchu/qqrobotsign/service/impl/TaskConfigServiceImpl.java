package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.mapper.TaskConfigMapper;
import com.hongchu.qqrobotsign.pojo.DTO.TaskConfigDTO;
import com.hongchu.qqrobotsign.pojo.VO.TaskConfigVO;
import com.hongchu.qqrobotsign.pojo.entity.TaskConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hongchu.qqrobotsign.pojo.DTO.TaskConfigDTO;
import com.hongchu.qqrobotsign.pojo.VO.TaskConfigVO;
import com.hongchu.qqrobotsign.pojo.entity.TaskConfig;
import com.hongchu.qqrobotsign.service.EmailService;
import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.service.ITaskConfigService;
import com.hongchu.qqrobotsign.utils.ChineseHolidayUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时任务配置服务实现
 */
@Service
@Slf4j
public class TaskConfigServiceImpl extends ServiceImpl<TaskConfigMapper, TaskConfig> implements ITaskConfigService {

    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private IUserService userService;
    @Autowired private EmailService emailService;
    @Autowired private ChineseHolidayUtils holidayUtils;
    @org.springframework.context.annotation.Lazy
    @Autowired private com.hongchu.qqrobotsign.task.SchedulingManager schedulingManager;

    private final Random random = new Random();
    private static final String DELAY_QUEUE = "sign:queue";

    @PostConstruct
    public void init() {
        initDefaultConfigs();
    }

    @Override
    public void initDefaultConfigs() {
        // 检查是否已初始化
        if (count() > 0) {
            return;
        }

        log.info("初始化默认任务配置...");

        // 1. 调度所有用户 - 每日 18:31（日历控制具体日期）
        TaskConfig scheduleConfig = new TaskConfig();
        scheduleConfig.setTaskName("调度所有用户");
        scheduleConfig.setTaskKey("schedule_users");
        scheduleConfig.setCronExpression("0 31 18 * * ?");
        scheduleConfig.setDescription("每天调度需要自动签到的用户，设置随机延迟");
        scheduleConfig.setEnabled(true);
        scheduleConfig.setCreatedAt(LocalDateTime.now());
        scheduleConfig.setUpdatedAt(LocalDateTime.now());
        save(scheduleConfig);

        // 2. 间隔执行签到 - 18:00-20:00 每分钟
        TaskConfig intervalConfig = new TaskConfig();
        intervalConfig.setTaskName("间隔执行签到");
        intervalConfig.setTaskKey("interval_sign");
        intervalConfig.setCronExpression("0 */1 18-20 * * ?");
        intervalConfig.setDescription("检查并执行到期的签到任务");
        intervalConfig.setEnabled(true);
        intervalConfig.setCreatedAt(LocalDateTime.now());
        intervalConfig.setUpdatedAt(LocalDateTime.now());
        save(intervalConfig);

        // 3. JWS续签 - 每日 18:00（间隔周数控制）
        TaskConfig jwsConfig = new TaskConfig();
        jwsConfig.setTaskName("JWS续签");
        jwsConfig.setTaskKey("refresh_jws");
        jwsConfig.setCronExpression("0 0 18 * * ?");
        jwsConfig.setDescription("按周期间隔续签所有用户的JWS");
        jwsConfig.setEnabled(true);
        jwsConfig.setJwsStartDate(LocalDate.now());
        jwsConfig.setJwsIntervalWeeks(1);
        jwsConfig.setCreatedAt(LocalDateTime.now());
        jwsConfig.setUpdatedAt(LocalDateTime.now());
        save(jwsConfig);

        log.info("默认任务配置初始化完成");
    }

    @Override
    public List<TaskConfigVO> getAllTaskConfigs() {
        List<TaskConfig> configs = list();
        return configs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public TaskConfigVO getTaskConfigByKey(String taskKey) {
        LambdaQueryWrapper<TaskConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskConfig::getTaskKey, taskKey);
        TaskConfig config = getOne(wrapper);
        return config != null ? convertToVO(config) : null;
    }

    @Override
    public TaskConfigVO updateTaskConfig(String taskKey, TaskConfigDTO dto) {
        LambdaQueryWrapper<TaskConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskConfig::getTaskKey, taskKey);
        TaskConfig config = getOne(wrapper);

        if (config == null) {
            throw new RuntimeException("任务配置不存在: " + taskKey);
        }

        // 构建新的Cron表达式
        String newCron = buildCronExpression(taskKey, dto);
        if (newCron != null) {
            config.setCronExpression(newCron);
        }

        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }

        // 保存延迟范围和日历配置
        if ("schedule_users".equals(taskKey) && dto.getScheduleConfig() != null) {
            var sc = dto.getScheduleConfig();
            config.setDelayRange(sc.getDelayRange());
            config.setScheduleYear(sc.getScheduleYear());
            if (sc.getScheduleDates() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    config.setScheduleDates(mapper.writeValueAsString(sc.getScheduleDates()));
                } catch (Exception e) {
                    log.error("序列化scheduleDates失败", e);
                }
            }
        }

        // 保存JWS间隔配置
        if ("refresh_jws".equals(taskKey) && dto.getJwsConfig() != null) {
            var jc = dto.getJwsConfig();
            if (jc.getStartDate() != null && !jc.getStartDate().isEmpty()) {
                config.setJwsStartDate(LocalDate.parse(jc.getStartDate()));
            }
            if (jc.getIntervalWeeks() != null) {
                config.setJwsIntervalWeeks(jc.getIntervalWeeks());
            }
        }

        config.setUpdatedAt(LocalDateTime.now());
        updateById(config);

        // 通知调度管理器刷新任务
        schedulingManager.refreshAllTasks();

        return convertToVO(config);
    }

    @Override
    public String buildCronExpression(String taskKey, TaskConfigDTO dto) {
        log.info("构建Cron表达式 - taskKey: {}, intervalConfig: {}", taskKey, dto.getIntervalConfig());
        if (dto.getIntervalConfig() != null) {
            log.info("intervalConfig详情 - startHour: {}, startMinute: {}, endHour: {}, endMinute: {}, intervalMinutes: {}",
                    dto.getIntervalConfig().getStartHour(),
                    dto.getIntervalConfig().getStartMinute(),
                    dto.getIntervalConfig().getEndHour(),
                    dto.getIntervalConfig().getEndMinute(),
                    dto.getIntervalConfig().getIntervalMinutes());
        }
        switch (taskKey) {
            case "schedule_users":
                return buildScheduleCron(dto.getScheduleConfig());
            case "interval_sign":
                return buildIntervalCron(dto.getIntervalConfig());
            case "refresh_jws":
                return buildJwsCron(dto.getJwsConfig());
            default:
                return null;
        }
    }

    /**
     * 构建调度任务的Cron（每日执行，日历控制具体日期）
     * 格式: 0 mm HH * * ?
     */
    private String buildScheduleCron(TaskConfigDTO.ScheduleConfig config) {
        if (config == null) return null;
        return String.format("0 %d %d * * ?",
                config.getMinute(),
                config.getHour());
    }

    /**
     * 构建间隔执行的Cron
     * 当 startMinute > 0 且跨度多个小时时，拆成两个 cron 用 || 连接：
     * - 第一个小时：从 startMinute 开始
     * - 后续小时：从第 0 分钟开始
     */
    private String buildIntervalCron(TaskConfigDTO.IntervalConfig config) {
        if (config == null) {
            log.warn("intervalConfig为null");
            return null;
        }
        Integer startMinute = config.getStartMinute() != null ? config.getStartMinute() : 0;
        Integer endMinute = config.getEndMinute() != null ? config.getEndMinute() : 59;
        Integer intervalMinutes = config.getIntervalMinutes() != null ? config.getIntervalMinutes() : 1;
        Integer startHour = config.getStartHour() != null ? config.getStartHour() : 18;
        Integer endHour = config.getEndHour() != null ? config.getEndHour() : 20;

        log.info("构建间隔执行Cron - startMinute: {}, endMinute: {}, intervalMinutes: {}, startHour: {}, endHour: {}",
                startMinute, endMinute, intervalMinutes, startHour, endHour);

        if (startMinute > 0 && endHour > startHour) {
            return String.format("0 %d-%d/%d %d %d * * ?||0 %d-%d/%d %d-%d * * ?",
                    startMinute, endMinute, intervalMinutes, startHour, startHour,
                    0, endMinute, intervalMinutes, startHour + 1, endHour);
        }
        return String.format("0 %d-%d/%d %d-%d * * ?",
                startMinute, endMinute, intervalMinutes, startHour, endHour);
    }

    /**
     * 构建JWS刷新Cron（每日执行，间隔周数控制）
     * 格式: 0 mm HH * * ?
     */
    private String buildJwsCron(TaskConfigDTO.JwsConfig config) {
        if (config == null) return null;
        return String.format("0 %d %d * * ?",
                config.getMinute(),
                config.getHour());
    }

    @Override
    public TaskConfigVO.ParsedCron parseCronExpression(String taskKey, String cron) {
        if (cron == null || cron.isEmpty()) {
            return null;
        }

        String[] parts = cron.split(" ");        if (parts.length != 7 && parts.length != 6) {
            return null;
        }

        TaskConfigVO.ParsedCron.ParsedCronBuilder builder = TaskConfigVO.ParsedCron.builder();

        switch (taskKey) {
            case "schedule_users":
                // 0 mm HH * * d,d,d
                builder.minute(parts[1])
                        .hour(parts[2])
                        .daysOfWeek(parts[5]);
                break;
            case "interval_sign":
                // 取第一段 cron 的 startHour/startMinute，取最后一段的 endHour/endMinute
                parseIntervalCronForDisplay(cron, builder);
                break;
            case "refresh_jws":
                // 0 mm HH * * ?
                builder.jwsMinute(parts[1])
                        .jwsHour(parts[2]);
                break;
        }

        return builder.build();
    }

    /**
     * 解析 interval_sign 的 cron 用于前端展示。
     * 若是 || 分隔的两段 cron，start 取第一段，end 取最后一段，保证展示完整时间范围。
     */
    private void parseIntervalCronForDisplay(String cron, TaskConfigVO.ParsedCron.ParsedCronBuilder builder) {
        String[] crons = cron.split("\\|\\|");
        String firstCron = crons[0].trim();
        String lastCron = crons[crons.length - 1].trim();

        // 解析第一段：取 startHour, startMinute, interval
        String[] firstParts = firstCron.split(" ");
        String minuteRange = firstParts[1];
        String[] firstHourRange = firstParts[2].split("-");
        String[] minuteParts = minuteRange.split("/");
        String interval = minuteParts.length > 1 ? minuteParts[1] : "1";
        String[] startEndMinutes = minuteParts[0].split("-");
        String startMinute = startEndMinutes[0];

        // 解析最后一段：取 endHour, endMinute
        String[] lastParts = lastCron.split(" ");
        String lastMinuteRange = lastParts[1];
        String[] lastHourRange = lastParts[2].split("-");
        String[] lastStartEndMinutes = lastMinuteRange.split("/")[0].split("-");
        String endMinute = lastStartEndMinutes.length > 1 ? lastStartEndMinutes[1] : "59";

        builder.interval(interval)
                .startHour(firstHourRange[0])
                .startMinute(startMinute)
                .endHour(lastHourRange[lastHourRange.length - 1])
                .endMinute(endMinute);
    }

    private TaskConfigVO convertToVO(TaskConfig config) {
        var parsedCron = parseCronExpression(config.getTaskKey(), config.getCronExpression());

        // 填充 JWS 间隔字段
        if ("refresh_jws".equals(config.getTaskKey())) {
            if (parsedCron != null && config.getJwsStartDate() != null) {
                parsedCron.setJwsStartDate(config.getJwsStartDate().toString());
                parsedCron.setJwsIntervalWeeks(config.getJwsIntervalWeeks());
                LocalDate nextRefresh = config.getJwsStartDate();
                LocalDate today = LocalDate.now();
                while (!nextRefresh.isAfter(today)) {
                    nextRefresh = nextRefresh.plusWeeks(config.getJwsIntervalWeeks());
                }
                parsedCron.setJwsNextRefresh(nextRefresh.toString());
            }
        }

        TaskConfigVO.TaskConfigVOBuilder builder = TaskConfigVO.builder()
                .id(config.getId())
                .taskName(config.getTaskName())
                .taskKey(config.getTaskKey())
                .cronExpression(config.getCronExpression())
                .description(config.getDescription())
                .enabled(config.getEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .parsedCron(parsedCron);

        // 添加调度任务配置（日历字段）
        if ("schedule_users".equals(config.getTaskKey()) && parsedCron != null) {
            TaskConfigVO.ScheduleConfig scheduleVO = new TaskConfigVO.ScheduleConfig();
            scheduleVO.setHour(parsedCron.getHour() != null ? Integer.parseInt(parsedCron.getHour()) : null);
            scheduleVO.setMinute(parsedCron.getMinute() != null ? Integer.parseInt(parsedCron.getMinute()) : null);
            scheduleVO.setDelayRange(config.getDelayRange());
            scheduleVO.setScheduleYear(config.getScheduleYear());
            // 解析已选日期
            if (config.getScheduleDates() != null && !config.getScheduleDates().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<String> dates = mapper.readValue(config.getScheduleDates(), new TypeReference<>() {});
                    scheduleVO.setScheduleDates(dates);
                } catch (Exception e) {
                    log.warn("解析scheduleDates失败: {}", e.getMessage());
                }
            }
            // 自动推荐和假期
            int year = config.getScheduleYear() != null ? config.getScheduleYear() : LocalDate.now().getYear();
            scheduleVO.setScheduleYear(year);
            scheduleVO.setHolidayDates(holidayUtils.getHolidays(year).stream()
                    .map(LocalDate::toString).collect(Collectors.toList()));
            scheduleVO.setAutoSelectedDates(holidayUtils.computeDefaultSchedule(year).stream()
                    .map(LocalDate::toString).collect(Collectors.toList()));
            builder.scheduleConfig(scheduleVO);
        }

        return builder.build();
    }

    @Override
    public void triggerImmediateSchedule(boolean sendEmail) {
        log.info("==================== service层-立即执行调度所有用户任务, sendEmail: {} ====================", sendEmail);

        TaskConfig config = getOne(new LambdaQueryWrapper<TaskConfig>().eq(TaskConfig::getTaskKey, "schedule_users"));
        log.info("schedule_users 配置查询结果: {}", config != null ? "存在" : "不存在!!!");

        int delayRange = 30;
        if (config != null && config.getDelayRange() != null) {
            delayRange = config.getDelayRange();
        }

        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        int todayValue = today.getValue() % 7;
        log.info("今天是星期{} (todayValue={}), delayRange={}", today, todayValue, delayRange);

        var allUsers = userService.list();
        log.info("数据库中总用户数: {}", allUsers.size());
        for (var u : allUsers) {
            log.info("  用户: username={}, autoSign={}, signDays={}, signStartTime={}, signEndTime={}",
                    u.getUsername(), u.getAutoSign(), u.getSignDays(), u.getSignStartTime(), u.getSignEndTime());
        }

        var users = userService.list().stream()
                .filter(user -> Boolean.TRUE.equals(user.getAutoSign()))
                .filter(user -> shouldSignToday(user, todayValue))
                .filter(user -> shouldSignTodayCalendar(config))
                .toList();

        log.info("经过筛选后，需要调度的用户数: {}", users.size());
        for (var u : users) {
            log.info("  需调度用户: username={}", u.getUsername());
        }

        if (users.isEmpty()) {
            log.warn("⚠️ 没有需要调度的用户！请检查: 1)用户是否开启了autoSign 2)signDays是否包含今天 3)日历配置");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (var user : users) {
            // 签到开始时间，默认 7:00
            LocalTime signStartTime = user.getSignStartTime();
            if (signStartTime == null) signStartTime = LocalTime.of(19, 0);
            // 签到结束时间，默认 22:00
            LocalTime signEndTime = user.getSignEndTime();
            if (signEndTime == null) signEndTime = LocalTime.of(22, 0);

            long baseMillis = LocalDate.now().atTime(signStartTime)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = LocalDate.now().atTime(signEndTime)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            int delayMinutes = 1 + random.nextInt(delayRange);
            long executeTime = baseMillis + (delayMinutes * 60L * 1000);
            // 如果调度触发晚了，计算出的执行时间已经过去，则从现在开始重新计算延迟
            long now = System.currentTimeMillis();
            if (executeTime < now) {
                executeTime = now + (1 + random.nextInt(delayRange)) * 60L * 1000;
            }
            // 不能超过签到结束时间
            if (executeTime > endMillis) executeTime = endMillis;
            String formattedTime = sdf.format(new Date(executeTime));

            redisTemplate.opsForZSet().add(DELAY_QUEUE, user.getUsername(), executeTime);
            log.info("✅ 已加入延迟队列: username={}, executeTime={} ({}), baseMillis={}, delayMinutes={}",
                    user.getUsername(), executeTime, formattedTime, baseMillis, delayMinutes);

            if (sendEmail && user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendScheduleNotice(user.getEmail(), user.getUsername(), formattedTime);
            }
        }

        // 加完后确认队列大小
        Long queueSize = redisTemplate.opsForZSet().size(DELAY_QUEUE);
        log.info("==================== 立即调度完成，共调度 {} 个用户，延迟范围: {}分钟, Redis队列当前大小: {} ====================",
                users.size(), delayRange, queueSize);
    }

    private boolean shouldSignToday(com.hongchu.qqrobotsign.pojo.entity.User user, int todayValue) {
        String signDays = user.getSignDays();
        if (signDays == null || signDays.isEmpty()) {
            return true;
        }
        return Arrays.stream(signDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(day -> {
                    try {
                        return Integer.parseInt(day) == todayValue;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
    }

    /**
     * 检查今天是否在调度日历中
     */
    private boolean shouldSignTodayCalendar(TaskConfig config) {
        if (config == null || config.getScheduleDates() == null || config.getScheduleDates().isEmpty()) {
            return true; // 没有配置日历则每天调度
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> dates = mapper.readValue(config.getScheduleDates(), new TypeReference<>() {});
            String today = LocalDate.now().toString();
            return dates.contains(today);
        } catch (Exception e) {
            log.warn("解析scheduleDates失败，默认允许调度: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 获取某年的日历数据
     */
    public Map<String, Object> getScheduleCalendar(int year) {
        Map<String, Object> result = new HashMap<>();
        List<LocalDate> autoSelected = holidayUtils.computeDefaultSchedule(year);
        List<LocalDate> holidays = holidayUtils.getHolidays(year);
        result.put("autoSelectedDates", autoSelected.stream().map(LocalDate::toString).collect(Collectors.toList()));
        result.put("holidayDates", holidays.stream().map(LocalDate::toString).collect(Collectors.toList()));
        // 跳过日期（假期+寒暑假+周五周六）
        result.put("skippedDates", holidayUtils.getSkippedDays(year).stream()
                .map(LocalDate::toString).collect(Collectors.toList()));
        return result;
    }

    /**
     * 判断今天是否应该执行JWS刷新（基于间隔周数）
     */
    public boolean shouldRefreshJwsToday(TaskConfig config) {
        if (config.getJwsStartDate() == null) return false;
        LocalDate startDate = config.getJwsStartDate();
        Integer interval = config.getJwsIntervalWeeks();
        if (interval == null || interval <= 0) interval = 1;
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return false;
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
        return daysBetween % (interval * 7L) == 0;
    }
}
