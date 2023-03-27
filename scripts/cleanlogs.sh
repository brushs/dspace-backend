#!/bin/bash

for FILE in *.txt;
do 
	echo "Processing file: $FILE";
    java -cp MigrationTools-1.0.jar org.dspace.tools.nrcan.migration.logfilecleaner.LogFileCleaner -f $FILE -o $FILE.clean
done

zip cleaned.zip *.clean
