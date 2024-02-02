<!--
  ~ Copyright 2011-2024 The OTP authors
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
  -->

# OTP Exception Reporter

This job searches for Exceptions in the OTP log and reports them via mail to the recipients.

## Setup

To set this up simply copy the script into an appropriate directory on the OTP server and provide
the configuration file (see the provided example file) in the home of the otp user. The expected
path for the config file is: `$HOME/.config/otp-exception-reporter.conf`

**Available config values are:**
  - `OTP_LOGS`: The absolute path to the OTP log directory.
  - `RECIPIENTS`: A ` ` delimited list of mails to which to send the reports.

Then setup a cronjob with the following recommended mask for the OTP user:
```
59 * * * * /home/otp/scripts/otp-exception-reporter.sh
```

This makes it so the report is generated hourly. `59` is used as to not swallow the last report at
midnight.

## Points to note

Exceptions caused by code run from the groovy console are also logged and thus also appear in the
report. Because of this not every exception can be trusted. If something seems suspicious, check
in the log and search in the stacktrace if this was caused by a script.
