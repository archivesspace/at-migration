package org.archiviststoolkit.plugin.utils.aspace;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/6/12
 * Time: 3:59 PM
 *
 * This class handles all posting and reading from the Record Test Servlet
 */
public class RecordTestServletClient {
    public static final String DEFAULT_URL = "http://variations1.bobst.nyu.edu:8080/SchemaMap/RecordTestServlet";

    private HttpClient httpclient = new HttpClient();

    private ASpaceClient aspaceClient; // used when testing multiple records

    private String host = "";
    private String username = "";
    private String password = "";

    // boolean to use when one once debug stuff
    private boolean debug = true;

    /**
     * default constructor
     */
    public RecordTestServletClient() {
        host = DEFAULT_URL;
    }

    /**
     * The main constructor
     *
     * @param host
     * @param username
     * @param password
     */
    public RecordTestServletClient(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

        /**
     * Method to store a record for testing
     *
     * @param jsonText
     * @return
     */
    public String storeRecord(String jsonText) throws Exception {
        String message = "";

        // check to see if we not testing

        // we need to try and create a JSON object and add a field named task and set it to store-AT
        JSONObject jsonObject = new JSONObject(jsonText);
        jsonObject.put("task", "store-AT");

        message = post(jsonObject, null);

        return message;
    }

    /**
     * Method to store multiple records for testing
     *
     * @param jsonArrayText
     * @return
     * @throws Exception
     */
    public String storeMultipleRecords(String jsonArrayText) throws Exception {
        String message = "";

        JSONArray recordsJA = new JSONArray(jsonArrayText);

        for(int i = 0; i < recordsJA.length(); i++) {
            String endpoint = recordsJA.getString(i);
            String jsonText = aspaceClient.get(endpoint, null);

            message += storeRecord(jsonText) + "\n";
        }

        return message;
    }

    /**
     * Method to test a record against a stored record
     *
     * @param jsonText
     * @return
     * @throws Exception
     */
    public String testRecord(String jsonText) throws Exception {
        String message = "";

        // we need to try and create a JSON object and add a field named task and set it to store-AT
        JSONObject jsonObject = new JSONObject(jsonText);
        jsonObject.put("task", "test-AT");

        message = post(jsonObject, null);

        return message;
    }

    /**
     * Method to store multiple records for testing
     *
     * @param jsonArrayText
     * @return
     * @throws Exception
     */
    public String testMultipleRecords(String jsonArrayText) throws Exception {
        String message = "";

        JSONArray recordsJA = new JSONArray(jsonArrayText);

        for(int i = 0; i < recordsJA.length(); i++) {
            String endpoint = recordsJA.getString(i);
            String jsonText = aspaceClient.get(endpoint, null);

            message += testRecord(jsonText) + "\n";
        }

        return message;
    }

    /**
     * Method to do a post to the json
     *
     * @param json The json record to store or test
     * @return
     */
    public String post(JSONObject json, NameValuePair[] params) throws Exception {
        // Prepare HTTP post method.
        String fullUrl = host;
        PostMethod post = new PostMethod(fullUrl);
        post.setRequestEntity(new StringRequestEntity(json.toString(), "application/json", null));

        // set any parameters
        if(params != null) {
            post.setQueryString(params);
        }

        return executePost(post);
    }

    /**
     * Method to actually execute the post method
     *
     * @param post
     * @return The id or session
     *
     * @throws Exception
     */
    private String executePost(PostMethod post) throws Exception {
        String message = "";

        // Execute request
        try {
            int statusCode = httpclient.executeMethod(post);

            // Display status code
            String statusMessage = "Status code: " + statusCode +
                    "\nStatus text: " + post.getStatusText();

            if (debug) System.out.println(statusMessage);

            // Display response
            String responseBody = post.getResponseBodyAsString();

            if (debug) {
                System.out.println("Response body: ");
                System.out.println(responseBody);
            }

            // if status code doesn't equal to success throw exception
            if (statusCode == HttpStatus.SC_OK) {
                if (debug) System.out.println(responseBody);
                message = responseBody;
            } else {
                post.releaseConnection();
                throw new Exception(statusMessage);
            }
        } finally {
            // Release current connection to the server
            post.releaseConnection();
        }

        return message;
    }

    /**
     * Method to get the params from a comma separated string
     *
     * @param paramString
     * @return
     */
    private  NameValuePair[] getParams(String paramString) {
        String[] parts = paramString.split("\\s*,\\s*");

        // make sure we have parameters, otherwise exit
        if(paramString.isEmpty() || parts.length < 1) {
            return null;
        } else {
            NameValuePair[] params = new NameValuePair[parts.length];

            for(int i = 0; i < parts.length; i++) {
                try {
                    String[] sa = parts[i].split("\\s*=\\s*");
                    params[i] = new NameValuePair(sa[0], sa[1]);
                } catch(Exception e) {
                    return null;
                }
            }

            return params;
        }
    }

    /**
     * Method to set the aspace client for getting records for testing
     *
     * @param aspaceClient
     */
    public void setASpaceClient(ASpaceClient aspaceClient) {
        this.aspaceClient = aspaceClient;
    }
}
