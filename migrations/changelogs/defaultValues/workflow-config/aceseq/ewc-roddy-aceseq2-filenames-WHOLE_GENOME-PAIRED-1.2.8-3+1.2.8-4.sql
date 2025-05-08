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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED',
        '{' ||
        '    "RODDY_FILENAMES": {' ||
        '        "filenames": [' ||
        '            {' ||
        '                "class": "SNPPositionFile",' ||
        '                "pattern": "${cnvSnpOutputDirectory}/${p' || 'id}.chr${PARM_CHR_INDEX}.${SNP_SUFFIX}",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageWindowsFile",' ||
        '                "pattern": "${cnvSnpOutputDirectory}/${p' || 'id}.chr${PARM_CHR_INDEX}.${CNV_SUFFIX}",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.snp.tab.gz",' ||
        '                "onMethod": "SNPPositionFileGroupByChromosome.mergeAndFilter"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.snp.fakeBAF.tab.gz",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.getGenotypes"' ||
        '            },' ||
        '            {' ||
        '                "class": "PhasedGenotypeFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"phasedGenotypesPrefix\",default=\"phased_chr\"}${jobParameter,name=\"PARM_CHR_INDEX\"}.${cvalue,name=\"phasedGenotypesSuffix\",default=\"vcf\"}",' ||
        '                "selectiontag": "withChromosomeIndex",' ||
        '                "onTool": "imputeGenotypes_noMpileup"' ||
        '            },' ||
        '            {' ||
        '                "class": "HaploblockGroupFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"haplogroupFilePrefix\",default=\"haploblocks_chr\"}${jobParameter,name=\"PARM_CHR_INDEX\"}.${cvalue,name=\"FILE_TXT_SUFFIX\",default=\"txt\"}",' ||
        '                "selectiontag": "withChromosomeIndex",' ||
        '                "onTool": "imputeGenotypes_noMpileup"' ||
        '            },' ||
        '            {' ||
        '                "class": "PhasedGenotypeFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"phasedGenotypesPrefix\",default=\"phased_chr\"}23.${cvalue,name=\"phasedGenotypesSuffix\",default=\"vcf\"}",' ||
        '                "selectiontag": "forChromosomeX",' ||
        '                "onTool": "imputeGenotypes_X_noMpileup"' ||
        '            },' ||
        '            {' ||
        '                "class": "HaploblockGroupFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"haplogroupFilePrefix\",default=\"haploblocks_chr\"}23.${cvalue,name=\"FILE_TXT_SUFFIX\",default=\"txt\"}",' ||
        '                "selectiontag": "forChromosomeX",' ||
        '                "onTool": "imputeGenotypes_X_noMpileup"' ||
        '            },' ||
        '            {' ||
        '                "class": "PhasedGenotypeFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"phasedGenotypesPrefix\",default=\"phased_chr\"}${jobParameter,name=\"PARM_CHR_INDEX\"}.${cvalue,name=\"phasedGenotypesSuffix\",default=\"vcf\"}",' ||
        '                "selectiontag": "withChromosomeIndex",' ||
        '                "onTool": "imputeGenotypes"' ||
        '            },' ||
        '            {' ||
        '                "class": "HaploblockGroupFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"haplogroupFilePrefix\",default=\"haploblocks_chr\"}${jobParameter,name=\"PARM_CHR_INDEX\"}.${cvalue,name=\"FILE_TXT_SUFFIX\",default=\"txt\"}",' ||
        '                "selectiontag": "withChromosomeIndex",' ||
        '                "onTool": "imputeGenotypes"' ||
        '            },' ||
        '            {' ||
        '                "class": "PhasedGenotypeFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"phasedGenotypesPrefix\",default=\"phased_chr\"}23.${cvalue,name=\"phasedGenotypesSuffix\",default=\"vcf\"}",' ||
        '                "selectiontag": "forChromosomeX",' ||
        '                "onTool": "imputeGenotypes_X"' ||
        '            },' ||
        '            {' ||
        '                "class": "HaploblockGroupFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue,name=\"haplogroupFilePrefix\",default=\"haploblocks_chr\"}23.${cvalue,name=\"FILE_TXT_SUFFIX\",default=\"txt\"}",' ||
        '                "selectiontag": "forChromosomeX",' ||
        '                "onTool": "imputeGenotypes_X"' ||
        '            },' ||
        '            {' ||
        '                "class": "UnphasedGenotypeFile",' ||
        '                "pattern": "${imputeOutputDirectory}/${cvalue, name=\"unphasedGenotypesPrefix\", default=\"unphased_chr\"}#CHROMOSOME_INDEX#.${cvalue,name=\"unphasedGenotypesSuffix\",default=\"vcf\"}",' ||
        '                "derivedFrom": "TextFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.snp.haplo.tab.gz",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.addHaploTypes"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${plotOutputDirectory}/checkpointBaf.txt",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.createControlBafPlot"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${cnvSnpOutputDirectory}/${p' || 'id}.chr#CHROMOSOME_INDEX#.${cvalue,name=\"CNV_ANNO_SUFFIX\"}",' ||
        '                "selectiontag": "annotatedCoverage",' ||
        '                "onMethod": "CoverageWindowsFileGroupByChromosome.annotate"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_sex.txt",' ||
        '                "selectiontag": "genderFile",' ||
        '                "onMethod": "CoverageWindowsFileGroupByChromosome.annotate"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/checkpointFakeControl.txt",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.replaceControl"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.cnv.tab.gz",' ||
        '                "onMethod": "CoverageWindowsFileAnnotationResult.mergeAndFilterCoverageWindowFiles"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.cnv.tab.gz",' ||
        '                "selectiontag": "covWinFileWithBadControl",' ||
        '                "derivedFrom": "TextFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all.cnv.corrected.tab.gz",' ||
        '                "selectiontag": "correctedWindows",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.correctGC"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${plotOutputDirectory}/${p' || 'id}_qc_gc_corrected.json",' ||
        '                "selectiontag": "tabSlim",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.correctGC"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${plotOutputDirectory}/${p' || 'id}_qc_gc_corrected.tsv",' ||
        '                "selectiontag": "tab",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.correctGC"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_knownsegments.txt",' ||
        '                "selectiontag": "knownsegments",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.pscbsGaps"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_pscbs_data.txt.gz",' ||
        '                "selectiontag": "snpPositions",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.pscbsGaps"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${crestOutputDirectory}/tumor_${p' || 'id}.${cvalue,name=\"crestDelDupInvSuffix\",default=\"DELDUPINV\"}",' ||
        '                "selectiontag": "crestDelDupInvFileTag",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeCrest"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${crestOutputDirectory}/tumor_${p' || 'id}.${cvalue,name=\"crestTransSuffix\",default=\"TX\"}",' ||
        '                "selectiontag": "crestTranslocFileTag",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeCrest"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_breakpoints.txt",' ||
        '                "selectiontag": "breakpointsSvs",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeCrest"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_sv_points.txt",' ||
        '                "selectiontag": "svPoints",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeCrest"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${svOutputDirectory}/svs_${p' || 'id}_filtered_somatic_minEventScore3.tsv",' ||
        '                "selectiontag": "dellyFileTag",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeDelly"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_breakpoints.txt",' ||
        '                "selectiontag": "breakpointsSvs",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeDelly"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_sv_points.txt",' ||
        '                "selectiontag": "svPoints",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.mergeDelly"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_fit.txt",' ||
        '                "selectiontag": "segments",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.getSegmentAndGetSnps"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_homozygous_deletion.txt.gz",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.markSegsWithHomozygDel"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all_seg.txt.gz",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.segsToSnpDataHomodel"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all_seg_1.txt.gz",' ||
        '                "selectiontag": "snps",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.clusterPruneSegments"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_clustered_and_pruned_and_normal.txt",' ||
        '                "selectiontag": "segments",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.clusterPruneSegments"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_all_seg_2.txt.gz",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.segsToSnpDataPruned"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_combi_level.txt",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.estimatePeaks"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/${p' || 'id}_ploidy_purity_2D.txt",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.estimatePurityPloidy"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/checkpointPlots.txt",' ||
        '                "selectiontag": "checkpoint",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.generatePlots"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/cnv_${p' || 'id}_parameter.json",' ||
        '                "selectiontag": "json",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.generatePlots"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${aceseqOutputDirectory}/checkpointVcf.txt",' ||
        '                "onMethod": "de.dkfz.b080.co.aceseq.ACESeqMethods.convertToVcf"' ||
        '            }' ||
        '        ]' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED', 22,
        'DEFAULT_VALUES', (SELECT id
                           FROM external_workflow_config_fragment
                           WHERE name = 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)'))
          AND workflow_version.workflow_version = '1.2.8-3')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)'))
          AND workflow_version.workflow_version = '1.2.8-4')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default filenames values for Roddy ACEseq (CNV calling) 1.2.8-3, 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = FALSE AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
