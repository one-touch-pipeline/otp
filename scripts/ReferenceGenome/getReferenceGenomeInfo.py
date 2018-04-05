#!/usr/bin/python

# This script is used for extracting names and length values of entries in the fasta file (reference genome).
# It can be used on the cluster or on a local machine, and depends on BioPython.
#
# the call: python2 ../getReferenceGenomeInfo.py hs37d5_GRCm38mm10_PhiX.conv.fa
#
# How to install BioPython for Python 2:
#    sudo zypper in python-pip python-devel python-numpy-devel gcc
#    sudo pip2 --proxy PROXY_HOST:PROXY_PORT install biopython
# or for Python 3:
#    sudo zypper in python3-pip python3-devel python3-numpy-devel gcc
#    sudo pip3 --proxy PROXY_HOST:PROXY_PORT install biopython

import sys
import os.path
import re
try:
    from Bio import SeqIO
except ImportError:
    sys.exit("BioPython not installed.")

# check input
if len(sys.argv) == 1:
    sys.exit("Please supply fasta file.")
if len(sys.argv) == 2:
    if not os.path.isfile(sys.argv[1]):
        sys.exit("Supplied fasta file cannot be found.")

print("Reading fasta file from " + sys.argv[1])

existing_aliases = []
previous_record_w = None

handle = open(sys.argv[1], "rU")
for record in SeqIO.parse(handle, "fasta"):
    # if a record is duplicated with the suffixes _W and _C appended respectively, combine them without the suffix
    # this assumes that the duplicated entries occur consecutively, with _W first
    if record.id.endswith("_W"):
        previous_record_w = record
        continue
    if previous_record_w:
        if record.id.endswith("_C") \
                and record.id[:-2] == previous_record_w.id[:-2] \
                and len(record.seq) == len(previous_record_w.seq) \
                and len(str(record.seq).replace("N", "")) == len(str(previous_record_w.seq).replace("N", "")):
            record.id = record.id[:-2]
            previous_record_w = None
        else:
            raise Exception("Unexpected duplicated entries with _W and _C?")

    # alias and classification
    alias = record.id
    classification = "UNDEFINED"
    chromosome = re.match(r"^(chr)?(\d{1,2}|X|Y)$", record.id)
    if chromosome:
        if chromosome.group(2) not in existing_aliases:
            alias = chromosome.group(2)
        classification = "CHROMOSOME"
    elif re.match(r"^(chr)?(M|MT)$", record.id):
        if "M" not in existing_aliases:
            alias = "M"
        classification = "MITOCHONDRIAL"
    elif re.match(r"^(chr)?(\d{1,2}|X|Y|Un)?_?[A-Z]{2}\d{6}.\d(_random)?$", record.id):
        classification = "CONTIG"
    existing_aliases.append(alias)
    # length including N
    withN = len(record.seq)
    # length excluding N
    withoutN = len(str(record.seq).replace("N", ""))

    # print directly in format for groovy
    print("new FastaEntry(\"{0}\", \"{1}\", {2}, {3}, Classification.{4}),".
          format(record.id, alias, withN, withoutN, classification))
handle.close()
