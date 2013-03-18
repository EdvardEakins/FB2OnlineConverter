package net.flibusta.persistence.dao;

import java.io.File;

public interface BatchDao {
    String findBatchPath(String batchId, String batchFormat);
    File addBatch(String batchId, String batchFormat, File batchFile);

    void delete(String batchId, String batchFormat);
}
