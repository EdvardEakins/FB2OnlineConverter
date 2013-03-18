package net.flibusta.persistence.dao.impl;

import net.flibusta.persistence.dao.BatchDao;
import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BatchDaoSql extends JdbcDaoSupport implements BatchDao {


    private File fileStoragePath;

    private Set<String> createdDirs = Collections.synchronizedSet(new HashSet<String>(512));


    @Override
    public String findBatchPath(String batchId, String batchFormat) {
        List<Map<String, Object>> rows = getJdbcTemplate().queryForList("select file_name from batch where batchid = ? and format = ?", batchId, batchFormat);
        if (rows.size() == 0) {
            return null;
        }
        if (rows.size() > 1) {
            getJdbcTemplate().update("delete from batch where batchid = ? and format = ?", batchId, batchFormat);
            throw new RuntimeException("Too many files in batch table for " + batchId + " in format " +  batchFormat);
        }

        String file_name = (String) rows.get(0).get("file_name");
        if (new File(fileStoragePath, file_name).exists()) {
            return file_name;
        } else {
            // file missing
            getJdbcTemplate().update("delete from batch where batchid = ? and format = ?", batchId, batchFormat);
            return null;
        }
    }

    @Override
    public File addBatch(String batchId, String batchFormat, File batchFile) {

//        String storageFileName = batchId + ".zip";
        String storageFileName = makeStorageFileName(batchId, batchFormat, batchFile);
        File storageFile = new File(fileStoragePath, storageFileName);

        makeDirs(storageFile);

        if (!storageFile.equals(batchFile)) {
            if (!storageFile.exists()) {
                try {
                    FileUtils.moveFile(batchFile, storageFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.warn("addBook: file already exists: bookId=" + batchId + " file=" + storageFileName);
                batchFile.delete();
            }
        }

        getJdbcTemplate().update("insert into batch (batchid, format, file_name) values (?, ?, ?)", batchId, batchFormat, storageFileName);
        return storageFile;
    }

    @Override
    public void delete(String batchId, String batchFormat) {
        String batchPath = findBatchPath(batchId, batchFormat);
        if (batchPath != null) {
            new File(fileStoragePath, batchPath).delete();
        }
        getJdbcTemplate().update("delete from batch where batchid = ? and format = ?", batchId, batchFormat);
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
        String dirPrefix = new SimpleDateFormat("'batch/'yyyy/MM/dd/").format(new Date());
        return dirPrefix + bookId + ".zip";
    }


    public void setFileStoragePath(String fileStoragePath) {
        this.fileStoragePath = new File(fileStoragePath);
    }

}
