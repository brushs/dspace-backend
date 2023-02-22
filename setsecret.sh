#!/bin/sh
echo setting DB password
export DB_PASSWORD=testpw
sed -i $(printf 's/@db-password@/%s/g' "$DB_PASSWORD") /app/dspace/config/local.cfg