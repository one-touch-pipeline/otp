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

The tool provide only the wes interface, it use cwltool to run the cwl workflows.

# Used documentation:
https://pypi.org/project/wes-service/


##Installation:
The tool can be installed on the cluster in your home with:
```
module load python/2.7.9

virtualenv ~/venv-wes
source ~/venv-wes/bin/activate

pip install cwlref-runner
pip install cwltool
pip install wes-service
```

##Prepare console
Since it use an environment, every new console needs to be prepared with:
```
module load python/2.7.9
source ~/venv-wes/bin/activate

export WES_API_HOST=localhost:8080
export WES_API_AUTH=my_api_token
export WES_API_PROTO=http
```

#start server:
```
wes-server --debug
```

#request:
```
wes-client --list

wes-client 1st-tool.cwl echo-job.yml
```
