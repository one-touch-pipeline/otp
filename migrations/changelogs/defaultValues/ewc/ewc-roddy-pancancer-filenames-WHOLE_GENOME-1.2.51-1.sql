/*
 * Copyright 2011-2022 The OTP authors
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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME',
        '{' ||
        '    "RODDY_FILENAMES": {' ||
        '        "filenames": [' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${p' ||
        'id}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "derivedFrom": "CoverageTextFile[2]"' ||
        '            },' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${p' ||
        'id}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "selectiontag": "singlePlot",' ||
        '                "derivedFrom": "CoverageTextFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamIndexFile",' ||
        '                "pattern": "${sourcefile}.bai",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "FastqcFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${laneindex}_sequence_fastqc.zip",' ||
        '                "onMethod": "LaneFile.calcFastqc"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "FlagstatsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
        '                "selectiontag": "extendedFlagstats",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomic}.dupmark_metrics.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/qualitycontrol.json",' ||
        '                "selectiontag": "qcJson",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${p' || 'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "FlagstatsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
        '                "selectiontag": "extendedFlagstats",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomic}.dupmark_metrics.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol.json",' ||
        '                "selectiontag": "qcJson",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsAlignmentSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
        '                "onMethod": "BamFile.collectMetrics"' ||
        '            }' ||
        '        ]' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME', 22, 'DEFAULT_VALUES',
        (SELECT id
         FROM external_workflow_config_fragment
         WHERE name = 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME'),
       (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
          AND workflow_version.workflow_version = '1.2.51-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for PanCancer alignment 1.2.51-1 WHOLE_GENOME'),
       (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = FALSE AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
