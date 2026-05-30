package com.meetingroom.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUserDetails {
    private Integer userId;
    private String username;
    private String role;
}