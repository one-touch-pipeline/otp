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


# Create a gradle configuration file to use a local maven repository

if [[ ! -z "${MAVEN_REPOSITORY_URL}" ]]; then

    echo "The local maven repository will be configured in ~/.gradle/init.d/repo.gradle"

    mkdir -p ~/.gradle/init.d/

(
cat << EOF
settingsEvaluated { settings ->
    settings.pluginManagement {
        repositories {
            maven {
                url '${MAVEN_REPOSITORY_URL}'
            }
        }
    }
}

allprojects {
    repositories {
        maven {
            url '${MAVEN_REPOSITORY_URL}'
        }
    }
    buildscript.repositories {
        maven {
            url '${MAVEN_REPOSITORY_URL}'
        }
    }
}
EOF
) > ~/.gradle/init.d/repo.gradle

else

    echo "The local maven repository won't be configured in ~/.gradle/init.d/repo.gradle"

fi
