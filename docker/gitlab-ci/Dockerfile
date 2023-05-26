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

FROM azul/zulu-openjdk-centos:11-latest
MAINTAINER OTP Developers <otp-devel@dkfz-heidelberg.de>

RUN rpm -ivh http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

# Install dependencies
RUN yum -y update && \
    yum -y install \
        alsa-lib \
        GConf2 \
        git \
        gtk2-devel  \
        gtk3-devel  \
        jq \
        libnotify-devel \
        libXScrnSaver  \
        nss  \
        postgresql.x86_64 \
        python-mako.noarch \
        unzip \
        which \
        xorg-x11-server-Xvfb  \
        yum-utils && \
    yum-config-manager \
        --add-repo \
        https://download.docker.com/linux/centos/docker-ce.repo && \
    yum -y install docker-ce docker-ce-cli containerd.io docker-compose-plugin && \
    yum clean all

# Configure system via environment variables
ENV GRADLE_USER_HOME /home/otp/.gradle

# User management
RUN useradd -u 911 --system --create-home --shell /bin/bash otp
RUN groupadd othergroup
RUN usermod --append --groups othergroup otp

USER otp
WORKDIR /home/otp

# OTP needs the USER environment variable to be defined.
ENV USER otp
