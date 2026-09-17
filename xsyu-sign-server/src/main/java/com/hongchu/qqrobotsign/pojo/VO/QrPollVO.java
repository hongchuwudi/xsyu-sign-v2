package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrPollVO {
    /** WAITING / EXPIRED / SUCCESS */
    private String status;
    private UserLoginVO loginVO;
}
