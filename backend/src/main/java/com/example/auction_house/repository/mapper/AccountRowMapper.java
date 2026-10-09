package com.example.auction_house.repository.mapper;

import com.example.auction_house.enums.account.AccountRole;
import com.example.auction_house.enums.account.AccountStatus;
import com.example.auction_house.model.Account;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

public class AccountRowMapper implements RowMapper<Account> {

    @Override
    public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Account(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("password_hash"),
                AccountStatus.valueOf(rs.getString("account_status").toUpperCase(Locale.ROOT)),
                rs.getObject("email_verified_at", LocalDateTime.class),
                rs.getObject("last_login_at", LocalDateTime.class),
                AccountRole.valueOf(rs.getString("role").toUpperCase(Locale.ROOT)),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getObject("deleted_at", LocalDateTime.class),
                rs.getObject("anonymized_at", LocalDateTime.class)
        );
    }
}
