package org.archiviststoolkit.plugin.utils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import net.sf.beanlib.hibernate.HibernateBeanReplicator;
import net.sf.beanlib.hibernate3.Hibernate3BeanReplicator;
import net.sf.beanlib.provider.collector.PrivateSetterMethodCollector;
import org.archiviststoolkit.ApplicationFrame;
import org.archiviststoolkit.exceptions.DuplicateLinkException;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.DomainObject;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.structure.NotesEtcTypes;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Mar 24, 2012
 * Time: 9:37:56 AM
 * <p/>
 * This is a test class used to copy object from one database to the next
 */
public class dbCopyUtil {
    // used to get session from the source and destination databases
    private RemoteDBConnectDialogLight sourceRCD;
    private RemoteDBConnectDialogLight destinationRCD;

    // array of remote connection dialogs for running in thread
    private RemoteDBConnectDialogLight[] sourceRCDs;

    // file where the record map is stored
    private static File recordMapFile = null;

    // Main application frame when running within the AT
    private ApplicationFrame mainFrame = null;

    // the main repository of the destination database
    private Repositories destinationRepository = null;

    // used to copy hibernate objects
    private HibernateBeanReplicator mapper = new Hibernate3BeanReplicator().initSetterMethodCollector(new PrivateSetterMethodCollector());

    // hashmap that maps repository from old database with copy in new database
    private HashMap<Long, Repositories> repositoryMap = new HashMap<Long, Repositories>();

    // hashmap that maps locations from old database with copy in new database
    private HashMap<Long, Locations> locationMap = new HashMap<Long, Locations>();

    // hashmap that maps notes types from old and new database
    private HashMap<String, NotesEtcTypes> noteTypeMap = new HashMap<String, NotesEtcTypes>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<Long, Subjects> subjectMap = new HashMap<Long, Subjects>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<Long, Names> nameMap = new HashMap<Long, Names>();

    // hashmap that maps accessions from old database with copy in new database
    private HashMap<Long, Accessions> accessionMap = new HashMap<Long, Accessions>();

    // hashmap that maps resource from old database with copy in new database
    private HashMap<Long, Long> resourceMap = new HashMap<Long, Long>();

    // hashmap that maps resources currently in the destination database
    private HashMap<String, Resources> destinationResourceMap = new HashMap<String, Resources>();

    // hashmap that maps digital objects from old database with copy in new database
    private HashMap<Long, DigitalObjects> digitalObjectMap = new HashMap<Long, DigitalObjects>();

    // stop watch object for keeping tract of time
    private StopWatch stopWatch = null;

    // utility for saving records when running in the AT
    private RecordUtil recordUtil = null;

    // utility to convert object to JSON text
    private XStream xstream = null;

    // File that point to the AT directory
    private File outputDirectory = null;

    // specify debug the boolean
    private boolean debug = false;

    // specify to generate unique ids
    private boolean makeUnique = true;

    // initiate a random string generator
    private RandomString randomString = new RandomString(8);

    // integer to keep track of # of records copied
    private int copyCount = 1;

    // integer that stores the total number of resources being copied
    private int totalResources = 0;

    /**
     * The main constructor, used when running from the command line
     *
     * @param sourceRCD
     * @param destinationRCD
     */
    public dbCopyUtil(RemoteDBConnectDialogLight sourceRCD, RemoteDBConnectDialogLight destinationRCD) {
        this.sourceRCD = sourceRCD;
        this.destinationRCD = destinationRCD;
        init();
    }

    /**
     * Constructor that takes an array of remote connection dialogs
     *
     * @param sourceRCDs
     * @param destinationRCD
     */
    public dbCopyUtil(RemoteDBConnectDialogLight[] sourceRCDs, RemoteDBConnectDialogLight destinationRCD) {
        this.sourceRCDs = sourceRCDs;
        this.sourceRCD = sourceRCDs[0];
        this.destinationRCD = destinationRCD;
        init();
    }

    /**
     * Constructor that only takes source connection. Used when running in the AT
     *
     * @param sourceRCD
     */
    public dbCopyUtil(ApplicationFrame mainFrame, RemoteDBConnectDialogLight sourceRCD) {
        this.mainFrame = mainFrame;
        this.sourceRCD = sourceRCD;
        init();
    }

