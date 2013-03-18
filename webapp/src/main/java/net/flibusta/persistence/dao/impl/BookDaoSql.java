package net.flibusta.persistence.dao.impl;

import net.flibusta.persistence.dao.BookDao;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BookDaoSql extends JdbcDaoSupport implements BookDao {


    private File fileStoragePath;

    private Set<String> createdDirs = Collections.synchronizedSet(new HashSet<String>(512));


    @Override
    public File findBook(String bookId, String type) {

        String fileName = findBookPath(bookId, type);
        if (fileName == null) {
            return null;
        }
        File file = new File(fileStoragePath, fileName);
        if (!file.exists()) {
            getJdbcTemplate().update("delete from book where bookid = ? and format = ?", bookId, type);
            return null;
        }
        return file;
    }

    @Override
    public String findBookPath(String bookId, String type) {
        List<Map<String, Object>> rows = getJdbcTemplate().queryForList("select file_name from book where bookid = ? and format = ?", bookId, type);
        if (rows.size() == 0) {
            return null;
        }
        if (rows.size() > 1) {
            getJdbcTemplate().update("delete from book where bookid = ? and format = ?", bookId, type);
            throw new RuntimeException("Too many files for " + bookId + " in format " + type);
        }

        return (String) rows.get(0).get("file_name");
    }

    @Override
    public File addBook(String bookId, String sourceFormat, File sourceFile) {
        String storageFileName = makeStorageFileName(bookId, sourceFormat, sourceFile);
        boolean hasToCompress = sourceFormat.equals("fb2");
        if (hasToCompress) {
            storageFileName = storageFileName + ".zip";
        }


        File storageFile = new File(fileStoragePath, storageFileName);
/*
        while (storageFile.exists()) { // avoid name collision
            storageFileName = storageFile.getName().substring(0, 1) + storageFile.getName();
            storageFile = new File(storageFile.getParentFile(), storageFileName);
        }
*/
        makeDirs(storageFile);

        if (!storageFile.equals(sourceFile)) {
            if (!storageFile.exists()) {
                if (!hasToCompress) {
                    try {
                        FileUtils.moveFile(sourceFile, storageFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        zipFile(sourceFile, storageFile);
                        sourceFile.delete();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                logger.warn("addBook: file already exists: bookId=" + bookId + " file=" + storageFileName);
                sourceFile.delete();
            }
        }

        getJdbcTemplate().update("insert into book (bookid, format, file_name) values (?, ?, ?)", bookId, sourceFormat, storageFileName);
        return storageFile;
    }

    @Override
    public void deleteBook(final String bookId) {
        getJdbcTemplate().query("select file_name from book where bookid = ?", new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, bookId);
                    }
                },
                new RowCallbackHandler() {
                    @Override
                    public void processRow(ResultSet rs) throws SQLException {
                        String file_name = rs.getString("file_name");
                        FileUtils.deleteQuietly(new File(fileStoragePath, file_name));
                    }
                }
        );
        getJdbcTemplate().update("delete from book where bookid = ?", bookId);
    }


    private void makeDirs(File storageFile) {
        File parentFile = storageFile.getParentFile();
        if (createdDirs.contains(parentFile.getPath())) {
            return;
        }
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        createdDirs.add(parentFile.getPath());
    }

    private String makeStorageFileName(String bookId, String format, File sourceFile) {
        String dirPrefix = new SimpleDateFormat("yyyy/MM/dd/").format(new Date());
        String sourceFileName = sourceFile.getName();
        String baseName = FilenameUtils.getBaseName(sourceFileName).replaceAll("[^\\p{Alnum}\\.\\_\\-]", "");
        return dirPrefix + baseName + "." + format;
//        return bookId.substring(0, 2) + "/" + baseName + "." + format;
    }


    public void setFileStoragePath(String fileStoragePath) {
        this.fileStoragePath = new File(fileStoragePath);
    }


    private File zipFile(File source, File target) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(target));
        zipOutputStream.setLevel(ZipOutputStream.DEFLATED);

        FileInputStream fileInputStream = new FileInputStream(source);
        try {
            byte[] buffer = new byte[8 * 1024];
            ZipEntry entry = new ZipEntry(source.getName());
            zipOutputStream.putNextEntry(entry);
            int read;
            while ((read = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
            IOUtils.closeQuietly(zipOutputStream);
        }
        return target;
    }

}
