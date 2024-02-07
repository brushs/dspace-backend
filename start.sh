#!/bin/bash
# Start supervisord that manages cron
/usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf &
/usr/sbin/sshd &
catalina.sh run