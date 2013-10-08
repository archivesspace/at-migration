package org.archiviststoolkit.plugin.utils;

import com.thoughtworks.xstream.XStream;
import org.archiviststoolkit.model.ATPluginData;
import org.archiviststoolkit.mydomain.DomainAccessObject;
import org.archiviststoolkit.mydomain.DomainAccessObjectFactory;
import org.archiviststoolkit.plugin.ATPluginUtils;
import org.archiviststoolkit.plugin.ScriptAT;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple DAO class that allows storing of scripts on the backend database
 * when running in stand alone mode
 *
 * Created by IntelliJ IDEA.
 *
 * @author: Nathan Stevens
 * Date: Jul 27, 2011
 * Time: 11:16:15 AM
 */
public class ScriptsDAO {
    private Session session = null;

    // This stores hashmap containing the stored scripts
    private ATPluginData pluginData = null;

    // contains the names of the built in scripts aka commands
    private static String[] builtInScriptNames = {
            "batchSearch.bsh", "doCreator.bsh"
    };

    /**
     * Constructor called when running within the AT
     * 
     * @throws Exception
     */
    public ScriptsDAO() throws Exception {
        Class clazz = ATPluginData.class;
        DomainAccessObject access =
                DomainAccessObjectFactory.getInstance().getDomainAccessObject(clazz);
        session = access.getLongSession();
    }

    /**
     * The constructor called when running in stand alone mode
     *
     * @param session
     */
    public ScriptsDAO(Session session) {
        this.session = session;
    }

    /**
     * Method to load the hashmap containing stored scripts
     */
    public HashMap<String, String> loadScripts() throws Exception {
        HashMap<String, String> storedScripts = new HashMap<String,String>();

        // load the scripts from the database
        final java.util.List completeList;
        Transaction tx = null;

        tx = session.beginTransaction();
        Criteria criteria = session.createCriteria(ATPluginData.class);
        criteria.add(Restrictions.eq("dataName", ScriptAT.DATA_NAME));
        criteria.add(Restrictions.eq("pluginName", ScriptAT.PLUGIN_NAME));

        // find the data we need
        completeList = criteria.list();
        tx.commit();

        // now get the first data element
        if (completeList.size() > 0) {
            pluginData = (ATPluginData) completeList.get(0);
            Object object = ATPluginUtils.getObjectFromPluginData(pluginData);
            storedScripts = (HashMap<String, String>) object;
        }

        // now load any built in scripts from the classpath
        loadBuiltInScripts(storedScripts);

        return storedScripts;
    }

    /**
     * Method to load the built in scripts aka commands. It will always
     * load the version in the classpath even though a version has already
     * been stored in the database. This is to allow updates to be automatically
     * be available if the plugin is updated
     *
     * @param storedScripts
     */
    private void loadBuiltInScripts(HashMap<String, String> storedScripts) {
        for(int i = 0; i < builtInScriptNames.length; i++) {
            String scriptName = builtInScriptNames[i];
            String scriptText = getTextForBuiltInScript(scriptName);
            storedScripts.put(scriptName, scriptText);
        }
    }

    /**
     * Method to return the String of a script that is stored in the classpath. This just calls the
     * default package where scripts are stored relative to the ScriptAT class
     *
     * @param scriptName
     * @return
     */
    public static String getTextForBuiltInScript(String scriptName) {
        return getTextForBuiltInScript("commands/", scriptName);
    }

    /**
     * Method to read in a text file to a string from the classpath
     * 
     * @param scriptName
     * @return
     */
    public static String getTextForBuiltInScript(String packageName, String scriptName) {
        try {
            InputStream is = ScriptAT.class.getResourceAsStream(packageName + scriptName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            is.close();

            return sb.toString();
        } catch(Exception e) {
            e.printStackTrace();
            return "print (\"Error Loading Built In Script\")";
        }
    }

    /**
     * Method to load scripts by a category. The category is specified
     * using annotation within the script. The names of the scripts are
     * returned in an array list
     *
     * @param category
     * @return
     * @throws Exception
     */
    public ArrayList<String> loadScriptsByCategory(String category) throws Exception {
        HashMap<String,String> scripts = loadScripts();
        ArrayList<String> foundScripts = new ArrayList<String>();

        if(scripts != null && scripts.size() > 0) {
            for(String scriptName: scripts.keySet()) {
                if(category == null) { // no category so add it
                    foundScripts.add(scriptName);
                } else {
                    String scriptText = scripts.get(scriptName);
                    if(scriptText.contains(category)) {
                        foundScripts.add(scriptName);
                    }
                }
            }
        }

        return foundScripts;
    }

    /**
     *  Method to save the hashmap containing the scripts to the database
     */
    public void saveScripts(HashMap<String,String> storedScripts) throws Exception {
        Transaction tx = null;
        try {
            // use Xstream to convert the java object to an xml string
            XStream xstream = new XStream();
            String dataString = xstream.toXML(storedScripts);

            if(pluginData != null) { // update the existing plugin data
               pluginData.setDataString(dataString);
            } else { // create new ATPlugin data object
               pluginData = new ATPluginData(ScriptAT.PLUGIN_NAME, true, 1,
                        ScriptAT.DATA_NAME, ScriptAT.DATA_NAME, dataString);
            }

            // update the plugin data object now
            tx = session.beginTransaction();
			session.saveOrUpdate(pluginData);
			tx.commit();
        } catch(Exception e) {
            try {
				tx.rollback();
			} catch (HibernateException he) {
				he.printStackTrace();
			}

            throw e;
        }
    }
}
