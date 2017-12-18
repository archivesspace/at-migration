This repo contains the source code ScriptAT plugin which contains the AT to ArchivesSpace data migration
utility. The bulk of the data migration code can be found under "src/org/archiviststoolkit/plugin/utils/aspace".

Detailed information the plugin functionality can be viewed found at:
https://docs.google.com/a/nyu.edu/document/d/1Uj2qKtSKLGMm1VPDmDtuNG4eL8zgAJNhOHc93j0S7TE/pub

NOTE: This tool works for migration into ArchivesSpace v1.4.2. If migrating from Archivists' Toolkit to ArchivesSpace v1.5.x, you need to install ArchivesSpace v1.4.2 and run this migration tool first.  Then upgrade your ArchivesSpace installation to ArchivesSpace v1.5.x. See upgrade considerations for different versions of ArchivesSpace https://github.com/archivesspace/archivesspace/blob/master/UPGRADING.md.

NOTES ON COMPILING THE PLUGIN FROM SOURCE CODE:

This assumes you already have basic knowledge of how to compile and run Java programs using your
IDE of choice. The current source needs to be compiled with JDK 1.6, or later. To build:

1. Clone this repo.
2. Import the source code into your IDE.
3. In your IDE, place all the *.jar files found in the "lib" folder in the Classpath.
4. You should now be able to successfully compile the plugin from source code. **You must use JDK 1.6 to compile 
   the code. 1.7 or later will not work as an AT plugin.**
5. You can edit the makep script to fit your development environment to generate the scriptAT.zip plugin
   file after code compilation.

Note: If you intend to modify user interface code, then you will need to use JFormdesigner to open
the *.jfd files. Consult the JFormDesigner documentation on how to install and use it.

NOTES FOR DEVELOPERS

The majority of development work will probably take place in a few classes, primarily in the package 
at-migration\src\org\archiviststoolkit\plugin\utils\aspace
A description of some of the classes is given below.

1. dbCopyFrame is the user interface for the migration tool. It mostly calls methods in ASpaceCopyUtil.
2. dbCopyCLI mainly does the same thing as dbCopyFrame but is for command line mode.
3. ASpaceCopyUtil has a method for copying each record type. Each of these is called by dbCopyFrame/dbCopyCLI. It 
   gets records from AT, calls on ASpaceMapper to map them to the ASpace model, then calls on ASpaceClient to save 
   them to ASpace.
4. ASpaceMapper takes AT model records and converts them to the appropriate ASpace JSON model.
5. TopContainerMapper serves the same function as ASpaceMapper but is specialized for top containers, which present 
   some special challenges.
6. ASpaceClient saves records to ASpace and gets records from ASpace.

NOTES ON TESTING

When writing tests you should always place them in the test directory. To write a test:

1. Create a class that extends Testing to write your tests in.
2. Import junit.framework.JUnit4TestAdapter, org.junit.Assert, and org.junit.Test.
3. Write tests with annotation @Test in this class.
4. Add the following method:
   public static junit.framework.Test suite() {return new JUnit4TestAdapter(*Name of your class here*.class);}
5. You can now run your test by calling a constructor for your class from the testing main method then running the 
   Testing main method.

NOTES ON RUNNING THE PLUGIN IN STANDALONE MODE

Though intended to run as a plugin within the AT application, the plugin can run in standalone
mode for testing purposes. To run in standalone mode:

1. Create a directory call "logs" in the project directory if it is not already there.
2. Run the "dbCopyFrame" class making sure all the *.jar files in "lib" are in the Classpath.


NOTES ON RUNNING THE PLUGIN IN COMMAND LINE MODE

1. Edit the dbcopy.properties to point to the desired AT database and ASpace Instance
2. Run the "dbCopyCLI" class making sure all the *.jar files in "lib" are in the Classpath.

NOTES ON DATA MIGRATION AND DEFAULTS

1. If a date is missing in AT but is required for AS the default date will be set to 1/1/1900 or 0 for integer dates.
2. The start date specified for a container location is the creation date of the associated resource/accession.
3. Many containers including all of those for accessions do not have meaningful identifiers in AT. Any such 
container will be assigned a default container indication which will include the words "unknown container" as 
well as information about it's content.
3. Sometimes AT records will have an indicator but no container type for either container 2 or 3. This is not 
allowed in AS so the default for AS will be "unknown_item" so that the indicator is not lost. The same goes for if 
a container 3 is specified without a container 2.
4. Some IDs are checked for uniqueness in AS but not AT. In these cases if an ID is not unique the problem will be 
corrected by adding ## along with some random string to the ID.
5. If an assessment is linked to records from multiple repositories, duplicate assessments are created - one for 
each linked repository.

CONTINUING A PREVIOUS MIGRATION

1. If a problem occurs during migration, URI maps should be saved and allow you to continue from where you left 
off by checking the 'continue previous migration' box.
2. If you are running the migration tool as an Archivist's Toolkit plugin and a connection problem occurs and 
causes the migration to stop, restart AT before attempting to continue migration.
2. You can also continue a migration if you manually cancel it.
3. This should work fairly seamlessly, but you should check the record the migration stopped at as the migrator 
may correct for a duplicate ID where there is not actually one.
4. It is not necessary to run the repository check again when continuing a migration.
5. You should save your error log and repository check report even if you plan to continue a migration, as these 
logs reset when you continue a migration.

HOW TOP CONTAINER UNIQUENESS IS DETERMINED

Archivist's Toolkit does not include the concept of distinct top containers but rather the same container may 
be entered many times for multiple instances. For this reason it was necessary to determine a set of rules for 
determining what constitutes a unique top container. For top containers created from instances, they are considered 
to be the same if either the repository and barcode match or if the repository, resource, indicator, and container type 
match. As for accessions, no instances exist in Archivist's Toolkit. In stead, a top container is created for each 
location the accession is linked to.

NOTE ON CPU AND MEMORY USAGE

Data migration is a memory intensive task due to the large amounts of objects being created.
As such, it’s recommended running the plugin on a machine with at list 2GB memory, and setting
the max heap space to 1024m.
