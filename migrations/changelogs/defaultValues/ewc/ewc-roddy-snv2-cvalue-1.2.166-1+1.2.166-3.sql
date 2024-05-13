/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3',
        '{' ||
        '    "RODDY": {' ||
        '        "cvalues": {' ||
        '            "JAVA_BINARY": {' ||
        '                "value": "java"' ||
        '            },' ||
        '            "BASE_QUALITY_CUTOFF": {' ||
        '                "value": 0,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "CHROMOSOME_INDICES": {' ||
        '                "value": "( {1..22} X Y )",' ||
        '                "type": "bashArray"' ||
        '            },' ||
        '            "CHROMOSOME_INDICES_SORTED": {' ||
        '                "value": "( 2 1 3 4 5 6 7 X 8 10 11 12 9 13 14 15 16 17 18 19 20 21 22 Y )",' ||
        '                "type": "bashArray"' ||
        '            },' ||
        '            "CHR_PREFIX": {' ||
        '                "value": "\"\""' ||
        '            },' ||
        '            "CHR_SUFFIX": {' ||
        '                "value": "\"\""' ||
        '            },' ||
        '            "SEQUENCER_PROTOCOL": {' ||
        '                "value": "paired"' ||
        '            },' ||
        '            "SEQUENCER_STRATEGY": {' ||
        '                "value": "wholeGenome"' ||
        '            },' ||
        '            "WINDOW_SIZE": {' ||
        '                "value": 1,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "RSCRIPT_VERSION": {' ||
        '                "value": "3.0.0",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "PERL_VERSION": {' ||
        '                "value": "5.20.2",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "SAMTOOLS_VERSION": {' ||
        '                "value": "0.1.19",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "PYTHON_VERSION": {' ||
        '                "value": "2.7.9",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "BEDTOOLS_VERSION": {' ||
        '                "value": "2.16.2",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "HTSLIB_VERSION": {' ||
        '                "value": "0.2.5",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "workflowEnvironmentScript": {' ||
        '                "value": "workflowEnvironment_tbiLsf",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mpileupOutputDirectory": {' ||
        '                "value": "./"' ||
        '            },' ||
        '            "MPILEUP_OPTS": {' ||
        '                "value": "\"-REI -q 30 -ug\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "BCFTOOLS_OPTS": {' ||
        '                "value": "\"-vcgN -p 2.0\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "MPILEUPCONTROL_OPTS": {' ||
        '                "value": "\"-ABRI -Q 0 -q 1\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "MPILEUPOUT_PREFIX": {' ||
        '                "value": "mp_",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "INDELFILE_PREFIX": {' ||
        '                "value": "indel_",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "RAW_SNV_FILTER_OPTIONS": {' ||
        '                "value": "\" --minVac=3 --minVaf=0.03 --minVacPS=2\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "DBSNP": {' ||
        '                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_141/00-All.SNV.vcf.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "DBSNP_COL": {' ||
        '                "value": "DBSNP",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "KGENOME": {' ||
        '                "value": "${hg19DatabasesDirectory}/1000genomes/ALL.wgs.phase1_integrated_calls.20101123.snps_chr.vcf.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "KGENOMES_COL": {' ||
        '                "value": "1K_GENOMES",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ANNOVAR_BINARY": {' ||
        '                "value": "${sharedFilesBaseDirectory}/annovar/annovar_Nov2014/annotate_variation.pl",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ANNOVAR_BUILDVER": {' ||
        '                "value": "hg19",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ANNOVAR_DBPATH": {' ||
        '                "value": "${sharedFilesBaseDirectory}/annovar/annovar_Nov2014/humandb/",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ANNOVAR_DBTYPE": {' ||
        '                "value": "''-dbtype wgEncodeGencodeCompV19''",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ANNOVAR_GENEANNO_COLS": {' ||
        '                "value": "\"ANNOVAR_FUNCTION,GENE,EXONIC_CLASSIFICATION,ANNOVAR_TRANSCRIPTS\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ANNOVAR_SEGDUP_COL": {' ||
        '                "value": "SEGDUP",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ANNOVAR_CYTOBAND_COL": {' ||
        '                "value": "CYTOBAND",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GERMLINE_AVAILABLE": {' ||
        '                "value": "1",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "MIN_CONFIDENCE_SCORE": {' ||
        '                "value": 8,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "SNV_FILTER_OPTIONS": {' ||
        '                "value": "--ncRNA=1 --synonymous=0",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "MINCOV": {' ||
        '                "value": 0,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "ALLELE_FREQ": {' ||
        '                "value": 0,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "CLINICALANNO": {' ||
        '                "value": "\"\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "biasPValThreshold": {' ||
        '                "value": "0.01",' ||
        '                "type": "float"' ||
        '            },' ||
        '            "biasRatioMinimum": {' ||
        '                "value": "0.53",' ||
        '                "type": "float"' ||
        '            },' ||
        '            "biasRatioThreshold": {' ||
        '                "value": "0.63",' ||
        '                "type": "float"' ||
        '            },' ||
        '            "nReads": {' ||
        '                "value": 20,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "nMuts": {' ||
        '                "value": 4,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "maxNumOppositeReadsSequencingWeakBias": {' ||
        '                "value": 0,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "maxNumOppositeReadsSequenceWeakBias": {' ||
        '                "value": 0,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "maxNumOppositeReadsSequencingStrongBias": {' ||
        '                "value": 1,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "maxNumOppositeReadsSequenceStrongBias": {' ||
        '                "value": 1,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "rVcf": {' ||
        '                "value": "0.1",' ||
        '                "type": "float"' ||
        '            },' ||
        '            "MAPABILITY": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "HISEQDEPTH": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/HiSeqDepthTop10Pct_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "SIMPLE_TANDEMREPEATS": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/repeats/SimpleTandemRepeats_chr.bed.gz:4",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "REPEAT_MASKER": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/Sept2013/UCSC_27Sept2013_RepeatMasker.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "DUKE_EXCLUDED": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/DukeExcluded_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "DAC_BLACKLIST": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/DACBlacklist_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "SELFCHAIN": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/selfChain_chr.bed.gz:4::--maxNrOfMatches=5",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "CpGislands": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/Sept2013/UCSC_27Sept2013_CpG_islands.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "CgiMountains": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/CustomDeepAnnotation/CgiMountains_chr.bed.gz:4",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "Enhancers": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/Enhancers_Vista_2011_01_14_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "miRNAs_snoRNAs": {' ||
        '                "value": "${hg19DatabasesDirectory}/miRNA/miRNA_snoRNAs_miRBaseRelease15_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "miRBase18": {' ||
        '                "value": "${hg19DatabasesDirectory}/miRNA/miRBase_version-18_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "miRNAtargets": {' ||
        '                "value": "${hg19DatabasesDirectory}/miRNA/miRNAsites_TargetScan_BartelLab_2011_01_14_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "phastConsElem20bp": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/phastConsElem_min20bp_chr.bed.gz:4",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "TFBScons": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/tfbsConsSites_noncoding_merged_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "COSMIC": {' ||
        '                "value": "${hg19DatabasesDirectory}/COSMIC/Cosmic_v66_hg19_coding_SNVs.bed.gz:7,8,9:1",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ENCODE_DNASE": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/Sept2013/UCSC_27Sept2013_DNase_cluster_V2.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ENCODE_TFBS": {' ||
        '                "value": "${hg19DatabaseUCSCDirectory}/Sept2013/UCSC_27Sept2013_wgEncodeRegTfbsClusteredV3.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "sampleDirectory": {' ||
        '                "value": "${inputBaseDirectory}/${p' || 'id}/${sample}/${SEQUENCER_PROTOCOL}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "sequenceDirectory": {' ||
        '                "value": "${sampleDirectory}/${run}/sequence",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "outputAnalysisBaseDirectory": {' ||
        '                "value": "${outputBaseDirectory}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "outputExecutionDirectory": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/exec_${executionTimeString}"' ||
        '            },' ||
        '            "mergedBamSuffix_markDuplicates": {' ||
        '                "value": "merged.bam.dupmarked.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergedBamSuffix_markDuplicatesShort": {' ||
        '                "value": "merged.mdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergedBamSuffix_removeDuplicates": {' ||
        '                "value": "merged.bam.rmdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergedBamSuffixList": {' ||
        '                "value": "${mergedBamSuffix_markDuplicates},${mergedBamSuffix_markDuplicatesShort},${mergedBamSuffix_removeDuplicates}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "defaultMergedBamSuffix": {' ||
        '                "value": "${mergedBamSuffix_markDuplicatesShort}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "pairedBamSuffix": {' ||
        '                "value": "paired.bam.sorted.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "alignmentOutputDirectory": {' ||
        '                "value": "alignment"' ||
        '            },' ||
        '            "fastx_qcOutputDirectory": {' ||
        '                "value": "fastx_qc"' ||
        '            },' ||
        '            "coverageOutputDirectory": {' ||
        '                "value": "coverage"' ||
        '            },' ||
        '            "flagstatsOutputDirectory": {' ||
        '                "value": "flagstats"' ||
        '            },' ||
        '            "structuralVariationOutputDirectory": {' ||
        '                "value": "structural_variation"' ||
        '            },' ||
        '            "insertSizesOutputDirectory": {' ||
        '                "value": "insertsize_distribution"' ||
        '            },' ||
        '            "metricsOutputDirectory": {' ||
        '                "value": "metrics"' ||
        '            },' ||
        '            "mpileupPlatypusOutputDirectory": {' ||
        '                "value": "platypus_indel"' ||
        '            },' ||
        '            "assembliesBaseDirectory": {' ||
        '                "value": "${sharedFilesBaseDirectory}/assemblies",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "assembliesHG191000GenomesDirectory": {' ||
        '                "value": "${assembliesBaseDirectory}/hg19_GRCh37_1000genomes",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "bwaIndexBaseDirectory_human": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/indexes/bwa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "bwaIndexBaseDirectory_methylCtools_human": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/indexes/methylCtools",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "referenceGenomeBaseDirectory_human": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/sequence",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesBaseDirectory_human": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/stats",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegionsBaseDirectory_human": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/targetRegions",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19DatabasesDirectory": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/databases",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19DatabaseUCSCDirectory": {' ||
        '                "value": "${hg19DatabasesDirectory}/UCSC",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19DatabaseDBSNPDirectory": {' ||
        '                "value": "${hg19DatabasesDirectory}/dbSNP",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19Database1000GenomesDirectory": {' ||
        '                "value": "${hg19DatabasesDirectory}/1000genomes",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19DatabaseIMPUTEDirectory": {' ||
        '                "value": "${hg19Database1000GenomesDirectory}/IMPUTE",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "hg19DatabaseENCODEDirectory": {' ||
        '                "value": "${hg19DatabasesDirectory}/ENCODE",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "bwaIndexBaseDirectory_mm10": {' ||
        '                "value": "${assembliesBaseDirectory}/mm10/indexes/bwa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "bwaIndexBaseDirectory_methylCtools_mm10": {' ||
        '                "value": "${assembliesBaseDirectory}/mm10/indexes/methylCtools",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesBaseDirectory_mm10": {' ||
        '                "value": "${assembliesBaseDirectory}/mm10/stats",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegionsBaseDirectory_mm10": {' ||
        '                "value": "${assembliesBaseDirectory}/mm10/targetRegions",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "meth_calls_converter_moabs": {' ||
        '                "value": "",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "indexPrefix_bwa05_1KGRef": {' ||
        '                "value": "${bwaIndexBaseDirectory_human}/bwa05_1KGRef/hs37d5.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa05_hg19_chr": {' ||
        '                "value": "${bwaIndexBaseDirectory_human}/bwa05_hg19_chr/hg19bwaidx",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_1KGRef": {' ||
        '                "value": "${bwaIndexBaseDirectory_human}/bwa06_1KGRef/hs37d5.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_hg19_chr": {' ||
        '                "value": "${bwaIndexBaseDirectory_human}/bwa06_hg19_chr/hg19_1-22_X_Y_M.fasta",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_mm10_GRC": {' ||
        '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_GRCm38mm10/GRCm38mm10.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_mm10": {' ||
        '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_mm10_UCSC/mm10_1-19_X_Y_M.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_methylCtools_mm10_GRC": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.conv.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_methylCtools_mm10_UCSC": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.conv.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "indexPrefix_bwa06_methylCtools_1KGRef": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.conv.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ch_pos_index_methylCtools_1KGRef": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.CG_CH.pos.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ch_pos_index_methylCtools_mm10GRC": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.pos.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ch_pos_index_methylCtools_mm10_UCSC": {' ||
        '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.pos.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "referenceGenome_1KGRef": {' ||
        '                "value": "${referenceGenomeBaseDirectory_human}/1KGRef/hs37d5.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "referenceGenome_hg19_chr": {' ||
        '                "value": "${referenceGenomeBaseDirectory_human}/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_hg19": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_hs37": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_mm10_GRC": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_mm10": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_1-19_X_Y_M.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_hs37_bisulfite": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_mm10_GRC_bisulfite": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeSizesFile_mm10_UCSC_bisulfite": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "chromosomeLengthFile_hg19": {' ||
        '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_chrTotalLength.tsv",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent4withoutUTRs_chr": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent4withoutUTRs_plain": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_plain.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent4withUTRs_plain": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withUTRs_plain.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent5withoutUTRs_chr": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent5withoutUTRs_plain": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_plain.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent5withUTRs_chr": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_chr.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "targetRegions_Agilent5withUTRs_plain": {' ||
        '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_plain.bed.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "dbSNP_FILE": {' ||
        '                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_135/00-All.SNV.vcf.gz",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "SNP_REFERENCE": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/sequence/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "SNP_REFERENCE_ANNOTATIONS": {' ||
        '                "value": "${assembliesHG191000GenomesDirectory}/Affymetrics/Affy5/chr/#CHROM#_AFFY.vcf",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "alignmentThreads": {' ||
        '                "value": "12",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "outputUMask": {' ||
        '                "value": "007",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "outputFileGroup": {' ||
        '                "value": "false"' ||
        '            },' ||
        '            "outputAccessRights": {' ||
        '                "value": "u+rw,g+rw,o-rwx"' ||
        '            },' ||
        '            "outputAccessRightsForDirectories": {' ||
        '                "value": "u+rwx,g+rwx,o-rwx"' ||
        '            },' ||
        '            "possibleControlSampleNamePrefixes": {' ||
        '                "value": "( blood BLOOD normal control CONTROL buffy_coat GERMLINE )",' ||
        '                "type": "bashArray"' ||
        '            },' ||
        '            "possibleTumorSampleNamePrefixes": {' ||
        '                "value": "( tumor TUMOR metastasis xenograft disease DISEASE relapse RELAPSE autopsy AUTOPSY metastasis METASTASIS )",' ||
        '                "type": "bashArray"' ||
        '            },' ||
        '            "useCentralAnalysisArchive": {' ||
        '                "value": "true"' ||
        '            },' ||
        '            "enableJobProfiling": {' ||
        '                "value": "false"' ||
        '            },' ||
        '            "JOB_PROFILER_BINARY": {' ||
        '                "value": "strace.sh"' ||
        '            },' ||
        '            "INDEX_PREFIX": {' ||
        '                "value": "${indexPrefix_bwa05_hg19_chr}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "BWA_ALIGNMENT_OPTIONS": {' ||
        '                "value": "\"-q 20\""' ||
        '            },' ||
        '            "BWA_SAMPESORT_OPTIONS": {' ||
        '                "value": "\"-a 1000\""' ||
        '            },' ||
        '            "SAMPESORT_MEMSIZE": {' ||
        '                "value": 2000000000,' ||
        '                "type": "integer"' ||
        '            },' ||
        '            "BWA_MEM_OPTIONS": {' ||
        '                "value": "\" -T 0 \"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "BWA_MEM_CONVEY_ADDITIONAL_OPTIONS": {' ||
        '                "value": "\"--bb_cny_timeout=5000000000 --bb_profile=1 -t 8\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicates_optionMarkDuplicates": {' ||
        '                "value": "\" REMOVE_DUPLICATES=FALSE\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicates_removeDuplicates": {' ||
        '                "value": "\" REMOVE_DUPLICATES=TRUE\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicates_argumentList": {' ||
        '                "value": "${mergeAndRemoveDuplicates_optionMarkDuplicates}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "LIB_ADD": {' ||
        '                "value": "addToOldLib"' ||
        '            },' ||
        '            "QUAL": {' ||
        '                "value": "phred"' ||
        '            },' ||
        '            "SNP_MINCOVERAGE": {' ||
        '                "value": "16"' ||
        '            },' ||
        '            "SNP_MAXCOVERAGE": {' ||
        '                "value": "300"' ||
        '            },' ||
        '            "CHROM_SIZES_FILE": {' ||
        '                "value": "${chromosomeSizesFile_hg19}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "CLIP_INDEX": {' ||
        '                "value": "${DIR_EXECUTION}/analysisTools/qcPipelineTools/trimmomatic/adapters/TruSeq3-PE.fa",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "ADAPTOR_TRIMMING_OPTIONS_0": {' ||
        '                "value": "\"PE -threads 12 -phred33\""' ||
        '            },' ||
        '            "ADAPTOR_TRIMMING_OPTIONS_1": {' ||
        '                "value": "\"ILLUMINACLIP:${CLIP_INDEX}:2:30:10:8:true SLIDINGWINDOW:4:15 MINLEN:36\""' ||
        '            },' ||
        '            "debugOptionsUseUndefinedVariableBreak": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "debugOptionsUseExitOnError": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "useBioBamBamSort": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "disableAutoBAMHeaderAnalysis": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3', 6, 'DEFAULT_VALUES', (SELECT id
                                                                                                                                                  FROM external_workflow_config_fragment
                                                                                                                                                  WHERE name = 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3')) ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3'),
       (SELECT id FROM workflow WHERE name = 'Roddy SNV calling') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'))
          AND workflow_version.workflow_version = '1.2.166-1') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy SNV calling 1.2.166-1, 1.2.166-3'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'))
          AND workflow_version.workflow_version = '1.2.166-3') ON CONFLICT DO NOTHING;
