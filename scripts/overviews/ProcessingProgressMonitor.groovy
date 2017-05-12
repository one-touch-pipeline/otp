/*
The following code allows to show the processing state for

* runs
* individuals (pid)
* ilse id
* project (can take some time, depending of project)
* all lanes in processing:
** all withdrawn lanes are ignored
** waiting and running data installation workflow
** waiting and running fastqc workflows for data loaded after 1.1.2015
** waiting/running alignments
*** seqtracks which belong to run segments where flag 'align' is set to false are ignored
*** running OTP alignments (WGS, WES)
*** running roddy alignments (WGS, WES, WGBS, RNA) (if not withdrawn)
** running variant calling (snv, indel, ...) (if not withdrawn)
** waiting variant calling (snv, indel, ...) if
*** a config is available
*** the last bam file is not withdrawn
*** the last bam file is in processing or has reached the threshold


Entries are trimmed (spaces before after are removed)
Names prefixed with # are ignored (handled as comment)
Empty lines are ignored


The script has four input areas, one for run names, one for patient names, one for ilse ids, and one for project names.
 */


import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.monitor.*
import de.dkfz.tbi.otp.monitor.alignment.*
import de.dkfz.tbi.otp.ngsdata.*

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

MonitorOutputCollector output = new MonitorOutputCollector(showFinishedEntries)

SeqType exomePaired = SeqType.exomePairedSeqType
List<SeqType> allignableSeqtypes = SeqType.allAlignableSeqTypes

MetaDataKey libPrepKitKey = MetaDataKey.findByName(MetaDataColumn.LIB_PREP_KIT.name())
MetaDataKey enrichmentKitKey = MetaDataKey.findByName("ENRICHMENT_KIT")
MetaDataKey commentKey = MetaDataKey.findByName(MetaDataColumn.COMMENT.name())

List blackList_MergingSetId = ([12174511] as long[]) as List

String INDENT = MonitorOutputCollector.INDENT


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
        output << output.prefix(output.objectsToStrings(map[true], {
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
        }).join('\n'), "${INDENT}${INDENT}")
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
        output << output.prefix(output.objectsToStrings(map[true]).join('\n'), "${INDENT}${INDENT}")
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
    Map<Boolean, List<SeqTrack>> groupAfterAlignable = seqTracks.groupBy({allignableSeqtypes.contains(it.seqType) })

    output << "\n\n"

    if (groupAfterAlignable[false]) {
        output << "count of lanes OTP can not align: ${groupAfterAlignable[false].size()}"
    }

    if (groupAfterAlignable[true]) {
        output << "count of lanes OTP can align: ${groupAfterAlignable[true].size()}"
    } else {
        output << "no lanes left of SeqTypes which OTP could align"
        return []
    }

    List<SeqTrack> seqTracksWithConfiguredReferenceGenome = seqTracksWithReferenceGenome(groupAfterAlignable[true])

    Map<Boolean, List<SeqTrack>> groupAfterExome = seqTracksWithConfiguredReferenceGenome.groupBy({exomePaired == it.seqType })

    List<SeqTrack> seqTracksToReturn = groupAfterExome[false] ?: []
    seqTracksToReturn.addAll(exomeSeqTracksWithBedFile(exomeSeqTracksWithEnrichmentKit(groupAfterExome[true] ?: [])))

    return seqTracksToReturn
}


