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

set -ev

#create link of maven cache to place were gitlab ci allows caching
mkdir -p $GRADLE_CACHES $GRADLE_WRAPPER $HOME/.gradle
ln -s $GRADLE_CACHES $HOME/.gradle/caches
ln -s $GRADLE_WRAPPER $HOME/.gradle/wrapper

#needed log dir
mkdir -p logs/jobs

#add required otp properties
echo 'otp.testing.group=othergroup' >> $HOME/.otp.properties
echo 'otp.testing.project.unix.group=otp' >> $HOME/.otp.properties

#set proxy if given
`dirname $0`/use-proxy.sh

#set local repository
`dirname $0`/use-local-repository.sh
