How to add a new reference genome to OTP:

1. Run getReferenceGenomeInfo.py on a fasta file for the new reference genome ${name}
2. Create groovy script (LoadReferenceGenome_${name}.groovy); use an exiting file
   which uses ReferenceGenomeService.loadReferenceGenome as template.
   Insert the output from getReferenceGenomeInfo.py, adjust if necessary.
   Execute the script.
3. Run WriteReferenceGenomeMetafile.groovy for reference genome ${name} which will
   create a simple tsv file holding the reference sequence names and the length
   values. This file is required by qa.jar
4. create bwa index files
