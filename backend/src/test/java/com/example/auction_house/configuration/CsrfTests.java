package com.example.auction_house.configuration;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "security.jwt.access-token-ttl=15m"
})
@ActiveProfiles("development")
@Import(CsrfTests.ProbeController.class)
@Transactional
class CsrfTests {
    private static final String BASE = "/api/v1";
    private static final String LOGIN = """
            {"email":"csrf-test@example.com","password":"correct-password"}
            """;

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void csrfEndpointReturnsMaskedTokenAndSessionCookieWithoutCreatingSession() throws Exception {
        CsrfData csrf = fetchCsrf();
        assertThat(csrf.cookie().isHttpOnly()).isTrue();
        assertThat(csrf.cookie().getMaxAge()).isEqualTo(-1);
        assertThat(csrf.cookie().getPath()).isEqualTo(BASE);
        assertThat(csrf.cookie().getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(csrf.token()).isNotEqualTo(csrf.cookie().getValue());
    }

    @Test
    void csrfCookieIsSecureOnHttps() throws Exception {
        mvc.perform(get(BASE + "/authentication/csrf").contextPath(BASE).secure(true))
                .andExpect(status().isOk()).andExpect(cookie().secure("XSRF-TOKEN", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/authentication/login", "/authentication/logout"})
    void loginAndLogoutRequireCsrf(String path) throws Exception {
        mvc.perform(post(BASE + path).contextPath(BASE).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRequiresMatchingCookieAndHeader() throws Exception {
        CsrfData csrf = fetchCsrf();
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE).cookie(csrf.cookie()))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE).header(csrf.header(), csrf.token()))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE)
                        .cookie(csrf.cookie()).header(csrf.header(), "incorrect"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE)
                        .cookie(csrf.cookie()).header(csrf.header(), csrf.token()))
                .andExpect(status().isOk()).andExpect(cookie().maxAge("XSRF-TOKEN", 0));
        CsrfData fresh = fetchCsrf();
        assertThat(fresh.cookie().getValue()).isNotEqualTo(csrf.cookie().getValue());
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE)
                        .cookie(fresh.cookie()).header(csrf.header(), csrf.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfTokenIsReusableAndGetDoesNotLogout() throws Exception {
        CsrfData csrf = fetchCsrf();
        mvc.perform(get(BASE + "/authentication/csrf").contextPath(BASE).cookie(csrf.cookie()))
                .andExpect(status().isOk()).andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mvc.perform(get(BASE + "/authentication/logout").contextPath(BASE).cookie(csrf.cookie()))
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        mvc.perform(post(BASE + "/authentication/logout").contextPath(BASE)
                        .cookie(csrf.cookie()).header(csrf.header(), csrf.token()))
                .andExpect(status().isOk());
    }

    @Test
    void loginRotatesCookieAndAuthenticatedWritesRequireCsrf() throws Exception {
        jdbcTemplate.update("INSERT INTO accounts (email, password_hash) VALUES (?, ?)",
                "csrf-test@example.com", passwordEncoder.encode("correct-password"));
        CsrfData csrf = fetchCsrf();
        MvcResult login = mvc.perform(post(BASE + "/authentication/login").contextPath(BASE)
                        .cookie(csrf.cookie()).header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andExpect(cookie().maxAge("XSRF-TOKEN", 0)).andReturn();
        String jwt = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        CsrfData fresh = fetchCsrf();
        assertThat(fresh.cookie().getValue()).isNotEqualTo(csrf.cookie().getValue());
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
            mvc.perform(request(method, BASE + "/csrf-test").contextPath(BASE).header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
            mvc.perform(request(method, BASE + "/csrf-test").contextPath(BASE).header("Authorization", "Bearer " + jwt)
                            .cookie(fresh.cookie()).header(fresh.header(), fresh.token()))
                    .andExpect(status().isOk());
        }
        mvc.perform(get(BASE + "/csrf-test").contextPath(BASE).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mvc.perform(post(BASE + "/csrf-test").contextPath(BASE)
                        .cookie(fresh.cookie()).header(fresh.header(), fresh.token()))
                .andExpect(status().isUnauthorized());
    }

    private CsrfData fetchCsrf() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "/authentication/csrf").contextPath(BASE))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        String json = result.getResponse().getContentAsString();
        return new CsrfData(result.getResponse().getCookie("XSRF-TOKEN"),
                JsonPath.read(json, "$.headerName"), JsonPath.read(json, "$.token"));
    }

    private record CsrfData(Cookie cookie, String header, String token) { }

    @RestController
    static class ProbeController {
        @RequestMapping("/csrf-test")
        String probe() { return "ok"; }
    }
}
