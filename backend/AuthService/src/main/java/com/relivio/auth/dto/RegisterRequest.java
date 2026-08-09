package com.relivio.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required.")
    @Size(max = 120, message = "Full name must be at most 120 characters.")
    private String name;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits.")
    private String phone;

    @Email(message = "Email must be a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 4, max = 72, message = "Password must be between 4 and 72 characters.")
    private String password;
}
