/*
 * Created by JFormDesigner on Sat Mar 07 06:21:42 EST 2009
 * This is a simple class which allows connection to a remote
 * database for loading resource records.
 *
 * The main purpose of this is so that a user can still access
 * collections from a database which is not running the same client
 * version.
 *
 */

package org.archiviststoolkit.plugin.dbdialog;


import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.archiviststoolkit.hibernate.SessionFactory;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.*;
import org.archiviststoolkit.structure.NotesEtcTypes;
import org.archiviststoolkit.util.DatabaseConnectionInformation;
import org.archiviststoolkit.util.DatabaseConnectionUtils;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Nathan Stevens
 */
public class RemoteDBConnectDialogLight extends JDialog implements DomainAccessListener {
    // the hibernate session factory
    private org.hibernate.SessionFactory sessionFactory = null;

    // The session that is used through out to load records
    private Session session = null;

    // The saved connection information
    private HashMap savedConnections = new HashMap();

    private ArrayList<String> savedConnectionUrls = new ArrayList<String>();

    // List that holds the records returned from the database
    private java.util.List recordList;

    // List that holds the records returned from the database
    private java.util.List<Users> userList;

    // boolean to specify that the window should close
    private boolean closeOnConnect = true;

    private boolean available = true;

    private Vector<DomainAccessListener> listeners = new Vector<DomainAccessListener>();

    // the lookup list object
    private ArrayList<LookupList> lookupLists = null;

    private String connectionMessage = "";

    /**
     * Constructor that doesn't take a parent owner used when running on the command line
     */
    public RemoteDBConnectDialogLight() {
        super();
        initComponents();
        loadDatabaseConnectionInformation();
    }

    public RemoteDBConnectDialogLight(Frame owner) {
        super(owner, "Remote Database Connection");
        initComponents();
        loadDatabaseConnectionInformation();
    }

    public RemoteDBConnectDialogLight(Dialog owner) {
        super(owner, "Remote Database Connection");
        initComponents();
        loadDatabaseConnectionInformation();
    }

