package com.example.concert_ticket_booking_platform.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "username khong duoc de trong")
    @Size(min = 3, max = 100, message = "username phai tu 3-100 ky tu")
    private String username;

    @NotBlank(message = "email khong duoc de trong")
    @Email(message = "email khong hop le")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "password khong duoc de trong")
    @Size(min = 6, max = 100, message = "password phai co it nhat 6 ky tu")
    private String password;
}