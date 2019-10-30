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
- https://github.com/IBMSpectrumComputing/cwlexec

#Installation:

Download the tool from: https://github.com/IBMSpectrumComputing/cwlexec/releases/download/v0.2.2/cwlexec-0.2.2.tar.gz

Extract the tool to your home in the cluster: ~/cwlexec-0.2.2

#Prepare console
The console needs to be prepared with:
```
module load java/1.8.0_131

export PATH="$PATH:~/cwlexec-0.2.2"
```

# Run jobs and workflows
It is possible to run each job itself, or also the complete workflow.

see script `script.sh`

#Notes:
 - it is possible to link input files in work dir
 - output files are always copied
   - to input of next job
   - to final output directory
 - does not handle scattering correctly --> can not be used
 - output comes always in a sub directory of the specified
 - work data are stay
 - workdir and outdir have to be absolute, otherwise there are problems with double paths elements
