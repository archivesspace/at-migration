#!/bin/sh
#
# Simple script to run the data migration plugin from the command line
# outside of the AT.  It assumes the plugin zip file, "scriptAT.zip", has
# been installed in the ATs' plugin directory.  Also, a file called dbcopy.properties
# must be present in the same directory as this script. This file stores all
# connection information for the AT database and ASpace instances.
#
# On Linux System without X11 display, then Xvbf needs to used by doing the following
#
# 1. Install Xvfb for the particular Linux distro
# 2. Run > Xvfb :99 -screen 0 800x600x24 &
# 3. Run > export DISPLAY=":99"
#
# Now the this script can be executed from the AT installation directory
#

java -Xmx1024m -cp "plugins/scriptAT.zip:lib/*" org.archiviststoolkit.plugin.dbCopyCLI