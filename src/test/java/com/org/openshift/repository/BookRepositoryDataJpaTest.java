package com.org.openshift.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.org.openshift.model.Book;
import com.org.openshift.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository slice test — only JPA infrastructure is loaded, backed by the real
 * Postgres Testcontainer (AutoConfigureTestDatabase.Replace.NONE keeps @ServiceConnection active
 * instead of swapping in an in-memory H2 database).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("BookRepository @DataJpaTest slice")
class BookRepositoryDataJpaTest extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    private Book newBook(String isbn) {
        return Book.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .isbn(isbn)
                .price(new BigDecimal("45.50"))
                .publishedYear(2018)
                .build();
    }

    @Test
    @DisplayName("save() persists a book and assigns an id + timestamps")
    void save_persistsBook() {
        Book saved = bookRepository.save(newBook("978-0134685991"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("existsByIsbn() reflects persisted state")
    void existsByIsbn_reflectsState() {
        assertThat(bookRepository.existsByIsbn("978-0134685992")).isFalse();

        bookRepository.save(newBook("978-0134685992"));

        assertThat(bookRepository.existsByIsbn("978-0134685992")).isTrue();
    }

    @Test
    @DisplayName("findByIsbn() returns the matching book")
    void findByIsbn_returnsBook() {
        bookRepository.save(newBook("978-0134685993"));

        assertThat(bookRepository.findByIsbn("978-0134685993"))
                .isPresent()
                .get()
                .satisfies(book -> assertThat(book.getAuthor()).isEqualTo("Joshua Bloch"));
    }

    @Test
    @DisplayName("the unique constraint on isbn is enforced at the database level")
    void isbnUniqueConstraint_isEnforced() {
        bookRepository.saveAndFlush(newBook("978-0134685994"));

        Book duplicate = newBook("978-0134685994");
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> bookRepository.saveAndFlush(duplicate));
    }
}
