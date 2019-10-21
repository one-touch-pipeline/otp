#!/usr/bin/env cwl-runner

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

cwlVersion: v1.0
class: Workflow

inputs:
  inputFile:
    type: File
  outputFileName:
    type: string
  md5sum:
    type: string

outputs:
  copiedFile:
    type: File
    outputSource: copy/copiedFile
  md5sumFile:
    type: File
    outputSource: createMd5sum/md5sumFile

steps:
  copy:
    run: copy.cwl
    in:
      inputFile: inputFile
      outputFileName: outputFileName
    out: [copiedFile]

  createMd5sum:
    run: createMd5sum.cwl
    in:
      fileName: outputFileName
      md5sum: md5sum
    out: [md5sumFile]

  checkMd5sum:
    run: checkMd5sum.cwl
    in:
      inputFile: copy/copiedFile
      md5sumFile: createMd5sum/md5sumFile
    out: []
