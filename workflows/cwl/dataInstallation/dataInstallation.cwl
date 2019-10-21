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
  inputFiles:
    type: File[]
  outputFileNames:
    type: string[]
  md5sums:
    type: string[]

outputs:
  copiedFiles:
    type: File[]
    outputSource: copyAndCheck/copiedFile
  md5sumFiles:
    type: File[]
    outputSource: copyAndCheck/md5sumFile

requirements:
  SubworkflowFeatureRequirement: {}
  ScatterFeatureRequirement: {}

steps:
  copyAndCheck:
    run: copyAndCheck.cwl
    scatter:
      - inputFile
      - outputFileName
      - md5sum
    scatterMethod: dotproduct
    in:
      inputFile: inputFiles
      outputFileName: outputFileNames
      md5sum: md5sums
    out: [copiedFile, md5sumFile]
