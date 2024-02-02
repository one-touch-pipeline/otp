-- Copyright 2011-2024 The OTP authors
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

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
