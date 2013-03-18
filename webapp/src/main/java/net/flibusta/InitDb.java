package net.flibusta;

import org.apache.commons.io.FileUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InitDb {


    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("path to database missing");
            return;
        }
        String dbPath = args[0];

        System.setProperty("catalina.base", "/tmp");
        new File("tmp/logs").mkdirs();

        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("");
        dataSource.setDatabaseName(dbPath);
        dataSource.setCreateDatabase("create");

        Connection connection = dataSource.getConnection();

        URL schema = InitDb.class.getClassLoader().getResource("schema.sql");
        String sql = FileUtils.readFileToString(new File(schema.getFile()));
        String[] commands = sql.split(";");
        Pattern pattern = Pattern.compile("create table (\\S+)");
        for (String command : commands) {
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                String tableName = matcher.group(1).toUpperCase();

                ResultSet books = connection.getMetaData().getTables(null, null, tableName, null);
                if (!books.next()) {
                    System.out.println("create table " + tableName);
                    Statement statement = connection.createStatement();
                    statement.execute(command);
                    statement.close();
                }
            }
        }

        connection.close();


    }
}
