package org.archiviststoolkit.plugin.utils;

import bsh.Interpreter;
import com.thoughtworks.xstream.XStream;
import org.archiviststoolkit.model.ATPluginData;
import org.archiviststoolkit.mydomain.DomainAccessObject;
import org.archiviststoolkit.mydomain.DomainAccessObjectFactory;
import org.archiviststoolkit.plugin.ScriptAT;

import java.io.*;
import java.util.Collection;

/**
 * This utility class defines methods for saving script data to the database
 * or to the file system
 *
 * <p/>
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Oct 8, 2011
 * Time: 8:35:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptDataUtils {
    public static final String DATA_TYPE = "Script Data";

    /**
     * Method to get an object that was save to the database by a script
     *
     * @param dataName
     * @return
     */
    public static Object getScriptData(String dataName) {
        ATPluginData pluginData = getPluginData(dataName);

        if(pluginData != null) {
            XStream xstream = new XStream();
            return xstream.fromXML(pluginData.getDataString());
        } else {
            return null;
        }
    }

    /**
     * Method to get script data that was saved to a binary file
     * 
     * @param file
     * @return
     */
    public static Object getScriptData(File file) {
        FileInputStream fis = null;
		ObjectInputStream in = null;

        try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			Object object = in.readObject();
			in.close();

            return object;
		} catch (IOException ex) {
            System.out.println("Error opening file " + file.getName());
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}

        // we got here so just return null
        return null;
    }

    /**
     * Method to save script data to a binary file
     * 
     * @param file
     * @param scriptData
     * @throws Exception
     */
    public static void saveScriptData(File file, Object scriptData) throws Exception {
        FileOutputStream fos = null;
		ObjectOutputStream out = null;

	    fos = new FileOutputStream(file);
		out = new ObjectOutputStream(fos);
		out.writeObject(scriptData);
		out.close();
    }

     /**
     * Method to save script data to the database using the ATplugni data model
     *
     * @param dataName
     * @param scriptData
     */
    public static void saveOrUpdateScriptData(String dataName, Object scriptData) throws Exception {
        try {
            // first convert the scriptData object to the xml representation
            XStream xstream = new XStream();
            String dataString = xstream.toXML(scriptData);

            // first see if the plugin data already exist
            ATPluginData pluginData = getPluginData(dataName);

            if(pluginData == null) { // create a new one for saving
                pluginData = new ATPluginData(ScriptAT.PLUGIN_NAME, true, 1,
                        dataName, DATA_TYPE, dataString);
            } else {
                pluginData.setDataString(dataString);
            }

            // now save the the plugin data object
            DomainAccessObject access =
                    DomainAccessObjectFactory.getInstance().getDomainAccessObject(ATPluginData.class);
            access.update(pluginData);
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Method to return a plugin data object from the data name.
     * This method should really be part of the ATPlugin util.
     *
     * @param dataName
     * @return
     */
    public static ATPluginData getPluginData(String dataName) {
        try {
            DomainAccessObject access =
                    DomainAccessObjectFactory.getInstance().getDomainAccessObject(ATPluginData.class);
            ATPluginData pluginData = new ATPluginData();
            pluginData.setPluginName(ScriptAT.PLUGIN_NAME);
            pluginData.setDataName(dataName);
            Collection collection = access.findByExample(pluginData);

            // get the plugin data object returned from the database.
            // only return the first one is used
            if (collection != null && collection.size() != 0) {
                Object[] dataFound = collection.toArray();
                pluginData = (ATPluginData) dataFound[0];
                return pluginData;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to delete data that was saved to the database by a script
     *
     * @param dataName
     */
    public static void deleteData(String dataName) throws Exception {
        try {
            ATPluginData pluginData = getPluginData(dataName);

            // if the plugin data is not null then delete
            if(pluginData != null) {
                DomainAccessObject access =
                    DomainAccessObjectFactory.getInstance().getDomainAccessObject(ATPluginData.class);
                access.delete(pluginData);
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
