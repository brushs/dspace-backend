#!/bin/sh
echo setting DB password
export DB_PASSWORD=$(cat ./db_password.txt)
sed -i $(printf 's/@db-password@/%s/g' "$DB_PASSWORD") /app/dspace/config/local.cfg

echo setting storage account
export STORAGE_ACCOUNT=$(cat ./storage_account.txt)
sed -i $(printf 's:@storage-account@:%s:g' "$STORAGE_ACCOUNT") /app/dspace/config/local.cfg