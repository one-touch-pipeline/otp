
postgres installation
=====================


- login on $DB_HOST as root
```
ssh root@$DB_HOST 
```

- check the used distribution:
```
cat /etc/os-release
```

- install postgres
  (see *https://yum.postgresql.org* if you want to install another version):
```
yum install https://download.postgresql.org/pub/repos/yum/testing/10/redhat/rhel-7-x86_64/pgdg-centos10-10-2.noarch.rpm
yum install postgresql10.x86_64 postgresql10-server.x86_64
```

- init the database:
```
sudo -u postgres /usr/pgsql-10/bin/initdb -D /var/lib/pgsql/10/data/
```

- enable, start and check the database:
```
systemctl enable postgresql-10
systemctl start postgresql-10
systemctl status postgresql-10
```

- create user and schema
```
sudo -u postgres createuser -P otp
sudo -u postgres createdb -O otp otp
```

- adapt access filter file of postgres
  - open file
```
sudo -u postgres vi /var/lib/pgsql/10/data/pg_hba.conf
```
  - add ip of web host entry at the end ($IP = the ip of $WEB_HOST)
```
host    all             all             $IP/32      md5
```
  - deactivate following entries (add # at the line or remove them)
```
#host    all             all             127.0.0.1/32            trust
#host    all             all             ::1/128                 trust
```
  - restart postgres to load changed config
```
systemctl restart postgresql-10
```

[Back to Rollout Overview](index.md)
