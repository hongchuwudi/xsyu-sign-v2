package com.hongchu.qqrobotsign.pojo.DTO;

import lombok.Data;

import java.util.List;

@Data
public class UserGroupDTO {
    private String name;
    private String description;
    private List<Long> userIds;
}
