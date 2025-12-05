#!/bin/bash
#
# Copyright 2011-2026 The OTP authors
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
#

# the script create an mocked fastqc zip file for the bash fastqc workflow , containing all expected files as dummy, + for the parsed file the expected entries

set -e -o pipefail

cd /tmp

mkdir -p fastq/asdf_fastqc
cd fastq/asdf_fastqc
mkdir Icons Images

cat <<EOF | while read line ; do echo $line > $line ; done
Icons/fastqc_icon.png
Icons/warning.png
Icons/error.png
Icons/tick.png
summary.txt
Images/per_base_quality.png
Images/per_tile_quality.png
Images/per_sequence_quality.png
Images/per_base_sequence_content.png
Images/per_sequence_gc_content.png
Images/per_base_n_content.png
Images/sequence_length_distribution.png
Images/duplication_levels.png
Images/adapter_content.png
Images/kmer_profiles.png
fastqc_report.html
fastqc.fo
EOF

(
  echo ""
  echo -e "Total Sequences\t100"
  echo -e "Sequence length\t100"
  echo ""
)> fastqc_data.txt

cd ..

zip -r asdf_fastqc.zip asdf_fastqc

mv asdf_fastqc.zip $REFERENCE_DATA/fastqFiles/fastqc/asdf_fastqc.zip

rm -rf asdf_fastqc
