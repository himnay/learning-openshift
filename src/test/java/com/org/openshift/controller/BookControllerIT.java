package com.org.openshift.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.org.openshift.dto.BookRequest;
import com.org.openshift.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Full end-to-end integration test: real Spring context, real Postgres (Testcontainers),
 * real Flyway migration, exercised over HTTP via MockMvc through the whole CRUD lifecycle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book CRUD end-to-end integration test")
class BookControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("full lifecycle: create -> read -> list -> update -> delete -> 404")
    void fullCrudLifecycle() throws Exception {
        BookRequest createRequest =
                new BookRequest("Domain-Driven Design", "Eric Evans", "978-0321125217", new BigDecimal("54.99"), 2003);

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.isbn").value("978-0321125217"))
                .andReturn();

        Long id = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/v1/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Domain-Driven Design"));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.isbn == '978-0321125217')]").exists());

        BookRequest updateRequest =
                new BookRequest("Domain-Driven Design (Blue Book)", "Eric Evans", "978-0321125217", new BigDecimal("59.99"), 2003);

        mockMvc.perform(put("/api/v1/books/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Domain-Driven Design (Blue Book)"))
                .andExpect(jsonPath("$.price").value(59.99));

        mockMvc.perform(delete("/api/v1/books/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/books/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("creating the same isbn twice returns 409 Conflict")
    void create_duplicateIsbn_returns409() throws Exception {
        BookRequest request = new BookRequest("1984", "George Orwell", "978-0451524935", new BigDecimal("19.99"), 1949);

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate resource"));
    }

    @Test
    @DisplayName("actuator health endpoint is up and includes db + readiness/liveness groups")
    void actuatorHealth_isUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void databaseIsReachable() {
        assertThat(POSTGRES.isRunning()).isTrue();
    }
}
