package com.hongchu.qqrobotsign.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChineseHolidayUtils {

    private final Map<Integer, Set<LocalDate>> holidays = new HashMap<>();

    @PostConstruct
    public void loadHolidays() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("holidays.json");
            InputStream is = resource.getInputStream();
            Map<String, List<String>> raw = mapper.readValue(is, new TypeReference<>() {});
            for (var entry : raw.entrySet()) {
                int year = Integer.parseInt(entry.getKey());
                Set<LocalDate> dates = entry.getValue().stream()
                        .map(LocalDate::parse)
                        .collect(Collectors.toSet());
                holidays.put(year, dates);
            }
            log.info("加载节假日配置完成，共{}年数据", holidays.size());
        } catch (Exception e) {
            log.error("加载holidays.json失败", e);
        }
    }

    public boolean isHoliday(LocalDate date) {
        Set<LocalDate> yearHolidays = holidays.get(date.getYear());
        return yearHolidays != null && yearHolidays.contains(date);
    }

    public boolean isVacationMonth(LocalDate date) {
        int month = date.getMonthValue();
        return month == 2 || month == 7 || month == 8;
    }

    public boolean isSkippedDay(LocalDate date) {
        if (isVacationMonth(date)) return true;
        if (isHoliday(date)) return true;
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY;
    }

    public boolean isSchoolDay(LocalDate date) {
        return !isSkippedDay(date);
    }

    /**
     * 生成默认签到日期：所有周日-周四，排除寒暑假月份和法定节假日
     */
    public List<LocalDate> computeDefaultSchedule(int year) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (isSchoolDay(date)) {
                result.add(date);
            }
        }
        return result;
    }

    public List<LocalDate> getHolidays(int year) {
        Set<LocalDate> yearHolidays = holidays.get(year);
        if (yearHolidays == null) return Collections.emptyList();
        List<LocalDate> sorted = new ArrayList<>(yearHolidays);
        sorted.sort(LocalDate::compareTo);
        return sorted;
    }

    /**
     * 获取跳过日期（假期+寒暑假月份+周五周六）—— 用于前端日历展示
     */
    public List<LocalDate> getSkippedDays(int year) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (isSkippedDay(date)) {
                result.add(date);
            }
        }
        return result;
    }
}
