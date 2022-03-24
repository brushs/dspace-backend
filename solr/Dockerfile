
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


RUN mkdir -p /tmp2
COPY core_setup.sh /tmp2
# RUN chmod +x /tmp2/core_setup.sh \
   # && (sleep 1;/tmp/core_setup.sh 2>&1 > /dev/null)

# Open port 2222 for SSH access
EXPOSE 8983 8009 2222

VOLUME /var/solr
WORKDIR /opt/solr
USER $SOLR_USER

ENTRYPOINT ["docker-entrypoint.sh"]



# CMD /usr/sbin/sshd -R && catalina.sh ru

CMD solr-foreground
