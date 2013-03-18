package net.flibusta.persistence;

import org.apache.derby.jdbc.EmbeddedDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DerbyEmbeddedDataSourceFactory  {

    private  EmbeddedDataSource dataSource = new EmbeddedDataSource();

    private String user;
    private String password;
    private String databaseName;

    public DataSource createDataSource() {
        dataSource.setCreateDatabase("create");
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setDatabaseName(databaseName);
        return dataSource;
    }

    public void shutdown() throws SQLException {
        dataSource.setShutdownDatabase("shutdown");
        dataSource.getConnection();
    }


    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}
