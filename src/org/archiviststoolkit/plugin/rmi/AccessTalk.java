package org.archiviststoolkit.plugin.rmi;

import javax.sql.rowset.CachedRowSet;
import java.rmi.Remote;
import java.sql.ResultSet;

/**
 *
 * A simple interface for making SQL calls to an Access Database
 *
 * Created by IntelliJ IDEA.
 *
 * @author: Nathan Stevens
 * Date: Oct 6, 2009
 * Time: 10:27:28 AM
 */
public interface AccessTalk extends Remote {
    public CachedRowSet callSql(String ODBC_Name, String sqlString)
        throws java.rmi.RemoteException;
    
}
