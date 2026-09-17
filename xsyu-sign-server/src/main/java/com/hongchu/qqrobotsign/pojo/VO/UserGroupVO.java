package com.hongchu.qqrobotsign.pojo.VO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserGroupVO {
    private Long id;
    private String name;
    private String description;
    private Integer memberCount;
    private List<EmailUserVO> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
