package com.meetingroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetingroom.dto.RoomRequest;
import com.meetingroom.entity.Booking;
import com.meetingroom.entity.MeetingRoom;
import com.meetingroom.mapper.BookingMapper;
import com.meetingroom.mapper.MeetingRoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeetingRoomService {

    private final MeetingRoomMapper meetingRoomMapper;
    private final BookingMapper bookingMapper;

    public MeetingRoomService(MeetingRoomMapper meetingRoomMapper, BookingMapper bookingMapper) {
        this.meetingRoomMapper = meetingRoomMapper;
        this.bookingMapper = bookingMapper;
    }

    public Map<String, Object> getRoomList(Integer page, Integer pageSize, Integer capacityMin, String equipment) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Page<MeetingRoom> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<>();

        if (capacityMin != null) {
            wrapper.ge(MeetingRoom::getCapacity, capacityMin);
        }
        if (StringUtils.hasText(equipment)) {
            wrapper.like(MeetingRoom::getEquipment, equipment);
        }
        wrapper.eq(MeetingRoom::getStatus, "available");

        Page<MeetingRoom> result = meetingRoomMapper.selectPage(pageObj, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("pageSize", result.getSize());
        return map;
    }

    public MeetingRoom getRoomById(Integer id) {
        MeetingRoom room = meetingRoomMapper.selectById(id);
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }
        return room;
    }

    public void addRoom(RoomRequest request) {
        MeetingRoom room = new MeetingRoom();
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setEquipment(request.getEquipment());
        room.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "available");
        meetingRoomMapper.insert(room);
    }

    public void updateRoom(Integer id, RoomRequest request) {
        MeetingRoom room = meetingRoomMapper.selectById(id);
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setEquipment(request.getEquipment());
        if (StringUtils.hasText(request.getStatus())) {
            room.setStatus(request.getStatus());
        }
        meetingRoomMapper.updateById(room);
    }

    public void deleteRoom(Integer id) {
        MeetingRoom room = meetingRoomMapper.selectById(id);
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        // 检查是否有未来预定
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getRoomId, id)
                .in(Booking::getStatus, "pending", "confirmed")
                .gt(Booking::getStartTime, LocalDateTime.now());
        long count = bookingMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("该会议室存在未开始的预定，无法删除");
        }

        meetingRoomMapper.deleteById(id);
    }

    public List<Booking> getRoomBookings(Integer roomId, String date) {
        return bookingMapper.getBookingsByRoomAndDate(roomId, date);
    }
}