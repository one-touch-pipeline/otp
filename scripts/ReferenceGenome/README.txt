# Copyright 2011-2021 The OTP authors
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

# Extension of the main docker-compose file to add a network connection to the keycloak_oauth
# network which comes from the WESkit API docker setup.
#
# Run `docker-compose -f docker-compose.yml -f docker-compose.weskit.auth.yml up`
# to start otp within the extended setup.

How to add a new reference genome to OTP:

Reference genomes with Phix or Lambda should mention it in its name

1. Run getReferenceGenomeInfo.py on a fasta file for the new reference genome ${name}
   python ./getReferenceGenomeInfo.py ${name} | tee ~/genome-entries.txt
2. Create groovy script (LoadReferenceGenome_${name}.groovy); use an exiting file as template.
   Insert the output from getReferenceGenomeInfo.py, adjust if necessary.
   Execute the script.
3. create bwa index files
