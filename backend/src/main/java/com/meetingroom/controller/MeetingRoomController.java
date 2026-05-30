package com.meetingroom.controller;

import com.meetingroom.config.SecurityContextHelper;
import com.meetingroom.dto.Result;
import com.meetingroom.dto.RoomRequest;
import com.meetingroom.entity.Booking;
import com.meetingroom.entity.MeetingRoom;
import com.meetingroom.service.MeetingRoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer capacityMin,
            @RequestParam(required = false) String equipment) {
        Map<String, Object> data = meetingRoomService.getRoomList(page, pageSize, capacityMin, equipment);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<MeetingRoom> getById(@PathVariable Integer id) {
        MeetingRoom room = meetingRoomService.getRoomById(id);
        return Result.success(room);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody RoomRequest request) {
        if (!SecurityContextHelper.isAdmin()) {
            return Result.forbidden("仅管理员可操作");
        }
        meetingRoomService.addRoom(request);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @Valid @RequestBody RoomRequest request) {
        if (!SecurityContextHelper.isAdmin()) {
            return Result.forbidden("仅管理员可操作");
        }
        meetingRoomService.updateRoom(id, request);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (!SecurityContextHelper.isAdmin()) {
            return Result.forbidden("仅管理员可操作");
        }
        meetingRoomService.deleteRoom(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/bookings")
    public Result<List<Booking>> getRoomBookings(@PathVariable Integer id,
                                                  @RequestParam(required = false) String date) {
        List<Booking> bookings = meetingRoomService.getRoomBookings(id, date);
        return Result.success(bookings);
    }
}