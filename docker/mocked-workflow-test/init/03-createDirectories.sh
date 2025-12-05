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

set -e -o pipefail

cd $REFERENCE_DATA

mkdir aceseq
mkdir adapters
mkdir bamFiles
mkdir bamFiles/small
mkdir bamFiles/small/div8
mkdir bamFiles/small/div32
mkdir bamFiles/small/div128
mkdir bamFiles/small/div64
mkdir bamFiles/wgs
mkdir bamFiles/wgs/first-bam-file
mkdir fastqFiles
mkdir fastqFiles/10x
mkdir fastqFiles/10x/normal
mkdir fastqFiles/10x/normal/paired
mkdir fastqFiles/chipSeq
mkdir fastqFiles/chipSeq/normal
mkdir fastqFiles/chipSeq/normal/paired
mkdir fastqFiles/chipSeq/normal/paired/run1
mkdir fastqFiles/chipSeq/normal/paired/run1/sequence
mkdir fastqFiles/chipSeq/normal/paired/run2
mkdir fastqFiles/chipSeq/normal/paired/run2/sequence
mkdir fastqFiles/wgs
mkdir fastqFiles/wgs/normal
mkdir fastqFiles/wgs/normal/paired
mkdir fastqFiles/wgs/normal/paired/run1
mkdir fastqFiles/wgs/normal/paired/run1/sequence
mkdir fastqFiles/wgs/normal/paired/run2
mkdir fastqFiles/wgs/normal/paired/run2/sequence
mkdir fastqFiles/wgs/tumor
mkdir fastqFiles/wgs/tumor/paired
mkdir fastqFiles/wgs/tumor/paired/run1
mkdir fastqFiles/wgs/tumor/paired/run1/sequence
mkdir fastqFiles/wgbs
mkdir fastqFiles/wgbs/normal
mkdir fastqFiles/wgbs/normal/paired
mkdir fastqFiles/wgbs/normal/paired/lib1
mkdir fastqFiles/wgbs/normal/paired/lib1/run1
mkdir fastqFiles/wgbs/normal/paired/lib1/run1/sequence
mkdir fastqFiles/wgbs/normal/paired/lib2
mkdir fastqFiles/wgbs/normal/paired/lib2/run1
mkdir fastqFiles/wgbs/normal/paired/lib2/run1/sequence
mkdir fastqFiles/wgbs/tumor
mkdir fastqFiles/wgbs/tumor/paired
mkdir fastqFiles/wgbs/tumor/paired/lib5
mkdir fastqFiles/wgbs/tumor/paired/lib5/run1
mkdir fastqFiles/wgbs/tumor/paired/lib5/run1/sequence
mkdir fastqFiles/wes
mkdir fastqFiles/wes/tumor
mkdir fastqFiles/wes/tumor/paired
mkdir fastqFiles/wes/tumor/paired/run4
mkdir fastqFiles/wes/tumor/paired/run4/sequence
mkdir fastqFiles/fastqc
mkdir fastqFiles/rna
mkdir reference-genomes
mkdir reference-genomes/hg_GRCh38
mkdir reference-genomes/hg_GRCh38/indexes
mkdir reference-genomes/hg_GRCh38/indexes/cellranger
mkdir reference-genomes/hg_GRCh38/indexes/cellranger/1.2.0
mkdir reference-genomes/hg_GRCh38/indexes/cellranger/1.2.0/genes
mkdir reference-genomes/hg_GRCh38/indexes/cellranger/1.2.0/fasta
mkdir reference-genomes/hg_GRCh38/indexes/cellranger/1.2.0/star
mkdir reference-genomes/hg_GRCh38/indexes/cellranger/1.2.0/pickle
mkdir reference-genomes/bwa06_1KGRef
mkdir reference-genomes/bwa06_1KGRef/stats
mkdir reference-genomes/bwa06_1KGRef/targetRegions
mkdir reference-genomes/bwa06_1KGRef/databases
mkdir reference-genomes/bwa06_1KGRef/databases/UCSC
mkdir reference-genomes/bwa06_1KGRef/databases/IMPUTE
mkdir reference-genomes/bwa06_1KGRef/databases/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono
mkdir reference-genomes/bwa06_1KGRef/databases/IMPUTE/ALL_1000G_phase1integrated_v3_impute
mkdir reference-genomes/bwa06_1KGRef/databases/ENCODE
mkdir reference-genomes/bwa06_1KGRef/fingerPrinting
mkdir reference-genomes/bwa06_1KGRef_PhiX
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_50
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_50/STAR_2.5.2b_1KGRef_PhiX_Gencode19_50bp
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/gatk
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/arriba-blacklist
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/kallisto
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_100
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_100/STAR_2.5.2b_1KGRef_PhiX_Gencode19_100bp
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/arriba-fusion
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_200
mkdir reference-genomes/bwa06_1KGRef_PhiX/indexes/star_200/STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp
mkdir reference-genomes/bwa06_1KGRef_PhiX/gencode
mkdir reference-genomes/bwa06_1KGRef_PhiX/gencode/gencode19
mkdir reference-genomes/bwa06_1KGRef_PhiX/stats
mkdir reference-genomes/bwa06_1KGRef_PhiX/targetRegions
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases/UCSC
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases/IMPUTE
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases/IMPUTE/ALL_1000G_phase1integrated_v3_impute
mkdir reference-genomes/bwa06_1KGRef_PhiX/databases/ENCODE
mkdir reference-genomes/bwa06_1KGRef_PhiX/fingerPrinting
mkdir reference-genomes/bwa06_hg19_chr
mkdir reference-genomes/bwa06_hg19_chr/targetRegions
mkdir reference-genomes/bwa_hg38
mkdir reference-genomes/bwa_hg38/stats
mkdir reference-genomes/bwa06_methylCtools_hs37d5_PhiX_Lambda
mkdir reference-genomes/bwa06_methylCtools_hs37d5_PhiX_Lambda/stats
mkdir runYapsa
mkdir sophia
