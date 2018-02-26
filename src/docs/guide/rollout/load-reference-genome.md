Load reference genome
================

Note
----

This documentation is of load a prepared reference genome. It does not describe how to prepare new reference genome.


Available reference genomes
---------------------------


- reference genomes not methylated (for non WGBS)
    - 1KGRef_PhiX
    - GCA_000001405.15_GRCh38
    - GRCm38mm10_PhiX
    - GRCm38mm10_PhiX_hD3A
    - hg38
    - hs37d5_Bovine_Phix
    - hs37d5_GRCm38mm_PhiX
- methylated reference genomes (for WGBS):
    - methylCtools_GRCm38mm10_PhiX_Lambda
    - methylCtools_hg38_PhiX_Lambda
    - methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda
    - methylCtools_hs37d5_PhiX_Lambda
    - methylCtools_mm10_UCSC_PhiX_Lambda
    - methylCtools_mm9_PhiX_Lambda


We have also the following reference genomes, but the load script for them is outdated:
- GRCm38mm10
- hg19
- hs37d5_GRCm38mm
- hs37d5_GRCm38mm_PhiX
- hs37d5



Variables
---------

- $REFERENCE_GENOME_BASE_PATH: The base path where all the reference genomes are stored,
  for example $WORK_DIRECTORY/reference_genome
- $REFERENCE_GENOME: The new reference genome


Prepare reference genome
------------------------

* Get the reference genome inclusive all depending files from the OTP team.
    <br> Get also the script to load it to OTP from the OTP team.

* Save it to the directory $REFERENCE_GENOME_BASE_PATH/$REFERENCE_GENOME_DIRECTORY

* Open the web console of OTP

* Copy the script to load the reference genome into it and execute it.



[Back to Rollout Overview](index.md)
