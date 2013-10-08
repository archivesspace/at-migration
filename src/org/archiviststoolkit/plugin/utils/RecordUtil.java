package org.archiviststoolkit.plugin.utils;

import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.*;
import org.archiviststoolkit.structure.NotesEtcTypes;

import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 8/1/12
 * Time: 11:13 AM
 *
 * Simple class that allows saving of records when plugin is used within the AT
 */
public class RecordUtil {
    // The DAO objects for reading and saving records
    private DomainAccessObject noteTypesDAO;

    private DomainAccessObject usersDAO;

    private DomainAccessObject repositoriesDAO;

    private DomainAccessObject locationsDAO;

    private ResourcesDAO resourcesDAO;

    private NamesDAO namesDAO;

    private SubjectsDAO subjectsDAO;

    AccessionsDAO accessionsDAO;

    DigitalObjectDAO digitalObjectsDAO;

    /**
     * Main constructor
     */
    public RecordUtil() throws Exception {
        locationsDAO = DomainAccessObjectFactory.getInstance().getDomainAccessObject(Locations.class);

        repositoriesDAO = DomainAccessObjectFactory.getInstance().getDomainAccessObject(Repositories.class);

        usersDAO = DomainAccessObjectFactory.getInstance().getDomainAccessObject(Users.class);

        noteTypesDAO = DomainAccessObjectFactory.getInstance().getDomainAccessObject(NotesEtcTypes.class);

        resourcesDAO = new ResourcesDAO();

        namesDAO = new NamesDAO();
        namesDAO.getLongSession();

        subjectsDAO = new SubjectsDAO();
        subjectsDAO.getLongSession();

        accessionsDAO = new AccessionsDAO();
        accessionsDAO.getLongSession();

        digitalObjectsDAO = new DigitalObjectDAO();
        digitalObjectsDAO.getLongSession();
    }

    /**
     * Method to save a domain object
     * @param record The domain object to save
     */
    public void saveRecord(DomainObject record, boolean closeSession) throws Exception {
        System.out.println("Implement saving please ...");
    }

    /**
     * Method to return the notes types current in the AT database
     *
     * @return
     */
    public ArrayList<NotesEtcTypes> getNoteTypes() {
        try {
            ArrayList<NotesEtcTypes> noteTypes = (ArrayList<NotesEtcTypes>) noteTypesDAO.findAll();
            return noteTypes;
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to return the resource current in the AT database
     *
     * @return
     */
    public ArrayList<Resources> getResources() {
        try {
            ArrayList<Resources> resources = (ArrayList<Resources>) resourcesDAO.findAll();
            return resources;
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to return the repository stored by primary key
     *
     * @param key
     * @return
     * @throws Exception
     */
    public Repositories getRepository(Long key) {
        try {
            return (Repositories)repositoriesDAO.findByPrimaryKeyLongSession(key);
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a location
     * @param key
     * @return
     */
    public Locations getLocation(Long key) {
        try {
            return (Locations)locationsDAO.findByPrimaryKeyLongSession(key);
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a Subject object
     * @param key
     * @return
     */
    public Subjects getSubject(Long key) {
        try {
            return (Subjects)subjectsDAO.findByPrimaryKeyLongSession(key);
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a Name object
     * @param key
     * @return
     */
    public Names getName(Long key) {
        try {
            return (Names)namesDAO.findByPrimaryKeyLongSession(key);
        } catch (LookupException e) {
            e.printStackTrace();
            return null;
        }
    }
}
