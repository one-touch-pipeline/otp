
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

- install postgres 10
  <br>(see *https://yum.postgresql.org* if you want to install another version,
  ensure that it is compatble with the jdbc driver used in OTP):
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

- adapt listener of ports
    - open file
        ```
        sudo -u postgres vi /var/lib/pgsql/10/data/postgresql.conf
        ```

    - adapt value for property listen_addresses ($IP = the ip of $WEB_HOST)
        <br>Set value to 'localhost,$IP_WEB,$IP_DB'

- adapt access filter file of postgres
    - open file
        ```
        sudo -u postgres vi /var/lib/pgsql/10/data/pg_hba.conf
        ```

    - add ip of web and db host entry at the end
        ```
        host    all             all             $IP_WEB/32     md5
        host    all             all             $IP_DB/32      md5
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

- open port 5432 and reload firewall
    ```
    firewall-cmd --permanent  --zone=public --add-port=5432/tcp
    firewall-cmd --reload
    ```

[Back to Rollout Overview](index.md)
