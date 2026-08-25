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
 */


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204',
        '{' ||
        '    "RODDY_FILENAMES": {' ||
        '        "filenames": [' ||
        '            {' ||
        '                "class": "AlignedSequenceFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${dataSet}_${sample}_${run}_${laneindex}_sequence.sai",' ||
        '                "fileStage": "INDEXEDLANE"' ||
        '            },' ||
        '            {' ||
        '                "class": "FastqcFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastqc.zip",' ||
        '                "onMethod": "LaneFile.calcFastqc"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastq_qcpass_status.txt",' ||
        '                "onMethod": "LaneFile.calcFastqc"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsAlignmentSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicates"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"TARGET_BAM_EXTENSION\",default=\"targetExtract.rmdup.bam\"}",' ||
        '                "onMethod": "extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamIndexFile",' ||
        '                "pattern": "${sourcefile}.bai",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsFile",' ||
        '                "pattern": "${sourcefile}.dupmark_metrics.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "FlagstatsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "AlignedSequenceFileGroup.pairAndSortSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "AlignedSequenceFileGroup.pairAndSortSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "onMethod": "BamFile.calcReadBinsCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "onMethod": "BamFile.calcCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_RawBamFile.txt",' ||
        '                "onMethod": "BamFile.rawBamCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomic}.DepthOfCoverage_Target.txt",' ||
        '                "onMethod": "BamFile.targetCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_TargetsWithCov.txt",' ||
        '                "selectiontag": "targetsWithCoverage",' ||
        '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_targetExtract.rmdup.bam.DepthOfCoverage_Target.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
        '                "selectiontag": "extendedFlagstats",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "OnTargetCoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomic}_TargetsWithCov.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "OnTargetCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_targetCovDistribution.png",' ||
        '                "fileStage": "de.dkfz.roddy.knowledge.files.FileStage.GENERIC"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsizes.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png_qcValues.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_wroteQcSummary.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomic}_qualitycontrol.json",' ||
        '                "selectiontag": "qcJson",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${p'||'id}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "derivedFrom": "CoverageTextFile[2]"' ||
        '            },' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${p'||'id}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "selectiontag": "singlePlot",' ||
        '                "derivedFrom": "CoverageTextFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "MethylationMetaCheckpointFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${methCallingOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "MethylationMetaMetricsCheckpointFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${methCallingMetricsOutputDirectory}/.${sourcefileAtomic}.checkpoint",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "AlignedSequenceFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${dataSet}_${sample}_${library}_${run}_${laneindex}_sequence.sai",' ||
        '                "fileStage": "INDEXEDLANE"' ||
        '            },' ||
        '            {' ||
        '                "class": "FastqcFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${library}_${run}_${laneindex}_sequence_fastqc.zip",' ||
        '                "fileStage": "INDEXEDLANE"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsAlignmentSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicates"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${library}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${p'||'id}_${cvalue,name=\"TARGET_BAM_EXTENSION\",default=\"targetExtract.rmdup.bam\"}",' ||
        '                "onMethod": "extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${alignmentOutputDirectory}/${sample}_${library}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamIndexFile",' ||
        '                "pattern": "${sourcefile}.bai",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "BamMetricsFile",' ||
        '                "pattern": "${sourcefile}.dupmark_metrics.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "FlagstatsFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "ChromosomeDiffValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_${library}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "selectiontag": "readBinsCoverage",' ||
        '                "onMethod": "AlignedSequenceFileGroup.pairAndSortSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "AlignedSequenceFileGroup.pairAndSortSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
        '                "onMethod": "BamFile.calcReadBinsCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
        '                "onMethod": "BamFile.calcCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_RawBamFile.txt",' ||
        '                "onMethod": "BamFile.rawBamCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomic}.DepthOfCoverage_Target.txt",' ||
        '                "onMethod": "BamFile.targetCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_TargetsWithCov.txt",' ||
        '                "selectiontag": "targetsWithCoverage",' ||
        '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "CoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_targetExtract.rmdup.bam.DepthOfCoverage_Target.txt",' ||
        '                "selectiontag": "genomeCoverage",' ||
        '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
        '                "selectiontag": "extendedFlagstats",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "OnTargetCoverageTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sourcefileAtomic}_TargetsWithCov.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "OnTargetCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${sample}_${p'||'id}_targetCovDistribution.png",' ||
        '                "fileStage": "de.dkfz.roddy.knowledge.files.FileStage.GENERIC"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesPlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesTextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsizes.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "InsertSizesValueFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png_qcValues.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "QCSummaryFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_wroteQcSummary.txt",' ||
        '                "selectiontag": "targetExtract",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${sourcefileAtomic}_qualitycontrol.json",' ||
        '                "selectiontag": "qcJson",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
        '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
        '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlimWithLibrary"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
        '                "onMethod": "BamFileGroup.mergeSlim"' ||
        '            },' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${p'||'id}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "derivedFrom": "CoverageTextFile[2]"' ||
        '            },' ||
        '            {' ||
        '                "class": "GenomeCoveragePlotFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/${coverageOutputDirectory}/${p'||'id}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
        '                "selectiontag": "singlePlot",' ||
        '                "derivedFrom": "CoverageTextFile"' ||
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
        '                "class": "BamIndexFile",' ||
        '                "pattern": "${sourcefile}.bai",' ||
        '                "derivedFrom": "BamFile"' ||
        '            },' ||
        '            {' ||
        '                "class": "FastqcFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${library}_${run}_${lane}_${laneindex}_sequence_fastqc.zip",' ||
        '                "onMethod": "LaneFile.calcFastqc"' ||
        '            },' ||
        '            {' ||
        '                "class": "TextFile",' ||
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${library}_${run}_${lane}_${laneindex}_sequence_fastq_qcpass_status.txt",' ||
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
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
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
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
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
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${library}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
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
        '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
        '                "selectiontag": "fingerprints",' ||
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
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204', 6, 'DEFAULT_VALUES', (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'
                                                       AND deprecation_date IS NULL))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id FROM workflow WHERE name = 'WGBS alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id in
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment'))
          AND workflow_version.workflow_version = '1.2.73-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id in
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment'))
          AND workflow_version.workflow_version = '1.2.73-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id in
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment'))
          AND workflow_version.workflow_version = '1.2.73-201')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id in
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment'))
          AND workflow_version.workflow_version = '1.2.73-202')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id in
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment'))
          AND workflow_version.workflow_version = '1.2.73-204')
ON CONFLICT DO NOTHING;
