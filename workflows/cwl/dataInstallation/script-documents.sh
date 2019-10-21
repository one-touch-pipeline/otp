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

#validate the cwl
cwl-runner --validate         dataInstallation.cwl dataInstallation.yml

#Print workflow visualization in graphviz format and exit
cwl-runner --print-dot        dataInstallation.cwl dataInstallation.yml > print-dot.txt

#Print CWL document after preprocessing.
cwl-runner --print-pre        dataInstallation.cwl dataInstallation.yml > print-pre.txt

#Print CWL document dependencies.
cwl-runner --print-deps       dataInstallation.cwl dataInstallation.yml > print-deps.txt

#Print input object document dependencies.
cwl-runner --print-input-deps dataInstallation.cwl dataInstallation.yml > print-input-deps.txt

#Print workflow subgraph that will execute
cwl-runner --print-subgraph   dataInstallation.cwl dataInstallation.yml > print-subgraph.txt

#Print targets (output parameters)
cwl-runner --print-targets    dataInstallation.cwl dataInstallation.yml > print-targets.txt
