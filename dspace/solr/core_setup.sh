#!/bin/bash

    
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