    public final void addListener(final DomainAccessListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from this DAO.
     *
     * @param listener the listener to remove
     */

    public final void removeListener(final DomainAccessListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all the listeners of an event.
     *
     * @param event the event which should be sent to all listeners
     */

    public final void notifyListeners(final DomainAccessEvent event) {
        for (Object listener1 : listeners) {
            DomainAccessListener listener = (DomainAccessListener) listener1;
            listener.domainChanged(event);
        }
    }

    /**
       Method to specify if to close window on connect
     */
    public void setHideWindowOnConnect(boolean b) {
        closeOnConnect = b;
    }

    /**
     * Method to set the connection url, then try to connect to the database.
     * Use mainly during testing
     * @param index
     */
    public void connectToDatabase(int index) {
        connectionUrl.setSelectedIndex(index);

        if(connectToDatabase2()) {
            loadUserRecords();
        }
    }

    /**
     * Method is going to be used to connect to a data by specifying
     */
    public void connectToDatabase(String databaseType, String url, String username, String password) {
        // set the database type
        comboBox2.setSelectedItem(databaseType);

        // set the url for tracer database
        connectionUrl.removeAllItems();
        connectionUrl.addItem(url);
        connectionUrl.setSelectedIndex(0);

        // set the user name and password
        textField1.setText(username);
        passwordField1.setText(password);

        if(connectToDatabase2()) {
            loadUserRecords();
        }
    }

    /**
     * Method to connect to the current AT database when running as a plugin
     */
    public void connectToCurrentDatabase() {
        System.out.println("\n**Connecting to Current AT Database ...**");

        sessionFactory = SessionFactory.getSessionFactory();
        session = sessionFactory.openSession();
    }

    /**
     * Method to update the connection url information displayed to user
     */
    private void updateConnectionUrlInformation() {
        String selectedUrl = (String) connectionUrl.getSelectedItem();
        if (savedConnections.containsKey(selectedUrl)) {
            DatabaseConnectionInformation dbInfo = (DatabaseConnectionInformation) savedConnections.get(selectedUrl);
            textField1.setText(dbInfo.getUsername());
            passwordField1.setText(dbInfo.getPassword());
            comboBox2.setSelectedItem(dbInfo.getDatabaseType());
        }
    }

    /**
     * Just set this window invisible
     */
    private void okButtonActionPerformed() {
        setVisible(false);
    }

    /**
     * The connect button calls but all the work is done in connectToDatabase2.
     */
    private void connectToDatabase() {
        // disable the connect button and start the progress bar going
        button1.setEnabled(false);
        progressBar1.setIndeterminate(true);
        progressBar1.setStringPainted(true);
        progressBar1.setString("Connecting to database");

        Thread performer = new Thread(new Runnable() {
            public void run() {
                // try connecting to the database and if successfully try loading the resource
                // records into the list
                if (connectToDatabase2()) {
                    progressBar1.setString("Connecting to database ....");
                    statusLabel.setText("Connected ...");
                    loadUserRecords();
                    addUsersToComboBox();
                } else {
                    statusLabel.setText("Failed to connect ...");
                }

                // all done, so enable the process button and stop the progress bar
                progressBar1.setIndeterminate(false);
                progressBar1.setStringPainted(false);
                button1.setEnabled(true);
            }
        });
        performer.start();
    }

    /**
     * Connect to the AT database at the given location. This does not check database version
     * so the it should be able to work with version 1.5 and 1.5.7
     */
    private boolean connectToDatabase2() {
        // based on the database type set the driver and hibernate dialect
        String databaseType = (String) comboBox2.getSelectedItem();
        String driverClass = "";
        String hibernateDialect = "";

        if (databaseType.equals(SessionFactory.DATABASE_TYPE_MYSQL)) {
            driverClass = "com.mysql.jdbc.Driver";
            hibernateDialect = "org.hibernate.dialect.MySQLInnoDBDialect";
        } else if (databaseType.equals(SessionFactory.DATABASE_TYPE_MICROSOFT_SQL_SERVER)) {
            driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            hibernateDialect = "org.hibernate.dialect.SQLServerDialect";
        } else if (databaseType.equals(SessionFactory.DATABASE_TYPE_ORACLE)) {
            driverClass = "oracle.jdbc.OracleDriver";
            hibernateDialect = "org.hibernate.dialect.Oracle10gDialect";
        } else if (databaseType.equals(SessionFactory.DATABASE_TYPE_INTERNAL)) {
            driverClass = "org.hsqldb.jdbcDriver";
            hibernateDialect = "org.hibernate.dialect.HSQLDialect";
        }else { // should never get here
            System.out.println("Unknown database type : " + databaseType);
            return false;
        }

        // now attempt to build the session factory
        String databaseUrl = (String) connectionUrl.getSelectedItem();
        String userName = textField1.getText();
        String password = new String(passwordField1.getPassword());

        try {
            connectionMessage = "Connecting to: " + databaseUrl;
            System.out.println(connectionMessage);

            Configuration config = new Configuration().configure();
            Properties properties = config.getProperties();
            properties.setProperty("hibernate.connection.driver_class", driverClass);
            if (databaseType.equals(SessionFactory.DATABASE_TYPE_MYSQL)) {
                properties.setProperty("hibernate.connection.url", databaseUrl + "?useUnicode=yes&characterEncoding=utf8");
            } else {
                properties.setProperty("hibernate.connection.url", databaseUrl);
            }
            //deal with oracle specific settings
            if (databaseType.equals(SessionFactory.DATABASE_TYPE_ORACLE)) {
                properties.setProperty("hibernate.jdbc.batch_size", "0");
                properties.setProperty("hibernate.jdbc.use_streams_for_binary", "true");
                properties.setProperty("SetBigStringTryClob", "true");
            }
            properties.setProperty("hibernate.connection.username", userName);
            properties.setProperty("hibernate.connection.password", password);
            properties.setProperty("hibernate.dialect", hibernateDialect);
            config.setProperties(properties);
            sessionFactory = config.buildSessionFactory();

            //test the session factory to make sure it is working
            testHibernate();

            session = sessionFactory.openSession();

            connectionMessage += "\nSuccess ...\n\n";
            System.out.println("Success ...");

            return true; // connected successfully so return true
        } catch (Exception hibernateException) {
            hibernateException.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "Failed to start hibernate engine ...",
                    "Hibernate Error",
                    JOptionPane.ERROR_MESSAGE);

            return false;
        }
    }

    /**
     * Method to test the hibernate connection to make sure it can connect to an
     * AT database.
     *
     * @throws java.sql.SQLException if it can't connect to the AT database
     */
    private void testHibernate() throws SQLException {
        Session testSession = sessionFactory.openSession();
        Query query = testSession.createQuery("select count(*) from Constants");
        Transaction tx = testSession.beginTransaction();
        query.list();
        tx.commit();
        testSession.close();
    }

    /**
     * Method to load list of AT users from the database. This is to allow
     * for users to be selected when running in stand alone mode
     */
    private void loadUserRecords() {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(Users.class);
            userList = criteria.list();
            tx.commit();

        } catch (RuntimeException ex) {
            try {
                tx.rollback();
            } catch (HibernateException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load user records ...",
                        "Record Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Method to add users to the combobox that allows selection of users
     */
    private void addUsersToComboBox() {
        // add the user records to the userComboBox in the UDT thread
        Runnable doWorkRunnable = new Runnable() {
            public void run() {
                // now sort the returned list
                Collections.sort(userList);

                // clear any records in the user combobox
                userComboBox.removeAllItems();

                for (Users users : userList) {
                    userComboBox.addItem(users);
                }

                userComboBox.setSelectedIndex(0);

                if(closeOnConnect) {
                    setVisible(false);
                }
            }
        };
        SwingUtilities.invokeLater(doWorkRunnable);
    }

    /**
     * Method to get the currently selected user
     *
     * @return  Return the currently selected user
     */
    public Users getSelectedUser() {
        Users user = (Users)userComboBox.getSelectedItem();
        return user;
    }

    /**
     * Method to get the first user in the list
     *
     * @return
     */
    public Users getFirstUser() {
        return userList.get(0);
    }

    /**
     * Method to save a domain object
     * @param record The domain object to save
     */
    public void saveRecord(DomainObject record) throws Exception {
        Transaction tx = null;

        try {
            setAuditInfo(record);
            tx = session.beginTransaction();
            session.saveOrUpdate(record);
            tx.commit();
        } catch (HibernateException hibernateException) {
            hibernateException.printStackTrace();

            try {
                tx.rollback();
            } catch (HibernateException e) { }
            throw new PersistenceException("failed to update", hibernateException);
        } catch (Exception exception) {
            try {
                tx.rollback();
            } catch (HibernateException e) { }
            throw new PersistenceException("failed to update", exception);
        }

        notifyListeners(new DomainAccessEvent(DomainAccessEvent.INSERT, record));
    }

    public void setRepository(DomainObject record) {
        if(record instanceof Users) {
            Users user = (Users)record;
            user.setRepository(getSelectedUser().getRepository());
        } else if(record instanceof Accessions) {
            Accessions accession = (Accessions)record;
            accession.setRepository(getSelectedUser().getRepository());
        } else if(record instanceof Resources) {
            Resources resource = (Resources)record;
            resource.setRepository(getSelectedUser().getRepository());
        } else if(record instanceof DigitalObjects) {
            DigitalObjects digitalObject = (DigitalObjects)record;
            digitalObject.setRepository(getSelectedUser().getRepository());
        }
    }

    /**
     * Method to get the repository for the first user
     *
     * @return
     */
    public Repositories getRepository() {
        return userList.get(0).getRepository();
    }

    /**
     * Method to set the audit info for a domain object
     *
     * @param record The record to set the domain object for
     */
    public void setAuditInfo(DomainObject record) {
        // create the audit info to add to the  domain object
        Date now = new Date();

	    AuditInfo auditInfo = new AuditInfo();
	    auditInfo.setCreated(now);
	    auditInfo.setLastUpdated(now);

        // set which user created this object
        String userName = getFirstUser().getUserName();
        auditInfo.setLastUpdatedBy(userName);
        auditInfo.setCreatedBy(userName);

        record.setAuditInfo(auditInfo);
    }

    /**
     * Method to load connection information into the AT
     */
    private void loadDatabaseConnectionInformation() {
        HashMap info = DatabaseConnectionUtils.loadDatabaseConnectionInformation();
        if (info != null) {
            savedConnections = info;

            Map sortedMap = new TreeMap(savedConnections);
            Collection c = sortedMap.values();

            //obtain an Iterator for Collection
            Iterator itr = c.iterator();

            //iterate through HashMap and ad to combo box
            while (itr.hasNext()) {
                DatabaseConnectionInformation dbInfo = (DatabaseConnectionInformation) itr.next();
                connectionUrl.addItem(dbInfo.toString());
                savedConnectionUrls.add(dbInfo.toString());
            }
        }
    }

    public ArrayList<DomainObject> getRecordList() {
        ArrayList<DomainObject> records = new ArrayList<DomainObject>();

        for (Object object : recordList) {
            records.add((DomainObject) object);
        }

        return records;
    }

    public Session getSession() {
        return session;
    }

    public Session refreshSession() {
        if(session != null && session.isOpen()) {
            session.close();
        }

        session = sessionFactory.openSession();

        return session;
    }

    public void closeSession() {
        if(session != null) {
            session.close();
        }
    }

    /**
     * Method to return the session factory
     * @return
     */
    public org.hibernate.SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Method to return the resource in a long session
     *
     * @param identifier
     * @return
     */
    public Resources getResource(Long identifier) {
        Transaction tx = null;
        Resources resource = null;

        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(Resources.class);
            criteria.setFetchMode("names", FetchMode.JOIN);
            criteria.setFetchMode("accessions", FetchMode.JOIN);
            criteria.setFetchMode("subjects", FetchMode.JOIN);
            criteria.add(Restrictions.idEq(identifier));
            resource = (Resources)criteria.uniqueResult();
            session.setReadOnly(resource, true);
            tx.commit();
        } catch (RuntimeException ex) {
            try {
                tx.rollback();
            } catch (HibernateException e) {
                e.printStackTrace();
            }
        }

        return resource;
    }

    public Repositories getRepository(Long key) {
        return (Repositories)getRecord(Repositories.class, key);
    }

    public Locations getLocation(Long key) {
        return (Locations)getRecord(Locations.class, key);
    }

    public Subjects getSubject(Long key) {
        return (Subjects)getRecord(Subjects.class, key);
    }

    public Names getName(Long key) {
        return (Names)getRecord(Names.class, key);
    }

    /**
     * Method to return a domain object from the database
     *
     * @param clazz
     * @param key
     * @return
     */
    public DomainObject getRecord(Class clazz, Long key) {
        Transaction tx = null;
        DomainObject record = null;

        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(clazz);
            criteria.add(Restrictions.idEq(key));
            record = (DomainObject)criteria.uniqueResult();
            tx.commit();
        } catch (RuntimeException ex) {
            try {
                tx.rollback();
            } catch (HibernateException e) {
                e.printStackTrace();
            }
        }

        return record;
    }

    public ArrayList<Locations> getLocations() {
        return (ArrayList<Locations>)getRecords(Locations.class, session);
    }

    public ArrayList<NotesEtcTypes> getNoteTypes() {
        return (ArrayList<NotesEtcTypes>)getRecords(NotesEtcTypes.class, session);
    }

    public ArrayList<Repositories> getRepositories() {
        return (ArrayList<Repositories>)getRecords(Repositories.class, session);
    }

    /**
     * Method to return only the parent digital objects
     *
     * @return
     */
    public ArrayList<DigitalObjects> getDigitalObjects() {
        ArrayList<DigitalObjects> allDigitalObjects = (ArrayList<DigitalObjects>)getRecords(DigitalObjects.class, session);
        ArrayList<DigitalObjects> parentDigitalObjects = new ArrayList<DigitalObjects>();

        for(DigitalObjects digitalObject: allDigitalObjects) {
            if(digitalObject.getParent() == null) {
                parentDigitalObjects.add(digitalObject);
            }
        }

        return parentDigitalObjects;
    }

    public ArrayList<Resources> getResources() {
        return (ArrayList<Resources>)getRecords(Resources.class, session);
    }

    public ArrayList<Accessions> getAccessions() {
        return (ArrayList<Accessions>)getRecords(Accessions.class, session);
    }

    public ArrayList<Names> getNames() {
        return (ArrayList<Names>)getRecords(Names.class, session);
    }

    public ArrayList<Users> getUsers() {
        return (ArrayList<Users>)getRecords(Users.class, session);
    }

    public ArrayList<Subjects> getSubjects() {
        return (ArrayList<Subjects>)getRecords(Subjects.class, session);
    }

    public ArrayList<LookupList> getLookupLists() {
        if(lookupLists == null) {
            lookupLists = (ArrayList<LookupList>)getRecords(LookupList.class, session);
        }

        return lookupLists;
    }

    /**
     * Method to return the language code
     *
     * @return
     */
    public HashMap<String, String> getLanguageCodes() {
        return getLookupListItemsAndCodes("Language codes", false);
    }

    /**
     * Method to get the name linked creator / subject roles
     *
     * @return
     */
    public HashMap<String, String> getNameLinkCreatorCodes() {
        return getLookupListItemsAndCodes("Name link creator / subject role", true);
    }

    /**
     * Method to get the lookup list item/values for a particular lookup list
     *
     * @param listName
     * @return
     */
    public HashMap<String, String> getLookupListItemsAndCodes(String listName, boolean addCodeToKey) {
        getLookupLists();

        HashMap<String, String> itemsAndCodes = new HashMap<String, String>();

        for(LookupList lookupList: lookupLists) {
            String name = lookupList.getListName();

            if(name.equalsIgnoreCase(listName)) {
                Set<LookupListItems> items = lookupList.getListItems();

                for (LookupListItems lookupListItem: items) {
                    String key = lookupListItem.getListItem();
                    String code = lookupListItem.getCode();

                    if(addCodeToKey) {
                        // add using key with no code as a work around for people ewho back loaded records into the AT
                        itemsAndCodes.put(key, code);

                        // now add using key which contains the code
                        key  = lookupListItem.getListItem() + " (" + code + ")";
                        itemsAndCodes.put(key, code);
                    } else {
                        itemsAndCodes.put(key, code);
                    }
                }

                // break out of this loop now
                break;
            }
        }

        return itemsAndCodes;
    }

    public ArrayList getRecords(Class clazz, Session session) {
        Transaction tx = null;
        ArrayList recordList = null;

        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(clazz);
            recordList = (ArrayList)criteria.list();
            tx.commit();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        return recordList;
    }

    /**
     * Method to add more extent types to the AT lookup list in case the user made additions
     * to the backend
     *
     * @param lookupList
     * @return
     */
    public void addExtentTypes(LookupList lookupList) {
        Transaction tx = null;
        ArrayList recordList = null;

        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(ArchDescriptionPhysicalDescriptions.class);
            criteria.setProjection(Projections.distinct(Projections.property("extentType")));
            recordList = (ArrayList)criteria.list();
            tx.commit();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        for(Object record: recordList) {
            String extentType = (String)record;

            if(!extentType.isEmpty()) {
                lookupList.addListItem(extentType);
            }
        }
    }

    /**
     * Method to add salutations to a dummy lookup list in order to add them to the ASpace
     * enum for this
     *
     * @param lookupList
     */
    public void addSalutations(LookupList lookupList) {
        Transaction tx = null;
        ArrayList recordList = null;

        try {
            tx = session.beginTransaction();
            Criteria criteria = session.createCriteria(Names.class);
            criteria.setProjection(Projections.distinct(Projections.property("salutation")));
            recordList = (ArrayList)criteria.list();
            tx.commit();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        for(Object record: recordList) {
            String salutation = (String)record;

            if(!salutation.isEmpty()) {
                lookupList.addListItem(salutation);
            }
        }
    }

    /**
     * Check to see if this RCD is available when running in multiple thread environment
     *
     * @return
     */
    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized void setAvailable(boolean available) {
        this.available = available;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        connectionUrl = new JComboBox();
        label2 = new JLabel();
        textField1 = new JTextField();
        label3 = new JLabel();
        passwordField1 = new JPasswordField();
        label4 = new JLabel();
        comboBox2 = new JComboBox();
        label5 = new JLabel();
        progressBar1 = new JProgressBar();
        buttonBar = new JPanel();
        button1 = new JButton();
        statusLabel = new JLabel();
        label8 = new JLabel();
        userComboBox = new JComboBox();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                    },
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- label1 ----
                label1.setText("Connection URL");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- connectionUrl ----
                connectionUrl.setEditable(true);
                connectionUrl.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        updateConnectionUrlInformation();
                    }
                });
                contentPanel.add(connectionUrl, cc.xy(3, 1));

                //---- label2 ----
                label2.setText("Username");
                contentPanel.add(label2, cc.xy(1, 3));

                //---- textField1 ----
                textField1.setColumns(25);
                contentPanel.add(textField1, cc.xy(3, 3));

                //---- label3 ----
                label3.setText("Password");
                contentPanel.add(label3, cc.xy(1, 5));
                contentPanel.add(passwordField1, cc.xy(3, 5));

                //---- label4 ----
                label4.setText("Database Type");
                contentPanel.add(label4, cc.xy(1, 7));

                //---- comboBox2 ----
                comboBox2.setModel(new DefaultComboBoxModel(new String[] {
                    "MySQL",
                    "Oracle",
                    "Microsoft SQL Server",
                    "Internal Database"
                }));
                contentPanel.add(comboBox2, cc.xy(3, 7));

                //---- label5 ----
                label5.setText("Progress");
                contentPanel.add(label5, cc.xy(1, 9));
                contentPanel.add(progressBar1, cc.xy(3, 9));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.GLUE_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.BUTTON_COLSPEC
                    },
                    RowSpec.decodeSpecs("pref")));

                //---- button1 ----
                button1.setText("Connect");
                button1.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        connectToDatabase();
                    }
                });
                buttonBar.add(button1, cc.xy(2, 1));

                //---- statusLabel ----
                statusLabel.setText("Not connected ...");
                buttonBar.add(statusLabel, cc.xy(4, 1));

                //---- label8 ----
                label8.setText("Select User");
                label8.setVisible(false);
                buttonBar.add(label8, cc.xy(6, 1));

                //---- userComboBox ----
                userComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "No Users Loaded"
                }));
                userComboBox.setVisible(false);
                buttonBar.add(userComboBox, cc.xy(8, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, cc.xy(10, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        setSize(635, 240);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JComboBox connectionUrl;
    private JLabel label2;
    private JTextField textField1;
    private JLabel label3;
    private JPasswordField passwordField1;
    private JLabel label4;
    private JComboBox comboBox2;
    private JLabel label5;
    private JProgressBar progressBar1;
    private JPanel buttonBar;
    private JButton button1;
    private JLabel statusLabel;
    private JLabel label8;
    private JComboBox userComboBox;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public void domainChanged(DomainAccessEvent event) {
        this.notifyListeners(event);
    }

    /**
     * Method to get the saved connections URLs.
     *
     * @return
     */
    public ArrayList<String> getSavedConnectionUrls() {
        return savedConnectionUrls;
    }

    /**
     * Method to get the current connection URL
     *
     * @return
     */
    public String getConnectionUrl() {
        if(connectionUrl.getSelectedItem() != null) {
            return connectionUrl.getSelectedItem().toString();
        } else {
            return "Current AT Database";
        }
    }

    /**
     * Method to return the connection message
     *
     * @return
     */
    public String getConnectionMessage() {
        return connectionMessage;
    }
}