def handleStateMap = {Map map, String workflow, Closure valueToShow, Closure objectToCheck = {it} ->
    output << "\n${workflow}: "
    def ret
    def keys = map.keySet().sort{it}

    keys.each { key->
        def values = map[key]
        switch (key.ordinal()) {
            case 0:
                output.showNotTriggered(workflow, values, valueToShow)
                break
            case 1:
                output.showWaiting(values, valueToShow)
                break
            case 2:
                output.showRunning(workflow, values, valueToShow, objectToCheck)
                break
            case 3:
                output.showFinished(values, valueToShow)
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

def showSeqTracks = {Collection<SeqTrack> seqTracks ->
    boolean allFinished = true

    //remove withdrawn
    List<SeqTrack> seqTracksNotWithdrawn = findNotWithdrawn(seqTracks)
    if (!seqTracksNotWithdrawn) {
        output << "No not withdrawn seq tracks left, stop"
        return
    }


    //data installation workflow
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> dataInstallationState =
            seqTracksNotWithdrawn.groupBy {it.dataInstallationState}

    allFinished &= dataInstallationState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksFinishedDataInstallationWorkflow = handleStateMap(dataInstallationState, "DataInstallationWorkflow", {
        "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  ilse: ${it.ilseId} id: ${it.id}"
    })

    if (!seqTracksFinishedDataInstallationWorkflow) {
        output << "\nnot all workflows are finished"
        return
    }

    //fastqc
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> seqTracksNotWithdrawnByFastqcState =
            seqTracksFinishedDataInstallationWorkflow.groupBy {it.fastqcState}

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

    output << "\n"

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
                output << "${seqTracksByAlignmentDeciderBeanName['noAlignmentDecider'].size()} SeqTracks are not configured to be aligned by OTP"
                break
            default:
                throw new RuntimeException("Unknown alignment decider: ${it}. Please inform maintainer (Stefan or Jan)")
        }
    }

    Collection<SeqTrack> seqTracksFinishedAlignment = []

    //alignment OTP
    seqTracksFinishedAlignment += showSeqTracksOtp(seqTracksByAlignmentDeciderBeanName['defaultOtpAlignmentDecider'])

    //alignment Roddy
    seqTracksFinishedAlignment += new AllRoddyAlignmentsChecker().handle(seqTracksByAlignmentDeciderBeanName['panCanAlignmentDecider'], output)

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

    //variant calling
    List<SamplePair> finishedSamplePair = new VariantCallingPipelinesChecker().handle(seqTracksFinishedAlignment, output)

    //end
    output << "\nFinshed variant calling sample pairs (${finishedSamplePair.size()}): "

    finishedSamplePair.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }

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

    List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
        ilseSubmission {
            eq('ilseNumber', Integer.parseInt(ilseId))
        }
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
                seqTrack.dataInstallationState not in ('${SeqTrack.DataProcessingState.FINISHED}', '${SeqTrack.DataProcessingState.UNKNOWN}')
            ) or (
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

    def needsProcessing = { String property, Pipeline.Type type ->
        return """
            (
                samplePair.${property} = '${SamplePair.ProcessingStatus.NEEDS_PROCESSING}'
                and exists (
                    select
                        config
                    from
                        ConfigPerProject config
                    where
                        config.project = samplePair.mergingWorkPackage1.sample.individual.project
                        and config.seqType = samplePair.mergingWorkPackage1.seqType
                        and config.pipeline.type = '${type}'
                        and config.obsoleteDate is null
                )
            )"""
    }

    def bamFileInProgressingOrThresouldReached = { String number ->
        return """
            and exists (
                from
                    AbstractMergedBamFile bamFile
                    join bamFile.workPackage mwp
                where
                    mwp = samplePair.mergingWorkPackage${number}
                    and bamFile.withdrawn = false
                    and bamFile.id = (select max( maxBamFile.id) from AbstractMergedBamFile maxBamFile where maxBamFile.workPackage = bamFile.workPackage)
                    and (
                        bamFile.fileOperationStatus <> '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
                        or exists (
                            from
                                ProcessingThresholds pt
                            where
                                pt.project = mwp.sample.individual.project
                                and pt.seqType = mwp.seqType
                                and pt.sampleType = mwp.sample.sampleType
                                and (pt.coverage is null OR pt.coverage <= bamFile.coverage)
                                and (pt.numberOfLanes is null OR pt.numberOfLanes <= bamFile.numberOfMergedLanes)
                        )
                    )
            )
        """
    }

    SamplePair.executeQuery("""
        select samplePair
        from SamplePair samplePair
        where (
            ${needsProcessing('snvProcessingStatus', Pipeline.Type.SNV)}
            or ${needsProcessing('indelProcessingStatus', Pipeline.Type.INDEL)}
            or ${needsProcessing('sophiaProcessingStatus', Pipeline.Type.SOPHIA)}
            or ${needsProcessing('aceseqProcessingStatus', Pipeline.Type.ACESEQ)}
        )
        ${bamFileInProgressingOrThresouldReached('1')}
        ${bamFileInProgressingOrThresouldReached('2')}
    """).each { SamplePair samplePair ->
        [samplePair.mergingWorkPackage1, samplePair.mergingWorkPackage2].each { MergingWorkPackage mergingWorkPackage ->
            seqTracks.addAll(mergingWorkPackage.findMergeableSeqTracks())
        }
    }

    //collect running VariantCallingInstances
    BamFilePairAnalysis.createCriteria().list {
        eq('processingState', AnalysisProcessingStates.IN_PROGRESS)
        eq('withdrawn', false)
        sampleType1BamFile {
            eq('withdrawn', false)
        }
        sampleType2BamFile {
            eq('withdrawn', false)
        }
    }.each { BamFilePairAnalysis bamFilePairAnalysis ->
        seqTracks.addAll(bamFilePairAnalysis.containedSeqTracks)
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
            seqTracks += mergeableSeqTracks - (it.seqType.isWgbs() ? ctx.WgbsAlignmentStartJob : ctx.PanCanStartJob).findUsableBaseBamFile(it)?.containedSeqTracks
        }
    }

    showSeqTracks(seqTracks.unique())

    output << "\n\nProjects in processing:"
    output << INDENT + seqTracks*.sample*.individual*.project*.name.unique().sort().join("\n${INDENT}")
}

output << '\n\n'
println output.getOutput().replace("<br>", " ")
//println outputSeqTrack.join("\n")

println ""
