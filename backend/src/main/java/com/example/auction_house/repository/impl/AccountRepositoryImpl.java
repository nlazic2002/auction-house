package com.example.auction_house.repository.impl;

import com.example.auction_house.model.Account;
import com.example.auction_house.repository.AccountRepository;
import com.example.auction_house.repository.mapper.AccountRowMapper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.conf.ParamType;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = new AccountRowMapper();

    private final JdbcTemplate jdbcTemplate;
    private final DSLContext dsl;

    public AccountRepositoryImpl(JdbcTemplate jdbcTemplate, DSLContext dsl) {
        this.jdbcTemplate = jdbcTemplate;
        this.dsl = dsl;
    }

    @Override
    public Optional<Account> findById(long id) {
        return findOne(field(unquotedName("id"), Long.class).eq(id));
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return findOne(field(unquotedName("email"), String.class).eq(email));
    }

    private Optional<Account> findOne(Condition condition) {
        Query query = dsl.select(
                        field("id"),
                        field("first_name"),
                        field("last_name"),
                        field("email"),
                        field("phone_number"),
                        field("password_hash"),
                        field("account_status"),
                        field("email_verified_at"),
                        field("last_login_at"),
                        field("role"),
                        field("created_at"),
                        field("updated_at"),
                        field("deleted_at"),
                        field("anonymized_at"))
                .from(table(("accounts")))
                .where(condition);

        return DataAccessUtils
                .optionalResult(
                        jdbcTemplate.query(
                                query.getSQL(ParamType.INDEXED),
                                ACCOUNT_ROW_MAPPER,
                                query
                                        .getBindValues()
                                        .toArray()
                        )
                );
    }
}
