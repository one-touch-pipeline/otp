
initial data
=============

OTP needs diverse initialisation to run.


.otp.properties
---------------

Since this configuration is already needed to setup tomcat it is described there.

.sql
----

OTP use two database views. Since in grails there are no difference to access tables or views,
the mapping is the same and they are created as tables. Therefore the tables needs to be deleted
and the views need to be created.

Please execute the following sql script to delete the tables:
```
BEGIN;

DROP TABLE sequences;

DROP TABLE aggregate_sequences;

COMMIT;
```

Please execute the sql scripts to create the database view. They are located in the 'scripts/dbview' directory.


groovy
------

Further initialisation needs to be done in the groovy web console.

The scripts used for initialisation are in the 'scripts/rollout' directory.
The values needs to be adapted to the location.

Also the workflows needs to be loaded. The scripts therefore are located 'scripts/workflows'.


[Back to Rollout Overview](index.md)
