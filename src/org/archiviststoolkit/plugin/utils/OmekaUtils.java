package org.archiviststoolkit.plugin.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import javax.security.auth.login.FailedLoginException;

/**
 * Created by IntelliJ IDEA.
 * User: nathan stevens
 * Date: 2/6/12
 * Time: 10:48 AM
 *
 * This class provides utility method for connecting to Omeka instance
 */
public class OmekaUtils {
    public static String omekaAdminSite = ""; // location of omeka admin site
    public static String pluginUrl = ""; // location of ATDropbox plugin
    public static HttpClient httpClient = null;

    /**
     * The default constructor
     *
     * @param omekaAdminSite
     */
    public OmekaUtils(String omekaAdminSite) {
        this.omekaAdminSite = omekaAdminSite;
    }

    public static boolean connectToOmeka(String username, String password) {
        String loginUrl = omekaAdminSite + "users/login";

        httpClient = new HttpClient();

        try {
            PostMethod authPost = new PostMethod(loginUrl);
            // authPost.setFollowRedirects(false);
            NameValuePair[] data = {
                new NameValuePair("username", username),
                new NameValuePair("password", password)
            };

            authPost.setRequestBody(data);
            int status = httpClient.executeMethod(authPost);    // 302, redirect to original requested page
            authPost.releaseConnection();

            if(status == 302) {
                pluginUrl = omekaAdminSite + "at-dropbox/index/";
                System.out.println("Successfully logged into Omeka @ " + loginUrl + " ...");
                return true;
            } else {
                System.out.println("Failed to log into Omeka @ " + loginUrl + " ...");
                httpClient = null;
                return false;
            }

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String addItem(String title, String tags, String collectionId) throws Exception {
        NameValuePair[] data = {
                            new NameValuePair("at_dropbox-title", title),
                            new NameValuePair("at_dropbox-collection-id", collectionId),
                            new NameValuePair("at_dropbox-tags", tags)
                        };
        
        return addItem(data);
    }

    public static String addItem(NameValuePair[] data) throws Exception {
        String url = pluginUrl + "additem";

        PostMethod addPost = new PostMethod(url);
        addPost.setRequestBody(data);
        int status = httpClient.executeMethod(addPost);
        String response = addPost.getResponseBodyAsString();
        addPost.releaseConnection();

        System.out.println("Status " + status  + "\n" + response);

        return response;
    }

    /**
     * Method to add a collection to the database
     * @param name
     * @param description
     * @return
     * @throws Exception
     */
    public static String addCollection(String name, String description, String parentId) throws Exception {
        NameValuePair[] data = {
            new NameValuePair("at_dropbox-name", name),
            new NameValuePair("at_dropbox-description", description),
            new NameValuePair("at_dropbox-parentId", parentId)
        };

        return addCollection(data);
    }

    /**
     * Method to do a post to create a collection
     *
     * @param data
     * @throws Exception
     */
    public static String addCollection(NameValuePair[] data) throws Exception {
        String url = pluginUrl + "addcollection";

        PostMethod addPost = new PostMethod(url);
        addPost.setRequestBody(data);
        int status = httpClient.executeMethod(addPost);
        String response = addPost.getResponseBodyAsString();
        addPost.releaseConnection();

        System.out.println("Status " + status  + "\n" + response);

        return response;
    }

    /**
     * Method to add a file to the at-dropbox folder
     *
     * @param file
     * @throws Exception
     */
    public static String addFile(File file) throws Exception {
        String url = pluginUrl + "addfile"; 
        PostMethod filePost = new PostMethod(url);

        Part[] parts = {
            new StringPart("item_id", "test"),
            new FilePart("at_dropbox-file", file)
        };

        filePost.setRequestEntity(
                new MultipartRequestEntity(parts, filePost.getParams())
        );

        int status = httpClient.executeMethod(filePost);
        String response = filePost.getResponseBodyAsString();

        System.out.println("Status: " + status + "\n" + response);

        return response;
    }

    /**
     * Method to clean out the database of collections and items.
     * should only be used in development.
     *
     * @return string contain list of items that were deleted
     */
    public static String cleanDatabase() throws Exception {
        String url = pluginUrl + "clean";

        PostMethod addPost = new PostMethod(url);
        int status = httpClient.executeMethod(addPost);
        String response = addPost.getResponseBodyAsString();
        addPost.releaseConnection();

        System.out.println("Status " + status  + "\n" + response);

        return response;
    }

    public static void main(String[] args) {
        omekaAdminSite = "http://quanta2.bobst.nyu.edu:9090/omeka/admin/";
        connectToOmeka("admin", "none100");

        // check to see if we logged in correctly
        if(httpClient != null) {
            try {
                cleanDatabase();

                //for(int i = 1; i <= 8; i++) {
                    //addCollection("Child Collection " + i, "Child Description " + i, "" + 1);

                    /*
                    for(int j = 1; j <= 25; j++) {
                         NameValuePair[] data = {
                            new NameValuePair("at_dropbox-title", "Test Tile " + j),
                            new NameValuePair("at_dropbox-collection-id", "" + i),
                            new NameValuePair("at_dropbox-tags", "tag1, tag2, tag3, tag4"),
                            new NameValuePair("at_dropbox-url[]", "http://www.cnn.com"),
                            new NameValuePair("at_dropbox-url[]", "http://www.cnn.com")
                        };

                        addItem(data);
                    }*/
                //}

                //addCollection("Test Collection 11", "Test Description"
                //File file = new File("/home/nathan/HSP1.sql");
                //addFile(file);

                // test creation of an item
                NameValuePair[] data = {
                    new NameValuePair("at_dropbox-title", "Test Tile"),
                    new NameValuePair("at_dropbox-collection-id", "1"),
                    new NameValuePair("at_dropbox-tags", "tag1, tage2, tag3, tag4"),
                    new NameValuePair("at_dropbox-url[]", "http://www.cnn.com"),
                    new NameValuePair("at_dropbox-url[]", "http://www.cnn.com")
                };

                //addItem(data);

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
