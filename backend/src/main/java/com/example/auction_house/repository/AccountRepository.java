package com.example.auction_house.repository;

import com.example.auction_house.model.Account;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(final long id);

    Optional<Account> findByEmail(final String email);
}
