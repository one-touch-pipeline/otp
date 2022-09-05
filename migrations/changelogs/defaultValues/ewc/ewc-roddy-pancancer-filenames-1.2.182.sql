
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default filenames values for PanCancer alignment 1.2.182',
'{' ||
'    "RODDY_FILENAMES": {' ||
'        "filenames": [' ||
'            {' ||
'                "class": "BamIndexFile",' ||
'                "pattern": "${sourcefile}.bai",' ||
'                "derivedFrom": "BamFile"' ||
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
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomic}_HartigansDip.txt",' ||
'                "selectiontag": "dipStatistics",' ||
'                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomic}_HartigansDip_densityPlot.png",' ||
'                "selectiontag": "dipStatistics",' ||
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
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_HartigansDip.txt",' ||
'                "selectiontag": "dipStatistics",' ||
'                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_HartigansDip_densityPlot.png",' ||
'                "selectiontag": "dipStatistics",' ||
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
'                "class": "BamFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${p'||'id}_${cvalue,name=\"TARGET_BAM_EXTENSION\",default=\"targetExtract.rmdup.bam\"}",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "FlagstatsFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
'                "selectiontag": "extendedFlagstats",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "ChromosomeDiffPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png_qcValues.txt",' ||
'                "selectiontag": "targetExtract",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsizes.txt",' ||
'                "selectiontag": "targetExtract",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesPlotFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png",' ||
'                "selectiontag": "targetExtract",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_HartigansDip.txt",' ||
'                "selectiontag": "dipStatistics",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomic}.DepthOfCoverage_Target.txt",' ||
'                "selectiontag": "genomeCoverage",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "CoverageTextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_TargetsWithCov.txt",' ||
'                "selectiontag": "targetsWithCoverage",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "QCSummaryFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_wroteQcSummary.txt",' ||
'                "selectiontag": "targetExtract",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol_targetExtract.json",' ||
'                "selectiontag": "qcJson",' ||
'                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
'            }' ||
'        ]' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default filenames values for PanCancer alignment 1.2.182', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default filenames values for PanCancer alignment 1.2.182'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for PanCancer alignment 1.2.182'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for PanCancer alignment 1.2.182'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.182')
    ON CONFLICT DO NOTHING;
