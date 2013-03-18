package net.flibusta.persistence.dao.impl;

import net.flibusta.persistence.dao.UrlDao;
import net.flibusta.persistence.dao.UrlInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class UrlDaoSql extends SimpleJdbcDaoSupport implements UrlDao {

    @Override
    public UrlInfo findUrlInfo(String url) {
        url = clearFlibustaUrl(url);
        String md5 = DigestUtils.md5Hex(url);
        List<Map<String, Object>> rows = getJdbcTemplate().queryForList("select bookId, format from book_source where url = ? and url_hash = ?", url, md5);
        if (rows.size() > 1) {
            throw new RuntimeException("URL duplicated: " + url);
        }
        if (rows.size() == 0) {
            return null;
        }

        UrlInfo urlInfo = new UrlInfo();
        urlInfo.setBookId((String) rows.get(0).get("bookId"));
        urlInfo.setSourceFormat((String) rows.get(0).get("format"));
        return urlInfo;
    }

    @Override
    public void addUrlReference(String url, String referencedBookId, String referencedBookFormat) {
        url = clearFlibustaUrl(url);
        String md5 = DigestUtils.md5Hex(url);
        getJdbcTemplate().update("insert into book_source (url, url_hash, bookId, format) values (?, ?, ?, ?)", url, md5, referencedBookId, referencedBookFormat);
    }

    @Override
    public void removeUrlReference(String url) {
        url = clearFlibustaUrl(url);
        String md5 = DigestUtils.md5Hex(url);
        getJdbcTemplate().update("delete from book_source where url = ? and url_hash = ?", url, md5);
    }

    private String clearFlibustaUrl(String url) {
        if (url.toLowerCase().contains("flibusta.net")) {
            try {
                URI uri = new URI(url);
                if (uri.getQuery() != null) {
                    return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null).toString();
                }
            } catch (URISyntaxException e) {
                logger.error("invalid url=" + url, e);
            }
        }
        return url;
    }
}
