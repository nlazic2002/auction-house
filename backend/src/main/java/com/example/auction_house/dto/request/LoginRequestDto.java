package com.example.auction_house.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String password
) {

}
