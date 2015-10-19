/*
The following code allows to show the processing state for

* runs
* individuals (pid)
* ilse id
* project (can take some time, depending of project)
* all lanes in processing:
** all withdrawn lanes are ignored
** lanes where fastqc not finished and data load after 1.1.2015 included
** running alignments
*** seqtracks which belong to run segments where flag 'align' is set to false are ignored
*** running OTP alignments
*** running pan can alignments
** sample pairs (snv) waiting for snv calling (ignore pairs without snv config)
** snv calling instance which are in processing

entries are trimmed (spaces before after are removed)
Names prefixed with # are ignored (handled as comment)
Empty lines are ignored


The script has four input areas, one for run names, one for patient names, one for ilse ids, and one for project names.
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*

//name of runs
def runString = """
#run 1
#run 2

"""


//pid of patients
def individualString ="""
#patient 1
#patient 2

"""

def ilseIdString ="""
# ilse id1
# ilse id2

"""

//name of projects, can take some time depending of project(s)
def projectString ="""
#project 1
#project 2

"""


//if enabled, shows all lanes in progressing.
//see file header comment for details
boolean allProcessed = false

//flag if for all workflows the finished entries should be shown
boolean showFinishedEntries = false

//==================================================

SeqType wholeGenomePaired = SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "PAIRED")
SeqType exonPaired = SeqType.findByNameAndLibraryLayout("EXON", "PAIRED")

MetaDataKey libPrepKitKey = MetaDataKey.findByName("LIB_PREP_KIT")
MetaDataKey enrichmentKitKey = MetaDataKey.findByName("ENRICHMENT_KIT")
MetaDataKey commentKey = MetaDataKey.findByName("COMMENT")


List blackList_MergingSetId = ([12174511] as long[]) as List

String INDENT = "    "


List<String> output = []
List<String> outputSeqTrack = []

def nameStringToList = {String nameString ->
    List list = []
    nameString.eachLine {
        if (it.trim().isEmpty() || it.trim().startsWith("#")) {
            return
        }
        list << it.trim()
    }
    return list
}

def prefix = { String text, String prefix ->
    "${prefix}${text.replace('\n', "\n${prefix}")}"
}

def objectsToString = { List objects, Closure valueToShow ->
    return objects.collect { valueToShow(it) }.sort { it }.join('\n')
}


def checkProcessesForObject = { String workflow, List noProcess, List processWithError, Object object, Closure valueToShow, Closure objectToCheck ->
    Object checkedObject = objectToCheck(object)
    boolean hasError = false

    def processes = ProcessParameter.findAllByValue(checkedObject.id, [sort: "id"])*.process.findAll {
        it.jobExecutionPlan.name == workflow
    }
    if (processes.size() == 1) {
        //normal case, no output needed
    } else if (processes.size() == 0) {
        output << "${INDENT}Attention: no process was created for the object ${valueToShow(object)} (${object.id})"
        output << "${INDENT}Please inform a maintainer"
        noProcess << object.id
        return true
    } else if (processes.size() > 1) {
        output << "${INDENT}Attention: There were ${processes.size()} processes created for the object ${valueToShow(object)} (${object.id}). That can make problems."
    }
    Process lastProcess = processes.sort {it.id}.last()
    ProcessingStep ps = ProcessingStep.findByProcessAndNextIsNull(lastProcess)
    ProcessingStepUpdate update = ps.latestProcessingStepUpdate
    def state = update?.state
    if (state == ExecutionState.FAILURE || update == null) {
        hasError = true
        output << "${INDENT}An error occur for the object: ${valueToShow(object)}"
        output << "${INDENT}${INDENT}object class/id: ${object.class} / ${object.id}"
        output << "${INDENT}${INDENT}the OTP link: https://otp.dkfz.de/otp/processes/process/${lastProcess.id}"
        output << "${INDENT}${INDENT}the error: ${ps.latestProcessingStepUpdate?.error?.errorMessage?.replaceAll('\n', "\n${INDENT}${INDENT}${INDENT}${INDENT}${INDENT}")}"
        if (ps.process.comment) {
            output << "${INDENT}${INDENT}the comment (${ps.process.commentDate.format("yyyy-MM-dd")}): ${ps.process.comment.replaceAll('\n', "\n${INDENT}${INDENT}${INDENT}${INDENT}${INDENT}")}"
        }
        if (update == null) {
            output << "${INDENT}${INDENT}no update available: Please inform a maintainer\n"
        }
        processWithError << object.id
    }
    return hasError
}

def checkProcessesForObjects = {String workflow, Collection<Object> objects, Closure valueToShow, Closure objectToCheck ->
    int errorCount = 0
    List noProcess = []
    List processWithError = []
    objects.sort{valueToShow(it)}.each {
        if (checkProcessesForObject(workflow, noProcess, processWithError, it, valueToShow, objectToCheck)) {
            errorCount++
        }
    }
    if (errorCount) {
        output << "\n${INDENT} Count of errors: ${errorCount}"
    }
    if (noProcess) {
        output << "\n${INDENT}objects without process: ${noProcess}"
    }
    if (processWithError) {
        output << "\n${INDENT}objects with error: ${processWithError}"
    }

    output << "\n\n"
}



def seqTracksWithReferenceGenome = {List<SeqTrack> seqTracks ->
    if (!seqTracks) {
        return []
    }
    Map map = seqTracks.groupBy {
        return !it.configuredReferenceGenome
    }
    if (map[true]) {
        output << "${INDENT}${map[true].size()} lanes removed, because the used project(s) has/have no reference genome(s): ${map[true]*.project.unique().sort {it.name}}"
    }
    return map[false] ?: []
}

def exomeSeqTracksWithEnrichmentKit = {List<SeqTrack> seqTracks->
    if (!seqTracks) {
        return []
    }

    Map map = seqTracks.groupBy { !it.libraryPreparationKit }
    if (map[true]) {
        output << "${INDENT}${map[true].size()} lanes removed, because no library preparationKit kit is set."
        output << prefix(objectsToString(map[true], {
            DataFile dataFile = DataFile.findBySeqTrack(it)
            MetaDataEntry libPreKitEntry = MetaDataEntry.findByDataFileAndKey(dataFile, libPrepKitKey)
            MetaDataEntry enrichmentKitEntry = MetaDataEntry.findByDataFileAndKey(dataFile, enrichmentKitKey)
            MetaDataEntry commentEntry = MetaDataEntry.findByDataFileAndKey(dataFile, commentKey)
            if (!it.libraryPreparationKit) {
                if (libPreKitEntry?.value) {
                    outputSeqTrack <<"""addKitToSeqTrack("${it.run.name}", "${it.laneId}", "${libPreKitEntry.value}") //${it.sample}, libprep kit"""
                } else if (enrichmentKitEntry?.value) {
                    outputSeqTrack <<"""addKitToSeqTrack("${it.run.name}", "${it.laneId}", "${enrichmentKitEntry.value}") //${it.sample}, enrichment kit"""
                } else if (commentEntry?.value) {
                    outputSeqTrack <<"""addKitToSeqTrack("${it.run.name}", "${it.laneId}", "${commentEntry.value}") //${it.sample}, comment"""
                } else {
                    outputSeqTrack <<"""//enrichment kit missed for ${it.run.name}  ${it.laneId}   ${it.sample}"""
                }
            }
            "${it}  ${!it.libraryPreparationKit ? "lib kit: ${libPreKitEntry?.value}, enrichment kit: ${enrichmentKitEntry?.value}, comment: ${commentEntry}":''}"
        }), "${INDENT}${INDENT}")
    }
    return map[false] ?: []
}

def exomeSeqTracksWithBedFile = {List<SeqTrack> seqTracks->
    if (!seqTracks) {
        return []
    }
    Map map = seqTracks.groupBy {
        ReferenceGenome referenceGenome = it.configuredReferenceGenome
        BedFile bedFile = BedFile.findByReferenceGenomeAndLibraryPreparationKit(referenceGenome, it.libraryPreparationKit)
        !bedFile
    }
    if (map[true]) {
        output << "${INDENT}${map[true].size()} lanes removed, because no bedfile is definied for: ${map[true].collect{"${it.project.name} ${it.seqType.name}"}.unique().sort {it}.join(', ')}"
        output << prefix(objectsToString(map[true], {it}), "${INDENT}${INDENT}")
    }
    return map[false] ?: []
}

def findNotWithdrawn = { List<SeqTrack> seqTracks ->
    Map map = seqTracks.groupBy {
        DataFile.findAllBySeqTrackAndFileWithdrawn(it, true) as Boolean
    }
    if (map[true]) {
        output << "the ${map[true].size()} withdrawn lanes are filtered out"
    }
    return map[false]
}

def findAlignable = { List<SeqTrack> seqTracks ->
    map = seqTracks.groupBy { it.seqType }

    List<SeqTrack> wholeGenomeSeqtracks = map[wholeGenomePaired]
    List<SeqTrack> exonSeqtracks = map[exonPaired]
    map.remove(wholeGenomePaired)
    map.remove(exonPaired)

    output << ""
    output << "count whole genome paired: ${wholeGenomeSeqtracks ? wholeGenomeSeqtracks.size() : 0 }"
    wholeGenomeSeqtracks = seqTracksWithReferenceGenome(wholeGenomeSeqtracks)

    output << "count exon paired: ${exonSeqtracks ? exonSeqtracks.size() : 0 }"
    exonSeqtracks = seqTracksWithReferenceGenome(exonSeqtracks)
    exonSeqtracks = exomeSeqTracksWithEnrichmentKit(exonSeqtracks)
    exonSeqtracks = exomeSeqTracksWithBedFile(exonSeqtracks)

    output << "count other lanes: ${map ? map.values().flatten().size() : 0 }"

    return [
            wholeGenomeSeqtracks,
            exonSeqtracks
    ].flatten().findAll()
}

def showUniqueList = {String info, List objects ->
    if (!objects) {
        return
    }
    output << prefix("""\
${info} (${objects.size}):
${prefix(objects.collect { it.toString() }.sort().groupBy{it}.collect {key, value -> "${key}  ${value.size() == 1 ? '': "(count: ${value.size()})"}"}.sort().join('\n'), INDENT)}
""", INDENT)
}

def showList = {String info, List objects, Closure valueToShow = {it as String} ->
    if (!objects) {
        return
    }
    output << prefix("""\
${info} (${objects.size}):
${prefix(objectsToString(objects, valueToShow), INDENT)}
""", INDENT)
}


def showNotTriggered = {String workflow, List objects, Closure valueToShow = {it as String} ->
    if (!objects) {
        return
    }
    output << prefix("""\
!!! not started (${objects.size}):
****************************************
ATTENTION: The following ${objects.size()} lanes are not triggered for ${workflow}, but they should:
${prefix(objectsToString(objects, valueToShow), INDENT)}
Please inform a maintainer
****************************************
The ids are: ${objects*.id.sort()}
""", INDENT)
}

def showWaiting = {List objects, Closure valueToShow = {it as String} ->
    showList('waiting', objects, valueToShow)
}

def showRunning = {String workflow, List objects, Closure valueToShow = {it as String}, Closure objectToCheck = {it} ->
    showList('running', objects, valueToShow)
    checkProcessesForObjects(workflow, objects, valueToShow, objectToCheck)
}

def showFinished = {List objects, Closure valueToShow = {it as String} ->
    if (!showFinishedEntries) {
        return
    }
    showList('finished', objects, valueToShow)
}

def showShouldStart = {List objects, Closure valueToShow = {it as String} ->
    showList('needs processing', objects, valueToShow)
}

def handleStateMap = {Map map, String workflow, Closure valueToShow, Closure objectToCheck = {it} ->
    output << "\n${workflow}: "
    def ret
    def keys = map.keySet().sort{it}

    keys.each { key->
        def values = map[key]
        switch (key.ordinal()) {
            case 0:
                showNotTriggered(workflow, values, valueToShow)
                break
            case 1:
                showWaiting(values, valueToShow)
                break
            case 2:
                showRunning(workflow, values, valueToShow, objectToCheck)
                break
            case 3:
                showFinished(values, valueToShow)
                ret = map[key]
                break
            default:
                switch (key) {
                //for the case of more values
                    default:
                        new RuntimeException("Not handled value: ${key}. Please inform a maintainer")
                }
        }
    }
    return ret
}

// map: MergingWorkPackages grouped by Workpackage.needsProcessing
// valueToShow: MergingWorkPackage properties to print out
def handleStateMapNeedsProcessing = {Map<Boolean, Collection<MergingWorkPackage>> map, Closure valueToShow ->
    output << "\nPanCanAlignmentWorkflow: \n${INDENT}Only SeqTracks are shown which finished fastqc-WF"

    showShouldStart(map[true], valueToShow)

    return map[false]
}

// map: RoddyBamFiles grouped by AbstractMergedBamFile.FileOperationStatus
// workflow: the workflow name
// valueToShow: RoddyBamFile properties to print out
// objectToCheck: defines which objects should be checked to have processes
def handleStateMapRoddy = {Map<AbstractMergedBamFile.FileOperationStatus, Collection<RoddyBamFile>> map, String workflow,
                           Closure valueToShow, Closure objectToCheck = {it} ->
    def ret
    def keys = map.keySet().sort{it}

    keys.each { key->
        def values = map[key]
        switch (key) {
            case AbstractMergedBamFile.FileOperationStatus.DECLARED :
                showList('running (declared)', values, valueToShow)
                checkProcessesForObjects(workflow, values, valueToShow, objectToCheck)
                break
            case AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING:
                showList('running (needs_processing)', values, valueToShow)
                checkProcessesForObjects(workflow, values, valueToShow, objectToCheck)
                break
            case AbstractMergedBamFile.FileOperationStatus.INPROGRESS:
                showList('running (in_progress)', values, valueToShow)
                checkProcessesForObjects(workflow, values, valueToShow, objectToCheck)
                break
            case AbstractMergedBamFile.FileOperationStatus.PROCESSED:
                if(showFinishedEntries) {
                    showList('processed', values, valueToShow)
                }
                ret = map[key]
                break
            default:
                new RuntimeException("Not handled value: ${key}. Please inform a maintainer")
        }
    }
    return ret
}

def handleStateMapSnv = { List next ->
    output << "\nsnvWorkflow"

    Map stateMap = ['disabled': [], 'noConfig': [], 'running': [], 'finished': [], 'waiting': [], 'notTriggered': []]

    List samplePairs = []
    List unknownDiseaseStatus = []
    List ignoredDiseaseStatus = []
    List unknownThreshold = []
    List noPairFound = []

    next.each { AbstractMergedBamFile ambf ->
        List samplePairsForBamFile = SamplePair.createCriteria().list {
            or {
                eq('mergingWorkPackage1', ambf.mergingWorkPackage)
                eq('mergingWorkPackage2', ambf.mergingWorkPackage)
            }
        }
        if (samplePairsForBamFile) {
            samplePairs += samplePairsForBamFile
        } else if (!SampleTypePerProject.findByProjectAndSampleType(ambf.project, ambf.sampleType)) {
            unknownDiseaseStatus << "${ambf.project} ${ambf.sampleType.name}"
        } else if (SampleTypePerProject.findByProjectAndSampleType(ambf.project, ambf.sampleType).category == SampleType.Category.IGNORED) {
            ignoredDiseaseStatus << "${ambf.project} ${ambf.sampleType.name}"
        } else if (!ProcessingThresholds.findByProjectAndSampleTypeAndSeqType(ambf.project, ambf.sampleType, ambf.seqType)) {
            unknownThreshold << "${ambf.project} ${ambf.sampleType.name} ${ambf.seqType}"
        } else {
            noPairFound << "${ambf.project} ${ambf.individual} ${ambf.sampleType.name} ${ambf.seqType}"
        }
    }

    showUniqueList('For the following project sample type combination the sample type was not classified as disease or control', unknownDiseaseStatus)
    showUniqueList('For the following project sample type combination the sample type category is set to IGNORED', ignoredDiseaseStatus)
    showUniqueList('For the following project sample type seqType combination no threshold is defined', unknownThreshold)
    showUniqueList('For the following AMBF no SamplePair could be found', noPairFound)

    if(samplePairs) {
        samplePairs.unique().each { SamplePair samplePair ->
            if (samplePair.processingStatus == SamplePair.ProcessingStatus.DISABLED) {
                stateMap.disabled << samplePair
            } else if (samplePair.processingStatus == SamplePair.ProcessingStatus.NEEDS_PROCESSING) {
                if (SnvConfig.findByProjectAndSeqTypeAndObsoleteDateIsNull(samplePair.project, samplePair.seqType)) {
                    stateMap.waiting << samplePair
                } else {
                    stateMap.noConfig << "${samplePair.project} ${samplePair.seqType}"
                }
            } else {
                // get latest SnvCallingInstance for this SamplePair
                SnvCallingInstance snvCallingInstance = SnvCallingInstance.createCriteria().get {
                    eq('samplePair', samplePair)
                    order('lastUpdated', 'desc')
                    maxResults(1)
                }
                if (snvCallingInstance && snvCallingInstance.processingState == SnvProcessingStates.IN_PROGRESS) {
                    stateMap.running << snvCallingInstance
                }
                if (snvCallingInstance && snvCallingInstance.processingState == SnvProcessingStates.FINISHED) {
                    stateMap.finished << samplePair
                }
                if (!snvCallingInstance && samplePair.processingStatus == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED) {
                    stateMap.notTriggered << samplePair
                }
            }
        }
    }

    showUniqueList('For the following project seqtype combination no config is defined', stateMap.noConfig)
    showList('disabled', stateMap.disabled)
    showList('notTriggered', stateMap.notTriggered)
    showWaiting(stateMap.waiting)
    showRunning("SnvWorkflow", stateMap.running)
    showFinished(stateMap.finished)

    return stateMap.finished
}

def showSeqTracksOtp = {List<SeqTrack> seqTracksToAlign ->
    boolean allFinished = true

    if(!seqTracksToAlign) {
        return []
    }

    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> seqTracksByAlignmentState = seqTracksToAlign.groupBy {it.alignmentState}

    allFinished &= seqTracksByAlignmentState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksFinishedConveyBwaAlignmentWorkflow =
            handleStateMap(seqTracksByAlignmentState, "ConveyBwaAlignmentWorkflow",
                    { "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  ilse: ${it.ilseId}  id: ${it.id}" }, {
                AlignmentPass.findAllBySeqTrack(it).find { it2 -> it2.isLatestPass()}
            })

    if (!seqTracksFinishedConveyBwaAlignmentWorkflow) {
        output << "\nnot all workflows are finished"
        return []
    }

    List<ProcessedBamFile> processedBamFiles = ProcessedBamFile.createCriteria().list {
        alignmentPass { 'in'('seqTrack', seqTracksFinishedConveyBwaAlignmentWorkflow) }
        eq('withdrawn', false)
    }

    Collection<ProcessedBamFile> mostRecentProcessedBamFiles = processedBamFiles.findAll { it.isMostRecentBamFile()}

    /*
    //qa
    Map<AbstractBamFile.QaProcessingStatus, Collection<ProcessedBamFile>> mostRecentProcessedBamFileByQualityAssessmentStatus =
        mostRecentProcessedBamFiles.groupBy ([{it.qualityAssessmentStatus}])

    handleStateMap(mostRecentProcessedBamFileByQualityAssessmentStatus, "qa", {
        def seqTrack = it.alignmentPass.seqTrack
        "${seqTrack.sample}  ${seqTrack.seqType}  ${seqTrack.run}  ${seqTrack.laneId}  ${seqTrack.project}  id: ${it.id}"
        }, {
        QualityAssessmentPass.findAllByProcessedBamFile(it).find { it2 -> it2.isLatestPass()}
    })
    //*/

    //create merging set
    Map<AbstractBamFile.State, Collection<ProcessedBamFile>> processedBamFilesByStatus = mostRecentProcessedBamFiles.groupBy ([{it.status}])

    allFinished &= processedBamFilesByStatus.keySet() == [AbstractBamFile.State.PROCESSED] as Set
    Collection<ProcessedBamFile> processedBamFilesFinishedCreateMergingSetWorkflow = handleStateMap(processedBamFilesByStatus, "createMergingSetWorkflow", {
        def seqTrack = it.alignmentPass.seqTrack
        "${seqTrack.sample}  ${seqTrack.seqType}  ${seqTrack.run}  ${seqTrack.laneId}  ${seqTrack.project}  ilse: ${seqTrack.ilseId}  id: ${it.id}"
    })

    if (!processedBamFilesFinishedCreateMergingSetWorkflow){
        output << "\nnot all workflows are finished"
        return []
    }

    //create merging
    List<MergingSet> mergingSets = processedBamFilesFinishedCreateMergingSetWorkflow.collect {
        MergingSetAssignment.findAllByBamFile(it)*.mergingSet.sort {it.identifier}.last()
    }.findAll{
        !blackList_MergingSetId.contains(it.id)
    }.unique()

    Map<MergingSet.State, Collection<MergingSet>> mergingSetsByStatus = mergingSets.groupBy ([{it.status}])

    allFinished &= mergingSetsByStatus.keySet() == [MergingSet.State.PROCESSED] as Set
    Collection<MergingSet> mergingSetsFinishedMergingWorkflow = handleStateMap(mergingSetsByStatus, "MergingWorkflow", { "${it.mergingWorkPackage.sample}  ${it.mergingWorkPackage.seqType}  ${it.mergingWorkPackage.sample.project}  ${it.isLatestSet() ? 'latest': 'outdated'}  id: ${it.id}" }, {
        MergingPass.findAllByMergingSet(it).find { it2 -> it2.isLatestPass()}
    })

    if (!mergingSetsFinishedMergingWorkflow) {
        output << "\nnot all workflows are finished"
        return []
    }

    //create merged qa
    List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
        mergingPass { 'in' ("mergingSet", mergingSetsFinishedMergingWorkflow) }
        eq('withdrawn', false)
    }

    processedMergedBamFiles = processedMergedBamFiles.findAll { ProcessedMergedBamFile processedMergedBamFile ->
        int maxIdentifier = MergingPass.createCriteria().get {
            eq("mergingSet", processedMergedBamFile.mergingPass.mergingSet)
            projections{ max("identifier") }
        }
        return processedMergedBamFile.mergingPass.identifier == maxIdentifier
    }

    Map<AbstractBamFile.QaProcessingStatus, Collection<ProcessedMergedBamFile>> processedMergedBamFilesByQualityAssessmentStatus =
            processedMergedBamFiles.groupBy ([{it.qualityAssessmentStatus}])

    allFinished &= processedMergedBamFilesByQualityAssessmentStatus.keySet() == [AbstractBamFile.QaProcessingStatus.FINISHED] as Set
    Collection<ProcessedBamFile> processedMergedBamFilesFinishedQualityAssessmentMergedWorkflow = handleStateMap(processedMergedBamFilesByQualityAssessmentStatus, "QualityAssessmentMergedWorkflow", {
        def mergingWorkPackage = it.mergingPass.mergingSet.mergingWorkPackage
        "${mergingWorkPackage.sample}  ${mergingWorkPackage.seqType}  ${mergingWorkPackage.sample.project}  id: ${it.id}"
    }, {
        QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(it).find { it2 -> it2.isLatestPass()}
    })

    if (!processedMergedBamFilesFinishedQualityAssessmentMergedWorkflow) {
        output << "\nnot all workflows are finished"
        return []
    }

    //transfer
    Map<AbstractMergedBamFile.FileOperationStatus, Collection<ProcessedMergedBamFile>> processedMergedBamFilesByFileOperationStatus =
            processedMergedBamFilesFinishedQualityAssessmentMergedWorkflow.groupBy ([{it.fileOperationStatus}])

    allFinished &= processedMergedBamFilesByFileOperationStatus.keySet() == [AbstractMergedBamFile.FileOperationStatus.PROCESSED] as Set
    Collection<ProcessedMergedBamFile> processedMergedBamFilesFinishedTransferMergedBamFileWorkflow = handleStateMap(processedMergedBamFilesByFileOperationStatus, "transferMergedBamFileWorkflow", {
        def mergingWorkPackage = it.mergingPass.mergingSet.mergingWorkPackage
        "${mergingWorkPackage.sample}  ${mergingWorkPackage.seqType}  ${mergingWorkPackage.sample.project}  id: ${it.id}"
    })

    if (!processedMergedBamFilesFinishedTransferMergedBamFileWorkflow) {
        output << "\nnot all workflows are finished"
        return []
    }

    return processedMergedBamFilesFinishedTransferMergedBamFileWorkflow
}

