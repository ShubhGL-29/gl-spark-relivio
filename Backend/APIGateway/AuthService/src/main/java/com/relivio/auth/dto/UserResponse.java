package com.relivio.auth.dto;

import com.relivio.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}
