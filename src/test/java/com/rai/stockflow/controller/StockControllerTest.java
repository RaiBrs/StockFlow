package com.rai.stockflow.controller;

import com.rai.stockflow.dto.StockMovementResponse;
import com.rai.stockflow.dto.StockMovementPageResponse;
import com.rai.stockflow.entity.StockMovementType;
import com.rai.stockflow.exception.ApiExceptionHandler;
import com.rai.stockflow.security.ActiveUserJwtConverter;
import com.rai.stockflow.security.MethodSecurityConfig;
import com.rai.stockflow.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import({MethodSecurityConfig.class, ApiExceptionHandler.class})
class StockControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StockService service;

    @MockitoBean
    private ActiveUserJwtConverter activeUserJwtConverter;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCreatesInitialEntry() throws Exception {
        when(service.initialize(any(), org.mockito.ArgumentMatchers.eq("admin@example.com")))
                .thenReturn(new StockMovementResponse(1L, 2L, 3L, 4L,
                        StockMovementType.INITIAL_ENTRY, 5, 0, 5, null, Instant.EPOCH));

        mvc.perform(post("/api/v1/stock/initial-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"warehouseId":3,"quantity":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INITIAL_ENTRY"))
                .andExpect(jsonPath("$.newBalance").value(5));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotInitializeOrAdjust() throws Exception {
        String change = """
                {"productId":2,"warehouseId":3,"quantity":5}
                """;
        mvc.perform(post("/api/v1/stock/initial-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(change))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/stock/adjustments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"warehouseId":3,"quantity":1,
                                 "direction":"OUT","reason":"Contagem"}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCanMakeCommonExit() throws Exception {
        when(service.exit(any(), any())).thenReturn(new StockMovementResponse(1L, 2L, 3L, 4L,
                StockMovementType.EXIT, 2, 5, 3, null, Instant.EPOCH));

        mvc.perform(post("/api/v1/stock/exits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"warehouseId":3,"quantity":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newBalance").value(3));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void negativeQuantityIsRejected() throws Exception {
        mvc.perform(post("/api/v1/stock/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"warehouseId":3,"quantity":-1}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void adjustmentWithoutReasonIsRejected() throws Exception {
        mvc.perform(post("/api/v1/stock/adjustments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"warehouseId":3,"quantity":1,
                                 "direction":"OUT","reason":" "}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousUserCannotReadBalance() throws Exception {
        mvc.perform(get("/api/v1/stock/balances/2/3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadMovementHistory() throws Exception {
        mvc.perform(get("/api/v1/stock/movements/2/3"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerReadsPagedHistory() throws Exception {
        when(service.movements(2L, 3L, 0)).thenReturn(new StockMovementPageResponse(
                List.of(new StockMovementResponse(1L, 2L, 3L, 4L,
                        StockMovementType.INITIAL_ENTRY, 5, 0, 5, null, Instant.EPOCH)), 0, 1, 1));

        mvc.perform(get("/api/v1/stock/movements/2/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("INITIAL_ENTRY"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
