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
- Tutorial: https://www.commonwl.org/user_guide/
- Specification of file system: https://www.commonwl.org/v1.0/Workflow.html

Since the tutorial uses the tool cwltool, I have also used it.

#Installation:
The tool can be installed on the cluster in your home with:
```
module load python/2.7.9
pip install virtualenv
virtualenv ~/venv2
source ~/venv2/bin/activate

pip install cwlref-runner
pip install cwltool
```

#Prepare console
Since it use an enviroment, every new console needs to be prepared with:
```
module load python/2.7.9
. ~/venv2/bin/activate
```

# Run jobs and workflows
It is possible to run each job itself, or also the complete workflow.

- job to copy file: \
  `cwl-runner --debug copy.cwl copy.yml`
- job to create md5sum file: \
  `cwl-runner --debug createMd5sum.cwl    createMd5sum.yml`
- job to check a md5sum: \
  `cwl-runner --debug checkMd5sum.cwl     checkMd5sum.yml`
- workflow to copy and check a single file: \
  `cwl-runner --debug copyAndCheck.cwl    copyAndCheck.yml`
- workflow to copy and check multiple files: \
  `cwl-runner dataInstallation.cwl dataInstallation.yml`
