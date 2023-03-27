#!/bin/bash
declare -i y=1

for FILE in $(find *.zip -type f);
do 
	echo "Processing file: $FILE";
    /dspace/bin/dspace import -a -e steve.brush@nrcan-rncan.gc.ca -c 123456789s/3 -m "/dspace/migration/mapfile${i}" -s "/dspace/migration/" -z $FILE > "/dspace/migration/miglog${i}.txt"
	((i++))
done

