
FROM solr:8.11-slim

USER root

# Install OpenSSH and set the password for root to "Docker!"
ENV SSH_PASSWD "root:Docker!"
RUN apt-get update \
        && apt-get install -y --no-install-recommends dialog \
        && apt-get update \
  && apt-get install -y --no-install-recommends openssh-server \
  && echo "$SSH_PASSWD" | chpasswd

# Copy the sshd_config file to the /etc/ssh/ directory
COPY sshd_config /etc/ssh/

# Copy and configure the ssh_setup file
RUN mkdir -p /tmp
COPY ssh_setup.sh /tmp
RUN chmod +x /tmp/ssh_setup.sh \
    && (sleep 1;/tmp/ssh_setup.sh 2>&1 > /dev/null)

#Copy 
RUN mkdir -p /data
COPY core_setup.sh /data
#working_dir: /var/solr/data
   # volumes:
    # Mount our local Solr core configs so that they are available as Solr configsets on container
  #  - ./dspace/solr/authority:/opt/solr/server/solr/configsets/authority
   # - ./dspace/solr/oai:/opt/solr/server/solr/configsets/oai
   # - ./dspace/solr/search:/opt/solr/server/solr/configsets/search
   # - ./dspace/solr/statistics:/opt/solr/server/solr/configsets/statistics
    # Keep Solr data directory between reboots
    #- solr_data:/var/solr/data

# Open port 2222 for SSH access
EXPOSE 8983 8009 2222

VOLUME /var/solr
WORKDIR /opt/solr
USER $SOLR_USER

ENTRYPOINT ["docker-entrypoint.sh"]



# CMD /usr/sbin/sshd -R && catalina.sh ru

CMD solr-foreground
