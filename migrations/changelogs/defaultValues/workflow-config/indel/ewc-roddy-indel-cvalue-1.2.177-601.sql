/*
 * Copyright 2011-2026 The OTP authors
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
 *
 */

    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for Roddy Indel calling 1.2.177-601',
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
'                "value": "100",' ||
'                "type": "string"' ||
'            },' ||
'            "mergeCol": {' ||
'                "value": "Sample"' ||
'            },' ||
'            "markCol": {' ||
'                "value": "Library"' ||
'            },' ||
'            "datasetCol": {' ||
'                "value": "PID"' ||
'            },' ||
'            "readLayoutCol": {' ||
'                "value": "ReadLayout"' ||
'            },' ||
'            "runCol": {' ||
'                "value": "Run"' ||
'            },' ||
'            "mateCol": {' ||
'                "value": "Mate"' ||
'            },' ||
'            "fileCol": {' ||
'                "value": "SequenceFile"' ||
'            },' ||
'            "metadataTableColumnIDs": {' ||
'                "value": "datasetCol,mergeCol,markCol,runCol,mateCol,fileCol,readLayoutCol",' ||
'                "type": "string"' ||
'            },' ||
'            "workflowEnvironmentScript": {' ||
'                "value": "workflowEnvironment_tbiCluster",' ||
'                "type": "string"' ||
'            },' ||
'            "condaEnvironmentName": {' ||
'                "value": "IndelCallingWorkflow",' ||
'                "type": "string"' ||
'            },' ||
'            "INDELFILE_PREFIX": {' ||
'                "value": "indel_",' ||
'                "type": "string"' ||
'            },' ||
'            "PLATYPUS_PARAMS": {' ||
'                "value": "''''",' ||
'                "type": "string"' ||
'            },' ||
'            "INDEL_ANNOTATION_PADDING": {' ||
'                "value": "10",' ||
'                "type": "string"' ||
'            },' ||
'            "INDEL_ANNOTATION_MINOVERLAPFRACTION": {' ||
'                "value": "0.7",' ||
'                "type": "string"' ||
'            },' ||
'            "INDEL_ANNOTATION_MAXBORDERDISTANCESUM": {' ||
'                "value": "20",' ||
'                "type": "string"' ||
'            },' ||
'            "INDEL_ANNOTATION_MAXNROFMATCHES": {' ||
'                "value": "5",' ||
'                "type": "string"' ||
'            },' ||
'            "ADDITIONAL_FILTER_OPTS": {' ||
'                "value": "''''",' ||
'                "type": "string"' ||
'            },' ||
'            "MIN_CONFIDENCE_SCORE": {' ||
'                "value": "8",' ||
'                "type": "string"' ||
'            },' ||
'            "PLOIDY_LEVEL": {' ||
'                "value": 2,' ||
'                "type": "integer"' ||
'            },' ||
'            "INDIVIDUAL_COUNT": {' ||
'                "value": 2,' ||
'                "type": "integer"' ||
'            },' ||
'            "CPU_COUNT": {' ||
'                "value": 10,' ||
'                "type": "integer"' ||
'            },' ||
'            "CALL_SNP": {' ||
'                "value": 1,' ||
'                "type": "integer"' ||
'            },' ||
'            "runIndelAnnotation": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "runIndelDeepAnnotation": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "runIndelVCFFilter": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "runTinda": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "CONFIDENCE_OPTS_INDEL": {' ||
'                "value": "",' ||
'                "type": "string"' ||
'            },' ||
'            "VCF_SCREENSHOTS_PREFIX": {' ||
'                "value": "indel_",' ||
'                "type": "string"' ||
'            },' ||
'            "REPEAT_MASKER": {' ||
'                "value": "${hg19DatabaseUCSCDirectory}/Sept2013/UCSC_27Sept2013_RepeatMasker.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "CHROM_SIZES_FILE": {' ||
'                "value": "${chromosomeSizesFile_hg19}",' ||
'                "type": "path"' ||
'            },' ||
'            "KGENOME": {' ||
'                "value": "${hg19DatabasesDirectory}/1000genomes/ALL.wgs.phase1_integrated_calls.20101123.indels_plain.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "DBSNP_INDEL": {' ||
'                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_147/00-All.INDEL.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "DBSNP_SNV": {' ||
'                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_147/00-All.SNV.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "ExAC": {' ||
'                "value": "${hg19DatabasesDirectory}/ExAC/ExAC_nonTCGA.r0.3.1.sites.vep.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "EVS": {' ||
'                "value": "${hg19DatabasesDirectory}/EVS/ESP6500SI-V2-SSA137.updatedProteinHgvs.ALL.snps_indels.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "LOCALCONTROL": {' ||
'                "value": "${hg19DatabasesDirectory}/LocalControls/LocalControlConsolidated_Indel.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "GNOMAD_WGS_COMMON_SNV": {' ||
'                "value": "${hg19DatabasesDirectory}/gnomAD/gnomad.genomes.r2.0.1.sites.chr1-22-X.SNVs-INDELs.Common.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "LOCAL_CONTROL_COMMON_SNV": {' ||
'                "value": "${hg19DatabasesDirectory}/LocalControls/pcawg_AF.All.CommonArtifacts.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "LOCAL_CONTROL_COMMON_SNV_2": {' ||
'                "value": "${hg19DatabasesDirectory}/LocalControls/LocalControlConsolidated_AF.INFO.ACgt10.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "EXOME_CAPTURE_KIT_BEDFILE": {' ||
'                "value": "${hg19DatabasesDirectory}/../targetRegions/Agilent5withUTRs_plain.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "VCF_NORMAL_HEADER_COL": {' ||
'                "value": "\"blood|normal|control|buffy_coat|germline\"",' ||
'                "type": "string"' ||
'            },' ||
'            "VCF_TUMOR_HEADER_COL": {' ||
'                "value": "\"tumor|metastasis|disease|relapse|autopsy\"",' ||
'                "type": "string"' ||
'            },' ||
'            "DBSNP_COL": {' ||
'                "value": "DBSNP",' ||
'                "type": "string"' ||
'            },' ||
'            "KGENOMES_COL": {' ||
'                "value": "1K_GENOMES",' ||
'                "type": "string"' ||
'            },' ||
'            "ExAC_COL": {' ||
'                "value": "ExAC",' ||
'                "type": "string"' ||
'            },' ||
'            "EVS_COL": {' ||
'                "value": "EVS",' ||
'                "type": "string"' ||
'            },' ||
'            "LOCALCONTROL_COL": {' ||
'                "value": "LocalControlAF",' ||
'                "type": "string"' ||
'            },' ||
'            "TINDA_RIGHT_BORDER": {' ||
'                "value": "0.45",' ||
'                "type": "string"' ||
'            },' ||
'            "TINDA_BOTTOM_BORDER": {' ||
'                "value": "0.01",' ||
'                "type": "string"' ||
'            },' ||
'            "TINDA_RUN_RSCRIPT": {' ||
'                "value": "0",' ||
'                "type": "string"' ||
'            },' ||
'            "SEQUENCE_TYPE": {' ||
'                "value": "WGS",' ||
'                "type": "string"' ||
'            },' ||
'            "ANNOVAR_BINARY": {' ||
'                "value": "${sharedFilesBaseDirectory}/annovar/annovar_Feb2016/annotate_variation.pl",' ||
'                "type": "path"' ||
'            },' ||
'            "ANNOVAR_BUILDVER": {' ||
'                "value": "hg19",' ||
'                "type": "string"' ||
'            },' ||
'            "ANNOVAR_DBPATH": {' ||
'                "value": "${sharedFilesBaseDirectory}/annovar/annovar_Feb2016/humandb/",' ||
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
'            "CONFIDENCE_OPTS": {' ||
'                "value": "\"-t 500 -c 0 -p 0\"",' ||
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
'            "GERMLINE_AVAILABLE": {' ||
'                "value": "1",' ||
'                "type": "string"' ||
'            },' ||
'            "FILTER_ExAC": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "FILTER_EVS": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "FILTER_1KGENOMES": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "FILTER_RECURRENCE": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "FILTER_LOCALCONTROL": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "FILTER_NON_CLINIC": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "CRIT_ExAC_maxMAF": {' ||
'                "value": "0.01",' ||
'                "type": "float"' ||
'            },' ||
'            "CRIT_EVS_maxMAF": {' ||
'                "value": "1.0",' ||
'                "type": "float"' ||
'            },' ||
'            "CRIT_1KGENOMES_maxMAF": {' ||
'                "value": "0.01",' ||
'                "type": "float"' ||
'            },' ||
'            "CRIT_RECURRENCE": {' ||
'                "value": 7,' ||
'                "type": "integer"' ||
'            },' ||
'            "CRIT_LOCALCONTROL_maxMAF": {' ||
'                "value": "0.01",' ||
'                "type": "float"' ||
'            },' ||
'            "MAPABILITY": {' ||
'                "value": "\"${hg19DatabaseUCSCDirectory}/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz:::--breakPointMode --aEndOffset=1\"",' ||
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
'                "value": "${hg19DatabasesDirectory}/COSMIC/Cosmic_v77_hg19_coding_SNVs.bed.gz:7,8,9:1",' ||
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
'                "value": "${inputBaseDirectory}/${p'||'id}/${sample}/${SEQUENCER_PROTOCOL}",' ||
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
'            "mpileupOutputDirectory": {' ||
'                "value": "mpileup"' ||
'            },' ||
'            "mpileupPlatypusOutputDirectory": {' ||
'                "value": "./"' ||
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
'            "outputAllowAccessRightsModification": {' ||
'                "value": "false"' ||
'            },' ||
'            "useBioBamBamSort": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "disableAutoBAMHeaderAnalysis": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "extractSamplesFromOutputFiles": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for Roddy Indel calling 1.2.177-601', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for Roddy Indel calling 1.2.177-601'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy Indel calling 1.2.177-601'), (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy Indel calling 1.2.177-601'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '1.2.177-601')
    ON CONFLICT DO NOTHING;
