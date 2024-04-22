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

FROM ghcr.io/linuxserver/openssh-server

ADD *.sh /custom-cont-init.d/
RUN mkdir -p /home/otp

# config per project and seq type files
ADD https://gitlab.com/one-touch-pipeline/otp/-/wikis/uploads/ci/config_files.tar.gz /home/otp

#cache file system for the dump
ADD https://gitlab.com/one-touch-pipeline/otp/-/wikis/uploads/ci/filesystem_20240405.tar.gz /home/otp/filesystem.tar.gz

# example data for fastq meta data import
ADD https://gitlab.com/one-touch-pipeline/otp/-/wikis/uploads/ci/v2/otp_example_data.tar.gz /home/otp

# example data for bam meta data import
ADD https://gitlab.com/one-touch-pipeline/otp/-/wikis/uploads/ci/bam-import.tar.gz /home/otp
