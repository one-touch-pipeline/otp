#!/bin/sh

# Copyright 2011-2024 The OTP authors
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

# needed log dir
mkdir -p logs/jobs

# add required otp properties
cp $OTP_PROPERTIES_BASE ~/.otp.properties

# npm cache redirect
mkdir -p $NPM_CACHE
ln -s $NPM_CACHE $HOME/.npm

# cypress cache redirect
mkdir -p $CYPRESS_CACHE
mkdir -p $HOME/.cache
ln -s $CYPRESS_CACHE $HOME/.cache/Cypress

# create gradle build cache dir redirect
# having the build dir cache within the cache directory make permission problems during restoring caches,
# since directories for restoring caches are created as root with 777, but gradle requires the permission 700 for the cache directory
mkdir -p $GRADLE_BUILD_CACHE
mkdir -p $GRADLE_USER_HOME/caches
rm -rf $GRADLE_USER_HOME/caches/build-cache-1
ln -s $GRADLE_BUILD_CACHE $GRADLE_USER_HOME/caches/build-cache-1
