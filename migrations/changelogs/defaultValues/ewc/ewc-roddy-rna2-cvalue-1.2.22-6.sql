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
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for RNA alignment 1.2.22-6',
        '{' ||
        '    "RODDY": {' ||
        '        "cvalues": {' ||
        '            "sampleDirectory": {' ||
        '                "value": "${inputBaseDirectory}/${p' || 'id}/${sample}/${SEQUENCER_PROTOCOL}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "sequenceDirectory": {' ||
        '                "value": "${sampleDirectory}/${run}/sequence",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "mergedBamSuffix_markDuplicatesShort": {' ||
        '                "value": "merged.mdup.bam",' ||
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
        '            "QUAL": {' ||
        '                "value": "phred"' ||
        '            },' ||
        '            "SEQUENCER_PROTOCOL": {' ||
        '                "value": "paired",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "CORES": {' ||
        '                "value": "8",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "FEATURE_COUNT_CORES": {' ||
        '                "value": "8"' ||
        '            },' ||
        '            "TEST_RUN": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "DO_FIRST": {' ||
        '                "value": "echo \"Do something first\"",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "alignmentOutputDirectory": {' ||
        '                "value": ".",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "qcOutputDirectory": {' ||
        '                "value": "qualitycontrol",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ALIGNMENT_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "SCRATCH": {' ||
        '                "value": "${ALIGNMENT_DIR}/roddy-scratch",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "COUNT_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/featureCounts",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "COUNT_DIR_EXON": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/featureCounts_dexseq",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "KALLISTO_UN_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/kallisto_un",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "KALLISTO_FR_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/kallisto_fr",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "KALLISTO_RF_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/kallisto_rf",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "QC_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/${qcOutputDirectory}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "RNASEQC_DIR": {' ||
        '                "value": "${QC_DIR}/RNAseQC",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "QUALIMAP_DIR": {' ||
        '                "value": "${QC_DIR}/QualiMap2",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ARRIBA_DIR": {' ||
        '                "value": "${outputAnalysisBaseDirectory}/fusions_arriba",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PREFIX": {' ||
        '                "value": "${SAMPLE}_${p' || 'id}_merged",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_CHIMERA_PREFIX": {' ||
        '                "value": "${SAMPLE}_${p' || 'id}_chimeric_merged",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_NOTSORTED_BAM": {' ||
        '                "value": "${STAR_PREFIX}.Aligned.out.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_SORTED_BAM": {' ||
        '                "value": "${STAR_PREFIX}.Aligned.sortedByCoord.out.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_SORTED_MKDUP_BAM": {' ||
        '                "value": "${STAR_PREFIX}.mdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_SORTED_MKDUP_BAM2": {' ||
        '                "value": "${STAR_PREFIX}.mdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_CHIMERA_SAM": {' ||
        '                "value": "${STAR_PREFIX}.Chimeric.out.sam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_CHIMERA_BAM_PREF": {' ||
        '                "value": "${STAR_PREFIX}.Chimeric.out",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_CHIMERA_MKDUP_BAM": {' ||
        '                "value": "${STAR_CHIMERA_PREFIX}.mdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "PYTHON_VERSION": {' ||
        '                "value": "2.7.9",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_VERSION": {' ||
        '                "value": "2.5.2b",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "SUBREAD_VERSION": {' ||
        '                "value": "1.5.1",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "SAMBAMBA_VERSION": {' ||
        '                "value": "0.6.5",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "SAMTOOLS_VERSION": {' ||
        '                "value": "1.6",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "RNASEQC_VERSION": {' ||
        '                "value": "1.1.8",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "KALLISTO_VERSION": {' ||
        '                "value": "0.43.0",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "QUALIMAP_VERSION": {' ||
        '                "value": "2.2.1",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ARRIBA_VERSION": {' ||
        '                "value": "0.8",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "R_VERSION": {' ||
        '                "value": "3.0.0",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "workflowEnvironmentScript": {' ||
        '                "value": "workflowEnvironment_tbiCluster",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "RUN_STAR": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_FEATURE_COUNTS": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_FEATURE_COUNTS_DEXSEQ": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_RNASEQC": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_QUALIMAP": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_KALLISTO": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_KALLISTO_FR": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_KALLISTO_RF": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_ARRIBA": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_QCJSON": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "RUN_CLEANUP": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "hg19BaseDirectory": {' ||
        '                "value": "/icgc/ngs_share/assemblies/hg19_GRCh37_1000genomes/",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "hg19DatabaseDirectory": {' ||
        '                "value": "${hg19BaseDirectory}/databases",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "hg19IndexDirectory": {' ||
        '                "value": "${hg19BaseDirectory}/indexes/",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mm10BaseDirectory": {' ||
        '                "value": "/icgc/ngs_share/assemblies/mm10/",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mm10DatabaseDirectory": {' ||
        '                "value": "${mm10BaseDirectory}/databases",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "mm10IndexDirectory": {' ||
        '                "value": "${mm10BaseDirectory}/indexes/",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "databaseDirectory": {' ||
        '                "value": "${hg19DatabaseDirectory}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "indexDirectory": {' ||
        '                "value": "${hg19IndexDirectory}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_FA": {' ||
        '                "value": "${indexDirectory}/bwa/bwa06_1KGRef_Phix/hs37d5_PhiX.fa",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_GATK_INDEX": {' ||
        '                "value": "${indexDirectory}/bwa/bwa06_1KGRef_Phix/hs37d5_PhiX.fa",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENE_MODELS": {' ||
        '                "value": "${databaseDirectory}/gencode/gencode19/gencode.v19.annotation_plain.gtf",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENE_MODELS_EXCLUDE": {' ||
        '                "value": "${databaseDirectory}/gencode/gencode19/gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENE_MODELS_DEXSEQ": {' ||
        '                "value": "${databaseDirectory}/gencode/gencode19/gencode.v19.annotation_plain.dexseq.gff",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENE_MODELS_GC": {' ||
        '                "value": "${databaseDirectory}/gencode/gencode19/gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_STAR_INDEX_50": {' ||
        '                "value": "${indexDirectory}/STAR/STAR_2.5.2b_1KGRef_PhiX_Gencode19_50bp",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_STAR_INDEX_100": {' ||
        '                "value": "${indexDirectory}/STAR/STAR_2.5.2b_1KGRef_PhiX_Gencode19_100bp",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_STAR_INDEX_200": {' ||
        '                "value": "${indexDirectory}/STAR/STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_KALLISTO_INDEX": {' ||
        '                "value": "${indexDirectory}/kallisto/kallisto-0.43.0_1KGRef_Gencode19_k31/kallisto-0.43.0_1KGRef_Gencode19_k31.noGenes.index",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ARRIBA_KNOWN_FUSIONS": {' ||
        '                "value": "${hg19BaseDirectory}/tools_data/arriba/known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ARRIBA_BLACKLIST": {' ||
        '                "value": "${hg19BaseDirectory}/tools_data/arriba/blacklist_hs37d5_gencode19_2017-01-09.tsv.gz",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "READ_COMMAND": {' ||
        '                "value": "${UNZIPTOOL} ${UNZIPTOOL_OPTIONS}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_TRUSEQ_LT_HT": {' ||
        '                "value": "AGATCGGAAGAGCACACGTCTGAACTCCAGTCA AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_TRUSEQ_DNAME": {' ||
        '                "value": "AGATCGGAAGAGCACACGTCTGAAC AGATCGGAAGAGCGTCGTGTAGGGA",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_TRUSEQ_SRNA": {' ||
        '                "value": "TGGAATTCTCGGGTGCCAAGG",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_TRUSEQ_RIBO": {' ||
        '                "value": "AGATCGGAAGAGCACACGTCT",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_NEXTERA": {' ||
        '                "value": "CTGTCTCTTATACACATCT",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ_NEXTERA_MP": {' ||
        '                "value": "CTGTCTCTTATACACATCT AGATGTGTATAAGAGACAG",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "ADAPTER_SEQ": {' ||
        '                "value": "${ADAPTER_SEQ_TRUSEQ_LT_HT}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_CLIP": {' ||
        '                "value": "--clip3pAdapterSeq ${ADAPTER_SEQ}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "GENOME_STAR_INDEX": {' ||
        '                "value": "$GENOME_STAR_INDEX_200",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_BASIC": {' ||
        '                "value": "--sjdbOverhang 200 --runThreadN ${CORES} --outFileNamePrefix ${STAR_PREFIX}. --genomeDir ${GENOME_STAR_INDEX} --runRNGseed 1234 --outTmpDir ${SCRATCH}/${SAMPLE}_${p' ||
        'id}_STAR",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_OUT": {' ||
        '                "value": "--outSAMtype BAM Unsorted SortedByCoordinate --limitBAMsortRAM 100000000000 --outBAMsortingThreadN=1 --outSAMstrandField intronMotif --outSAMunmapped Within KeepPairs --outFilterMultimapNmax 1 --outFilterMismatchNmax 5 --outFilterMismatchNoverLmax 0.3",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_2PASS": {' ||
        '                "value": "--twopassMode Basic --twopass1readsN -1 --genomeLoad NoSharedMemory",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_CHIMERIC": {' ||
        '                "value": "--chimSegmentMin 15 --chimScoreMin 1 --chimScoreJunctionNonGTAG 0 --chimJunctionOverhangMin 15  --chimSegmentReadGapMax 3 --alignSJstitchMismatchNmax 5 -1 5 5",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_INTRONS": {' ||
        '                "value": "--alignIntronMax 1100000 --alignMatesGapMax 1100000 --alignSJDBoverhangMin 3 --alignIntronMin 20",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS_ADDITIONAL": {' ||
        '                "value": " ",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "STAR_PARAMS": {' ||
        '                "value": "${STAR_PARAMS_BASIC} ${STAR_PARAMS_OUT} ${STAR_PARAMS_2PASS} ${STAR_PARAMS_CHIMERIC} ${STAR_PARAMS_INTRONS} ${STAR_PARAMS_CLIP} ${STAR_PARAMS_ADDITIONAL}",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "disableDoC_GATK": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "JSON_PREFIX": {' ||
        '                "value": "\"\"",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "runFingerprinting": {' ||
        '                "value": "true",' ||
        '                "type": "boolean"' ||
        '            },' ||
        '            "fingerprintingSitesFile_hs37": {' ||
        '                "value": "${databaseDirectory}/fingerprinting/hovestadt_v1.1/snp138Common.n1000.vh20140318.bed",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "fingerprintingSitesFile": {' ||
        '                "value": "${fingerprintingSitesFile_hs37}",' ||
        '                "type": "path"' ||
        '            },' ||
        '            "outputFileGroup": {' ||
        '                "value": "false"' ||
        '            },' ||
        '            "outputUMask": {' ||
        '                "value": "007",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "outputAccessRights": {' ||
        '                "value": "u+rw,g+rw,o-rwx"' ||
        '            },' ||
        '            "outputAccessRightsForDirectories": {' ||
        '                "value": "u+rwx,g+rwx,o-rwx"' ||
        '            },' ||
        '            "outputAllowAccessRightsModification": {' ||
        '                "value": "false"' ||
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
        '            "mergedBamSuffix_removeDuplicates": {' ||
        '                "value": "merged.bam.rmdup.bam",' ||
        '                "type": "string"' ||
        '            },' ||
        '            "pairedBamSuffix": {' ||
        '                "value": "paired.bam.sorted.bam",' ||
        '                "type": "string"' ||
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
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for RNA alignment 1.2.22-6', 6, 'DEFAULT_VALUES', (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6')) ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6'),
       (SELECT id FROM workflow WHERE name = 'RNA alignment') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6'),
       (SELECT id FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment'))
          AND workflow_version.workflow_version = '1.2.22-6') ON CONFLICT DO NOTHING;