def showSeqTracksRoddy = {List<SeqTrack> seqTracksToAlign ->
    boolean allFinished = true

    if(!seqTracksToAlign) {
        return []
    }

    List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.createCriteria().list {
        and {
          sample {
            'in' ('id', seqTracksToAlign*.sample*.id)
          }
          seqType{
            'in' ('id', seqTracksToAlign*.seqType*.id)
          }
        }
    }

    mergingWorkPackages = mergingWorkPackages.findAll { it.findMergeableSeqTracks()*.id.intersect(seqTracksToAlign*.id) }

    Map<Boolean, Collection<MergingWorkPackage>> mergingWorkPackagesByNeedsProcessing =
            mergingWorkPackages.groupBy {it.needsProcessing}

    allFinished &= mergingWorkPackagesByNeedsProcessing.keySet() == [false] as Set
    Collection<MergingWorkPackage> mergingWorkPackagesInProcessing = handleStateMapNeedsProcessing(mergingWorkPackagesByNeedsProcessing, {
        "${it}"
    })

    if (!mergingWorkPackagesInProcessing) {
        output << "\nnot all workflows are finished"
        return []
    }

    List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageInList(mergingWorkPackagesInProcessing).findAll { it.isMostRecentBamFile() && !it.withdrawn }

    Map<AbstractMergedBamFile.FileOperationStatus, Collection<RoddyBamFile>> roddyBamFileByFileOperationStatus =
            roddyBamFiles.groupBy {it.fileOperationStatus}

    allFinished &= roddyBamFileByFileOperationStatus.keySet() == [AbstractMergedBamFile.FileOperationStatus.PROCESSED] as Set
    Collection<RoddyBamFile> roddyBamFilesFinishedPanCanWorkflow = handleStateMapRoddy(roddyBamFileByFileOperationStatus, "PanCanWorkflow", {
        "${it}"
    })

    if (!roddyBamFilesFinishedPanCanWorkflow) {
        output << "\nnot all workflows are finished"
        return []
    }

    return roddyBamFilesFinishedPanCanWorkflow
}

