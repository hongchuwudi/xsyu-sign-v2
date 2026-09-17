package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrSessionVO {
    private String qrSessionId;
    private String qrImageBase64;
}
