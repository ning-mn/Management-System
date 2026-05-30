package com.meetingroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    private String password;

    private String role;

    private String realName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}