package com.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetingroom.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    /**
     * 检查指定会议室在指定时间段是否存在冲突预定
     */
    @Select("SELECT COUNT(*) FROM booking WHERE room_id = #{roomId} " +
            "AND status IN ('pending', 'confirmed') " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    long countConflictingBookings(@Param("roomId") Integer roomId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定会议室在指定日期的所有已占用时间段
     */
    @Select("SELECT * FROM booking WHERE room_id = #{roomId} " +
            "AND status IN ('pending', 'confirmed') " +
            "AND DATE(start_time) = #{date} " +
            "ORDER BY start_time")
    List<Booking> getBookingsByRoomAndDate(@Param("roomId") Integer roomId,
                                           @Param("date") String date);
}