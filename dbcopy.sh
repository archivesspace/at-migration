#!/bin/sh
#
# Simple script to run the data migration plugin from the command line
# outside of the AT.  It assumes the plugin zip file "scriptAT.zip" has already
# been installed in the ATs' plugin directory.  Also a file called dbcopy.properties must
# also be present in the same directory as this script. This file stores all
# connection information for the AT database, and ASpace instances. Please note that
# the plugin reads all AT database connection information from a file called atdbinfo.txt
# located in the users home directory.
#
# On Linux System without X11 display, then Xvbf needs to used by doing the following
#
# 1. Install Xvfb for the particular Linux distro
# 2. Run > Xvfb :99 -screen 0 800x600x24 &
# 3. Run > export DISPLAY=":99"
#
# Now the this script can be executed

java -Xmx1024m -cp "plugins/scriptAT.zip:lib/*" org.archiviststoolkit.plugin.dbCopyCLI