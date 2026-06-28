package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private String title;
    private String content;
    private Integer type;
    private Long businessId;
    private Integer readStatus;
    private LocalDateTime createTime;
}
