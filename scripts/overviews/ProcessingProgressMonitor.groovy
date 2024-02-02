/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

// name of runs
String runString = """
#run 1
#run 2

"""

// pid of patients
String individualString ="""
#patient 1
#patient 2

"""

String ilseIdString ="""
# ilse id1
# ilse id2

"""

// name of projects, can take some time depending of project(s)
String projectString ="""
#project 1
#project 2

"""

// if enabled, shows all lanes in progressing.
// see file header comment for details
boolean allProcessed = true

// flag if for all workflows the finished entries should be shown
boolean showFinishedEntries = false

// flag if all seqTypes that are not supported by the workflow should be listed in the output
boolean showUnsupportedSeqTypes = false

// ==================================================

ProcessingOptionService processingOptionService = ctx.processingOptionService
MonitorOutputCollector output = new MonitorOutputCollector(showFinishedEntries, showUnsupportedSeqTypes)
output.configService = ctx.configService
output.showWorkflowSystemSlots()

SeqType exomePaired = SeqTypeService.exomePairedSeqType
List<SeqType> alignableSeqtypes = SeqTypeService.allAlignableSeqTypes

MetaDataKey libPrepKitKey = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.LIB_PREP_KIT.name()))
MetaDataKey enrichmentKitKey = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName("ENRICHMENT_KIT")) // old name for LIB_PREP_KIT, keep as-is!
MetaDataKey commentKey = CollectionUtils.atMostOneElement(MetaDataKey.findAllByName(MetaDataColumn.COMMENT.name()))

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

Closure<List<String>> nameStringToList = { String nameString ->
    List<String> list = []
    nameString.eachLine {
        if (it.trim().isEmpty() || it.trim().startsWith("#")) {
            return
        }
        list << it.trim()
    }
    return list
}

Closure<List<SeqTrack>> findNotWithdrawn = { List<SeqTrack> seqTracks ->
    Map<Boolean, List<SeqTrack>> map = seqTracks.groupBy {
        RawSequenceFile.findAllBySeqTrackAndFileWithdrawn(it, true) as Boolean
    }
    if (map[true]) {
        output << "the ${map[true].size()} withdrawn lanes are filtered out"
    }
    return map[false]
}

Closure handleStateMap = { Map map, String workflow, Closure objectToCheck = {it} ->
    output.showWorkflowNewSystem(workflow)
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

Closure showSeqTracks = { Collection<SeqTrack> seqTracks ->
    boolean allFinished = true

    // remove withdrawn
    List<SeqTrack> seqTracksNotWithdrawn = findNotWithdrawn(seqTracks)
    if (!seqTracksNotWithdrawn) {
        output << "No not withdrawn seq tracks left, stop"
        return
    }

    // data installation workflow
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> dataInstallationState =
            seqTracksNotWithdrawn.groupBy {it.dataInstallationState}

    allFinished &= dataInstallationState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksFinishedDataInstallationWorkflow = handleStateMap(dataInstallationState, "FASTQ installation")

    if (!seqTracksFinishedDataInstallationWorkflow) {
        output << "\nnot all workflows are finished"
        return
    }

    // fastqc
    Map<SeqTrack.DataProcessingState, Collection<SeqTrack>> seqTracksNotWithdrawnByFastqcState =
            seqTracksFinishedDataInstallationWorkflow.groupBy {it.fastqcState}

    allFinished &= seqTracksNotWithdrawnByFastqcState.keySet() == [SeqTrack.DataProcessingState.FINISHED] as Set
    Collection<SeqTrack> seqTracksNotWithdrawnFinishedFastqcWorkflow = handleStateMap(seqTracksNotWithdrawnByFastqcState, "FastQC")

    if (!seqTracksNotWithdrawnFinishedFastqcWorkflow) {
        output << "\nnot all workflows are finished"
        return
    }

    output << "\n"

    Collection<RoddyBamFile> bamFilesFinishedAlignment =
            new AllAlignmentsChecker().handle(seqTracksNotWithdrawnFinishedFastqcWorkflow, output)

    if (!bamFilesFinishedAlignment) {
        return
    }

    // finished aligned
    output << "\nFinished aligned samples (${bamFilesFinishedAlignment.size()}): "
    bamFilesFinishedAlignment.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }

    if (!allFinished) {
        output << "\nnot all workflows are finished till alignment"
    }

    // variant calling
    List<SamplePair> finishedSamplePair = new VariantCallingPipelinesChecker().handle(bamFilesFinishedAlignment, output)

    // end
    output << "\nFinished variant calling sample pairs (${finishedSamplePair.size()}): "

    finishedSamplePair.collect {
        "${INDENT}${INDENT}${it} ${it.project}"
    }.sort { it }.each { output << it }
}

