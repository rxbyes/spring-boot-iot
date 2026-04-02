package com.ghlzm.iot.system.dto;

public record UserProfileUpdateDTO(
        String nickname,
        String realName,
        String phone,
        String email,
        String avatar
) {
}
