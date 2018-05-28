
tomcat installation
===================

local
-----

- Download the latest [tomcat](http://tomcat.apache.org/) 8.5 version
  from [tomcat 8 download](http://tomcat.apache.org/download-80.cgi)

- Download OTP from [gitlab](https://odcf-gitlab.dkfz.de/DMG/otp/-/jobs/artifacts/master/download?job=war)
  with name artifacts.zip

- copy files the thw web server of the web host
    ```
    scp $DOWNLOAD_PATH/$TOMCAT_VERSION $WEB_USER@$WEB_HOST:
    scp $DOWNLOAD_PATH/artifacts.zip $WEB_USER@$WEB_HOST:
    ```

remote as root
--------------

- login on $WEB_HOST as root
    ```
    ssh root@$WEB_HOST
    ```

- check the used distribution:
    ```
    cat /etc/os-release
    ```

- install java
    ```
    sudo yum install java
    ```

- open port 8080 and reload firewall
    ```
    firewall-cmd --permanent  --zone=public --add-port=5432/tcp
    firewall-cmd --reload
    ```

- increase count of open files in '/etc/security/limits.conf'
    ```
    * hard nofile 131072
    * soft nofile 65536
    ```

- logout
    ```
    exit
    ```

remote as web user
------------------

- login on $WEB_HOST as $WEB_USER
    ```
    ssh $WEB_USER@$WEB_HOST
    ```

- create otp property file "~.otp.properties" with the following content
    ```
    # settings for LDAP authentication
    otp.security.ldap.server=$LDAP_SERVER
    otp.security.ldap.enabled=true
    otp.security.ldap.search.subTree=true
    otp.security.ldap.managerPw=$LDAP_MANAGER_PASSWD
    otp.security.ldap.search.base=$LDAP_SEARCH_BASE
    otp.security.ldap.managerDn=$LDAP_MANAGER_DN
    otp.security.ldap.search.filter=$LDAP_SEARCH_FILTER

    # basepath of the folders with the final genome data
    otp.root.path=$WORK_DIRECTORY/project
    # basepath of the folders where the temporary processing data are stored
    otp.processing.root.path=$WORK_DIRECTORY/processing

    # basepath for the logs of the cluster jobs
    otp.logging.root.path=$WORK_DIRECTORY/staging/log/cluster
    # the logs of the OTP jobs are stored here
    otp.logging.jobLogDir=$WORK_DIRECTORY/staging/log/joblogs
    # directory where error messages/stacktraces from OTP jobs are written
    otp.errorLogging.stacktraces=$WORK_DIRECTORY/staging/otpStacktraces

    # settings for database
    otp.database.server=$DB_HOST
    otp.database.port=5432
    otp.database.database=otp
    otp.database.username=otp
    otp.database.password=otp

    # URL where OTP is reachable
    otp.server.url=http://$WEB_HOST:8080/otp

    # settings for Mails
    otp.mail.allowOtpToSendMails=false

    # flag, if job system should try to start or not on OTP start
    otp.jobsystem.start=false

    # settings for SSH connections
    # should ssh-agent be used to get the password for the ssh key (true or false)
    # if false, only key files without password can be used
    # if true, an ssh-agent must be running and the key must be added to it, even if the key file doesn't have a password
    otp.ssh.authMethod=sshagent
    otp.ssh.user=$CLUSTER_USER
    ```

- create the different work directories
    ```
    mkdir -p \
        $WORK_DIRECTORY/project \
        $WORK_DIRECTORY/processing \
        $WORK_DIRECTORY/staging/project \
        $WORK_DIRECTORY/staging/log/cluster/log/status/ \
        $WORK_DIRECTORY/staging/log/joblogs \
        $WORK_DIRECTORY/staging/otpStacktraces
    ```

- create a rsa ssh key without passphrase
    ```
    ssh-keygen -t rsa -b 4096
    ```

- copy key to cluster server
    ```
    ssh-copy-id $CLUSTER_USER@$SUBMISSION_HOST
    ```

- unzip the otp artifacts
    ```
    unzip artifacts.zip
    ```

- unpack the archive
    ```
    tar -xf apache-tomcat-$TOMCAT_VERSION.tar.gz
    ```

- link tomcat version as tomcat (optional)
    ```
    ln -s apache-tomcat-$TOMCAT_VERSION tomcat
    ```

- remove default application (optional)
    ```
    rm -r  ~/apache-tomcat-$TOMCAT_VERSION/webapps/*
    ```

- mv otp to the webapps directory
    ```
    mv ~/otp.war ~/apache-tomcat-$TOMCAT_VERSION/webapps/
    ```

- start tomcat and check log
    ```
    ~/apache-tomcat-$TOMCAT_VERSION/bin/startup.sh ; tail -f ~/apache-tomcat-$TOMCAT_VERSION/logs/catalina.out
    ```



[Back to Rollout Overview](index.md)