def showSeqTracks = {Collection<SeqTrack> seqTracks ->
    boolean allFinished = true

    List<DataFile> datafiles = DataFile.findAllBySeqTrackInList(seqTracks)
    if (datafiles) {
        Map <Collection<RunSegment.FilesStatus>, Collection<DataFile>> dataFilesByInProgress = datafiles.groupBy{
            RunSegment.FilesStatus.FILES_CORRECT != it.runSegment.filesStatus
        }
        if (dataFilesByInProgress[true]) {
            output << "\nDataInstallationWorkflow: "
            output << "${INDENT}${dataFilesByInProgress[true].size()} Lanes are still in data installation workflow. They will be ignored"
            showRunning('DataInstallationWorkflow', dataFilesByInProgress[true]*.run.unique(), {it}, {it})
            seqTracks = dataFilesByInProgress[false]*.seqTrack?.unique()
            allFinished=false
            if (!seqTracks){
                output << "No lanes left"
                return
            }
        }
        datafiles = null
    }

    //remove withdrawn
    List<SeqTrack> seqTracksNotWithdrawn = findNotWithdrawn(seqTracks)
    if (!seqTracksNotWithdrawn) {
        output << "No not withdrawn seq tracks left, stop"
        return
    }

    //fastqc
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> seqTracksNotWithdrawnByFastqcState =
            seqTracksNotWithdrawn.groupBy {it.fastqcState}

    allFinished &= seqTracksNotWithdrawnByFastqcState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksNotWithdrawnFinishedFastqcWorkflow = handleStateMap(seqTracksNotWithdrawnByFastqcState, "FastqcWorkflow", {
        "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  ilse: ${it.ilseId} id: ${it.id}"
    })

    if (!seqTracksNotWithdrawnFinishedFastqcWorkflow) {
        output << "\nnot all workflows are finished"
        return
    }

    //filter for alignable seq tracks
    List<SeqTrack> seqTracksToAlign = findAlignable(seqTracksNotWithdrawnFinishedFastqcWorkflow)
    if (!seqTracksToAlign) {
        output << "No alignable seq tracks left, stop"
        return
    }

    output << "\nThere are ${seqTracksToAlign.size()} seqtracks to align"

    Map<String, Collection<SeqTrack>> seqTracksByAlignmentDeciderBeanName = seqTracksToAlign.groupBy { it.project.alignmentDeciderBeanName }

    seqTracksByAlignmentDeciderBeanName.keySet().sort().each {
        switch(it) {
            case 'defaultOtpAlignmentDecider':
                output << "${seqTracksByAlignmentDeciderBeanName['defaultOtpAlignmentDecider'].size()} SeqTracks use the default OTP alignment"
                break
            case 'panCanAlignmentDecider':
                output << "${seqTracksByAlignmentDeciderBeanName['panCanAlignmentDecider'].size()} SeqTracks use the Pan-Cancer alignment"
                break
            case 'noAlignmentDecider':
                output << "${seqTracksByAlignmentDeciderBeanName['noAlignmentDecider'].size()} SeqTracks don't align"
                break
            default:
                throw new RuntimeException("Unknown alignment decider: ${it}. Please inform a maintainer")
        }
    }

    Collection<SeqTrack> seqTracksFinishedAlignment = []

    //alignment OTP
    seqTracksFinishedAlignment += showSeqTracksOtp(seqTracksByAlignmentDeciderBeanName['defaultOtpAlignmentDecider'])

    //alignment Roddy
    seqTracksFinishedAlignment += showSeqTracksRoddy(seqTracksByAlignmentDeciderBeanName['panCanAlignmentDecider'])

    if (!seqTracksFinishedAlignment) {
        return
    }

    //finished aligned
    output << "\nFinshed aligned samples (${seqTracksFinishedAlignment.size()}): "
    seqTracksFinishedAlignment.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }

    if (!allFinished) {
        output << "\nnot all workflows are finished till alignment"
    }



    //snv
    Collection<SeqTrack> seqTracksFinishedSnv = handleStateMapSnv(seqTracksFinishedAlignment)

    if (!seqTracksFinishedSnv) {
        output << "\nnot all workflows are finished"
        return
    }

    //end
    output << "\nFinshed snv sample pairs (${seqTracksFinishedSnv.size()}): "
    seqTracksFinishedSnv.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }

    if (!allFinished) {
        output << "\nnot all workflows are finished inclusive snv"
    }
}


