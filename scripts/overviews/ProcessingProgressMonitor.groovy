/*
The following code allows to show the processing state for

* runs
* individuals (pid)
* ilse id
* project (can take some time, depending of project)
* all lanes in processing:
** all withdrawn lanes are ignored
** waiting and running data installation workflow
** waiting and running fastqc workflows for data loaded after 1.1.2015 and are not in blacklist
** waiting/running alignments
*** seqTracks which belong to run segments where flag 'align' is set to false are ignored
*** running OTP alignments (WGS, WES)
*** running roddy alignments (WGS, WES, WGBS, RNA, ChipSeq) (if not withdrawn)
** running variant calling (snv, indel, ...) (if not withdrawn)
** waiting variant calling (snv, indel, ...) if
*** a config is available
*** the last bam file is not withdrawn
*** the last bam file is in processing or has reached the threshold
*** the bam files has reached the min coverage for the analysis
*** depending analysis is finished


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

//flag if for all workflows the finished entries should be shown
boolean showUnsupportedSeqTypes = false

//==================================================

MonitorOutputCollector output = new MonitorOutputCollector(showFinishedEntries, showUnsupportedSeqTypes)

SeqType exomePaired = SeqType.exomePairedSeqType
List<SeqType> alignableSeqtypes = SeqType.allAlignableSeqTypes

MetaDataKey libPrepKitKey = MetaDataKey.findByName(MetaDataColumn.LIB_PREP_KIT.name())
MetaDataKey enrichmentKitKey = MetaDataKey.findByName("ENRICHMENT_KIT") // old name for LIB_PREP_KIT, keep as-is!
MetaDataKey commentKey = MetaDataKey.findByName(MetaDataColumn.COMMENT.name())

// Heidelberg SeqTracksIDs for which Merging is broken
List blackList_MergingSetId = ([12174511] as long[]) as List

// Heidelberg SeqTrackIDs for which FastQc workflow is broken
List blackList_SeqTrackForFastQc = ([
        33881224, 34409106, 34409201, 34409296, 34408057, 34409961, 34407757, 34407859, 34407957, 34408631,
        34407552, 34408346, 34408916, 34408726, 34407655, 34408441, 34409011, 34408536, 34407448, 34408251,
        34408821, 34408155, 34410056, 34406946, 34407041, 34407144, 34406471, 34407239, 34405996, 34406281,
        34409486, 34406756, 34409771, 34406091, 34406376, 34409581, 34406851, 34409866, 34409676, 34405901,
        34406186, 34409391, 34406661, 34406566, 34407340, 33143452, 33145115, 33144944, 33145058, 33143567,
        33252540, 33252598, 33252655, 33929114, 33929510, 33903632, 33904059, 33904158, 33904257, 33928786,
        33928885, 33903929, 33904356, 33929213, 33928687, 33903523, 33903731, 33903830, 33098976, 33099020,
        33099108, 33099064, 33099152, 33099196, 33099284, 33099240, 33099372, 33099328, 33099460, 33099416,
        33099548, 33099504, 1496693, 33928984, 33929312, 33929411, 1496693,
] as long[]) as List

long firstSeqTrackIdToCheckForFastQcWorkflow = 20077258 // Heidelberg 1.1.2015


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
    Map mapSpecificReferenceGenomeType = seqTracks.groupBy {
        return it.sampleType.specificReferenceGenome == SampleType.SpecificReferenceGenome.UNKNOWN
    }
    if (mapSpecificReferenceGenomeType[true]) {
        output << "${INDENT}${mapSpecificReferenceGenomeType[true].size()} lanes removed, because the used sampleType has not defined the type of reference genome (Project or sample type specific): ${mapSpecificReferenceGenomeType[true]*.sampleType*.name.unique().sort()}}"
    }
    Map mapNoReferenceGenome = (mapSpecificReferenceGenomeType[false] ?: [:]).groupBy {
        return !it.configuredReferenceGenome
    }
    if (mapNoReferenceGenome[true]) {
        output << "${INDENT}${mapNoReferenceGenome[true].size()} lanes removed, because the used project(s) has/have no reference genome(s): ${mapNoReferenceGenome[true]*.project.unique().sort {it.name}}"
    }
    return mapNoReferenceGenome[false] ?: []
}

def exomeSeqTracksWithEnrichmentKit = {List<SeqTrack> seqTracks ->
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

def exomeSeqTracksWithBedFile = {List<SeqTrack> seqTracks ->
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
    Map<Boolean, List<SeqTrack>> groupAfterAlignable = seqTracks.groupBy({alignableSeqtypes.contains(it.seqType) })

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


def handleStateMap = {Map map, String workflow, Closure objectToCheck = {it} ->
    output.showWorkflow(workflow)
    def ret
    def keys = map.keySet().sort{it}

    valueToShow = { "${it.sample}  ${it.seqType}  ${it.run}  ${it.laneId}  ${it.project}  ilse: ${it.ilseId} id: ${it.id}" }

    keys.each { key ->
        def values = map[key]
        switch (key.ordinal()) { // HACK: not all workflows use same key-enum, but all enums used have the same ordinals
            case 0:
                output.showNotTriggered(values, valueToShow)
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
                new RuntimeException("Not handled value: ${key}. Please inform a maintainer")
        }
    }
    return ret
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
    Collection<SeqTrack> seqTracksFinishedDataInstallationWorkflow = handleStateMap(dataInstallationState, "DataInstallationWorkflow")

    if (!seqTracksFinishedDataInstallationWorkflow) {
        output << "\nnot all workflows are finished"
        return
    }

    //fastqc
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> seqTracksNotWithdrawnByFastqcState =
            seqTracksFinishedDataInstallationWorkflow.groupBy {it.fastqcState}

    allFinished &= seqTracksNotWithdrawnByFastqcState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksNotWithdrawnFinishedFastqcWorkflow = handleStateMap(seqTracksNotWithdrawnByFastqcState, "FastqcWorkflow")

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

    Collection<RoddyBamFile> bamFilesFinishedAlignment =
            new AllRoddyAlignmentsChecker().handle(seqTracksByAlignmentDeciderBeanName['panCanAlignmentDecider'], output)

    if (!bamFilesFinishedAlignment) {
        return
    }

    //finished aligned
    output << "\nFinished aligned samples (${bamFilesFinishedAlignment.size()}): "
    bamFilesFinishedAlignment.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }

    if (!allFinished) {
        output << "\nnot all workflows are finished till alignment"
    }

    //variant calling
    List<SamplePair> finishedSamplePair = new VariantCallingPipelinesChecker().handle(bamFilesFinishedAlignment, output)

    //end
    output << "\nFinished variant calling sample pairs (${finishedSamplePair.size()}): "

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
                and seqTrack.id >= ${firstSeqTrackIdToCheckForFastQcWorkflow}
                and not seqTrack.id in (${blackList_SeqTrackForFastQc.join(', ')})
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
        Double minCoverage = ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE, type.toString(),null) as Double ?: 0.0
        return """
                (
                    samplePair.${property} = '${SamplePair.ProcessingStatus.NEEDS_PROCESSING}'
                    and exists (
                        select
                            config
                        from
                            ConfigPerProjectAndSeqType config
                        where
                            config.project = samplePair.mergingWorkPackage1.sample.individual.project
                            and config.seqType = samplePair.mergingWorkPackage1.seqType
                            and config.pipeline.type = '${type}'
                            and config.obsoleteDate is null
                    )
                    and bamFile1.coverage >= ${minCoverage}
                    and bamFile2.coverage >= ${minCoverage}
                )"""
    }

    def connectBamFile = {String number ->
        return """
            mwp${number} = samplePair.mergingWorkPackage${number}
            and bamFile${number}.withdrawn = false
            and
            (
                (
                    bamFile${number}.qcTrafficLightStatus not in (
                        '${AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED.name()}',
                        '${AbstractMergedBamFile.QcTrafficLightStatus.REJECTED.name()}'
                    )
                )
                or bamFile${number}.qcTrafficLightStatus is NULL
            )
            and bamFile${number}.id = (
                select max( maxBamFile.id)
                from AbstractMergedBamFile maxBamFile
                where maxBamFile.workPackage = bamFile${number}.workPackage
            )
            and (
                bamFile${number}.fileOperationStatus <> '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'
                or exists (
                    from
                        ProcessingThresholds pt
                    where
                        pt.project = mwp${number}.sample.individual.project
                        and pt.seqType = mwp${number}.seqType
                        and pt.sampleType = mwp${number}.sample.sampleType
                        and (pt.coverage is null OR pt.coverage <= bamFile${number}.coverage)
                        and (pt.numberOfLanes is null OR pt.numberOfLanes <= bamFile${number}.numberOfMergedLanes)
                )
            )
        """
    }

    SamplePair.executeQuery("""
        select samplePair
        from
            SamplePair samplePair,
            AbstractMergedBamFile bamFile1
                join bamFile1.workPackage mwp1,
            AbstractMergedBamFile bamFile2
                join bamFile2.workPackage mwp2
        where (
            ${needsProcessing('snvProcessingStatus', Pipeline.Type.SNV)}
            or ${needsProcessing('indelProcessingStatus', Pipeline.Type.INDEL)}
            or ${needsProcessing('sophiaProcessingStatus', Pipeline.Type.SOPHIA)}
            or (
                ${needsProcessing('aceseqProcessingStatus', Pipeline.Type.ACESEQ)}
                and exists (
                    select
                        sophiaInstance
                    from
                        SophiaInstance sophiaInstance
                    where
                        sophiaInstance.samplePair = samplePair
                        and sophiaInstance.processingState = '${AnalysisProcessingStates.FINISHED}'
                )
            )
        )
        and ${connectBamFile('1')}
        and ${connectBamFile('2')}
    """).each { SamplePair samplePair ->
        [samplePair.mergingWorkPackage1, samplePair.mergingWorkPackage2].each { MergingWorkPackage mergingWorkPackage ->
            seqTracks.addAll(mergingWorkPackage.seqTracks)
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
        def mergeableSeqTracks = it.seqTracks
        if(mergeableSeqTracks) {
            seqTracks += mergeableSeqTracks - (it.seqType.isWgbs() ? ctx.WgbsAlignmentStartJob : ctx.PanCanStartJob).findUsableBaseBamFile(it)?.containedSeqTracks
        }
    }

    showSeqTracks(seqTracks.unique())

    output << "\n\nProjects in processing:"
    output << INDENT + seqTracks*.project*.name.unique().sort().join("\n${INDENT}")
}

output << '\n\n'
println output.getOutput().replace("<br>", " ")
//println outputSeqTrack.join("\n")

println ""