    /**
     * Method to initiate certain variables that are needed to work
     */
    private void init() {
        print("Starting DB copy ... ");

        // set the file that contains the record map
        recordMapFile = new File(System.getProperty("user.home") + "/recordMaps.bin");

        // set the destination repository, and other fields depending on if we running in stand alone
        // or within the AT
        if (destinationRCD != null) {
            destinationRepository = destinationRCD.getRepository();
        } else if (mainFrame != null) {
            try {
                destinationRepository = mainFrame.getCurrentUserRepository();
                recordUtil = new RecordUtil();
            } catch (Exception e) {
                print("Unable to initialize db copy utility ...");
                e.printStackTrace();
            }
        }

        // initialize the JSON converter
        xstream = new XStream(new JsonHierarchicalStreamDriver());
        xstream.setMode(XStream.ID_REFERENCES);

        // TEST CODE
        outputDirectory = new File("/Users/nathan/TestATOutput/");
        //outputDirectory = new File("C:/Users/nathan/TestATOut/");

        // start the stop watch object
        startWatch();
    }

    /**
     * Method to start the start the time watch
     */
    private void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    private String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }

    /**
     * Method to do certain task after the copy has completed
     */
    public void cleanUp() {
        print("Finish coping data ... Total time: " + stopWatch());
    }

    /**
     * Method to load the notes ETC types from the destination database
     */
    public void loadDestinationNoteTypes() {
        ArrayList<NotesEtcTypes> noteTypes;

        if (destinationRCD != null) {
            noteTypes = destinationRCD.getNoteTypes();
        } else {
            noteTypes = recordUtil.getNoteTypes();
        }

        for (NotesEtcTypes noteType : noteTypes) {
            noteTypeMap.put(noteType.getNotesEtcName(), noteType);
            print("Mapped destination note type: " + noteType);
        }
    }

    /**
     * Method that load resources currently in the database to avoid duplicates
     */
    public void loadDestinationResources() {
        ArrayList<Resources> resources;

        if (destinationRCD != null) {
            resources = destinationRCD.getResources();
        } else {
            resources = recordUtil.getResources();
        }

        for (Resources resource : resources) {
            destinationResourceMap.put(resource.getResourceIdentifier(), resource);
            print("Mapped destination resource: " + resource);
        }
    }

    /**
     * Method to copy the repository records
     *
     * @throws Exception
     */
    public void copyRepositoryRecords() throws Exception {
        print("Copying repository records ...");
        ArrayList<Repositories> records = sourceRCD.getRepositories();

        for (Repositories repository : records) {
            if (!repository.getShortName().equals(destinationRepository.getShortName())) {
                Repositories repositoryCopy = mapper.shallowCopy(repository);
                repositoryMap.put(repository.getIdentifier(), repositoryCopy);
                repositoryCopy.setIdentifier(null);
                saveRecord(repositoryCopy, false);
                print("Copied Repository: " + repositoryCopy);
            } else {
                print("Repository already in database: " + repository);
            }
        }
    }

    /**
     * Method to copy the location records
     */
    public void copyLocationRecords() throws Exception {
        print("Copying locations records ...");
        ArrayList<Locations> records = sourceRCD.getLocations();

        for (Locations location : records) {
            Locations locationCopy = mapper.shallowCopy(location);
            locationCopy.setRepository(mapRepository(location.getRepository()));
            locationMap.put(location.getIdentifier(), locationCopy);
            locationCopy.setIdentifier(null);

            saveRecord(locationCopy, false);
            print("Copied Location: " + locationCopy);
        }
    }

    /**
     * Copy the user records, remapping the repository
     *
     * @throws Exception
     */
    public void copyUserRecords() throws Exception {
        print("Copying Users records ...");
        ArrayList<Users> records = sourceRCD.getUsers();

        for (Users user : records) {
            Users userCopy = mapper.shallowCopy(user);
            userCopy.setRepository(mapRepository(user.getRepository()));
            userCopy.setIdentifier(null);

            saveRecord(userCopy, false);
            print("Copied User: " + userCopy);
        }
    }

    /**
     * Copy the name records
     *
     * @throws Exception
     */
    public void copyNameRecords() throws Exception {
        print("Copying Name records ...");
        ArrayList<Names> records = sourceRCD.getNames();

        for (Names name : records) {
            Names nameCopy = mapper.shallowCopy(name);
            nameMap.put(name.getIdentifier(), nameCopy);
            nameCopy.setIdentifier(null);
            saveRecord(nameCopy, false);

            print("Copied Name: " + nameCopy + " ID " + nameCopy.getIdentifier());
        }
    }

    /**
     * Method to copy the subject records
     *
     * @throws Exception
     */
    public void copySubjectRecords() throws Exception {
        print("Copying Subject records ...");
        ArrayList<Subjects> records = sourceRCD.getSubjects();

        for (Subjects subject : records) {
            Subjects subjectCopy = mapper.shallowCopy(subject);
            subjectMap.put(subject.getIdentifier(), subjectCopy);
            subjectCopy.setIdentifier(null);
            saveRecord(subjectCopy, false);
            print("Copied Subject: " + subjectCopy + " ID " + subjectCopy.getIdentifier());
        }
    }

    /**
     * Method to copy accession records
     *
     * @throws Exception
     */
    public void copyAccessionRecords() throws Exception {
        ArrayList<Accessions> records = sourceRCD.getAccessions();

        int count = 1; // used to create unique ids when in debug

        for (Accessions accession : records) {
            Accessions accessionCopy = mapper.shallowCopy(accession);
            accessionMap.put(accession.getIdentifier(), accessionCopy);

            // if we are in debug mode, lets make all the accession ids unique
            if (makeUnique) {
                accessionCopy.setAccessionNumber1(randomString.nextString());
                count++;
            }

            // copy names, subjects
            addSubjects(accessionCopy, accession);
            addNames(accessionCopy, accession);

            //set the repository
            accessionCopy.setRepository(mapRepository(accession.getRepository()));

            accessionCopy.setIdentifier(null);
            saveRecord(accessionCopy, false);
            print("Copied Accession " + accessionCopy.getTitle() + " ID " + accessionCopy.getIdentifier());
        }
    }

    /**
     * Method to copy the digital object records
     *
     * @throws Exception
     */
    public void copyDigitalObjectRecords() throws Exception {
        ArrayList<DigitalObjects> records = sourceRCD.getDigitalObjects();

        for (DigitalObjects digitalObject : records) {
            DigitalObjects digitalObjectCopy = mapper.shallowCopy(digitalObject);
            digitalObjectMap.put(digitalObject.getIdentifier(), digitalObjectCopy);

            // TODO make recursive copy of digital object child records


            // copy the file versions
            Set<FileVersions> fileVersions = digitalObject.getFileVersions();
            for (FileVersions fileVersion : fileVersions) {
                FileVersions fileVersionCopy = mapper.shallowCopy(fileVersion);
                fileVersionCopy.setIdentifier(null);
                digitalObjectCopy.addFileVersion(fileVersionCopy);
            }

            // copy names, subjects, and notes
            addSubjects(digitalObjectCopy, digitalObject);
            addNames(digitalObjectCopy, digitalObject);
            addRepeatingData(digitalObjectCopy, digitalObject);

            // set the repository
            digitalObjectCopy.setRepository(mapRepository(digitalObject.getRepository()));

            // save to database now
            digitalObjectCopy.setIdentifier(null);
            saveRecord(digitalObjectCopy, false);

            print("Copied Digital Object: " + digitalObjectCopy.getTitle() + " ID " + digitalObjectCopy.getIdentifier());
        }
    }

    /**
     * Method to copy resource records in a different thread in order to increase performance?
     *
     * @throws Exception
     */
    public void copyResourceRecordsFast() throws Exception {
        final ArrayList<Resources> records = sourceRCD.getResources();
        totalResources = records.size();

        print("Copying Resource records in " + sourceRCDs.length + " threads ...");

        copyCount = 0; // reset this variable

        // start the controller thread that goe through list of records
        Thread performer = new Thread(new Runnable() {
            public void run() {
                try {
                    for (Resources resource : records) {
                        if (destinationResourceMap.containsKey(resource.getResourceIdentifier())) {
                            print("Not Copied: Resource already in database " + resource);
                            continue;
                        }

                        findSourceRCD(resource);
                    }

                    // wait for all the jobs to be done to print the export time
                    while(totalResources != copyCount) {
                        print("Waiting for last set of records to be copied ...");
                        Thread.sleep(60000); //wait 60 seconds before checking again
                    }

                    cleanUp();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        performer.start();
    }

    /**
     * Method that finds a free remote connection to start the copy process
     * in a separate thread to copy the resource record
     *
     * @param resource
     */
    private void findSourceRCD(Resources resource) throws Exception {
        while (true) {
            for (int i = 0; i < sourceRCDs.length; i++) {
                if (sourceRCDs[i].isAvailable()) {
                    sourceRCDs[i].setAvailable(false);
                    startCopyResourceRecordThread(sourceRCDs[i], resource.getIdentifier());
                    return;
                }
            }

            Thread.sleep(2000); // wait 2 seconds before trying to find an other connection
        }
    }

    /**
     * Method to start a thread to copy the resource record
     *
     * @param sourceRCD
     * @param resourceId
     */
    private void startCopyResourceRecordThread(final RemoteDBConnectDialogLight sourceRCD, final Long resourceId) throws Exception {
        Thread performer = new Thread(new Runnable() {
            // The mapper object for copying the objects
            HibernateBeanReplicator mapper2 = new Hibernate3BeanReplicator().initSetterMethodCollector(new PrivateSetterMethodCollector());

            public void run() {
                Resources resource = sourceRCD.getResource(resourceId);
                Resources resourceCopy = mapper2.shallowCopy(resource);

                // indicate we are copying the resource record
                print("Thread Copying Resource: " + resource.getTitle());

                try {
                    // do recursive copy of resource components
                    Set<ResourcesComponents> resourceComponents = resource.getResourcesComponents();
                    for (ResourcesComponents component : resourceComponents) {
                        ResourcesComponents componentCopy = mapper2.shallowCopy(component);
                        componentCopy.setIdentifier(null);
                        componentCopy.setResource(resourceCopy);
                        setAuditInfo(componentCopy);

                        resourceCopy.addComponent(componentCopy);

                        // call the recursive method to add child components
                        copyResourceComponents2(componentCopy, component);
                    }

                    // add names and subjects
                    addSubjects(resourceCopy, resource);
                    addNames(resourceCopy, resource);

                    // copy notes
                    addRepeatingData2(resourceCopy, resource);

                    // copy instances
                    addInstances2(resourceCopy, resource);

                    // set the repository now
                    resourceCopy.setIdentifier(null);
                    resourceCopy.setRepository(mapRepository(resource));

                    // if we are in debug mode, lets make the ids unique
                    if (makeUnique) {
                        resourceCopy.setResourceIdentifier1(randomString.nextString());
                    }

                    // save the record now
                    saveRecord(resourceCopy, true);

                    updateResourceMap(resource.getIdentifier(), resourceCopy.getIdentifier());

                    // refresh the destination and source session so we don't store the object
                    // around for no reason and use up all the memory
                    sourceRCD.refreshSession();
                    sourceRCD.setAvailable(true);

                    print("Copied Resource: " + resourceCopy.getTitle() + " ID " + resourceCopy.getIdentifier());
                    incrementCopyCount();

                    // **TEST CODE** save as json object
                    //String filename = "resource_" + resourceCopy.getIdentifier() + ".json";
                    //saveRecordAsJSON(filename, resourceCopy);
                    //print("Saved as JSON: " + filename);

                    //Try to free some memory
                    //Runtime.getRuntime().gc();

                    long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    print("Records Copied: " + copyCount + " || Memory Used: " + memUsed / 1048576 + "MB\n");
                } catch (Exception e) {
                    print("Error saving " + resourceCopy.getTitle());
                }
            }

            /**
             * Method to do recursive copy of components
             *
             * @param componentCopy
             * @param component
             * @throws Exception
             */
            private void copyResourceComponents2(ResourcesComponents componentCopy, ResourcesComponents component) throws Exception {
                if (component.isHasChild()) {
                    for (ResourcesComponents childComponent : component.getResourcesComponents()) {
                        ResourcesComponents childComponentCopy = mapper2.shallowCopy(childComponent);
                        childComponentCopy.setIdentifier(null);
                        childComponentCopy.setResourceComponentParent(componentCopy);
                        setAuditInfo(childComponentCopy);

                        componentCopy.addComponent(childComponentCopy);
                        copyResourceComponents2(childComponentCopy, childComponent);
                    }
                }

                // add names and subjects
                addSubjects(componentCopy, component);
                addNames(componentCopy, component);

                // add notes
                addRepeatingData2(componentCopy, component);

                // add instances
                addInstances2(componentCopy, component);

                if (debug) print("Processing Component: " + component.getTitle());
            }

            /**
             * Add any repeating data to the resource or resource component record
             *
             * @param recordCopy
             * @param record
             * @throws Exception
             */
            private void addRepeatingData2(ArchDescription recordCopy, ArchDescription record) throws Exception {
                Set<ArchDescriptionRepeatingData> rdatas = record.getRepeatingData();
                for (ArchDescriptionRepeatingData repeatingData : rdatas) {
                    ArchDescriptionRepeatingData repeatingDataCopy = mapper2.shallowCopy(repeatingData);

                    if (repeatingData instanceof ArchDescriptionNotes) {
                        ArchDescriptionNotes noteCopy = (ArchDescriptionNotes) repeatingData;
                        noteCopy.setNotesEtcType(mapNoteType(((ArchDescriptionNotes) repeatingData).getNotesEtcType()));
                    }

                    repeatingDataCopy.setIdentifier(null);
                    repeatingDataCopy.linkArchDescription(recordCopy);
                    recordCopy.addRepeatingData(repeatingDataCopy);

                    if (debug) print("Added repeating data to " + record.getTitle());
                }
            }

            /**
             * Method to add an instance to resource, or resource component record
             * @param recordCopy
             * @param record
             * @throws Exception
             */
            private void addInstances2(ResourcesCommon recordCopy, ResourcesCommon record) throws Exception {
                Set<ArchDescriptionInstances> ainstances = record.getInstances();

                for (ArchDescriptionInstances ainstance : ainstances) {
                    ArchDescriptionInstances newInstance = mapper2.shallowCopy(ainstance);
                    newInstance.setIdentifier(null);

                    // set the resource record or component record
                    if (record instanceof Resources) {
                        newInstance.setResource((Resources) recordCopy);
                    } else {
                        newInstance.setResourcesComponents((ResourcesComponents) recordCopy);
                    }

                    if (newInstance instanceof ArchDescriptionAnalogInstances) {
                        // TO-DO Link to location object
                    } else {
                        // TO-DO link to digital object
                    }

                    if (debug) print("Added instance to " + record.getTitle());
                }
            }

        });

        performer.start();
    }

    /**
     * Method to add to resource map in a thread safe manner
     *
     * @param oldIdentifier
     * @param newIdentifier
     */
    private synchronized void updateResourceMap(Long oldIdentifier, Long newIdentifier) {
        resourceMap.put(oldIdentifier, newIdentifier);
    }

    /**
     * Method to increment the number of resource records copied
     */
    private synchronized void incrementCopyCount() {
        copyCount++;
    }


    /**
     * Method to copy resource records from one database to the next
     *
     * @throws Exception
     */
    public void copyResourceRecords() throws Exception {
        ArrayList<Resources> records = sourceRCD.getResources();

        print("Copying " + records.size() + " Resource records ...");

        copyCount = 0; // keep track of the number of resource records copied

        for (Resources resource : records) {
            if (destinationResourceMap.containsKey(resource.getResourceIdentifier())) {
                print("Not Copied: Resource already in database " + resource);
                continue;
            }

            // indicate we are copying the resource record
            print("Copying Resource: " + resource.getTitle());

            // grab the resource in a new session since we going to be doing a
            // recursion to get all the resource components
            resource = sourceRCD.getResource(resource.getIdentifier());

            Resources resourceCopy = mapper.shallowCopy(resource);

            // do recursive copy of resource components
            Set<ResourcesComponents> resourceComponents = resource.getResourcesComponents();
            for (ResourcesComponents component : resourceComponents) {
                ResourcesComponents componentCopy = mapper.shallowCopy(component);
                componentCopy.setIdentifier(null);
                componentCopy.setResource(resourceCopy);
                setAuditInfo(componentCopy);

                resourceCopy.addComponent(componentCopy);

                // call the recursive method to add child components
                copyResourceComponents(componentCopy, component);
            }

            // add names and subjects
            addSubjects(resourceCopy, resource);
            addNames(resourceCopy, resource);

            // copy notes
            addRepeatingData(resourceCopy, resource);

            // copy instances
            addInstances(resourceCopy, resource);

            // set the repository now
            resourceCopy.setIdentifier(null);
            resourceCopy.setRepository(mapRepository(resource));

            // if we are in debug mode, lets make all the accession ids unique
            if (makeUnique) {
                resourceCopy.setResourceIdentifier1(randomString.nextString());
            }

            // save the record now
            try {
                saveRecord(resourceCopy, true);

                resourceMap.put(resource.getIdentifier(), resourceCopy.getIdentifier());

                // refresh the destination and source session so we don't store the object
                // around for no reason and use up all the memory
                sourceRCD.refreshSession();

                print("Copied Resource: " + resourceCopy.getTitle() + " ID " + resourceCopy.getIdentifier());
                copyCount++;

                // **TEST CODE** save as json object
                String filename = "resource_" + resourceCopy.getIdentifier() + ".json";
                saveRecordAsJSON(filename, resourceCopy);
                print("Saved as JSON: " + filename);

                //Try to free some memory
                Runtime.getRuntime().gc();

                long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                print("Records Copied: " + copyCount + " || Memory Used: " + memUsed / 1048576 + "MB\n");

                if (debug && copyCount >= 20000) break;
            } catch (Exception e) {
                print("Error saving " + resourceCopy);
            }
        }
    }

    /**
     * Method to do recursive copy of resource components
     *
     * @param componentCopy
     * @param component
     * @throws Exception
     */
    private void copyResourceComponents(ResourcesComponents componentCopy, ResourcesComponents component) throws Exception {
        if (component.isHasChild()) {
            for (ResourcesComponents childComponent : component.getResourcesComponents()) {
                ResourcesComponents childComponentCopy = mapper.shallowCopy(childComponent);
                childComponentCopy.setIdentifier(null);
                childComponentCopy.setResourceComponentParent(componentCopy);
                setAuditInfo(childComponentCopy);

                componentCopy.addComponent(childComponentCopy);
                copyResourceComponents(childComponentCopy, childComponent);
            }
        }

        // add names and subjects
        addSubjects(componentCopy, component);
        addNames(componentCopy, component);

        // add notes
        addRepeatingData(componentCopy, component);

        // add instances
        addInstances(componentCopy, component);

        if (debug) print("Processing Component: " + component.getTitle());
    }


    /**
     * Add the subjects to the resource or resource component record
     *
     * @param recordCopy
     * @param record
     * @throws Exception
     */
    private synchronized void addSubjects(ArchDescription recordCopy, ArchDescription record) throws Exception {
        Set<ArchDescriptionSubjects> asubjects = record.getSubjects();

        for (ArchDescriptionSubjects asubject : asubjects) {
            Subjects oldSubject = asubject.getSubject();
            Subjects newSubject = subjectMap.get(oldSubject.getIdentifier());
            if (newSubject != null) {
                try {
                    recordCopy.addSubject(newSubject);
                    if (debug) print("Added subject to " + record.getTitle());
                } catch (DuplicateLinkException e) {
                    if (debug) print("Subject already added to " + record.getTitle());
                }
            } else {
                print("No mapped subject found ...");
            }
        }
    }

    /**
     * Add the names to the resource or resource component record
     *
     * @param recordCopy
     * @param record
     * @throws Exception
     */
    private synchronized void addNames(ArchDescription recordCopy, ArchDescription record) throws Exception {
        Set<ArchDescriptionNames> anames = record.getNames();

        for (ArchDescriptionNames aname : anames) {
            Names oldName = aname.getName();
            Names newName = nameMap.get(oldName.getIdentifier());
            if (newName != null) {
                String function = aname.getNameLinkFunction();
                String role = aname.getRole();
                String form = aname.getForm();
                try {
                    recordCopy.addName(newName, function, role, form);
                    if (debug) print("Added name to " + record.getTitle());
                } catch (DuplicateLinkException e) {
                    if (debug) print("Name already added to " + record.getTitle());
                }
            } else {
                print("No mapped name found ...");
            }
        }
    }

    /**
     * Add any repeating data to the resource or resource component record
     *
     * @param recordCopy
     * @param record
     * @throws Exception
     */
    private void addRepeatingData(ArchDescription recordCopy, ArchDescription record) throws Exception {
        Set<ArchDescriptionRepeatingData> rdatas = record.getRepeatingData();
        for (ArchDescriptionRepeatingData repeatingData : rdatas) {
            ArchDescriptionRepeatingData repeatingDataCopy = mapper.shallowCopy(repeatingData);

            if (repeatingData instanceof ArchDescriptionNotes) {
                ArchDescriptionNotes noteCopy = (ArchDescriptionNotes) repeatingData;
                noteCopy.setNotesEtcType(mapNoteType(((ArchDescriptionNotes) repeatingData).getNotesEtcType()));
            }

            repeatingDataCopy.setIdentifier(null);
            repeatingDataCopy.linkArchDescription(recordCopy);
            recordCopy.addRepeatingData(repeatingDataCopy);

            if (debug) print("Added repeating data to " + record.getTitle());
        }
    }

    /**
     * Method to add an instance to resource, or resource component record
     *
     * @param recordCopy
     * @param record
     * @throws Exception
     */
    private void addInstances(ResourcesCommon recordCopy, ResourcesCommon record) throws Exception {
        Set<ArchDescriptionInstances> ainstances = record.getInstances();

        for (ArchDescriptionInstances ainstance : ainstances) {
            ArchDescriptionInstances newInstance = mapper.shallowCopy(ainstance);
            newInstance.setIdentifier(null);

            // set the resource record or component record
            if (record instanceof Resources) {
                newInstance.setResource((Resources) recordCopy);
            } else {
                newInstance.setResourcesComponents((ResourcesComponents) recordCopy);
            }

            if (newInstance instanceof ArchDescriptionAnalogInstances) {
                // TO-DO Link to location object
            } else {
                // TO-DO link to digital object
            }

            if (debug) print("Added instance to " + record.getTitle());
        }
    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param oldRepository
     * @return
     */
    private Repositories mapRepository(DomainObject oldRepository) {
        Long id = oldRepository.getIdentifier();
        Repositories repo = repositoryMap.get(id);
        if (repo != null) {
            return repo;
        } else {
            return destinationRepository;
        }
    }

    /**
     * Method to return the corresponding note type in the new database
     *
     * @param oldNoteType
     * @return
     */
    private NotesEtcTypes mapNoteType(NotesEtcTypes oldNoteType) {
        String key = oldNoteType.getNotesEtcName();
        NotesEtcTypes noteType = noteTypeMap.get(key);
        if (noteType != null) {
            return noteType;
        } else {
            // TO-DO Note type should really be added to database here
            return noteTypeMap.get(NotesEtcTypes.CANNONICAL_NAME_GENERAL_NOTE);
        }
    }

    /**
     * Method to save the resource record that takes into account running in stand alone
     * or within the AT
     *
     * @param record
     * @param closeSession
     */
    public synchronized void saveRecord(DomainObject record, boolean closeSession) {
        try {
            if (destinationRCD != null) {
                destinationRCD.saveRecord(record);

                if (closeSession) {
                    destinationRCD.refreshSession();
                }
            } else {
                recordUtil.saveRecord(record, closeSession);
            }
        } catch (Exception e) {
            print("Error saving record " + record);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Method to set the audit info for a domain object. Only used when running outside of the
     * AT
     *
     * @param record The record to set the domain object for
     */
    private synchronized void setAuditInfo(DomainObject record) {
        if (destinationRCD != null) {
            destinationRCD.setAuditInfo(record);
        }
    }

    /**
     * Method to convert an AT record to json
     *
     * @param record
     * @return
     */
    public synchronized String recordToJSON(DomainObject record) {
        return xstream.toXML(record);
    }



    /**
     * Method to save text to a file
     *
     * @param record
     */
    public synchronized void saveRecordAsJSON(String filename, DomainObject record) {
        try {
            // create file name base on record type
            File file = new File(outputDirectory, filename);

            String json = recordToJSON(record);

            // write out to the file now
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to load any record maps from the file on operating system
     *
     * @return
     */
    public boolean loadRecordMaps() throws Exception {
        HashMap<String, HashMap<Long, Long>> recordMap = null;

        try {
            recordMap = (HashMap<String, HashMap<Long, Long>>) ScriptDataUtils.getScriptData(recordMapFile);
        } catch (Exception e) {
            print("Unable to load record map file " + recordMapFile.getName());
        }

        if (recordMap != null) {
            HashMap<Long, Long> rmap = recordMap.get("repositories");
            print("Loading Repositories : " + rmap.size());

            for (Map.Entry<Long, Long> entry : rmap.entrySet()) {
                Long old_key = entry.getKey();
                Long new_key = entry.getValue();
                Repositories record;

                if (destinationRCD != null) {
                    record = destinationRCD.getRepository(new_key);
                } else {
                    record = recordUtil.getRepository(new_key);
                }

                if (record != null) {
                    repositoryMap.put(old_key, record);
                    if (debug) print("Loaded repository: " + record);
                }
            }

            HashMap<Long, Long> lmap = recordMap.get("locations");
            print("Loading locations : " + lmap.size());

            for (Map.Entry<Long, Long> entry : rmap.entrySet()) {
                Long old_key = entry.getKey();
                Long new_key = entry.getValue();

                Locations record = null;

                if (destinationRCD != null) {
                    record = destinationRCD.getLocation(new_key);
                } else {
                    record = recordUtil.getLocation(new_key);
                }

                if (record != null) {
                    locationMap.put(old_key, record);
                    if (debug) print("Loaded location: " + record);
                }
            }

            HashMap<Long, Long> smap = recordMap.get("subjects");
            print("Loading Subjects : " + smap.size());

            for (Map.Entry<Long, Long> entry : smap.entrySet()) {
                Long old_key = entry.getKey();
                Long new_key = entry.getValue();
                Subjects record = null;

                if (destinationRCD != null) {
                    record = destinationRCD.getSubject(new_key);
                } else {
                    record = recordUtil.getSubject(new_key);
                }

                if (record != null) {
                    subjectMap.put(old_key, record);
                    if (debug) print("Loaded subject: " + record);
                }
            }

            HashMap<Long, Long> nmap = recordMap.get("names");
            print("Loading Names : " + nmap.size());

            for (Map.Entry<Long, Long> entry : nmap.entrySet()) {
                Long old_key = entry.getKey();
                Long new_key = entry.getValue();
                Names record = null;

                if (destinationRCD != null) {
                    record = destinationRCD.getName(new_key);
                } else {
                    record = recordUtil.getName(new_key);
                }

                if (record != null) {
                    nameMap.put(old_key, record);
                    if (debug) print("Loaded name: " + record);
                }
            }

            print("Loaded record map file " + recordMapFile.getName());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method to store the record maps on to the disk
     */
    public void storeRecordMaps() {
        // create the hash map the holds all the keys for repositories, users, and names
        HashMap<String, HashMap<Long, Long>> recordMap = new HashMap<String, HashMap<Long, Long>>();

        HashMap<Long, Long> rmap = new HashMap<Long, Long>(); // maps for repositories
        HashMap<Long, Long> lmap = new HashMap<Long, Long>(); // maps for locations
        HashMap<Long, Long> smap = new HashMap<Long, Long>();     // maps for subjects
        HashMap<Long, Long> nmap = new HashMap<Long, Long>();     // maps for names

        // map repositories
        for (Map.Entry<Long, Repositories> entry : repositoryMap.entrySet()) {
            Long key = entry.getKey();
            DomainObject record = entry.getValue();
            rmap.put(key, record.getIdentifier());
            print("Storing repository map: " + key + " => " + record.getIdentifier());
        }

        // map locations
        for (Map.Entry<Long, Locations> entry : locationMap.entrySet()) {
            Long key = entry.getKey();
            DomainObject record = entry.getValue();
            lmap.put(key, record.getIdentifier());
            print("Storing location map: " + key + " => " + record.getIdentifier());
        }

        // map subjects
        for (Map.Entry<Long, Subjects> entry : subjectMap.entrySet()) {
            Long key = entry.getKey();
            DomainObject record = entry.getValue();
            smap.put(key, record.getIdentifier());
            print("Storing subject map: " + key + " => " + record.getIdentifier());
        }

        // map names
        for (Map.Entry<Long, Names> entry : nameMap.entrySet()) {
            Long key = entry.getKey();
            DomainObject record = entry.getValue();
            nmap.put(key, record.getIdentifier());
            print("Storing name map: " + key + " => " + record.getIdentifier());
        }

        // finally save it
        recordMap.put("repositories", rmap);
        recordMap.put("locations", lmap);
        recordMap.put("subjects", smap);
        recordMap.put("names", nmap);

        // save to file system now
        print("\nSaving record Maps ...");
        try {
            ScriptDataUtils.saveScriptData(recordMapFile, recordMap);
        } catch (Exception e) {
            print("Unable to save record map file " + recordMapFile.getName());
        }
    }

    /**
     * Convenient print method for printing string in the text console in the future
     *
     * @param string
     */
    private synchronized void print(String string) {
        System.out.println(string);
    }
}
