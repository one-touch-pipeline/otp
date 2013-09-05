#!/usr/bin/python
import sys
import os.path

# This script can be used on the tbi cluster or on a local machine
# and depends on BioPython. It's used for extracting names and
# length values of entries in the fasta file (reference genome)

# check input
if len(sys.argv) == 1:
	sys.exit("Please supply fasta file!")
if len(sys.argv) == 2:
	if os.path.isfile(sys.argv[1]) == False:
		sys.exit("Supplied fasta file cannot be found!")

print "Reading fasta file from " + sys.argv[1]

from Bio import SeqIO
handle = open(sys.argv[1], "rU")
for record in SeqIO.parse(handle, "fasta") :
    # length including N
    withN = len(record.seq)
    # length excluding N
    withoutN = len(str(record.seq).replace("N", ""))
    # print directly in list format for groovy
    print "[\"" + record.id + "\", \"" + record.id + "\", " + str(withN) + ", " + str(withoutN) + ", \"UNDEFINED\"],"
handle.close()
