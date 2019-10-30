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


# Used documentation:
I use the following documentation:
- Documentation: https://cromwell.readthedocs.io/en/develop/tutorials/FiveMinuteIntro/
- Repository: https://github.com/broadinstitute/cromwell

The tool can execute workflows locally and on many cluster systems including LSF.

In my example it is configured for using LSF. Therefore, it needs to be executed on a LSF submission host.


#Installation:
Download the jar: https://github.com/broadinstitute/cromwell/releases/download/47/cromwell-47.jar

Save the jar in your home: ~/cromwell-47.jar


#Prepare console
The Java module needs to be loaded:
```
module load java/1.8.0_131
```

# Run jobs and workflows
It is possible to run each job itself, or also the complete workflow.

see script `script.sh`


# Server Mode
When running cromwell in [server mode](https://cromwell.readthedocs.io/en/develop/tutorials/ServerMode/), the input files have to be absolute with the file protocol prefix (`file://`). This setup uses cromwell in the non-server "Run" mode.


#WES
There exist a directory "wes2cromwell/src/main/" in the repository, but how to use is not mention in the documentation.
Also there exist a separate repository "https://github.com/broadinstitute/wes2cromwell", but without any documentation and no changes since two years.
