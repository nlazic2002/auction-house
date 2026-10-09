package com.example.auction_house.service;

import com.example.auction_house.model.Account;
import org.springframework.security.oauth2.jwt.Jwt;

public interface JwtService {

    Jwt generateToken(Account account);
}
