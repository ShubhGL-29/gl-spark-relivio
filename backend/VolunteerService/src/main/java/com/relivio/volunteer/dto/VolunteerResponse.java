package com.relivio.volunteer.dto;

import com.relivio.volunteer.enums.AvailabilityStatus;
import com.relivio.volunteer.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerResponse {
    private Long volunteerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer age;
    private Gender gender;
    private String skills;
    private String address;
    private String city;
    private String state;
    private AvailabilityStatus availabilityStatus;
    private Long assignedIncidentId;
    private String assignedArea;
    private Long assignedReliefRequestId;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private LocalDateTime registrationDate;
    private LocalDateTime updatedAt;
}
