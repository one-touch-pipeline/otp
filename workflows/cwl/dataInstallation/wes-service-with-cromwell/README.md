<!--
  ~ Copyright 2011-2019 The OTP authors
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


#Info
The documentation use wes-server with cromwell.

Therefore a wrapper script is used to connect both. Since it is only for testing, only
the parameters workflow and input file are supported, which both are required.

See also:
 * [wes server](../wes-server/README.md)
 * [cromwell](../cromwell/README.md)

#Installation:
## wes server
The wes tool can be installed on the cluster in your home with:
```
module load python/2.7.9

virtualenv ~/venv-wes-cromewell
source ~/venv-wes-cromewell/bin/activate

pip install wes-service
```

##cromwell
Download the jar: https://github.com/broadinstitute/cromwell/releases/download/47/cromwell-47.jar

Save the jar in your home: ~/cromwell-47.jar

#Prepare console
Since it use an environment, every new console needs to be prepared with:
```
module load python/2.7.9
module load java/1.8.0_131

source ~/venv-wes-cromewell/bin/activate

export WES_API_HOST=localhost:8080
export WES_API_AUTH=my_api_token
export WES_API_PROTO=http
```

#start server:
Ensure, that the script 'cwl-runner' is available in the PATH variable.
```
wes-server --debug
```

# Run jobs and workflows
It is possible to run each job itself, or also the complete workflow.
The server needs to be run on the same host.

see script `script.sh`
