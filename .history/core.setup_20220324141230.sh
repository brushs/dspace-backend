#!bin/sh

    - ./dspace/solr/authority:/opt/solr/server/solr/configsets/authority
    - ./dspace/solr/oai:/opt/solr/server/solr/configsets/oai
    - ./dspace/solr/search:/opt/solr/server/solr/configsets/search
    - ./dspace/solr/statistics:/opt/solr/server/solr/configsets/statistics
    # Keep Solr data directory between reboots
    - solr_data:/var/solr/data
    # Initialize all DSpace Solr cores using the mounted local configsets (see above), then start Solr
    # * First, run precreate-core to create the core (if it doesn't yet exist). If exists already, this is a no-op
    # * Second, copy updated configs from mounted configsets to this core. If it already existed, this updates core
    #   to the latest configs. If it's a newly created core, this is a no-op.
    entrypoint:
    - /bin/bash
    - '-c'
    - |
      init-var-solr
      precreate-core authority /opt/solr/server/solr/configsets/authority
      cp -r -u /opt/solr/server/solr/configsets/authority/* authority
      precreate-core oai /opt/solr/server/solr/configsets/oai
      cp -r -u /opt/solr/server/solr/configsets/oai/* oai
      precreate-core search /opt/solr/server/solr/configsets/search
      cp -r -u /opt/solr/server/solr/configsets/search/* search
      precreate-core statistics /opt/solr/server/solr/configsets/statistics
      cp -r -u /opt/solr/server/solr/configsets/statistics/* statistics
      exec solr -f