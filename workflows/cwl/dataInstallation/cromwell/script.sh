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

module load java/1.8.0_131

set -v

export BASE_COMMAND="java -Dconfig.file=config.conf -jar $HOME/cromwell-47.jar run --type CWL "

# job to copy file
#$BASE_COMMAND ../copy.cwl -i ../copy.yml

#job to create md5sum file
#$BASE_COMMAND ../createMd5sum.cwl -i ../createMd5sum.yml

#job to check a md5sum (file name may not contain a path component, since it is used for the md5sum name)
#$BASE_COMMAND ../checkMd5sum.cwl -i checkMd5sum.yml

#workflow to copy file, create md5sum and check md5sum
#$BASE_COMMAND ../copyAndCheck.cwl -i ../copyAndCheck.yml

#workflow to copy and check multiple files
$BASE_COMMAND ../dataInstallation.cwl -i ../dataInstallation.yml
