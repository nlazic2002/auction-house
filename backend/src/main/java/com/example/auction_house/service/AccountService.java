package com.example.auction_house.service;

import com.example.auction_house.model.Account;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends UserDetailsService {

    Account loadUserById(final long id);

    Account loadUserByEmail(final String email);
}
