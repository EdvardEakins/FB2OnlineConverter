package net.flibusta.persistence.dao;

public interface UrlDao {

    UrlInfo findUrlInfo(String url);

    void addUrlReference(String url, String referencedBookId, String referencedBookFormat);

    void removeUrlReference(String url);
}
