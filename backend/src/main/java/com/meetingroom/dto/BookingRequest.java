package com.meetingroom.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "会议室ID不能为空")
    private Integer roomId;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须是将来的时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须是将来的时间")
    private LocalDateTime endTime;

    private String remark;
}