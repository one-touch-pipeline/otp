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
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for Roddy Sophia (structural variation calling) 2.2.0',
'{' ||
'    "RODDY": {' ||
'        "cvalues": {' ||
'            "tumorMedianIsize": {' ||
'                "value": ""' ||
'            },' ||
'            "controlMedianIsize": {' ||
'                "value": ""' ||
'            },' ||
'            "tumorStdIsizePercentage": {' ||
'                "value": ""' ||
'            },' ||
'            "controlStdIsizePercentage": {' ||
'                "value": ""' ||
'            },' ||
'            "tumorProperPairPercentage": {' ||
'                "value": ""' ||
'            },' ||
'            "controlProperPairPercentage": {' ||
'                "value": ""' ||
'            },' ||
'            "tumorDefaultReadLength": {' ||
'                "value": 151,' ||
'                "type": "integer"' ||
'            },' ||
'            "controlDefaultReadLength": {' ||
'                "value": 151,' ||
'                "type": "integer"' ||
'            },' ||
'            "sampleDirectory": {' ||
'                "value": "${inputBaseDirectory}/${p'||'id}/${sample}/${SEQUENCER_PROTOCOL}",' ||
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
'            "extractSamplesFromOutputFiles": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            },' ||
'            "possibleControlSampleNamePrefixes": {' ||
'                "value": "( blood BLOOD normal control CONTROL buffy_coat GERMLINE )",' ||
'                "type": "bashArray"' ||
'            },' ||
'            "possibleTumorSampleNamePrefixes": {' ||
'                "value": "( tumor TUMOR metastasis xenograft disease DISEASE relapse RELAPSE autopsy AUTOPSY metastasis METASTASIS )",' ||
'                "type": "bashArray"' ||
'            },' ||
'            "possibleSingleCellSampleNamePrefixes": {' ||
'                "value": "( SingleCell single_cell )",' ||
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
'            "alignmentOutputDirectory": {' ||
'                "value": "alignment"' ||
'            },' ||
'            "insertSizesOutputDirectory": {' ||
'                "value": "insertsize_distribution"' ||
'            },' ||
'            "qcOutputDirectory": {' ||
'                "value": "qualitycontrol",' ||
'                "type": "string"' ||
'            },' ||
'            "sophiaOutputDirectory": {' ||
'                "value": ".",' ||
'                "type": "path"' ||
'            },' ||
'            "workflowEnvironmentScript": {' ||
'                "value": "workflowEnvironment_tbiLsf",' ||
'                "type": "string"' ||
'            },' ||
'            "LOAD_MODULE": {' ||
'                "value": "true",' ||
'                "type": "boolean"' ||
'            },' ||
'            "MODULE_ENV": {' ||
'                "value": "HIPO2_sv/v1",' ||
'                "type": "string"' ||
'            },' ||
'            "SAMTOOLS_VERSION": {' ||
'                "value": "1.6",' ||
'                "type": "string"' ||
'            },' ||
'            "SOPHIA_VERSION": {' ||
'                "value": "35",' ||
'                "type": "string"' ||
'            },' ||
'            "BEDTOOLS_VERSION": {' ||
'                "value": "2.24.0",' ||
'                "type": "string"' ||
'            },' ||
'            "PYTHON_VERSION": {' ||
'                "value": "3.6.1",' ||
'                "type": "string"' ||
'            },' ||
'            "R_VERSION": {' ||
'                "value": "3.4.2",' ||
'                "type": "string"' ||
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
'            "STANDARDHEADER": {' ||
'                "value": "\"#chrom1\\tstart1\\tend1\\tchrom2\\tstart2\\tend2\\tsomaticity1\\tsomaticity2\\tsvtype\\teventScore\\teventSize\\teventInversion\\tevidence1\\tclonalityRatio1\\tevidence2\\tclonalityRatio2\\tsource1\\tsource2\\toverhang1\\toverhang2\\tgene1\\tcancerGene1\\tnearestCodingGeneUpstream1\\tnearestCodingGeneUpstreamDistance1\\tnearestCancerGeneUpstream1\\tnearestCancerGeneUpstreamDistance1\\tnearestCodingGeneDownstream1\\tnearestCodingGeneDownstreamDistance1\\tnearestCancerGeneDownstream1\\tnearestCancerGeneDownstreamDistance1\\tgene2\\tcancerGene2\\tnearestCodingGeneUpstream2\\tnearestCodingGeneUpstreamDistance2\\tnearestCancerGeneUpstream2\\tnearestCancerGeneUpstreamDistance2\\tnearestCodingGeneDownstream2\\tnearestCodingGeneDownstreamDistance2\\tnearestCancerGeneDownstream2\\tnearestCancerGeneDownstreamDistance2\\tdbSUPERenhancer1\\tdbSUPERenhancer2\\trescuedEnhancerHitCandidate\\tTADindices\\taffectedGenesTADestimate\\taffectedCancerGenesTADestimate\\tchrom1PreDecoyRemap\\tstart1PreDecoyRemap\\tend1PreDecoyRemap\\tchrom2PreDecoyRemap\\tstart2PreDecoyRemap\\tend2PreDecoyRemap\""' ||
'            },' ||
'            "REFERENCES_BASE": {' ||
'                "value": "${assembliesHG191000GenomesDirectory}/tools_data/sophia/35/",' ||
'                "type": "path"' ||
'            },' ||
'            "decoyRangeRefBed": {' ||
'                "value": "${REFERENCES_BASE}/decoyRangeRefNew.bed",' ||
'                "type": "path"' ||
'            },' ||
'            "consensusTadReferenceBed": {' ||
'                "value": "${REFERENCES_BASE}/consensusTADs_3_new_allChromosomes_cytobands_gencodeV27genes.names.bed",' ||
'                "type": "path"' ||
'            },' ||
'            "geneRefBed": {' ||
'                "value": "${REFERENCES_BASE}/gencode.v27.basic.Genes.names.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "geneRefBedCancer": {' ||
'                "value": "${REFERENCES_BASE}/gencode.v27.basic.Genes.cancer.names.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "spliceJunctionsRefBed": {' ||
'                "value": "${REFERENCES_BASE}/mergedIntrons.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "exonsOnlyPaddedRefBed": {' ||
'                "value": "${REFERENCES_BASE}/gencode.v27.basic.ExonsPad3.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "intronExonRefBed": {' ||
'                "value": "${REFERENCES_BASE}/gencode.v27.basic.IntronsExons.names.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "intronExonRefBedCancer": {' ||
'                "value": "${REFERENCES_BASE}/gencode.v27.basic.IntronsExons.cancer.names.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "roadmapEnhancerRefBed": {' ||
'                "value": "${REFERENCES_BASE}/ROADMAP_EnhA_EnhBiv_EnhG.min5_Sorted.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "combinedSuperEnhancerRefBed": {' ||
'                "value": "${REFERENCES_BASE}/dbSUPERenhancers_hg19_060417.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "chromSizesRef": {' ||
'                "value": "${REFERENCES_BASE}/hg19_chromSizes_withContigsDecoys.genome",' ||
'                "type": "path"' ||
'            },' ||
'            "pidsInMref": {' ||
'                "value": "3417",' ||
'                "type": "string"' ||
'            },' ||
'            "mRef": {' ||
'                "value": "${REFERENCES_BASE}/mergedMref_strictBreaks_v${SOPHIA_VERSION}_${pidsInMref}_mergedBpCounts_min3.bed.gz",' ||
'                "type": "path"' ||
'            },' ||
'            "mapqThreshold": {' ||
'                "value": 13,' ||
'                "type": "integer"' ||
'            },' ||
'            "clipThreshold": {' ||
'                "value": 10,' ||
'                "type": "integer"' ||
'            },' ||
'            "qualThreshold": {' ||
'                "value": 23,' ||
'                "type": "integer"' ||
'            },' ||
'            "qualThresholdLow": {' ||
'                "value": 12,' ||
'                "type": "integer"' ||
'            },' ||
'            "lowQualOverhangThreshold": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "isizeSigmaThreshold": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "bpSupportThreshold": {' ||
'                "value": 3,' ||
'                "type": "integer"' ||
'            },' ||
'            "smallEventThreshold": {' ||
'                "value": "5000",' ||
'                "type": "string"' ||
'            },' ||
'            "artifactLoFreq": {' ||
'                "value": 33,' ||
'                "type": "integer"' ||
'            },' ||
'            "artifactHiFreq": {' ||
'                "value": 66,' ||
'                "type": "integer"' ||
'            },' ||
'            "clonalityLoFreq": {' ||
'                "value": 5,' ||
'                "type": "integer"' ||
'            },' ||
'            "clonalityStrictLoFreq": {' ||
'                "value": 20,' ||
'                "type": "integer"' ||
'            },' ||
'            "clonalityHiFreq": {' ||
'                "value": 85,' ||
'                "type": "integer"' ||
'            },' ||
'            "bpFreq": {' ||
'                "value": 3,' ||
'                "type": "integer"' ||
'            },' ||
'            "germlineFuzziness": {' ||
'                "value": 3,' ||
'                "type": "integer"' ||
'            },' ||
'            "germlineDbLimit": {' ||
'                "value": 10,' ||
'                "type": "integer"' ||
'            },' ||
'            "circlizeScoreThreshold": {' ||
'                "value": 3,' ||
'                "type": "integer"' ||
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
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for Roddy Sophia (structural variation calling) 2.2.0', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for Roddy Sophia (structural variation calling) 2.2.0'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy Sophia (structural variation calling) 2.2.0'), (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for Roddy Sophia (structural variation calling) 2.2.0'), (SELECT id FROM workflow_version WHERE api_version_id in (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')) AND workflow_version.workflow_version = '2.2.0')
    ON CONFLICT DO NOTHING;
