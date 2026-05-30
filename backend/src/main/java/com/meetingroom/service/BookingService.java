package com.meetingroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meetingroom.config.SecurityContextHelper;
import com.meetingroom.dto.BookingRequest;
import com.meetingroom.entity.Booking;
import com.meetingroom.entity.MeetingRoom;
import com.meetingroom.mapper.BookingMapper;
import com.meetingroom.mapper.MeetingRoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private final BookingMapper bookingMapper;
    private final MeetingRoomMapper meetingRoomMapper;

    public BookingService(BookingMapper bookingMapper, MeetingRoomMapper meetingRoomMapper) {
        this.bookingMapper = bookingMapper;
        this.meetingRoomMapper = meetingRoomMapper;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        Integer userId = SecurityContextHelper.getCurrentUserId();

        // 检查会议室是否存在
        MeetingRoom room = meetingRoomMapper.selectById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        // 检查时间有效性
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("不能预定过去的时间");
        }

        // 检查冲突
        long conflictCount = bookingMapper.countConflictingBookings(
                request.getRoomId(), request.getStartTime(), request.getEndTime());
        if (conflictCount > 0) {
            throw new IllegalStateException("该时间段已被占用，请选择其他时间");
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(request.getRoomId());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setRemark(request.getRemark());
        booking.setStatus("pending");
        bookingMapper.insert(booking);

        return bookingMapper.selectById(booking.getId());
    }

    @Transactional
    public void cancelBooking(Integer id) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new IllegalArgumentException("预定记录不存在");
        }

        Integer currentUserId = SecurityContextHelper.getCurrentUserId();
        String currentRole = SecurityContextHelper.getCurrentUserRole();

        // 只能取消自己的预定或管理员取消任意预定
        if (!booking.getUserId().equals(currentUserId) && !"admin".equals(currentRole)) {
            throw new IllegalArgumentException("无权取消该预定");
        }

        // 只能取消未开始的预定
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("只能取消未开始的预定");
        }

        booking.setStatus("cancelled");
        bookingMapper.updateById(booking);
    }

    public Map<String, Object> getMyBookings(Integer page, Integer pageSize, String status) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Integer userId = SecurityContextHelper.getCurrentUserId();
        Page<Booking> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Booking::getStatus, status);
        }
        wrapper.orderByDesc(Booking::getStartTime);

        Page<Booking> result = bookingMapper.selectPage(pageObj, wrapper);

        // 填充会议室名称和用户名
        List<Booking> records = result.getRecords();
        for (Booking b : records) {
            MeetingRoom room = meetingRoomMapper.selectById(b.getRoomId());
            if (room != null) {
                b.setRoomId(room.getId());
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("records", records);
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("pageSize", result.getSize());
        return map;
    }

    public Map<String, Object> getAllBookings(Integer page, Integer pageSize,
                                               Integer roomId, String status,
                                               String startTimeBegin, String startTimeEnd) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        Page<Booking> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();

        if (roomId != null) {
            wrapper.eq(Booking::getRoomId, roomId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Booking::getStatus, status);
        }
        if (StringUtils.hasText(startTimeBegin)) {
            wrapper.ge(Booking::getStartTime, startTimeBegin);
        }
        if (StringUtils.hasText(startTimeEnd)) {
            wrapper.le(Booking::getStartTime, startTimeEnd);
        }
        wrapper.orderByDesc(Booking::getStartTime);

        Page<Booking> result = bookingMapper.selectPage(pageObj, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("page", result.getCurrent());
        map.put("pageSize", result.getSize());
        return map;
    }

    @Transactional
    public void updateBookingStatus(Integer id, String newStatus) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new IllegalArgumentException("预定记录不存在");
        }

        if (!"confirmed".equals(newStatus) && !"rejected".equals(newStatus)) {
            throw new IllegalArgumentException("无效的状态值");
        }

        if (!"pending".equals(booking.getStatus())) {
            throw new IllegalArgumentException("只能审批待审批的预定");
        }

        booking.setStatus(newStatus);
        bookingMapper.updateById(booking);
    }
}