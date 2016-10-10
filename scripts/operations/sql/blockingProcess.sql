/*
Show all blocked database processes with query and the blocking process with query.

If a entries stay for longer time it is probably a deadlock.

You can solve a deadlock which killing one of the processes. That can be done which the following command:
SELECT pg_cancel_backend($pid);

The command has to be executed on the server in the psql as postgres user.

The query is taken from: https://wiki.postgresql.org/wiki/Lock_Monitoring
 */

SELECT
  bl.pid          AS blocked_pid,
  a.usename       AS blocked_user,
  kl.pid          AS blocking_pid,
  ka.usename      AS blocking_user,
  a.current_query AS blocked_statement
FROM pg_catalog.pg_locks bl
  JOIN pg_catalog.pg_stat_activity a ON a.procpid = bl.pid
  JOIN pg_catalog.pg_locks kl ON kl.transactionid = bl.transactionid AND kl.pid != bl.pid
  JOIN pg_catalog.pg_stat_activity ka ON ka.procpid = kl.pid
WHERE NOT bl.GRANTED;
