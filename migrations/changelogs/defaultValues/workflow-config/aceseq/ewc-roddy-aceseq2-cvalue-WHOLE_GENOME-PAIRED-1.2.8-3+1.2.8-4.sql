/*
 * Copyright 2011-2025 The OTP authors
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
        VALUES(NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED',
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
'            "HTSLIB_VERSION": {' ||
'                "value": "0.2.5",' ||
'                "type": "string"' ||
'            },' ||
'            "PERL_VERSION": {' ||
'                "value": "5.20.2",' ||
'                "type": "string"' ||
'            },' ||
'            "PYTHON_VERSION": {' ||
'                "value": "2.7.9",' ||
'                "type": "string"' ||
'            },' ||
'            "RSCRIPT_VERSION": {' ||
'                "value": "3.3.1",' ||
'                "type": "string"' ||
'            },' ||
'            "BEDTOOLS_VERSION": {' ||
'                "value": "2.16.2",' ||
'                "type": "string"' ||
'            },' ||
'            "VCFTOOLS_VERSION": {' ||
'                "value": "0.1.10",' ||
'                "type": "string"' ||
'            },' ||
'            "SAMTOOLS_VERSION": {' ||
'                "value": "0.1.19",' ||
'                "type": "string"' ||
'            },' ||
'            "workflowEnvironmentScript": {' ||
'                "value": "workflowEnvironment_tbiLsf",' ||
'                "type": "string"' ||
'            },' ||
'            "condaEnvironmentName": {' ||
'                "value": "ACEseqWorkflow",' ||
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
'            "baseDirectoryReference": {' ||
'                "value": "/icgc/ngs_share/assemblies/hg19_GRCh37_1000genomes",' ||
'                "type": "path"' ||
'            },' ||
'            "referenceGenome_1KGRef": {' ||
'                "value": "${referenceGenomeBaseDirectory_human}/1KGRef/hs37d5.fa",' ||
'                "type": "path"' ||
'            },' ||
'            "REFERENCE_GENOME": {' ||
'                "value": "${referenceGenome_1KGRef}",' ||
'                "type": "string"' ||
'            },' ||
'            "dbSNP_FILE": {' ||
'                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_135/00-All.SNV.vcf.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "MAPPABILITY_FILE": {' ||
'                "value": "${baseDirectoryReference}/databases/UCSC/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "CHROMOSOME_LENGTH_FILE": {' ||
'                "value": "${baseDirectoryReference}/stats/chrlengths.txt",' ||
'                "type": "path"' ||
'            },' ||
'            "REPLICATION_TIME_FILE": {' ||
'                "value": "${baseDirectoryReference}/databases/ENCODE/ReplicationTime_10cellines_mean_10KB.Rda",' ||
'                "type": "path"' ||
'            },' ||
'            "GC_CONTENT_FILE": {' ||
'                "value": "${baseDirectoryReference}/stats/hg19_GRch37_100genomes_gc_content_10kb.txt",' ||
'                "type": "path"' ||
'            },' ||
'            "imputeBaseDirectory": {' ||
'                "value": "${baseDirectoryReference}/databases/1000genomes/IMPUTE/",' ||
'                "type": "path"' ||
'            },' ||
'            "GENETIC_MAP_FILE": {' ||
'                "value": "${imputeBaseDirectory}/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/genetic_map_chr${CHR_NAME}_combined_b37.txt",' ||
'                "type": "path"' ||
'            },' ||
'            "KNOWN_HAPLOTYPES_FILE": {' ||
'                "value": "${imputeBaseDirectory}/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.haplotypes.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "KNOWN_HAPLOTYPES_LEGEND_FILE": {' ||
'                "value": "${imputeBaseDirectory}/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.legend.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "GENETIC_MAP_FILE_X": {' ||
'                "value": "${imputeBaseDirectory}/ALL_1000G_phase1integrated_v3_impute/genetic_map_chrX_nonPAR_combined_b37.txt",' ||
'                "type": "path"' ||
'            },' ||
'            "KNOWN_HAPLOTYPES_FILE_X": {' ||
'                "value": "${imputeBaseDirectory}/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.hap.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "KNOWN_HAPLOTYPES_LEGEND_FILE_X": {' ||
'                "value": "${imputeBaseDirectory}/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.legend.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "outputExecutionDirectory": {' ||
'                "value": "${outputAnalysisBaseDirectory}/exec_${executionTimeString}"' ||
'            },' ||
'            "mergedBamSuffix": {' ||
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
'            "aceseqOutputDirectory": {' ||
'                "value": "${outputAnalysisBaseDirectory}/ACEseq_${tumorSample}",' ||
'                "type": "path"' ||
'            },' ||
'            "svOutputDirectory": {' ||
'                "value": "${outputAnalysisBaseDirectory}/SOPHIA_${tumorSample}_${controlSample}",' ||
'                "type": "path"' ||
'            },' ||
'            "crestOutputDirectory": {' ||
'                "value": "${outputAnalysisBaseDirectory}/crest",' ||
'                "type": "path"' ||
'            },' ||
'            "cnvSnpOutputDirectory": {' ||
'                "value": "${aceseqOutputDirectory}/cnv_snp",' ||
'                "type": "path"' ||
'            },' ||
'            "imputeOutputDirectory": {' ||
'                "value": "${aceseqOutputDirectory}/phasing",' ||
'                "type": "path"' ||
'            },' ||
'            "plotOutputDirectory": {' ||
'                "value": "${aceseqOutputDirectory}/plots",' ||
'                "type": "path"' ||
'            },' ||
'            "test": {' ||
'                "value": "",' ||
'                "type": "string"' ||
'            },' ||
'            "runWithoutControl": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "CHR_NR": {' ||
'                "value": "${CHR_PREFIX}${PARM_CHR_INDEX}${CHR_SUFFIX}",' ||
'                "type": "string"' ||
'            },' ||
'            "CHR_NAME": {' ||
'                "value": "${PARM_CHR_INDEX}",' ||
'                "type": "string"' ||
'            },' ||
'            "CHROMOSOME_INDICES_OPTIMIZED_CNV_SNV_MPILEUP": {' ||
'                "value": "( 2 1 3 5 4 6 8 12 7 11 10 9 X 16 13 15 14 18 17 19 20 22 21 Y )",' ||
'                "type": "bashArray"' ||
'            },' ||
'            "AUTOSOME_INDICES": {' ||
'                "value": "( {1..22} )",' ||
'                "type": "bashArray"' ||
'            },' ||
'            "CREST": {' ||
'                "value": "yes",' ||
'                "type": "string"' ||
'            },' ||
'            "runOnPancan": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "libloc_PSCBS": {' ||
'                "value": "",' ||
'                "type": "string"' ||
'            },' ||
'            "libloc_flexclust": {' ||
'                "value": "",' ||
'                "type": "string"' ||
'            },' ||
'            "mpileup_qual": {' ||
'                "value": 0,' ||
'                "type": "integer"' ||
'            },' ||
'            "CNV_MPILEUP_OPTS": {' ||
'                "value": "\"-A -R -B -Q ${mpileup_qual} -q 1 -I \"",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_VCF_SUF": {' ||
'                "value": "vcf",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_TXT_SUF": {' ||
'                "value": "txt",' ||
'                "type": "string"' ||
'            },' ||
'            "phasedGenotypesFilePrefix": {' ||
'                "value": "phased_chr",' ||
'                "type": "string"' ||
'            },' ||
'            "unphasedGenotypesFilePrefix": {' ||
'                "value": "unphased_chr",' ||
'                "type": "string"' ||
'            },' ||
'            "phasedGenotypesFileSuffix": {' ||
'                "value": "${FILE_VCF_SUF}",' ||
'                "type": "string"' ||
'            },' ||
'            "unphasedGenotypesFileSuffix": {' ||
'                "value": "${FILE_VCF_SUF}",' ||
'                "type": "string"' ||
'            },' ||
'            "BCFTOOLS_OPTS": {' ||
'                "value": "\"-vgN \"",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_UNPHASED_PRE": {' ||
'                "value": "${imputeOutputDirectory}/${unphasedGenotypesFilePrefix}",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_UNPHASED_GENOTYPE": {' ||
'                "value": "${imputeOutputDirectory}/unphased_genotype_chr",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_PHASED_PRE": {' ||
'                "value": "${imputeOutputDirectory}/${phasedGenotypesFilePrefix}",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_PHASED_GENOTYPE": {' ||
'                "value": "${imputeOutputDirectory}/phased_genotype_chr",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_INFO": {' ||
'                "value": "info",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_INFO_SAMPLE": {' ||
'                "value": "info_by_sample",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_HAPS": {' ||
'                "value": "haps",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_HAPS_CONF": {' ||
'                "value": "haps_confidence",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_SUMMARY": {' ||
'                "value": "summary",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_WARNINGS": {' ||
'                "value": "warnings",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_PART": {' ||
'                "value": "part",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_SAMPLE_G": {' ||
'                "value": "${imputeOutputDirectory}/sample_g.txt",' ||
'                "type": "path"' ||
'            },' ||
'            "minHT": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "SNP_VCF_CNV_PATH": {' ||
'                "value": "${cnvSnpOutputDirectory}/${p'||'id}.chr",' ||
'                "type": "path"' ||
'            },' ||
'            "SNP_VCF_CNV_PATH_STR": {' ||
'                "value": "${SNP_VCF_CNV_PATH}",' ||
'                "type": "string"' ||
'            },' ||
'            "SNP_SUFFIX": {' ||
'                "value": "snp.tab.gz",' ||
'                "type": "string"' ||
'            },' ||
'            "snp_min_coverage": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "MALE_FAKE_CONTROL_PRE": {' ||
'                "value": "/icgc/dkfzlsdf/analysis/mmml/whole_genome_pcawg/results_per_pid/4128477/ACEseq/cnv_snp/4128477.chr",' ||
'                "type": "path"' ||
'            },' ||
'            "FEMALE_FAKE_CONTROL_PRE": {' ||
'                "value": "/icgc/dkfzlsdf/analysis/mmml/whole_genome_pcawg/results_per_pid/4109142/ACEseq/cnv_snp/4109142.chr",' ||
'                "type": "path"' ||
'            },' ||
'            "FAKE_CONTROL_POST": {' ||
'                "value": ".cnv.anno.tab.gz",' ||
'                "type": "string"' ||
'            },' ||
'            "PATIENTSEX": {' ||
'                "value": "male",' ||
'                "type": "string"' ||
'            },' ||
'            "CNV_ANNO_SUFFIX": {' ||
'                "value": "cnv.anno.tab.gz",' ||
'                "type": "string"' ||
'            },' ||
'            "CNV_SUFFIX": {' ||
'                "value": "cnv.tab.gz",' ||
'                "type": "string"' ||
'            },' ||
'            "min_X_ratio": {' ||
'                "value": "0.8",' ||
'                "type": "float"' ||
'            },' ||
'            "min_Y_ratio": {' ||
'                "value": "0.12",' ||
'                "type": "float"' ||
'            },' ||
'            "cnv_min_coverage": {' ||
'                "value": 5000,' ||
'                "type": "integer"' ||
'            },' ||
'            "mapping_quality": {' ||
'                "value": 1000,' ||
'                "type": "integer"' ||
'            },' ||
'            "min_windows": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "LOWESS_F": {' ||
'                "value": "0.1",' ||
'                "type": "float"' ||
'            },' ||
'            "SCALE_FACTOR": {' ||
'                "value": "0.9",' ||
'                "type": "float"' ||
'            },' ||
'            "COVERAGEPLOT_YLIMS": {' ||
'                "value": "4",' ||
'                "type": "float"' ||
'            },' ||
'            "FILENAME_GC_CORRECT_PLOT": {' ||
'                "value": "${plotOutputDirectory}/${p'||'id}_gc_corrected.png",' ||
'                "type": "path"' ||
'            },' ||
'            "GC_bias_json_key": {' ||
'                "value": "gc-bias",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_DENSITYBETA": {' ||
'                "value": "${aceseqOutputDirectory}/densityBeta.pdf",' ||
'                "type": "path"' ||
'            },' ||
'            "min_DDI_length": {' ||
'                "value": 1000,' ||
'                "type": "integer"' ||
'            },' ||
'            "selSVColumn": {' ||
'                "value": "eventScore",' ||
'                "type": "string"' ||
'            },' ||
'            "min_seg_width": {' ||
'                "value": 2000,' ||
'                "type": "integer"' ||
'            },' ||
'            "undo_SD": {' ||
'                "value": 24,' ||
'                "type": "integer"' ||
'            },' ||
'            "pscbs_prune_height": {' ||
'                "value": 0,' ||
'                "type": "integer"' ||
'            },' ||
'            "min_segment_map": {' ||
'                "value": "0.6",' ||
'                "type": "float"' ||
'            },' ||
'            "min_seg_length_prune": {' ||
'                "value": 9000,' ||
'                "type": "integer"' ||
'            },' ||
'            "min_num_SNPs": {' ||
'                "value": 15,' ||
'                "type": "integer"' ||
'            },' ||
'            "clustering": {' ||
'                "value": "yes",' ||
'                "type": "string"' ||
'            },' ||
'            "min_cluster_number": {' ||
'                "value": 1,' ||
'                "type": "integer"' ||
'            },' ||
'            "min_membership": {' ||
'                "value": "0.8",' ||
'                "type": "float"' ||
'            },' ||
'            "min_distance": {' ||
'                "value": "0.05",' ||
'                "type": "float"' ||
'            },' ||
'            "haplogroupFilePrefix": {' ||
'                "value": "haploblocks_chr",' ||
'                "type": "string"' ||
'            },' ||
'            "haplogroupFileSuffix": {' ||
'                "value": "txt",' ||
'                "type": "string"' ||
'            },' ||
'            "haplogroupFilePath": {' ||
'                "value": "${imputeOutputDirectory}/${haplogroupFilePrefix}",' ||
'                "type": "path"' ||
'            },' ||
'            "minLim": {' ||
'                "value": "0.47",' ||
'                "type": "float"' ||
'            },' ||
'            "maxLim": {' ||
'                "value": "0.53",' ||
'                "type": "float"' ||
'            },' ||
'            "min_length_purity": {' ||
'                "value": 1000000,' ||
'                "type": "integer"' ||
'            },' ||
'            "min_hetSNPs_purity": {' ||
'                "value": 500,' ||
'                "type": "integer"' ||
'            },' ||
'            "dh_stop": {' ||
'                "value": "max",' ||
'                "type": "string"' ||
'            },' ||
'            "min_length_dh_stop": {' ||
'                "value": 1000000,' ||
'                "type": "integer"' ||
'            },' ||
'            "dh_zero": {' ||
'                "value": "no",' ||
'                "type": "string"' ||
'            },' ||
'            "purity_min": {' ||
'                "value": "0.15",' ||
'                "type": "float"' ||
'            },' ||
'            "purity_max": {' ||
'                "value": "1.0",' ||
'                "type": "float"' ||
'            },' ||
'            "ploidy_min": {' ||
'                "value": "1.0",' ||
'                "type": "float"' ||
'            },' ||
'            "ploidy_max": {' ||
'                "value": "6.5",' ||
'                "type": "float"' ||
'            },' ||
'            "PLOT_PRE": {' ||
'                "value": "${aceseqOutputDirectory}/${p'||'id}_plot",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_MOST_IMPORTANT_INFO_SEG_PRE": {' ||
'                "value": "${p'||'id}_most_important_info",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_MOST_IMPORTANT_INFO_SEG_POST": {' ||
'                "value": ".txt",' ||
'                "type": "string"' ||
'            },' ||
'            "FILE_SEGMENT_VCF_PRE": {' ||
'                "value": "${aceseqOutputDirectory}/${p'||'id}",' ||
'                "type": "path"' ||
'            },' ||
'            "FILE_SEGMENT_VCF_POST": {' ||
'                "value": ".cnv.vcf",' ||
'                "type": "string"' ||
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
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED', 22, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'), (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')) AND workflow_version.workflow_version = '1.2.8-3')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')) AND workflow_version.workflow_version = '1.2.8-4')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = FALSE AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