//======================================================

nameStringToList(runString).each { String runName ->
    output << "\n\n\n==============================\nrunNames = ${runName}\n==============================\n"

    Run run = Run.findByName(runName)
    if (!run) {
        output << "No run with name ${runName} could be found"
        return
    }

    List<SeqTrack> seqTracks = SeqTrack.findAllByRun(run)
    if (!seqTracks) {
        output << "No Lanes for run ${runName} could be found !!!!"
        return
    }
    showSeqTracks(seqTracks)
}


nameStringToList(individualString).each { String individualName ->
    output << "\n\n\n==============================\nindividual name = ${individualName}\n==============================\n"

    Individual individual = Individual.findByPid(individualName)
    if (!individual) {
        output << "No individual with pid ${individualName} could be found"
        return
    }

    List<SeqTrack> seqTracks = SeqTrack.createCriteria().list{
        sample { eq('individual', individual) }
    }
    if (!seqTracks) {
        output << "No Lanes for individual ${individualName} could be found !!!!"
        return
    }
    showSeqTracks(seqTracks)
}


nameStringToList(ilseIdString).each { String ilseId ->
    output << "\n\n\n==============================\nilseId = ${ilseId}\n==============================\n"

    List<SeqTrack> seqTracks = SeqTrack.createCriteria().list{
        eq('ilseId', ilseId)
    }

    if (!seqTracks) {
        output << "No Lanes for ilseId ${ilseId} could be found !!!!"
        return
    }
    showSeqTracks(seqTracks)
}

