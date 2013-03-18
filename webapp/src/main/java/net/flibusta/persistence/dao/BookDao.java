package net.flibusta.persistence.dao;

import java.io.File;

public interface BookDao {
    File findBook(String bookId, String type);

    String findBookPath(String bookId, String type);

    File addBook(String bookId, String sourceFormat, File sourceFile);

    void deleteBook(String bookId);
}
