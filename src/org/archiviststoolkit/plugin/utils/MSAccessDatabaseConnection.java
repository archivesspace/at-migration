package org.archiviststoolkit.plugin.utils;

import bsh.Interpreter;
import org.archiviststoolkit.plugin.rmi.AccessTalk;
import javax.sql.rowset.CachedRowSet;
import javax.sql.RowSet;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.net.MalformedURLException;
import java.sql.*;

import com.sun.rowset.CachedRowSetImpl;

/**
 * This is a class which allows access a MS access database
 * either local or through the RMI server component which allows
 * remote access to the database
 *
 * @author: Nathan Stevens
 * Date: Oct 6, 2009
 * Time: 1:34:53 PM
 */
public class MSAccessDatabaseConnection {
    private Connection con;
    private Statement stmt;
    private String Connection;
    private String Driver = "sun.jdbc.odbc.JdbcOdbcDriver";
    private String User = "";
    private String Password = "";
    private ResultSet rs;
    private String hostIp = "localhost";
    private AccessTalk accessTalk = null;
    private String odbcName = "";
    private Interpreter interpreter = null; // The bean shell interpreter

    public MSAccessDatabaseConnection(String odbcName, Interpreter interpreter) {
        this.odbcName = odbcName;
        this.interpreter = interpreter;
    }

    // Method to test the connection to the access database, either locally or
    // through RMI
    public boolean testConnection(String testSQL, String columnName) {
        // check to see if a hostname wasn't included as part of the odbc name
        // if it was then connect through RMI, otherwise do local connection
        if (odbcName.indexOf(":") == -1) {
            interpreter.print("Connecting locally ...\n\n");

            Connection = "jdbc:odbc:" + odbcName;

            try {
                // Load and register the driver and create the connection
                if (con == null) {
                    Class.forName(Driver);
                    con = DriverManager.getConnection(Connection, User, Password);
                }

                // Create the Statement
                stmt = con.createStatement();

                // put the results of the executeQuery into the ResultSet object rs
                rs = stmt.executeQuery(testSQL);

                // alert the user that they successfully connected
                interpreter.print("Connected ...\n");

                if(columnName != null) {
                    while (rs.next()) {
                        interpreter.print(rs.getString(columnName) + "\n");
                    }
                }

                return true;
            }
            catch (ClassNotFoundException e) {
                interpreter.print(e.getMessage());
            }
            catch (SQLException e) {
                interpreter.print("SQL Msg1:" + e);
            }
        } else {
            String[] info = odbcName.split(":");

            interpreter.print("Connecting Remotely to " + info[0] + " ...\n\n");

            try {
                AccessTalk accessTalk = (AccessTalk)
                        Naming.lookup("rmi://" + info[0] + "/AccessTalk");

                RowSet rowSet = accessTalk.callSql(info[1], testSQL);

                interpreter.print("Connected ...\n");

                if(columnName != null) {
                    while (rowSet.next()) {
                        interpreter.print(rowSet.getString(columnName) + "\n");
                    }
                }

                return true;
            }
            catch (MalformedURLException murle) {
                interpreter.print("Malformed URL Exception\n");
                murle.printStackTrace();
            }
            catch (RemoteException re) {
                interpreter.print("Remote Exception\n");
                re.printStackTrace();
            }
            catch (NotBoundException nbe) {
                interpreter.print("Not Bound Exception\n");
                nbe.printStackTrace();
            }
            catch (SQLException sqe) {
                interpreter.print("SQL Exception\n");
                sqe.printStackTrace();
            }
        }

        return false;
    }

    // Method to send sql commands to the access database either running locally or
    // through RMI server component
    public CachedRowSet sendSQLCommands(String sqlCommand) {
        // check to see if a host name wasn't included as part of the odbc name
        // if it was then connect through RMI, otherwise do local connection
        if (odbcName.indexOf(":") == -1) {
            interpreter.print("Connecting locally ...\n");

            try {
                Connection = "jdbc:odbc:" + odbcName;

                // Load and register the driver and create the connection
                if (con == null) {
                    Class.forName(Driver);
                    con = DriverManager.getConnection(Connection, User, Password);
                }

                // print out the sql command being executed
                interpreter.print("\nSQL Command: " + sqlCommand + "\n");

                // Create the Statement
                stmt = con.createStatement();

                // put the results of the executeQuery into the ResultSet object rs
                rs = stmt.executeQuery(sqlCommand);

                CachedRowSetImpl crset = new CachedRowSetImpl();
                crset.populate(rs);

                // return to the client the CachedRowSet object rset
                return crset;
            }
            catch (ClassNotFoundException e) {
                interpreter.print(e.getMessage());
            }
            catch (SQLException e) {
                interpreter.print("SQL Msg1:" + e);
            }
            return (null);


        } else {
            String[] info = odbcName.split(":");

            try {
                if (accessTalk == null) {
                    accessTalk = (AccessTalk)
                            Naming.lookup("rmi://" + info[0] + "/AccessTalk");
                }

                return accessTalk.callSql(info[1], sqlCommand);
            }
            catch (MalformedURLException murle) {
                interpreter.print("Malformed URL Exception\n");
                murle.printStackTrace();
            }
            catch (RemoteException re) {
                interpreter.print("Remote Exception\n");
                re.printStackTrace();
            }
            catch (NotBoundException nbe) {
                interpreter.print("Not Bound Exception\n");
                nbe.printStackTrace();
            }
        }

        return null; // if something went wrong, return null
    }
}
