package framework.utils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.openspaces.admin.gsa.GridServiceOptions;
import org.openspaces.admin.machine.Machine;


public class DBUtils {

    /**
     * Loads 1 HSQL on the specified machine listening on the specified port.
     */
    public static int loadHSQLDB(Machine machine, String dbName, long port) {
        return loadHSQLDB(machine, dbName, port,null);
    }
    
    /**
     * Loads 1 HSQL on the specified machine listening on the specified port.
     * places the database file in the specified directory.
     * Use different directories to isolate tests.
     * @param machine - the machine to start the database
     * @param dbName - the database name
     * @param port - the database port
     * @param workDirectory - the directory to read/write the database file.
     * @return the database process id.
     */
    public static int loadHSQLDB(Machine machine, String dbName, long port, File workDirectory) {
    	String dbFilename = dbName;
    	if (workDirectory != null) {
    		dbFilename = "file:"+new File(workDirectory,dbName).getAbsolutePath();
    	}
    	return machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
    					new GridServiceOptions("hsqldb")
    						.argument("-database").argument(dbFilename)
    						.argument("-port").argument(String.valueOf(port)));
    }

    /**
     * return an available port at given machine
     */
    public static int getAvailablePort(Machine machine) {
        for (int i = 9000; i < 10000; i++) {
            try {
                ServerSocket srv = new ServerSocket(i);
                srv.close();
                srv = null;
                return i;
            } catch (IOException e) {
            }
        }
        throw new RuntimeException("Cant find open port at machine " + machine.getHostName());
    }

    /**
     * return an available port as java.lang.String at given machine
     */
    public static String getAvailablePortAsString(Machine machine) {
        return String.valueOf(getAvailablePort(machine));
    }

    public static ResultSet runSQLQuery(String sql, String ip, int port, String username, String passward) {
        try {
            Class.forName("org.hsqldb.jdbcDriver").newInstance();
            String url = "jdbc:hsqldb:hsql://" + ip + ":" + port;
            Connection connection = DriverManager.getConnection(url, username, passward);
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query:" + sql, e);
        }
    }
    public static ResultSet runSQLQuery(String sql, String ip, int port){
          return runSQLQuery(sql, ip, port, "sa", null);
    }
}
