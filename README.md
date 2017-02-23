This repo contains the source code ScriptAT plugin which contains the AT to ArchivesSpace data migration
utility. The bulk of the data migration code can be found under "src/org/archiviststoolkit/plugin/utils/aspace".

Detailed information the plugin functionality can be viewed found at:
https://docs.google.com/a/nyu.edu/document/d/1Uj2qKtSKLGMm1VPDmDtuNG4eL8zgAJNhOHc93j0S7TE/pub

NOTE: This tool works for migration into ArchivesSpace v1.4.2. If migrating from Archivists' Toolkit to ArchivesSpace v1.5.x, you need to install ArchivesSpace v1.4.2 and run this migration tool first.  Then upgrade your ArchivesSpace installation to ArchivesSpace v1.5.x.

NOTES ON COMPILING THE PLUGIN FROM SOURCE CODE:

This assumes you already have basic knowledge of how to compile and run Java programs using your
IDE of choice. The current source needs to be compiled with JDK 1.6, or later. To build:

1. Clone this repo.
2. Import the source code into your IDE.
3. In your IDE, place all the *.jar files found in the "lib" folder in the Classpath.
4. You should now be able to successfully compile the plugin from source code.
5. You can edit the makep script to fit your development environment to generate the scriptAT.zip plugin
   file after code compilation.

Note: If you intend to modify user interface code, then you will need to use JFormdesigner to open
the *.jfd files. Consult the JFormDesigner documentation on how to install and use it.


NOTES ON RUNNING THE PLUGIN IN STANDALONE MODE

Though intended to run as a plugin within the AT application, the plugin can run in standalone
mode for testing purposes. To run in standalone mode:

1. Create a directory call "logs" in the project directory.
2. Run the "dbCopyFrame" class making sure all the *.jar files in "lib" are in the Classpath.


NOTES ON RUNNING THE PLUGIN IN COMMAND LINE MODE

1. Edit the dbcopy.properties to point to the desired AT database and ASpace Instance
2. Run the "dbCopyCLI" class making sure all the *.jar files in "lib" are in the Classpath.


NOTE ON CPU AND MEMORY USAGE

Data migration is a memory intensive task due to the large amounts of objects being created.
As such, it’s recommended running the plugin on a machine with at list 2GB memory, and setting
the max heap space to 1024m.
