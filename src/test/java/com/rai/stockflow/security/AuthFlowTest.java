package com.rai.stockflow.security;

import com.jayway.jsonpath.JsonPath;
import com.rai.stockflow.config.PasswordConfig;
import com.rai.stockflow.controller.AuthController;
import com.rai.stockflow.controller.ProductController;
import com.rai.stockflow.dto.ProductResponse;
import com.rai.stockflow.entity.User;
import com.rai.stockflow.exception.ApiExceptionHandler;
import com.rai.stockflow.repository.UserRepository;
import com.rai.stockflow.service.LoginService;
import com.rai.stockflow.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ProductController.class})
@Import({SecurityConfig.class, MethodSecurityConfig.class, ActiveUserJwtConverter.class,
        PasswordConfig.class, LoginService.class, ApiExceptionHandler.class})
@TestPropertySource(properties = "JWT_SECRET_BASE64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductService productService;

    @Test
    void adminLogsInAndAccessesProtectedProduct() throws Exception {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(productService.findById(1L))
                .thenReturn(new ProductResponse(1L, "AB-12", "Monitor", "Displays", true));

        String token = loginAndReadToken("correct");

        mvc.perform(get("/api/v1/products/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("AB-12"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas."));

        verifyNoInteractions(productService);
    }

    @Test
    void existingTokenStopsWorkingAfterUserIsDeactivated() throws Exception {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        String token = loginAndReadToken("correct");
        user.deactivate();

        mvc.perform(get("/api/v1/products/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    @Test
    void operatorTokenCannotCreateProduct() throws Exception {
        User user = new User("admin@example.com", passwordEncoder.encode("correct"), Role.OPERATOR);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        String token = loginAndReadToken("correct");

        mvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"AB-12","name":"Monitor","category":"Displays"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("stockflow")
                .subject("admin@example.com")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .claim("roles", List.of("ADMIN"))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mvc.perform(get("/api/v1/products/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(productService);
    }

    private String loginAndReadToken(String password) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
