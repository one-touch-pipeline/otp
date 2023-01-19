/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import javax.sql.DataSource
import java.nio.file.Paths
import java.text.MessageFormat

import static org.springframework.util.Assert.notNull

@Transactional
class SeqTrackService {

    FileTypeService fileTypeService

    /**
     * Dependency Injection of Project Service.
     *
     * Needed for access control on data protected by Projects.
     */
    ProjectService projectService
    /**
     * Dependency Injection of LSDF File Service.
     *
     * Required for exporting the view by pid path of a datafile
     */
    LsdfFilesService lsdfFilesService

    AlignmentDeciderService alignmentDeciderService

    @Autowired
    DataSource dataSource

    @Autowired
    ApplicationContext applicationContext

    /**
     * Retrieves the Sequences matching the given filtering the user has access to.
     * The access restriction is done through the Projects the user has access to.
     * @param offset Offset in data
     * @param max Maximum number of elements, capped at 100
     * @param sortOrder true for ascending, false for descending
     * @param column The column to perform the sorting on
     * @param filtering Filtering restrictions
     * @return List of matching Sequences
     */
    List<Sequence> listSequences(int offset, int max, boolean sortOrder, SequenceColumn column, SequenceFiltering filtering) {
        if (filtering.enabled) {
            Closure filteringClosure = createSequenceFilteringClosure()
            return Sequence.withCriteria {
                filteringClosure.delegate = delegate
                filteringClosure.resolveStrategy = Closure.DELEGATE_FIRST
                filteringClosure(filtering)
                if (max != -1) { //-1 indicate in jquery datatable, that no paging is used. Therefore in that case no maxResult are set
                    maxResults(max)
                }
                firstResult(offset)
                order(column.columnName, sortOrder ? "asc" : "desc")
            }
        }
        List<Project> projects = projectService.allProjects()
        return projects ? Sequence.findAllByProjectIdInList(projects*.id, [
                offset: offset,
                max   : max,
                sort  : column.columnName,
                order : sortOrder ? "asc" : "desc",
        ]) : []
    }

    /**
     * Counts the Sequences the User has access to by applying the provided filtering.
     * @param filtering The filters to apply on the data
     * @return Number of Sequences matching the filtering
     */
    int countSequences(SequenceFiltering filtering) {
        if (filtering.enabled) {
            Closure filteringClosure = createSequenceFilteringClosure()
            return Sequence.createCriteria().get {
                filteringClosure.delegate = delegate
                filteringClosure.resolveStrategy = Closure.DELEGATE_FIRST
                filteringClosure(filtering)
                projections { count('mockPid') }
            }
        }
        // shortcut for unfiltered results
        List<Project> projects = projectService.allProjects()
        return projects ? Sequence.countByProjectIdInList(projects*.id) : 0
    }

    Closure createSequenceFilteringClosure() {
        return { SequenceFiltering filtering ->
            'in'('projectId', projectService.allProjects()*.id)
            if (filtering.project) {
                'in'('projectId', filtering.project)
            }
            if (filtering.individual) {
                or {
                    filtering.individual.each {
                        ilike('mockPid', "%${it}%")
                    }
                }
            }
            if (filtering.sampleType) {
                'in'('sampleTypeId', filtering.sampleType)
            }
            if (filtering.seqType) {
                'in'('seqTypeDisplayName', filtering.seqType)
            }
            if (filtering.libraryLayout) {
                'in'('libraryLayout', filtering.libraryLayout)
            }
            if (filtering.singleCell) {
                'in'('singleCell', filtering.singleCell)
            }
            if (filtering.seqCenter) {
                'in'('seqCenterId', filtering.seqCenter)
            }
            if (filtering.libraryPreparationKit) {
                'in'('libraryPreparationKit', filtering.libraryPreparationKit)
            }
            if (filtering.antibodyTarget) {
                'in'('antibodyTarget', filtering.antibodyTarget)
            }
            if (filtering.ilseId) {
                'in'('ilseId', filtering.ilseId)
            }
            if (filtering.run) {
                or {
                    filtering.run.each {
                        ilike('name', "%${it}%")
                    }
                }
            }
            if (filtering.speciesCommonName) {
                'in'('speciesCommonName', filtering.speciesCommonName)
            }
            if (filtering.scientificName) {
                'in'('scientificName', filtering.scientificName)
            }
            if (filtering.strain) {
                'in'('strain', filtering.strain)
            }
        }
    }

