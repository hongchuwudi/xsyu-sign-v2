package com.hongchu.qqrobotsign.annotation;

import java.lang.annotation.*;

/**
 * 操作日志记录注解，用于标记需要记录日志的 Controller 方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogRecord {

    /** 操作描述 */
    String value() default "";
}
