#!/bin/bash

# Copyright 2011-2019 The OTP authors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

set -e

module load python/2.7.9

. ~/venv2/bin/activate

cd ~/cwl/dataInstallation

set -v

# job to copy file
#cwl-runner --debug copy.cwl            copy.yml

#job to create md5sum file
#cwl-runner --debug createMd5sum.cwl    createMd5sum.yml

#job to check a md5sum
#cwl-runner --debug checkMd5sum.cwl     checkMd5sum.yml

#workflow to copy file, create md5sum and check md5sum
#cwl-runner --debug copyAndCheck.cwl    copyAndCheck.yml

#workflow to copy and check multiple files
cwl-runner dataInstallation.cwl dataInstallation.yml
