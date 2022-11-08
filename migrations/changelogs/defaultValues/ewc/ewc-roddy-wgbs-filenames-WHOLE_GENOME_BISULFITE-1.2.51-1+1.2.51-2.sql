
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE',
'{' ||
'    "RODDY_FILENAMES": {' ||
'        "filenames": [' ||
'            {' ||
'                "class": "GenomeCoveragePlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${p'||'id}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
'                "derivedFrom": "CoverageTextFile[2]"' ||
'            },' ||
'            {' ||
'                "class": "GenomeCoveragePlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${p'||'id}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
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
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${library}_${run}_${laneindex}_sequence_fastqc.zip",' ||
'                "onMethod": "LaneFile.calcFastqc"' ||
'            },' ||
'            {' ||
'                "class": "BamFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${library}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
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
'                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
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
'                "class": "BamFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${p'||'id}_${library}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "FlagstatsFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
'                "selectiontag": "extendedFlagstats",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "BamMetricsFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${sourcefileAtomic}.dupmark_metrics.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "QCSummaryFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
'                "selectiontag": "genomeCoverage",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
'                "selectiontag": "readBinsCoverage",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/qualitycontrol.json",' ||
'                "selectiontag": "qcJson",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
'            },' ||
'            {' ||
'                "class": "BamFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "MethylationMetaCheckpointFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/methylation/merged/${methCallingOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
'                "onMethod": "BamFile.mergedMethylationCallingMeta"' ||
'            },' ||
'            {' ||
'                "class": "MethylationMetaMetricsCheckpointFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/methylation/merged/${methCallingMetricsOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
'                "onMethod": "BamFile.mergedMethylationCallingMeta"' ||
'            },' ||
'            {' ||
'                "class": "MethylationMetaCheckpointFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/methylation/${library}/${methCallingOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
'                "onMethod": "BamFile.libraryMethylationCallingMeta"' ||
'            },' ||
'            {' ||
'                "class": "MethylationMetaMetricsCheckpointFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/methylation/${library}/${methCallingMetricsOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
'                "onMethod": "BamFile.libraryMethylationCallingMeta"' ||
'            },' ||
'            {' ||
'                "class": "FlagstatsFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
'                "selectiontag": "extendedFlagstats",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "BamMetricsFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomic}.dupmark_metrics.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "QCSummaryFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
'                "selectiontag": "genomeCoverage",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
'                "selectiontag": "readBinsCoverage",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol.json",' ||
'                "selectiontag": "qcJson",' ||
'                "onMethod": "BamFileGroup.mergeSlim"' ||
'            }' ||
'        ]' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE', 22, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE'), (SELECT id FROM workflow WHERE name = 'WGBS alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.51-1')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.51-2')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.51-1, 1.2.51-2 WHOLE_GENOME_BISULFITE'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME_BISULFITE' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
