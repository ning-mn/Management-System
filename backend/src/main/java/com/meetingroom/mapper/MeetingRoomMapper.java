package com.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meetingroom.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetingRoomMapper extends BaseMapper<MeetingRoom> {
}