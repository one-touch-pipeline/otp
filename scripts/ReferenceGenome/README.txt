How to add a new reference genome to OTP:

1. Run getReferenceGenomeInfo.py on a fasta file for the new reference genome X
2. Create groovy script (LoadReferenceGenome_X.groovy) containing the output from
   getReferenceGenomeInfo.py, the script also contains the mapping from reference
   sequence names in the fasta file and the alias used in OTP. Execute the script.
3. Run WriteReferenceGenomeMetafile.groovy for reference genome X which will
   create a simple tsv file holding the reference sequence names and the length
   values. This file is required by qa.jar
4. create bwa index files
