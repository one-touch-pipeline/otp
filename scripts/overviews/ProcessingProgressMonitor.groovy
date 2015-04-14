/*
The following code allows to show the processing state for

* runs
* individuals (pid)
* ilse id
* project (can take some time, depending of project)
* all lanes in processing:
** all withdrawn lanes are ignored
** lanes where fastqc not finished and data load after 1.1.2015 included
** alignment for seqtrack was triggered and not finished (transfere workflow finished), ignoring:
*** lanes of run segment where flag align is set to false:
*** seqtracks of the following projects:
**** PROJECT_NAME
**** PROJECT_NAME
**** PROJECT_NAME
**** PROJECT_NAME
**** PROJECT_NAME
**** MMML
** sample pairs (snv) waiting for snv calling
** snv calling instance which are in processing

entries are trimmed (spaces before after are removed)
Names prefixed with # are ignored (handled as comment)
Empty lines are ignored


The script has three input areas, one for run names, one for patient names, and one for project name.
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates

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
        hasError = true
    } else if (processes.size() > 1) {
        output << "${INDENT}Attention: There were ${processes.size()} processes created for the object ${valueToShow(object)} (${object.id}). That can make problems."
    }
    processes.each {
        ProcessingStep ps = ProcessingStep.findByProcessAndNextIsNull(it)
        def update = ps.latestProcessingStepUpdate
        def state = update?.state
        if (state == ExecutionState.FAILURE || update == null) {
            hasError = true
            output << "${INDENT}An error occur for the object: ${valueToShow(object)}  (${object.id})"
            output << "${INDENT}${INDENT}object class/id: ${object.class}/${object.id}"
            output << "${INDENT}${INDENT}the OTP link: https://otp.dkfz.de/otp/processes/process/${it.id}"
            output << "${INDENT}${INDENT}the error: ${ps.latestProcessingStepUpdate?.error?.errorMessage}"
            if (ps.process.comment) {
                output << "${INDENT}${INDENT}the comment (${ps.process.commentDate.format("yyyy-MM-dd")}): ${ps.process.comment.replaceAll('\n', "${INDENT}${INDENT}${INDENT}\n")}"
            }
            if (update == null) {
                output << "${INDENT}${INDENT}no update available: Please inform a maintainer\n"
            }
            processWithError << object.id
        }
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

def showNotTriggered = {String workflow, List objects, Closure valueToShow ->
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

def showWaiting = {String workflow, List objects, Closure valueToShow ->
    output << prefix("""\
waiting (${objects.size}):
${prefix(objectsToString(objects, valueToShow), INDENT)}
""", INDENT)
}

def showRunning = {String workflow, List objects, Closure valueToShow, Closure objectToCheck ->
    output << prefix("""\
running (${objects.size}):
${prefix(objectsToString(objects, valueToShow), INDENT)}
""", INDENT)
    checkProcessesForObjects(workflow, objects, valueToShow, objectToCheck)
}

def showFinished = {String workflow, List objects, Closure valueToShow ->
    if (!showFinishedEntries) {
        return
    }
    output << prefix("""\
finished (${objects.size}):
${prefix(objectsToString(objects, valueToShow), INDENT)}
""", INDENT)
}

def handleStateMap = {Map map, String workflow, Closure valueToShow, Closure objectToCheck = {it}->
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
                showWaiting(workflow, values, valueToShow)
                break
            case 2:
                showRunning(workflow, values, valueToShow, objectToCheck)
                break
            case 3:
                showFinished(workflow, values, valueToShow)
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

def handleStateMapSnv = { List next ->
    output << "\nsnvWorkflow"

    Map stateMap = ['running': [], 'finished': [], 'waiting': [], 'notTriggered': []]

    List samplePairs = []

    next.each { ProcessedMergedBamFile pMBF ->
        samplePairs += SamplePair.createCriteria().list {
            eq('individual', pMBF.individual)
            eq('seqType', pMBF.seqType)
            or {
                eq('sampleType1', pMBF.sampleType)
                eq('sampleType2', pMBF.sampleType)
            }
        }
    }

    if(samplePairs) {
        samplePairs.unique().each { SamplePair samplePair ->
            if (samplePair.processingStatus == SamplePair.ProcessingStatus.NEEDS_PROCESSING) {
                stateMap.waiting << samplePair
            } else {
                // get latest SnvCallingInstance for this SamplePair
                SnvCallingInstance snvCallingInstance = SnvCallingInstance.createCriteria().get {
                    eq('samplePair', samplePair)
                    order('lastUpdated', 'desc')
                    maxResults(1)
                }
                if (snvCallingInstance && snvCallingInstance.processingState == SnvProcessingStates.IN_PROGRESS) {
                    stateMap.running << samplePair
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

    if(stateMap.notTriggered) {
        showNotTriggered("snvWorkflow", stateMap.notTriggered, {it})
    }

    if(stateMap.waiting) {
        showWaiting("snvWorkflow", stateMap.waiting, {it})
    }

    if(stateMap.running) {
        showRunning("snvWorkflow", stateMap.running, {it}, {it})
    }

    if(stateMap.finished) {
        showFinished("snvWorkflow", stateMap.finished, {it})
    }

    return stateMap.finished
}

def showSeqTracks = {Collection<SeqTrack> seqTracks ->
    boolean allFinished = true

    List<DataFile> datafiles = DataFile.findAllBySeqTrackInList(seqTracks)
    if (datafiles) {
        Map processingMap = datafiles.groupBy{
            RunSegment.PROCESSING_FILE_STATUSES.contains(it.runSegment.filesStatus)
        }
        if (processingMap[true]) {
            output << "\nDataInstallationWorkflow: "
            output << "${INDENT}${processingMap[true].size()} Lanes are still in data installation workflow. They will be ignored"
            showRunning('DataInstallationWorkflow', processingMap[true]*.run.unique(), {it}, {it})
            seqTracks = processingMap[false]*.seqTrack?.unique()
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
    def map = seqTracksNotWithdrawn.groupBy {it.fastqcState}
    allFinished &= map.size() == 1
    def next = handleStateMap(map, "FastqcWorkflow", {
        "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  id: ${it.id}"
    }
    )

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }

    //filter for alignable seq tracks
    List<SeqTrack> seqTracksToAlign = findAlignable(next)
    if (!seqTracksToAlign) {
        output << "No alignable seq tracks left, stop"
        return
    }

    output << "\nThere are ${seqTracksToAlign.size()} seqtracks to align"

    //alignment
    map = seqTracksToAlign.groupBy {it.alignmentState}
    allFinished &= map.size() == 1
    next = handleStateMap(map, "ConveyBwaAlignmentWorkflow", { "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  id: ${it.id}" }, {
        AlignmentPass.findAllBySeqTrack(it).find { it2 -> it2.isLatestPass()}
    })

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }


    List<ProcessedBamFile> processedBamFiles = ProcessedBamFile.createCriteria().list {
        alignmentPass { 'in'('seqTrack', next) }
        eq('withdrawn', false)
    }
    processedBamFiles = processedBamFiles.findAll { it.isMostRecentBamFile()}

    /*
    //qa
    map = processedBamFiles.groupBy ([{it.qualityAssessmentStatus}])
    handleStateMap(map, "qa", {
        def seqTrack = it.alignmentPass.seqTrack
        "${seqTrack.sample}  ${seqTrack.seqType}  ${seqTrack.run}  ${seqTrack.laneId}  ${seqTrack.project}  id: ${it.id}"
        }, {
        QualityAssessmentPass.findAllByProcessedBamFile(it).find { it2 -> it2.isLatestPass()}
    })
    //*/

    //create merging set
    map = processedBamFiles.groupBy ([{it.status}])
    allFinished &= map.size() == 1
    next = handleStateMap(map, "createMergingSetWorkflow", {
        def seqTrack = it.alignmentPass.seqTrack
        "${seqTrack.sample}  ${seqTrack.seqType}  ${seqTrack.run}  ${seqTrack.laneId}  ${seqTrack.project}  id: ${it.id}"
    })

    if (!next){
        output << "\nnot all workflows are finished"
        return
    }

    //create merging
    List<MergingSet> mergingSets = next.collect {
        MergingSetAssignment.findAllByBamFile(it)*.mergingSet.sort {it.identifier}.last()
    }.findAll{
        !blackList_MergingSetId.contains(it.id)
    }.unique()



    map = mergingSets.groupBy ([{it.status}])
    allFinished &= map.size() == 1
    next = handleStateMap(map, "MergingWorkflow", { "${it.mergingWorkPackage.sample}  ${it.mergingWorkPackage.seqType}  ${it.mergingWorkPackage.sample.project}  ${it.isLatestSet() ? 'latest': 'outdated'}  id: ${it.id}" }, {
        MergingPass.findAllByMergingSet(it).find { it2 -> it2.isLatestPass()}
    })

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }

    //create merged qa
    List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
        mergingPass { 'in' ("mergingSet", next) }
        eq('withdrawn', false)
    }

    processedMergedBamFiles = processedMergedBamFiles.findAll { ProcessedMergedBamFile processedMergedBamFile ->
        int maxIdentifier = MergingPass.createCriteria().get {
            eq("mergingSet", processedMergedBamFile.mergingPass.mergingSet)
            projections{ max("identifier") }
        }
        return processedMergedBamFile.mergingPass.identifier == maxIdentifier
    }
    map = processedMergedBamFiles.groupBy ([{it.qualityAssessmentStatus}])
    allFinished &= map.size() == 1
    next = handleStateMap(map, "QualityAssessmentMergedWorkflow", {
        def mergingWorkPackage = it.mergingPass.mergingSet.mergingWorkPackage
        "${mergingWorkPackage.sample}  ${mergingWorkPackage.seqType}  ${mergingWorkPackage.sample.project}  id: ${it.id}"
    }, {
        QualityAssessmentMergedPass.findAllByProcessedMergedBamFile(it).find { it2 -> it2.isLatestPass()}
    })

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }


    //transfer
    map = next.groupBy ([{it.fileOperationStatus}])
    allFinished &= map.size() == 1
    next = handleStateMap(map, "transferMergedBamFileWorkflow", {
        def mergingWorkPackage = it.mergingPass.mergingSet.mergingWorkPackage
        "${mergingWorkPackage.sample}  ${mergingWorkPackage.seqType}  ${mergingWorkPackage.sample.project}  id: ${it.id}"
    })

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }

    //snv
    next = handleStateMapSnv(next)

    if (!next) {
        output << "\nnot all workflows are finished"
        return
    }

    //end
    output << "\nFinshed samples: "
    next.collect {
        def mergingWorkPackage = it.mergingPass.mergingSet.mergingWorkPackage
        "        ${mergingWorkPackage.sample}  ${mergingWorkPackage.seqType}  ${mergingWorkPackage.sample.project}"
    }.sort { it }.each { output << it }

    if (!allFinished) {
        output << "\nnot all workflows are finished"
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
    output << "\n\n\n==============================\nprocessed and not finished seqtracks\nignore project MMML, PROJECT_NAME, PROJECT_NAME, PROJECT_NAME, and PROJECT_NAME\n==============================\n"

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
                ) and not project.name in (
                    'PROJECT_NAME',
                    'PROJECT_NAME',
                    'PROJECT_NAME',
                    'PROJECT_NAME',
                    'MMML',
                    'PROJECT_NAME'
                ) and not seqTrack.id in (
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
                                    and processedMergedBamFile.fileOperationStatus = '${AbstractBamFile.FileOperationStatus.PROCESSED}'
                                    and processedMergedBamFile.qualityAssessmentStatus = '${AbstractBamFile.QaProcessingStatus.FINISHED}'
                                    and mergingPass.identifier = (select max(identifier) from MergingPass mergingPass where mergingPass.mergingSet = mergingSet)
                        )
                )
            ) or (
                seqTrack.id in (
                    select
                        seqTrack.id
                    from
                        SamplePair samplePair,
                        SeqTrack seqTrack
                    where
                        samplePair.processingStatus = '${SamplePair.ProcessingStatus.NEEDS_PROCESSING}'
                        and samplePair.individual = seqTrack.sample.individual
                        and samplePair.seqType = seqTrack.seqType
                        and (
                            samplePair.sampleType1 = seqTrack.sample.sampleType
                            or samplePair.sampleType2 = seqTrack.sample.sampleType
                        )
                )
            ) or (
                seqTrack.id in (
                    select
                        alignmentPass.seqTrack.id
                    from
                        SnvCallingInstance snvCallingInstance,
                        AlignmentPass alignmentPass
                    where
                        snvCallingInstance.processingState = '${SnvProcessingStates.IN_PROGRESS}'
                        and snvCallingInstance.sampleType1BamFile.withdrawn = false
                        and snvCallingInstance.sampleType2BamFile.withdrawn = false
                        and (
                            alignmentPass.workPackage = snvCallingInstance.sampleType1BamFile.mergingPass.mergingSet.mergingWorkPackage
                            or alignmentPass.workPackage = snvCallingInstance.sampleType2BamFile.mergingPass.mergingSet.mergingWorkPackage
                        )
                )
            )
        )
    """)
    showSeqTracks(seqTracks)

    output << "\n\nProjects in processing:"
    output << INDENT + seqTracks*.sample*.individual*.project*.name.unique().sort().join("\n${INDENT}")
}

println output.join("\n").replace("<br>", " ")
println "\n\n"
//println outputSeqTrack.join("\n")

println ""