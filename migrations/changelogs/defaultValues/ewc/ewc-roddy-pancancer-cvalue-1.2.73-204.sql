
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for PanCancer alignment 1.2.73-204',
'{' ||
'    "RODDY": {' ||
'        "cvalues": {' ||
'            "workflowEnvironmentScript": {' ||
'                "value": "workflowEnvironment_tbiLsf",' ||
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
'                "value": 500000000,' ||
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
'            "outputAllowAccessRightsModification": {' ||
'                "value": "false"' ||
'            },' ||
'            "useBioBamBamSort": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "runFastQC": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "useCombinedAlignAndSampe": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "runSlimWorkflow": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "useAcceleratedHardware": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "BWA_VERSION": {' ||
'                "value": "0.7.15",' ||
'                "type": "string"' ||
'            },' ||
'            "markDuplicatesVariant": {' ||
'                "value": "sambamba",' ||
'                "type": "string"' ||
'            },' ||
'            "SAMBAMBA_MARKDUP_VERSION": {' ||
'                "value": "0.6.5",' ||
'                "type": "string"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for PanCancer alignment 1.2.73-204', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-204'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-204'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-204'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-204')
    ON CONFLICT DO NOTHING;
