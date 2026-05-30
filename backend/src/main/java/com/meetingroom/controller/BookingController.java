package com.meetingroom.controller;

import com.meetingroom.config.SecurityContextHelper;
import com.meetingroom.dto.BookingRequest;
import com.meetingroom.dto.Result;
import com.meetingroom.dto.StatusRequest;
import com.meetingroom.entity.Booking;
import com.meetingroom.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Result<Booking> create(@Valid @RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            return Result.success("预定成功", booking);
        } catch (IllegalStateException e) {
            return Result.conflict(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Integer id) {
        bookingService.cancelBooking(id);
        return Result.success("取消成功", null);
    }

    @GetMapping("/my")
    public Result<Map<String, Object>> my(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        Map<String, Object> data = bookingService.getMyBookings(page, pageSize, status);
        return Result.success(data);
    }

    @GetMapping("/all")
    public Result<Map<String, Object>> all(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTimeBegin,
            @RequestParam(required = false) String startTimeEnd) {
        if (!SecurityContextHelper.isAdmin()) {
            return Result.forbidden("仅管理员可查看");
        }
        Map<String, Object> data = bookingService.getAllBookings(page, pageSize, roomId, status, startTimeBegin, startTimeEnd);
        return Result.success(data);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id,
                                     @Valid @RequestBody StatusRequest request) {
        if (!SecurityContextHelper.isAdmin()) {
            return Result.forbidden("仅管理员可操作");
        }
        bookingService.updateBookingStatus(id, request.getStatus());
        return Result.success("状态更新成功", null);
    }
}