package com.org.openshift.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.openshift.dto.BookRequest;
import com.org.openshift.dto.BookResponse;
import com.org.openshift.exception.DuplicateResourceException;
import com.org.openshift.exception.ResourceNotFoundException;
import com.org.openshift.service.BookService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer slice test — only the MVC infrastructure + this controller are loaded, BookService is mocked. */
@WebMvcTest(BookController.class)
@DisplayName("BookController web slice tests")
class BookControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    private BookResponse sampleResponse() {
        Instant now = Instant.now();
        return new BookResponse(1L, "Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008, now, now);
    }

    @Test
    @DisplayName("POST /api/v1/books — valid request returns 201 with Location header")
    void create_validRequest_returns201() throws Exception {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008);
        when(bookService.create(any(BookRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isbn").value("978-0132350884"));
    }

    @Test
    @DisplayName("POST /api/v1/books — blank title returns 400 with field error")
    void create_blankTitle_returns400() throws Exception {
        BookRequest invalid = new BookRequest("", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008);

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("POST /api/v1/books — duplicate isbn returns 409")
    void create_duplicateIsbn_returns409() throws Exception {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008);
        when(bookService.create(any(BookRequest.class)))
                .thenThrow(new DuplicateResourceException("Book with isbn '978-0132350884' already exists"));

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/books returns the full list")
    void findAll_returnsList() throws Exception {
        when(bookService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} — unknown id returns 404")
    void findById_unknown_returns404() throws Exception {
        when(bookService.findById(99L)).thenThrow(new ResourceNotFoundException("Book with id 99 not found"));

        mockMvc.perform(get("/api/v1/books/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/books/{id} — valid request returns 200")
    void update_validRequest_returns200() throws Exception {
        BookRequest request = new BookRequest("Clean Code (2nd ed.)", "Robert C. Martin", "978-0132350884", new BigDecimal("44.99"), 2009);
        when(bookService.update(eq(1L), any(BookRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} returns 204")
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1")).andExpect(status().isNoContent());
        verify(bookService).delete(anyLong());
    }
}
