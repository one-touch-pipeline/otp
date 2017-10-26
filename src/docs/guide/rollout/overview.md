General remarks
===============

Host
----

For the installation 2 host are recommended:
- one for the database, in following referenced: $DB_HOST
- one for the Webserver (tomcat), in following referenced: $WEB_HOST

This  documentation use following versions:
- Host system: CentOS Linux release 7.4
- postgres version: 10
- tomcat version: 8.5.23

If you use other version adjustment may be necessary.


CLUSTER
-------

Additional a cluster is needed, where the complex jobs are running. Currently the following system are supported:
- PBS
- LSF (support currently added)


User
----

For the installation the following users are needed:
- postgres user on $DB_HOST for running postgres. It is created on $DB_HOST during postgres installation.
- tomcat user on $WEB_HOST: The user under which tomcat runs: $WEB_USER
- file and cluster management user on the cluster: $CLUSTER_USER


GROUP
-----

Additional a common group for $WEB_USER and $CLUSTER_USER is needed, which is used to protect directories
from access for normal users: $COMMON_GROUP


LDAP
----
- $LDAP_SERVER: name of the ldap server
- $LDAP_MANAGER_PASSWD: Password to access the ldap, can be empty
- $LDAP_SEARCH_BASE: part of the ldap tree to search in, for example: 'ou\=users,dc\=example,dc\=com'
- $LDAP_MANAGER_DN:
- $LDAP_SEARCH_FILTER: The filter to search for people, for example: '(uid\={0})'

Please note that all '=' in the values needs to be escaped with backslash


Additional Variables
--------------------

The following additional variables are used:
- $IP_WEB: the ip of the host $WEB_HOST
- $IP_DB: the ip of the host $DB_HOST
- $TOMCAT_VERSION: the version of tomcat, here 8.5.23
- $DOWNLOAD_PATH: the path used for download
- $WORK_DIRECTORY


[Back to Rollout Overview](index.md)
