package com.org.openshift.service;

import com.org.openshift.dto.BookRequest;
import com.org.openshift.dto.BookResponse;
import com.org.openshift.exception.DuplicateResourceException;
import com.org.openshift.exception.ResourceNotFoundException;
import com.org.openshift.model.Book;
import com.org.openshift.repository.BookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateResourceException("Book with isbn '%s' already exists".formatted(request.isbn()));
        }
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .isbn(request.isbn())
                .price(request.price())
                .publishedYear(request.publishedYear())
                .build();
        return toResponse(bookRepository.save(book));
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream().map(this::toResponse).toList();
    }

    public BookResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = getOrThrow(id);

        bookRepository.findByIsbn(request.isbn()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Book with isbn '%s' already exists".formatted(request.isbn()));
            }
        });

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPrice(request.price());
        book.setPublishedYear(request.publishedYear());
        return toResponse(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = getOrThrow(id);
        bookRepository.delete(book);
    }

    private Book getOrThrow(Long id) {
        return bookRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id %d not found".formatted(id)));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPrice(),
                book.getPublishedYear(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
