package com.rai.stockflow.controller;

import com.rai.stockflow.entity.User;
import com.rai.stockflow.exception.ApiExceptionHandler;
import com.rai.stockflow.exception.UserNotFoundException;
import com.rai.stockflow.security.ActiveUserJwtConverter;
import com.rai.stockflow.security.MethodSecurityConfig;
import com.rai.stockflow.security.Role;
import com.rai.stockflow.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({MethodSecurityConfig.class, ApiExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserRegistrationService service;

    @MockitoBean
    private ActiveUserJwtConverter activeUserJwtConverter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesUserWithoutExposingPassword() throws Exception {
        User saved = mock(User.class);
        when(saved.getId()).thenReturn(7L);
        when(saved.getEmail()).thenReturn("manager@example.com");
        when(saved.getRole()).thenReturn(Role.MANAGER);
        when(saved.isActive()).thenReturn(true);
        when(service.register("manager@example.com", "secret", Role.MANAGER))
                .thenReturn(saved);

        mvc.perform(post("/api/v1/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"manager@example.com","password":"secret","role":"MANAGER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("manager@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminQueriesUser() throws Exception {
        User saved = mock(User.class);
        when(saved.getId()).thenReturn(7L);
        when(saved.getEmail()).thenReturn("manager@example.com");
        when(saved.getRole()).thenReturn(Role.MANAGER);
        when(saved.isActive()).thenReturn(true);
        when(service.findById(7L)).thenReturn(saved);

        mvc.perform(get("/api/v1/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@example.com"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotCreateUser() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"manager@example.com","password":"secret","role":"MANAGER"}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidEmailReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid","password":"secret","role":"OPERATOR"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingUserReturnsNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new UserNotFoundException(99L));
        mvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado: 99"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadUsers() throws Exception {
        mvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
