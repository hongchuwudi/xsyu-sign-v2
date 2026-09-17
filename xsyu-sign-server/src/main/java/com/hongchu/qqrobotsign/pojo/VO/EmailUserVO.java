package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailUserVO {
    private Long id;
    private String username;
    private String name;
    private String email;
}