// ======================================================

nameStringToList(runString).each { String runName ->
    output << "\n\n\n==============================\nrunNames = ${runName}\n==============================\n"

    Run run = CollectionUtils.atMostOneElement(Run.findAllByName(runName))
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

    Individual individual = CollectionUtils.atMostOneElement(Individual.findAllByPid(individualName))
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

    Project project = CollectionUtils.atMostOneElement(Project.findAllByName(projectName))
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

    List<SeqTrack> seqTracks = SeqTrack.executeQuery("""
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
                RawSequenceFile datafile
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
                        roddyBamFile.fileOperationStatus <> '${AbstractBamFile.FileOperationStatus.PROCESSED}'
                        and roddyBamFile.withdrawn = false
                    )
            )
        )
    """.toString())

    // collect waiting SamplePairs
    Closure<GString> needsProcessing = { String property, Pipeline.Type type ->
        double minCoverage = processingOptionService.findOptionAsDouble(ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE, type.toString())
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

    Closure<GString> connectBamFile = { String number ->
        return """
            mwp${number} = samplePair.mergingWorkPackage${number}
            and bamFile${number}.withdrawn = false
            and bamFile${number}.id = (
                select max( maxBamFile.id)
                from AbstractBamFile maxBamFile
                where maxBamFile.workPackage = bamFile${number}.workPackage
            )
            and (
                bamFile${number}.fileOperationStatus <> '${AbstractBamFile.FileOperationStatus.PROCESSED}'
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

    def needsProcessingWithDependency = { String processingStatusProperty, Pipeline.Type pipelineType, String dependencyInstanceClass ->
        return """(
            ${needsProcessing(processingStatusProperty, pipelineType)}
            and exists (
                select
                    dependency
                from
                    ${dependencyInstanceClass} dependency
                where
                    dependency.samplePair = samplePair
                    and dependency.processingState = '${AnalysisProcessingStates.FINISHED}'
            )
        )
        """
    }

    SamplePair.executeQuery("""
        select samplePair
        from
            SamplePair samplePair,
            AbstractBamFile bamFile1
                join bamFile1.workPackage mwp1,
            AbstractBamFile bamFile2
                join bamFile2.workPackage mwp2
        where (
            ${needsProcessing('snvProcessingStatus', Pipeline.Type.SNV)}
            or ${needsProcessing('indelProcessingStatus', Pipeline.Type.INDEL)}
            or ${needsProcessing('sophiaProcessingStatus', Pipeline.Type.SOPHIA)}
            or ${needsProcessingWithDependency('aceseqProcessingStatus', Pipeline.Type.ACESEQ, 'SophiaInstance')}
            or ${needsProcessingWithDependency('runYapsaProcessingStatus', Pipeline.Type.MUTATIONAL_SIGNATURE, 'RoddySnvCallingInstance')}
        )
        and ${connectBamFile('1')}
        and ${connectBamFile('2')}
    """.toString()).each { SamplePair samplePair ->
        [samplePair.mergingWorkPackage1, samplePair.mergingWorkPackage2].each { MergingWorkPackage mergingWorkPackage ->
            seqTracks.addAll(mergingWorkPackage.seqTracks)
        }
    }

    // collect running VariantCallingInstances
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
    """.toString()).each {
        Collection<SeqTrack> mergeableSeqTracks = it.seqTracks
        if(mergeableSeqTracks) {
            seqTracks += mergeableSeqTracks
        }
    }

    showSeqTracks(seqTracks.unique())

    output << "\n\nProjects in processing:"
    output << INDENT + seqTracks*.project*.name.unique().sort().join("\n${INDENT}")
}

output << '\n\n'
println output.output.replace("<br>", " ")
// println outputSeqTrack.join("\n")

println ""
