package com.org.openshift.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.org.openshift.dto.BookRequest;
import com.org.openshift.dto.BookResponse;
import com.org.openshift.exception.DuplicateResourceException;
import com.org.openshift.exception.ResourceNotFoundException;
import com.org.openshift.model.Book;
import com.org.openshift.repository.BookRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pure unit tests — repository is mocked, no Spring context, no database. */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService unit tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book persistedBook() {
        Instant now = Instant.now();
        return Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0132350884")
                .price(new BigDecimal("39.99"))
                .publishedYear(2008)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("create() persists a new book when isbn is not already used")
    void create_newIsbn_savesBook() {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008);
        when(bookRepository.existsByIsbn(request.isbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenReturn(persistedBook());

        BookResponse response = bookService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isbn()).isEqualTo("978-0132350884");
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("create() rejects a duplicate isbn")
    void create_duplicateIsbn_throws() {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("39.99"), 2008);
        when(bookRepository.existsByIsbn(request.isbn())).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request)).isInstanceOf(DuplicateResourceException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById() returns the mapped response when the book exists")
    void findById_existing_returnsResponse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(persistedBook()));

        BookResponse response = bookService.findById(1L);

        assertThat(response.title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("findById() throws ResourceNotFoundException when missing")
    void findById_missing_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findAll() maps every entity to a response")
    void findAll_returnsAllMapped() {
        when(bookRepository.findAll()).thenReturn(List.of(persistedBook()));

        List<BookResponse> responses = bookService.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isbn()).isEqualTo("978-0132350884");
    }

    @Test
    @DisplayName("update() overwrites fields and bumps updatedAt via the entity")
    void update_existing_updatesFields() {
        Book existing = persistedBook();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn("978-0132350884")).thenReturn(Optional.of(existing));

        BookRequest request = new BookRequest("Clean Code (2nd ed.)", "Robert C. Martin", "978-0132350884", new BigDecimal("44.99"), 2009);
        BookResponse response = bookService.update(1L, request);

        assertThat(response.title()).isEqualTo("Clean Code (2nd ed.)");
        assertThat(response.price()).isEqualByComparingTo("44.99");
    }

    @Test
    @DisplayName("update() throws when the target id does not exist")
    void update_missing_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        BookRequest request = new BookRequest("X", "Y", "Z", BigDecimal.ONE, 2020);

        assertThatThrownBy(() -> bookService.update(99L, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete() removes an existing book")
    void delete_existing_deletesBook() {
        Book existing = persistedBook();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        bookService.delete(1L);

        verify(bookRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("delete() throws when the book does not exist")
    void delete_missing_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(bookRepository, never()).delete(any(Book.class));
    }
}
