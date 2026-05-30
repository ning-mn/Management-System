package com.meetingroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("meeting_room")
public class MeetingRoom {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer capacity;

    private String equipment;

    private String status;
}