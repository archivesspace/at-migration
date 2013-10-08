package org.archiviststoolkit.plugin.rmi;

import com.sun.rowset.CachedRowSetImpl;
import javax.sql.rowset.CachedRowSet;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.sql.*;

/**
 * This starts up and RMI Server which is used to provide access
 * to a Access Database over TCP-IP
 *
 * Created by IntelliJ IDEA.
 *
 * @author: Nathan Stevens
 * Date: Oct 6, 2009
 * Time: 10:56:01 AM
 */
public class AccessTalkImpl extends UnicastRemoteObject implements AccessTalk {
    static Connection con;
    static Statement stmt;
    static String Connection;
    static String Driver = "sun.jdbc.odbc.JdbcOdbcDriver";
    static String User = "";
    static String Password = "";
    static ResultSet rs;
    static String hostIp = "localhost";

    public AccessTalkImpl() throws RemoteException {
        super();
    }

    public CachedRowSet callSql(String ODBC_Name, String sqlString) throws RemoteException {
        Connection = "jdbc:odbc:" + ODBC_Name;

        try {
            //Load and register the driver and create the connection
            if(con == null) {
                Class.forName(Driver);
                con = DriverManager.getConnection(Connection, User, Password);
            }

            // print out the sql command being executed
            System.out.println("\nSQL Command: " + sqlString + "\n");

            // Create the Statement
            stmt = con.createStatement();

            // put the results of the executeQuery into the ResultSet object rs
            rs = stmt.executeQuery(sqlString);

            CachedRowSetImpl crset = new CachedRowSetImpl();
            crset.populate(rs);

            // return to the client the CachedRowSet object rset
            return crset;
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        catch (SQLException e) {
            System.out.println("SQL Msg1:" + e);
        }
        return (null);
    }

    public static void main(String[] arg) {
        try {
            AccessTalkImpl accessTalkImpl = new AccessTalkImpl();
            Naming.rebind("rmi://" + hostIp + "/AccessTalk", accessTalkImpl);
            System.out.println("Started AccessTalk Server 1.01 ...");
        }
        catch (Exception e) {
            System.out.println("AccessTalkImpl: " + e);
        }
    }
}