    /**
     * Calls the {@link AlignmentDecider#decideAndPrepareForAlignment(SeqTrack, boolean)} method of the
     * {@link AlignmentDecider} specified by the {@link Project#alignmentDeciderBeanName} property of the specified
     * {@link SeqTrack}'s {@link Project}.
     */
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign = false) {
        AlignmentDecider decider = alignmentDeciderService.getAlignmentDecider(seqTrack.project)
        return decider.decideAndPrepareForAlignment(seqTrack, forceRealign)
    }

    static boolean mayAlign(SeqTrack seqTrack, boolean log = true) {
        Closure notAligning = { String reason ->
            if (log) {
                AbstractAlignmentDecider.logNotAligning(seqTrack, reason)
            }
        }

        if (seqTrack.withdrawn) {
            notAligning('it is withdrawn')
            return false
        }

        if (!DataFile.withCriteria {
            eq 'seqTrack', seqTrack
            fileType {
                eq 'type', FileType.Type.SEQUENCE
            }
            eq 'fileWithdrawn', false
        }) {
            notAligning('it has no sequence files')
            return false
        }

        if (seqTrack.seqType.exome &&
                seqTrack.libraryPreparationKit == null &&
                seqTrack.kitInfoReliability == InformationReliability.UNKNOWN_VERIFIED) {
            notAligning('kitInfoReliability is UNKNOWN_VERIFIED')
            return false
        }

        if (seqTrack.seqPlatformGroup == null) {
            notAligning("seqPlatformGroup is null for ${seqTrack.seqPlatform}")
            return false
        }

        return true
    }

    void markFastqcFinished(SeqTrack seqTrack) {
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert (seqTrack.save(flush: true))
    }

    List<DataFile> getSequenceFilesForSeqTrack(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<DataFile> filteredFiles = []
        files.each {
            if (fileTypeService.isGoodSequenceDataFile(it)) {
                filteredFiles.add(it)
            }
        }
        return filteredFiles
    }

    void fillBaseCount(SeqTrack seqTrack) {
        seqTrack.nBasePairs = seqTrack.dataFilesWhereIndexFileIsFalse.sum { DataFile it -> it.NBasePairs } as Long ?: 0
        assert seqTrack.save(flush: true)
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or (returnObject == null) or hasPermission(returnObject.sample.individual.project, 'OTP_READ_ACCESS')")
    SeqTrack getSeqTrack(String identifier) {
        if (!identifier) {
            return null
        }
        SeqTrack seqTrack = null
        if (identifier.long) {
            seqTrack = SeqTrack.get(identifier as Long)
        }
        return seqTrack
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual.project, 'OTP_READ_ACCESS')")
    List<SeqTrack> getSeqTrackSet(Individual individual, SampleType sampleType, SeqType seqType) {
        return SeqTrack.createCriteria().list {
            sample {
                eq("individual", individual)
                eq("sampleType", sampleType)
            }
            eq("seqType", seqType)
        }
    }

    /**
     * This method determines if a fastq file has to be linked or copied to the project folder and stores the information in the seqTrack.
     * If a fastq file fulfills the following constraints it has to be linked:
     * - provided via storage that allows linking
     * - will be aligned
     * - the project allows linking
     * - the sequencing type allows linking (depends on whether the corresponding alignment workflow allows incremental merging)
     * - doesn't need adapter trimming
     */
    void determineAndStoreIfFastqFilesHaveToBeLinked(SeqTrack seqTrack, boolean willBeAligned) {
        assert seqTrack: "The input seqTrack for determineAndStoreIfFastqFilesHaveToBeLinked must not be null"
        boolean importDirAllowsLinking = doesImportDirAllowLinking(seqTrack)
        boolean projectAllowsLinking = !seqTrack.project.forceCopyFiles
        boolean seqTypeAllowsLinking = seqTrack.seqType.seqTypeAllowsLinking()
        boolean hasIndexFiles = hasIndexFiles(seqTrack)
        boolean adapterTrimming = RoddyWorkflowConfig.getLatestForIndividual(seqTrack.individual, seqTrack.seqType,
                CollectionUtils.atMostOneElement(Pipeline.findAllByName(seqTrack.seqType.isRna() ?
                        Pipeline.Name.RODDY_RNA_ALIGNMENT : Pipeline.Name.PANCAN_ALIGNMENT)))?.adapterTrimmingNeeded ?: false
        boolean link = willBeAligned && importDirAllowsLinking && projectAllowsLinking && seqTypeAllowsLinking && !hasIndexFiles && !adapterTrimming
        seqTrack.log("Fastq files{0} will be ${link ? "linked" : "copied"}, because " +
                "willBeAligned=${willBeAligned}, importDirAllowsLinking=${importDirAllowsLinking}, projectAllowsLinking=${projectAllowsLinking}, " +
                "seqTypeAllowsLinking=${seqTypeAllowsLinking}, needs adapter trimming=${adapterTrimming}, hasIndexFiles=${hasIndexFiles}")
        if (link) {
            seqTrack.linkedExternally = true
            assert seqTrack.save(flush: true)
        }
    }

    private boolean hasIndexFiles(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack for doesImportDirAllowLinking must not be null"
        return seqTrack.dataFiles.find {
            it.indexFile
        }
    }

    private boolean doesImportDirAllowLinking(SeqTrack seqTrack) {
        assert seqTrack: "The input seqTrack for doesImportDirAllowLinking must not be null"
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        return files.every { DataFile dataFile ->
            seqTrack.seqCenter.importDirsAllowLinking.any {
                Paths.get(dataFile.initialDirectory).startsWith(it)
            }
        }
    }

    List<ExternallyProcessedMergedBamFile> returnExternallyProcessedMergedBamFiles(List<SeqTrack> seqTracks) {
        notNull(seqTracks, "The input of returnExternallyProcessedMergedBamFiles is null")
        assert !seqTracks.empty: "The input list of returnExternallyProcessedMergedBamFiles is empty"

        return seqTracks.collect { val ->
            CollectionUtils.atMostOneElement(
                    ExternallyProcessedMergedBamFile.createCriteria().list {
                        workPackage {
                            eq('sample', val.sample)
                            eq('seqType', val.seqType)
                        }
                    }
            )
        }.unique().findAll()
    }

    /**
     * Returns all the SamplePairs given by individual, sampleType, seqType and sampleName.
     * Only individual is required, others are optional. Missing parameters or null values means without these condition/constrains.
     * For example: if sampleType is null or missing, then all sampleTypes are taken into account.
     * The same is true with seqType and sampleName.
     *
     * @param individual required
     * @param sampleType all sample types if missing
     * @param seqType all seq types if missing
     * @param sampleName all samples if missing
     * @return all the SamplePairs
     */
    List<SeqTrack> findAllByIndividualSampleTypeSeqTypeSampleNameMd5sum(Individual individual,
                                                                        SampleType sampleType = null, SeqType seqType = null, String sampleName = null,
                                                                        String md5sum = null) {
        return DataFile.withCriteria {
            seqTrack {
                sample {
                    eq('individual', individual)
                    if (sampleType) {
                        eq('sampleType', sampleType)
                    }
                }
                if (seqType) {
                    eq('seqType', seqType)
                }
                if (sampleName) {
                    eq('sampleIdentifier', sampleName)
                }
            }
            if (md5sum) {
                eq('md5sum', md5sum)
            }
        }*.seqTrack.unique()
    }

    static void logToSeqTrack(SeqTrack seqTrack, String message, boolean saveInSeqTrack = true) {
        LogThreadLocal.threadLog?.info(MessageFormat.format(message, " " + seqTrack))
        if (saveInSeqTrack) {
            seqTrack.save(flush: true)
            SeqTrack.withTransaction {
                LogMessage logMessage = new LogMessage(message: MessageFormat.format(message, ""))
                logMessage.save(flush: true)
                seqTrack.logMessages.add(logMessage)
                seqTrack.save(flush: true)
            }
        }
    }

    /**
     * Transforms a List of SeqTracks into a nested Map of SeqTrackSets, grouped by SeqType and then SampleType.
     *
     * @param inputSeqTracks the SeqTracks to transform
     * @return the transformed Map
     */
    static Map<SeqType, Map<SampleType, SeqTrackSet>> getSeqTrackSetsGroupedBySeqTypeAndSampleType(List<SeqTrack> inputSeqTracks) {
        Map<SeqType, Map<SampleType, SeqTrackSet>> fullyGroupedAsSets = [:]
        inputSeqTracks.groupBy({ it.seqType }, { it.sampleType }).collectEntries(fullyGroupedAsSets) { SeqType seqType,
                                                                                                       Map<SampleType, List<SeqTrack>> seqTracksPerSampleType ->
            Map<SampleType, SeqTrackSet> setsPerSampleType = [:]
            seqTracksPerSampleType.collectEntries(setsPerSampleType) { SampleType sampleType, List<SeqTrack> seqTracks ->
                return [(sampleType): new SeqTrackSet(seqTracks)]
            }
            return [(seqType): setsPerSampleType]
        }
        return fullyGroupedAsSets
    }

    /**
     * Returns a sublist of given SeqTracks which all have an analysable SeqType.
     *
     * @param seqTracks list of sequence tracks
     * @return sublist of the given seqTracks with analysable SeqTypes.
     */
    static List<SeqTrack> getAnalysableSeqTracks(List<SeqTrack> seqTracks) {
        List<SeqType> analysableSeqTypes = SeqTypeService.allAnalysableSeqTypes

        return seqTracks.findAll { SeqTrack seqTrack ->
            seqTrack.seqType in analysableSeqTypes
        }
    }

    /**
     * In case there are ExternallyProcessedMergedBamFile attached to the lanes to swap, the script shall stop
     */
    void throwExceptionInCaseOfExternalMergedBamFileIsAttached(List<SeqTrack> seqTracks) {
        List<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = returnExternallyProcessedMergedBamFiles(seqTracks)
        assert externallyProcessedMergedBamFiles.empty: "There are ExternallyProcessedMergedBamFiles attached: ${externallyProcessedMergedBamFiles}"
    }

    /**
     * In case the seqTracks are only linked, the script shall stop
     */
    void throwExceptionInCaseOfSeqTracksAreOnlyLinked(List<SeqTrack> seqTracks) {
        int linkedSeqTracks = seqTracks.findAll { SeqTrack seqTrack -> seqTrack.linkedExternally }.size()
        assert !linkedSeqTracks: "There are ${linkedSeqTracks} seqTracks only linked"
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#sample.project, 'OTP_READ_ACCESS')")
    List<SeqTrack> findAllBySampleAndSeqTypeAndAntibodyTarget(Sample sample, SeqType seqType, AntibodyTarget antibodyTarget) {
        return SeqTrack.findAllWhere(
                sample: sample,
                seqType: seqType,
                antibodyTarget: antibodyTarget,
        )
    }
}
