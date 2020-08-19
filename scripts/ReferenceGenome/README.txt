How to add a new reference genome to OTP:

Reference genomes with Phix or Lambda should mention it in its name

1. Run getReferenceGenomeInfo.py on a fasta file for the new reference genome ${name}
   python ./getReferenceGenomeInfo.py ${name} | tee ~/genome-entries.txt
2. Create groovy script (LoadReferenceGenome_${name}.groovy); use an exiting file as template.
   Insert the output from getReferenceGenomeInfo.py, adjust if necessary.
   Execute the script.
3. create bwa index files
