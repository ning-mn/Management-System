package com.meetingroom.controller;

import com.meetingroom.config.SecurityContextHelper;
import com.meetingroom.dto.LoginRequest;
import com.meetingroom.dto.RegisterRequest;
import com.meetingroom.dto.Result;
import com.meetingroom.entity.User;
import com.meetingroom.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    @GetMapping("/info")
    public Result<User> info() {
        Integer userId = SecurityContextHelper.getCurrentUserId();
        User user = userService.getUserById(userId);
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }
}