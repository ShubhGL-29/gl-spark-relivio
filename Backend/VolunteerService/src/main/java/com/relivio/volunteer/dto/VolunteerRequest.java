package com.relivio.volunteer.dto;

import com.relivio.volunteer.enums.AvailabilityStatus;
import com.relivio.volunteer.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerRequest {

    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be in a valid format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotNull(message = "Age is mandatory")
    @Min(value = 18, message = "Volunteer must be at least 18 years old")
    @Max(value = 65, message = "Volunteer cannot be older than 65 years")
    private Integer age;

    @NotNull(message = "Gender is mandatory")
    private Gender gender;

    private AvailabilityStatus availabilityStatus;

    @Size(max = 1000, message = "Skills description cannot exceed 1000 characters")
    private String skills;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "Emergency contact name is mandatory")
    @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact number must be exactly 10 digits")
    private String emergencyContactNumber;
}
