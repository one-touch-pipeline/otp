/*
 * Copyright 2011-2019 The OTP authors
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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataExport.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ScriptInputHelperService

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Script to export data (fastq-, bam-file(s) and analysis) to external source.
 *
 * The following options are available:
 *
 *      - scriptOutputFile: absolute path to the desired script file
 *      - targetOutputFolder: absolute path to the desired output location
 *      - copyFastqFiles: enable to copy fastq files
 *      - copyBamFiles: enable to copy bam files
 *      - copyAnalyses: enable to copy the analysis files for each analysis instance individually
 *      - checkFileStatus: if enabled script only checks files and prints information. To get an output script disable this option.
 *      - getFileList: if enabled a list of files is additionally provided
 *      - unixGroup: set a new unix group for the copied data
 *      - external: if enabled, copied data is additionally granted read and execute permissions at the group level
 */

//input area
/**
 * List of pids, one per line
 */
String selectByIndividual = """
#pid1
#pid2

"""

/**
 * Multi selector using:
 * - pid: required
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName
 *
 * The columns can be separated by comma, semicolon or tab. Each value is trimmed
 */
String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2

"""

//************ Path to save creation script to filesystem including filename. (absolute path) ************//
String scriptOutputFile = ""

//************ Path to copy files. Underneath, 'PID folders' will be created. (absolute path) ************//
String targetOutputFolder = ""

//************ Select whether FASTQ files should be copied (true/false) ************//
boolean copyFastqFiles = false

//************ Select whether BAM files should be copied (true/false) ************//
boolean copyBamFiles = false

//************ Select whether analyses should be copied (true/false) ************//
Map<PipelineType, Boolean> copyAnalyses = [:]

copyAnalyses.put(PipelineType.INDEL,        false)
copyAnalyses.put(PipelineType.SOPHIA,       false)
copyAnalyses.put(PipelineType.ACESEQ,       false)
copyAnalyses.put(PipelineType.SNV,          false)
copyAnalyses.put(PipelineType.RUN_YAPSA,    false)

//! Note: RNA_ANALYSIS will be exported only if copyBamFile is also set to be true !//
copyAnalyses.put(PipelineType.RNA_ANALYSIS, false)

//************ Check if and which files exist (true/false) ************//
boolean checkFileStatus = true

//************ Generate a script for file list (true/false) [checkFileStatus must be false] ************//
boolean getFileList = false

//************ Select new unix group ************//
String unixGroup = ""

//************ Select the permissions of the files. If true group can read/execute. If false group/others can read/execute************//
boolean external = true

//************ adds COPY_TARGET_BASE and COPY_CONNECTION environment variables to mkdir and rsync************//
boolean copyExternal = false

// work area
assert scriptOutputFile  : "scriptOutputPath should not be empty"
assert targetOutputFolder: "targetOutputFolder should not be empty"
assert unixGroup         : "no group given"

ScriptInputHelperService scriptInputHelperService = ctx.scriptInputHelperService
ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService
SeqTypeService seqTypeService = ctx.seqTypeService
SeqTrackService seqTrackService = ctx.seqTrackService
AbstractMergedBamFileService abstractMergedBamFileService = ctx.abstractMergedBamFileService
SamplePairService samplePairService = ctx.samplePairService
DataExportService dataExportService = ctx.dataExportService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

Path targetFolder = fileSystem.getPath(targetOutputFolder)

// where to put output
Path scriptOutputPath = fileSystem.getPath(scriptOutputFile)

String outputFileName = scriptOutputPath.fileName
String outputDir = scriptOutputPath.parent.toString()

Path outputDirPath = fileService.toPath(new File(outputDir), fileSystem)
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputDirPath, realm)
Path outputFile = fileService.createOrOverwriteScriptOutputFile(outputDirPath, outputFileName, realm)

assert scriptOutputPath.absolute: "scriptOutputPath is not an absolute path"
assert targetFolder.absolute    : "targetOutputFolder is not an absolute path"

// Data structure for processing
List<SeqTrack> seqTrackList = []
List<AbstractMergedBamFile> bamFileList = []
Map<PipelineType, List<BamFilePairAnalysis>> analysisListMap = [:]

class DataExportOverviewItem {
    PipelineType pipelineType

    Individual individual
    SampleType sampleType, sampleType2
    SeqType seqType
    String sampleIdentifier

    List<SeqTrack> withdrawnFastQs = []
    List<SeqTrack> swappedFastQs = []
    List<SeqTrack> allFastQs = []

    List<AbstractMergedBamFile> externalBamFiles = []
}

List<DataExportOverviewItem> dataExportOverview = []

// parse the input
scriptInputHelperService.parseAndSplitHelper([selectByIndividual, multiColumnInput].join("\n")).each {List<String> params ->
    int paramsCount = params.size()
    assert paramsCount > 0: "Input must contain at least one column of PID"
    assert paramsCount in [1,2,5,6]: "Missing input data for seqType determination"

    //required column
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(params[0], params[0], params[0]),
            "Could not find one individual with name ${params[0]}")

    //optional columns
    SampleType sampleType = null
    if (paramsCount > 1) {
        sampleType = SampleTypeService.findSampleTypeByName(params[1])
        assert sampleType: "Could not find one sampleType with name ${params[1]}"
    }

    SeqType seqType = null
    if (paramsCount > 4) {
        SequencingReadType libraryLayout = SequencingReadType.findByName(params[3])
        assert libraryLayout: "${params[3]} is no valid sequencingReadType"
        boolean singleCell = Boolean.parseBoolean(params[4])
        seqType = seqTypeService.findByNameOrImportAlias(params[2], [
                libraryLayout: libraryLayout,
                singleCell   : singleCell,
        ])
        assert seqType: "Could not find one seqType for ${params[2]}, ${params[3]}, ${params[4]}"
    }

    String sampleName = null
    if (paramsCount > 5) {
        sampleName = params[5]
    }

    //find the seqTracks, which might be used for analysis
    List<SeqTrack> seqTracks = seqTrackService.findAllByIndividualSampleTypeSeqTypeSampleName(
            individual,
            sampleType,
            seqType,
            sampleName
    )
    assert seqTracks: "Could not find any seqtracks for ${params.join(' ')}"
    seqTrackList.addAll(seqTracks)

    //find the bam files, which might be used for analysis
    List<AbstractMergedBamFile> bamFiles = abstractMergedBamFileService.findAllByIndividualSampleTypeSeqType(
            individual,
            sampleType,
            seqType
    )

    // FastQ files
    if (copyFastqFiles) {
        seqTracks.collect { SeqTrack seqTrack ->
            [seqTrack.individual, seqTrack.sampleType, seqTrack.seqType]
        }.unique().each {
            List<SeqTrack> lanes = seqTrackService.findAllByIndividualSampleTypeSeqTypeSampleName(
                    it[0],it[1],it[2]
            )
            dataExportOverview.add(new DataExportOverviewItem(
                    pipelineType    : PipelineType.FASTQ,
                    individual      : it[0],
                    sampleType      : it[1],
                    seqType         : it[2],
                    sampleIdentifier: sampleName ? sampleName : "",
                    allFastQs       : lanes,
                    swappedFastQs   : lanes.findAll { SeqTrack st ->
                        st.swapped
                    },
                    withdrawnFastQs : lanes.findAll { SeqTrack st ->
                        st.isWithdrawn()
                    },
            ))
        }
    }

    // Bam Files
    if (copyBamFiles) {
        if (bamFiles) {
            bamFileList.addAll(bamFiles)
            bamFiles.collect { AbstractMergedBamFile bam ->
                [bam.individual, bam.sampleType, bam.seqType]
            }.unique().each {
                dataExportOverview.add(new DataExportOverviewItem(
                        pipelineType: PipelineType.BAM,
                        individual  : it[0],
                        sampleType  : it[1],
                        seqType     : it[2],
                        externalBamFiles: bamFiles.findAll { AbstractMergedBamFile bamFile ->
                            bamFile.workPackage.pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED
                        },
                ))
            }
        }
    }

    // Analysis Files
    copyAnalyses.findAll { Map.Entry<PipelineType, Boolean> entry ->
        entry.value
    }.each { Map.Entry<PipelineType, Boolean> instance ->
        List<BamFilePairAnalysis> analysisPerMultiImport = analysisListMap.get(instance.key)
        samplePairService.findAllByIndividualSampleTypeSeqType(
                individual,
                sampleType,
                seqType
        ).each { samplePair ->
            List<SampleType> sampleTypeOfIndividual = seqTrackList.findAll { SeqTrack seqTrack ->
                seqTrack.individual == samplePair.individual
            }.collect {
                it.sampleType
            }.unique()
            if (sampleTypeOfIndividual.containsAll([samplePair.sampleType1, samplePair.sampleType2])) {
                List<BamFilePairAnalysis> bamFilePairAnalyses = BamFilePairAnalysis.withCriteria {
                    eq('samplePair', samplePair)
                    eq('processingState', AnalysisProcessingStates.FINISHED)
                    like('instanceName', "%${instance.key}%")
                    order("id", "desc")
                }
                if (!bamFilePairAnalyses.isEmpty()) {
                    BamFilePairAnalysis analysis = bamFilePairAnalyses.first()
                    if (analysisPerMultiImport) {
                        analysisPerMultiImport.add(analysis)
                    } else {
                        analysisListMap.put(instance.key, [analysis])
                    }
                    dataExportOverview.add(new DataExportOverviewItem(
                            pipelineType    : instance.key,
                            individual      : analysis.individual,
                            sampleType      : analysis.sampleType1BamFile.sampleType,
                            sampleType2     : analysis.sampleType2BamFile.sampleType,
                            seqType         : analysis.sampleType1BamFile.seqType,
                    ))
                }
            }
        }
    }
}

DataExportInput dataExportParameters = new DataExportInput([
        //input parameters
        targetFolder        : targetFolder,
        checkFileStatus     : checkFileStatus,
        getFileList         : getFileList,
        unixGroup           : unixGroup,
        external            : external,
        copyExternal        : copyExternal,
        copyAnalyses        : copyAnalyses,
        //preprocessed data for export
        seqTrackList        : seqTrackList,
        bamFileList         : bamFileList,
        analysisListMap     : analysisListMap,
])

DataExportOutput emptyOutput = new DataExportOutput(
        bashScript: "",
        listScript: "",
        consoleLog: "")
List<DataExportOutput> outputList = [
    //Output header information
    headerInfoOutput = dataExportService.exportHeaderInfo(dataExportParameters),
    //Processing FASTQ Files
    seqTrackOutput = copyFastqFiles ? dataExportService.exportDataFiles(dataExportParameters) : emptyOutput,
    //Processing BAM Files
    bamFileOutput = copyBamFiles ? dataExportService.exportBamFiles(dataExportParameters) : emptyOutput,
    //Processing Analysis Files
    analysisOutput = dataExportService.exportAnalysisFiles(dataExportParameters),
]

String getConsoleLog(List<DataExportOutput> outputList) {
    return outputList.collect {
        it.consoleLog
    }.join("\n")
}

String getBashScript(List<DataExportOutput> outputList) {
    return outputList.collect {
        it.bashScript
    }.join("\n")
}

String getListScript(List<DataExportOutput> outputList) {
    return outputList.collect {
        it.listScript
    }.join("\n")
}

StringBuilder summaryString = new StringBuilder()

summaryString.append("PID\tSAMPLE-TPYE-1\tSAMPLE-TPYE-2\tSEQ-TPYE\tFILE-TPYE\tMORE INFOS\n")
dataExportOverview.findAll { DataExportOverviewItem dataExportOverviewItem ->
    dataExportOverviewItem.pipelineType == PipelineType.BAM &&
    (dataExportOverviewItem.seqType == SeqTypeService.rnaSingleSeqType || dataExportOverviewItem.seqType == SeqTypeService.rnaPairedSeqType)
}.each { DataExportOverviewItem dataExportOverviewItem ->
    int idx = dataExportOverview.findLastIndexOf { it.individual == dataExportOverviewItem.individual}
    dataExportOverview.add(idx + 1, new DataExportOverviewItem(
            pipelineType    : PipelineType.RNA_ANALYSIS,
            individual      : dataExportOverviewItem.individual,
            sampleType      : dataExportOverviewItem.sampleType,
            sampleType2     : null,
            seqType         : dataExportOverviewItem.seqType,
    ))
}
dataExportOverview.each {
    summaryString.append("${it.individual.mockPid}")

    summaryString.append("\t" + (it.sampleType   ? "${it.sampleType.name}"     : "-"))
    summaryString.append("\t" + (it.sampleType2  ? "${it.sampleType2.name}"    : "-"))
    summaryString.append("\t" + (it.seqType      ? "${it.seqType.displayNameWithLibraryLayout}" : "-"))
    summaryString.append("\t" + (it.pipelineType ? "${it.pipelineType}"        : "-"))

    summaryString.append("\t")
    int count = it.swappedFastQs.size()
    if (count) {
        summaryString.append("${count} out of ${it.allFastQs.size()} swapped FastQ files; ")
    }

    count = it.withdrawnFastQs.size()
    if (count) {
        summaryString.append("${count} out of ${it.allFastQs.size()} withdrawn FastQ files; ")
    }
    count = it.externalBamFiles.size()
    if (count) {
        summaryString.append("${count} external BAM files; ")
    }

    summaryString.append("\n")
}

//Out put the result
outputFile << getBashScript(outputList)
//set the correct permission (440) to the script file
fileService.setPermission(outputFile, FileService.DEFAULT_FILE_PERMISSION)

println "\nCreation script has been saved to ${outputFile}"

println "\n----------- ${checkFileStatus ? "to get an output script -> set checkFileStatus = false" : "output script"} -----------"
println getConsoleLog(outputList)

println "\n\n----------- ${!getFileList ? "to get an output list -> set getFileList = true and checkFileStatus = false" : "output list"} -----------"
println getListScript(outputList)

println "\n*********************** Summary of Data Export ********************"
println summaryString.toString()
