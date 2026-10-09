package com.example.auction_house.service.impl;

import com.example.auction_house.model.Account;
import com.example.auction_house.repository.AccountRepository;
import com.example.auction_house.service.AccountService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account loadUserById(final long id) {
        return accountRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Override
    public Account loadUserByEmail(final String email) {
        return accountRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Override
    public Account loadUserByUsername(final String username) throws UsernameNotFoundException {
        return loadUserByEmail(username);
    }
}