nameStringToList(projectString).each { String projectName ->
    output << "\n\n\n==============================\nproject name = ${projectName}\n==============================\n"

    Project project = Project.findByName(projectName)
    if (!project) {
        output << "No project with name ${projectName} could be found"
        return
    }

    List<SeqTrack> seqTracks = SeqTrack.createCriteria().list{
        sample {
            individual { eq("project", project) }
        }
    }
    if (!seqTracks) {
        output << "No Lanes for project ${projectName} could be found !!!!"
        return
    }
    showSeqTracks(seqTracks)
}


if (allProcessed) {
    output << "\n\n\n==============================\nseqtracks in processing (as defined in header comment)\n==============================\n"

    long firstIdToCheck = 20077258 // 1.1.2015


    def  seqTracks = SeqTrack.executeQuery(
            """
    select
        seqTrack
    from
        SeqTrack seqTrack
        join seqTrack.seqType seqType
        join seqTrack.sample.individual.project project
    where
        seqTrack.id not in (
            select
                seqTrack.id
            from
                DataFile datafile
                join datafile.seqTrack seqTrack
            where
                datafile.fileWithdrawn = true
        ) and (
            (
                seqTrack.fastqcState != '${SeqTrack.DataProcessingState.FINISHED}'
                and seqTrack.id >= ${firstIdToCheck}
            ) or (
                seqTrack.id in (
                    select
                        alignmentPass.seqTrack.id
                    from
                        AlignmentPass alignmentPass
                    where
                        alignmentPass.alignmentState != '${AlignmentPass.AlignmentState.UNKNOWN}'
                ) and project.alignmentDeciderBeanName = 'defaultOtpAlignmentDecider'
                and not seqTrack.id in (
                    select
                        seqTrack.id
                    from
                        DataFile datafile
                        join datafile.seqTrack seqTrack
                    where
                        datafile.runSegment.align = false
                ) and not seqTrack.id in (
                    select
                        seqTrack.id
                    from
                        MergingSetAssignment mergingSetAssignment
                        join mergingSetAssignment.bamFile bamFile
                        join bamFile.alignmentPass alignmentPass
                        join alignmentPass.seqTrack seqTrack
                    where
                        mergingSetAssignment.mergingSet.id in (
                            select
                                    mergingSet.id
                            from
                                    ProcessedMergedBamFile processedMergedBamFile
                                    join processedMergedBamFile.mergingPass mergingPass
                                    join mergingPass.mergingSet mergingSet
                                    join mergingSet.mergingWorkPackage mergingWorkPackage
                            where
                                    processedMergedBamFile.withdrawn = false
                                    and processedMergedBamFile.fileOperationStatus = '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
                                    and processedMergedBamFile.qualityAssessmentStatus = '${AbstractBamFile.QaProcessingStatus.FINISHED}'
                                    and mergingPass.identifier = (select max(identifier) from MergingPass mergingPass where mergingPass.mergingSet = mergingSet)
                        )
                )
            ) or (
                seqTrack.id in (
                    select
                        seqTrack.id
                    from
                        RoddyBamFile roddyBamFile join roddyBamFile.seqTracks seqTrack
                    where
                        roddyBamFile.fileOperationStatus <> '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
                        and roddyBamFile.withdrawn = false
                    )
            )
        )
    """)

    //collect waiting SamplePairs
    SamplePair.findAllByProcessingStatus(SamplePair.ProcessingStatus.NEEDS_PROCESSING).each {SamplePair samplePair ->
        //see also: OTP-1497 and/or OTP-1673
        if (SnvConfig.findByProjectAndSeqTypeAndObsoleteDateIsNull(samplePair.project, samplePair.seqType)) {
            [samplePair.mergingWorkPackage1, samplePair.mergingWorkPackage2].each { MergingWorkPackage mergingWorkPackage ->
                seqTracks.addAll(mergingWorkPackage.findMergeableSeqTracks())
            }
        }
    }

    //collect running SnvCallingInstances
    SnvCallingInstance.createCriteria().list{
        eq('processingState', SnvProcessingStates.IN_PROGRESS)
        sampleType1BamFile {
            eq('withdrawn', false)
        }
        sampleType2BamFile {
            eq('withdrawn', false)
        }
    }.each { SnvCallingInstance snvCallingInstance ->
        [snvCallingInstance.sampleType1BamFile, snvCallingInstance.sampleType2BamFile].each {AbstractMergedBamFile abstractMergedBamFile ->
            seqTracks.addAll(abstractMergedBamFile.containedSeqTracks)
        }
    }



    MergingWorkPackage.executeQuery("""
        select
            mergingWorkPackage
        from
            MergingWorkPackage mergingWorkPackage
            join mergingWorkPackage.sample.individual.project project
        where
            mergingWorkPackage.needsProcessing = true
    """).each {
        def mergeableSeqTracks = it.findMergeableSeqTracks()
        if(mergeableSeqTracks) {
            seqTracks += mergeableSeqTracks - RoddyAlignmentStartJob.findUsableBaseBamFile(it)?.containedSeqTracks
        }
    }

    showSeqTracks(seqTracks.unique())

    output << "\n\nProjects in processing:"
    output << INDENT + seqTracks*.sample*.individual*.project*.name.unique().sort().join("\n${INDENT}")
}

println output.join("\n").replace("<br>", " ")
println "\n\n"
//println outputSeqTrack.join("\n")

println ""
