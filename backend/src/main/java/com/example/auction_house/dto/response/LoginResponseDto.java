package com.example.auction_house.dto.response;

public record LoginResponseDto(String accessToken, String tokenType, long expiresIn) {
}
