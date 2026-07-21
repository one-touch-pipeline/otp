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
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Transactional
import groovy.transform.Canonical
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataswap.AbstractDataSwapService
import de.dkfz.tbi.otp.egaSubmission.EgaSubmission
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.ExternalAlignmentWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcLinkFileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.utils.exceptions.NotSupportedException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Files
import java.nio.file.Path

import static org.hibernate.proxy.HibernateProxyHelper.getClassWithoutInitializingProxy
import static org.springframework.util.Assert.notNull

/**
 * This class is written for scripts, so it needs the output in stdout.
 */
@SuppressWarnings('Println')
@Transactional
class DeletionService {

    // Conservative batch size for IN-list queries while walking the artefact graph.
    // Not the JDBC limit (65,535, see WorkflowRunService#getCriteria) -- just a safe chunk size.
    static final int ARTEFACT_QUERY_CHUNK_SIZE = 1000

    AbstractBamFileService abstractBamFileService
    AnalysisDeletionService analysisDeletionService
    CommentService commentService
    ConfigService configService
    DataProcessingFilesService dataProcessingFilesService
    FastqcLinkFileService fastqcLinkFileService
    FileService fileService
    IndividualService individualService
    LsdfFilesService lsdfFilesService
    RunService runService
    SeqTrackService seqTrackService
    WorkflowDeletionService workflowDeletionService
    CellRangerWorkFileService cellRangerWorkFileService
    FilestoreService filestoreService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService
    ExternalAlignmentWorkFileService externalAlignmentWorkFileService
    PanCancerLinkFileService panCancerLinkFileService
    ProcessingOptionService processingOptionService

    @CompileDynamic
    void deleteProjectContent(Project project) {
        assert project.state != Project.State.ARCHIVED

        assert !EgaSubmission.findAllByProject(project): "There are Ega Submissions connected to this Project, thus it can not be deleted"

        // Delete any project requests associated with this project
        ProjectRequest.findAllByProject(project).each {
            it.delete(flush: true)
        }

        // Delete individuals for a project
        Individual.findAllByProject(project).each { individual ->
            deleteIndividual(individual, false)
        }

        // There are files which are not connected to a seqTrack -> they have to be deleted, too
        RawSequenceFile.findAllByProject(project).each { rawSequenceFile ->
            deleteRawSequenceFile(rawSequenceFile)
        }

        workflowDeletionService.deleteWorkflowVersionSelector(project)

        workflowDeletionService.deleteReferenceGenomeSelector(project)

        workflowDeletionService.deleteWorkflowRun(project)

        // remove project from ExternalWorkflowConfigSelector or delete selector completely
        deleteProjectsExternalWorkflowConfigSelector(project)
    }

    @CompileDynamic
    void deleteProject(Project project) {
        assert project.state != Project.State.ARCHIVED

        deleteProjectContent(project)
        deleteProjectDependencies(project)
        project.delete(flush: true)
    }

    List<String> formatRemoveUUIDFolders(Project project) {
        return filestoreService.getWorkFolders(project).collect { "|rm -rf ${filestoreService.getWorkFolderPath(it)}" } as List<String>
    }

    @SuppressWarnings('JavaIoPackageAccess')
    @CompileDynamic
    String deleteIndividual(Individual individual, boolean check = true) {
        assert individual.project.state != Project.State.ARCHIVED

        StringBuilder deletionScript = new StringBuilder()

        List<Sample> samples = Sample.findAllByIndividual(individual)

        List<SeqType> seqTypes = []

        samples.each { Sample sample ->
            List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)

            seqTracks.each { SeqTrack seqTrack ->
                seqTrack.sequenceFiles.each { RawSequenceFile rawSequenceFile ->
                    String filePath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
                    if (filePath) {
                        deletionScript << "rm -rf ${new File(filePath).absolutePath}\n"
                    }
                }
                List<File> seqTrackDirsToDelete = deleteSeqTrack(seqTrack, check)

                seqTrackDirsToDelete.each {
                    deletionScript << "rm -rf ${it.absolutePath}\n"
                }
                seqTypes.add(seqTrack.seqType)

                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(seqTrack.id.toString(), seqTrack.class.name))
            }

            deleteSample(sample)
        }

        seqTypes.unique().each { SeqType seqType ->
            deletionScript << "rm -rf ${individualService.getViewByPidPath(individual, seqType)}\n"
        }

        deleteClusterJobs(ClusterJob.findAllByIndividual(individual))
        if (Individual.exists(individual.id)) {
            individual.delete(flush: true)
        }

        return deletionScript.toString()
    }

    @CompileDynamic
    void deleteEmptyRun(Run run) {
        assert run: "The input run of the method deleteRun is null"
        deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(run.id.toString(), Run.name))
        if (Run.exists(run.id)) {
            run.delete(flush: true)
        }
    }

    /**
     * Deletes all processed files after the fastqc step from the give project.
     *
     * For the cases where the fastq files are not available in the project folder it has to be checked if the fastq files are still available on midterm.
     * If this is the case the GPCF has to be informed that the must not delete these fastq files during the sample swap.
     * If ExternallyProcessedBamFiles were imported for this project, an exception is thrown to give the opportunity for clarification.
     * If everything was clarified the method can be called with true for "everythingVerified" so that the mentioned checks won't be executed anymore.
     * If the fastq files are not available an error is thrown.
     * If withdrawn data should be ignored, set ignoreWithdrawn "true"
     *
     * If explicitSeqTracks is defined, the defined list of seqTracks will be querried
     *
     * Return a list containing the affected seqTracks
     */
    @SuppressWarnings('JavaIoPackageAccess')
    @CompileDynamic
    List<SeqTrack> deleteProcessingFilesOfProject(String projectName, Path scriptOutputDirectory, boolean everythingVerified = false,
                                                  boolean ignoreWithdrawn = false, List<SeqTrack> explicitSeqTracks = []) throws FileNotFoundException {
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName(projectName))
        assert project: "Project does not exist"
        assert project.state != Project.State.ARCHIVED

        Set<String> dirsToDelete = [] as Set
        Set<String> externalMergedBamFolders = [] as Set

        StringBuilder output = new StringBuilder()

        List<RawSequenceFile> rawSequenceFiles

        if (explicitSeqTracks.empty) {
            rawSequenceFiles = RawSequenceFile.createCriteria().list {
                seqTrack {
                    sample {
                        individual {
                            eq('project', project)
                        }
                    }
                }
            }
        } else {
            assert CollectionUtils.exactlyOneElement(explicitSeqTracks*.project.unique()) == project
            rawSequenceFiles = explicitSeqTracks ? RawSequenceFile.findAllBySeqTrackInList(explicitSeqTracks) : []
        }

        output << "found ${rawSequenceFiles.size()} data files for this project\n\n"
        assert !rawSequenceFiles.empty: "There are no SeqTracks attached to this project ${projectName}"

        List<RawSequenceFile> withdrawnFiles = []
        List<RawSequenceFile> missingFiles = []
        List<String> filesToClarify = []

        boolean throwException = false

        rawSequenceFiles.each {
            if (Files.exists(rawSequenceDataViewFileService.getFilePath(it))) {
                if (it.seqTrack.linkedExternally && !everythingVerified) {
                    filesToClarify << lsdfFilesService.getFileInitialPathAsPath(it).toString()
                    throwException = true
                }
            } else {
                // withdrawn data must have no existing fastq files,
                // to distinguish between missing files and withdrawn files this gets queried
                // an error is thrown as long as ignoreWithdrawn is false
                if (it.fileWithdrawn) {
                    withdrawnFiles << it
                    if (!ignoreWithdrawn) {
                        throwException = true
                    }
                } else {
                    throwException = true
                    missingFiles << it
                }
            }
        }

        if (withdrawnFiles) {
            output << "The fastq files of the following ${withdrawnFiles.size()} data files are withdrawn: \n${withdrawnFiles.join("\n")}\n\n"
        }

        if (missingFiles) {
            output << "The fastq files of the following ${missingFiles.size()} data files are missing: \n${missingFiles.join("\n")}\n\n"
        }

        if (filesToClarify) {
            output << "Talk to the sequencing center not to remove the following ${filesToClarify.size()} fastq files until the realignment is finished:" +
                    "\n ${filesToClarify.join("\n")}\n\n"
        }

        rawSequenceFiles = rawSequenceFiles - withdrawnFiles
        List<SeqTrack> seqTrackList = rawSequenceFiles*.seqTrack.unique()

        // in case there are no dataFiles/seqTracks left this can be ignored.
        if (seqTrackList) {
            List<ExternallyProcessedBamFile> externallyProcessedBamFiles = seqTrackService.returnExternallyProcessedBamFiles(seqTrackList)
            assert (!externallyProcessedBamFiles || everythingVerified):
                    "There are ${externallyProcessedBamFiles.size()} external merged bam files attached to this project. " +
                            "Clarify if the realignment shall be done anyway."
        }

        if (throwException) {
            println output
            throw new FileNotFoundException("Files not found")
        }

        output << "delete content in db...\n\n"

        seqTrackList.each { SeqTrack seqTrack ->
            File processingDir = new File(dataProcessingFilesService.getOutputDirectory(
                    seqTrack.individual, DataProcessingFilesService.OutputDirectories.MERGING).toString())
            if (processingDir.exists()) {
                dirsToDelete.add(processingDir.path)
            }

            Set<AbstractBamFile> bamFiles = (RoddyBamFile.createCriteria().listDistinct {
                seqTracks {
                    eq("id", seqTrack.id)
                }
            } + SingleCellBamFile.createCriteria().listDistinct {
                seqTracks {
                    eq("id", seqTrack.id)
                }
            }).findAll { AbstractBamFile bamFile ->
                bamFile.isMostRecentBamFile()
            } as Set
            bamFiles.each { AbstractBamFile bamfile ->
                if (bamfile) {
                    Path mergingDir = abstractBamFileService.getBaseDirectory(bamfile)
                    if (Files.exists(mergingDir)) {
                        List<ExternallyProcessedBamFile> files = seqTrackService.returnExternallyProcessedBamFiles([seqTrack])
                        files.each {
                            externalMergedBamFolders.add(externalAlignmentWorkFileService.getNonOtpFolder(it).toString())
                            if (it.workflowArtefact?.producedBy?.workFolder) {
                                externalMergedBamFolders.add(filestoreService.getWorkFolderPath(it.workflowArtefact.producedBy))
                            }
                        }
                        Files.list(mergingDir).each {
                            dirsToDelete.add(it.toString())
                        }
                    }
                }
            }
            deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, false).each {
                if (it) {
                    dirsToDelete.add(it)
                }
            }
        }

        String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "Delete_${projectName}.sh", unixGroup)
        bashScriptToMoveFiles << AbstractDataSwapService.BASH_HEADER

        (dirsToDelete*.toString() - externalMergedBamFolders).each {
            bashScriptToMoveFiles << "rm -rf ${it}\n"
        }

        output << "bash script to remove files on file system created:\n${bashScriptToMoveFiles}\n\n"

        println output

        return seqTrackList
    }

    /**
     * Delete all processing information and results in the DB which are connected to one SeqTrack
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information and the seqTrack are not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    @CompileDynamic
    List<File> deleteAllProcessingInformationAndResultOfOneSeqTrack(SeqTrack seqTrack, boolean enableChecks = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteAllProcessingInformationAndResultOfOneSeqTrack is null")
        assert seqTrack.project.state != Project.State.ARCHIVED
        List<File> dirsToDelete = []

        if (enableChecks) {
            seqTrackService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
            seqTrackService.throwExceptionInCaseOfExternallyProcessedBamFileIsAttached([seqTrack])
        }

        // Verification set: the processing results this method is responsible for deleting, found via the legacy
        // hardcoded queries. It does not drive the graph deletion order -- it is the completeness cross-check deciding
        // whether the graph path can be taken, and the input of the legacy fallback when it cannot.
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.createCriteria().listDistinct {
            seqTracks {
                eq("id", seqTrack.id)
            }
        }
        List<SingleCellBamFile> singleCellBamFiles = SingleCellBamFile.createCriteria().list {
            seqTracks {
                eq("id", seqTrack.id)
            }
        }
        List<AbstractBamFile> verificationBamFiles = (roddyBamFiles + singleCellBamFiles) as List<AbstractBamFile>
        List<BamFilePairAnalysis> verificationAnalyses = verificationBamFiles ?
                BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(verificationBamFiles, verificationBamFiles) : []
        List<Artefact> verificationSet = (verificationBamFiles + verificationAnalyses) as List<Artefact>

        // Graph-ordered downstream artefacts (consumers before producers), filtered to the ones this method deletes.
        ArtefactGraph graph = collectArtefactGraph(seqTrack)
        List<ArtefactDeletionEntry> deletableEntries = graph.entriesInDeletionOrder.findAll {
            isDeletableArtefact(it.concreteArtefact)
        }

        if (graphCoversVerificationSet(verificationSet, deletableEntries)) {
            // Abort before deleting anything if cleaning up the graph metadata would also remove a retained artefact.
            assertGraphDeletionTouchesNoRetainedArtefact(graph, deletableEntries)
            dirsToDelete.addAll(deleteProcessingResultsInGraphOrder(deletableEntries))
        } else {
            // Legacy data whose results are missing a workflowArtefact cannot be deleted in graph order. Fall back to
            // the pre-graph deletion flow over the verification set; any partial workflow metadata stays untouched.
            dirsToDelete.addAll(deleteConcreteArtefacts(verificationAnalyses, verificationBamFiles))
        }

        List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.createCriteria().list {
            seqTracks {
                eq('id', seqTrack.id)
            }
        }
        mergingWorkPackages.each {
            if (AbstractBamFile.countByWorkPackage(it)) {
                it.seqTracks.remove(seqTrack)
                it.save(flush: true, validate: false)
            } else {
                SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(it, it)*.delete(flush: true)
                it.delete(flush: true)
            }
        }

        return dirsToDelete
    }

    /**
     * Whether the given artefact is one this method deletes: a processing result (analysis or bam file). Metadata-only
     * graph entries have no concrete object, and any other concrete type is left untouched here -- e.g. FastQC and raw
     * sequence files are removed separately by deleteSeqTrack via deleteRawSequenceFile.
     */
    private static boolean isDeletableArtefact(Artefact artefact) {
        // getClassWithoutInitializingProxy resolves the artefact's real domain class. It is used for every type check
        // and class-name lookup in this service, because instanceof and artefact.class both misreport a Hibernate
        // proxy's subtype and would otherwise misclassify a proxied artefact.
        Class<?> artefactClass = getClassWithoutInitializingProxy(artefact)
        return BamFilePairAnalysis.isAssignableFrom(artefactClass) ||
                RoddyBamFile.isAssignableFrom(artefactClass) ||
                SingleCellBamFile.isAssignableFrom(artefactClass)
    }

    /**
     * Batch-fetches the output {@link WorkflowArtefact}s of the given runs in one query per chunk, instead of the
     * N+1 pattern of reading each run's {@code outputArtefacts} (itself a per-run query) inside a loop.
     */
    @CompileDynamic
    private static List<WorkflowArtefact> findOutputArtefactsOf(List<WorkflowRun> runs) {
        return runs.collate(ARTEFACT_QUERY_CHUNK_SIZE).collectMany { List<WorkflowRun> chunk ->
            WorkflowArtefact.findAllByProducedByInList(chunk)
        }
    }

    /**
     * Whether the graph traversal reached every processing result the legacy queries found. False for legacy data
     * whose {@code workflowArtefact} is null: the result exists but cannot be deleted in graph order.
     */
    @CompileDynamic
    private static boolean graphCoversVerificationSet(List<Artefact> verificationSet, List<ArtefactDeletionEntry> deletableEntries) {
        Set<String> reachableKeys = deletableEntries.collect { artefactKey(it.concreteArtefact) } as Set<String>
        return verificationSet.every { reachableKeys.contains(artefactKey(it)) }
    }

    /**
     * Deletes the given concrete processing results: analyses first (they consume bam files), then orphaned
     * SamplePairs, then the bam files, so a bam file's MergingWorkPackage can be removed without a SamplePair or
     * analysis still referencing it. Shared by the graph-ordered path and the legacy fallback.
     */
    @CompileDynamic
    private List<File> deleteConcreteArtefacts(List<Artefact> analyses, List<Artefact> bamFiles) {
        List<File> dirsToDelete = []
        analyses.each { Artefact analysis ->
            dirsToDelete.addAll(deleteConcreteArtefact(analysis))
        }
        List<SamplePair> samplePairs = bamFiles.findAll { RoddyBamFile.isAssignableFrom(getClassWithoutInitializingProxy(it)) }.collectMany {
            SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(it.workPackage, it.workPackage)
        }.unique { it.id }
        dirsToDelete.addAll(analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs))
        bamFiles.each { Artefact bamFile ->
            dirsToDelete.addAll(deleteConcreteArtefact(bamFile))
        }
        return dirsToDelete
    }

    /**
     * Deletes the concrete objects of the given entries in graph order (consumers before producers -- analyses always
     * precede the bam files they consume), then cleans the now-orphaned workflow graph metadata for exactly the
     * deleted artefacts.
     */
    @CompileDynamic
    private List<File> deleteProcessingResultsInGraphOrder(List<ArtefactDeletionEntry> deletableEntries) {
        List<ArtefactDeletionEntry> analysisEntries = deletableEntries.findAll {
            BamFilePairAnalysis.isAssignableFrom(getClassWithoutInitializingProxy(it.concreteArtefact))
        }
        List<ArtefactDeletionEntry> bamEntries = deletableEntries - analysisEntries
        List<File> dirsToDelete = deleteConcreteArtefacts(analysisEntries*.concreteArtefact, bamEntries*.concreteArtefact)

        // deleteWorkflowRun recurses into downstream consumers, so skip runs a previous call already removed.
        deletableEntries.collect { it.workflowArtefact.producedBy }.findAll { it }.unique { it.id }.each { WorkflowRun run ->
            if (WorkflowRun.exists(run.id)) {
                workflowDeletionService.deleteWorkflowRun(run)
            }
        }
        return dirsToDelete
    }

    /**
     * Aborts if deleting the graph metadata of the deletable artefacts would also remove a retained artefact (one this
     * method must not delete, e.g. FastQC). {@link WorkflowDeletionService#deleteWorkflowRun} recurses from each
     * producing run into co-outputs and downstream consumers; if any reached artefact still has a concrete object that
     * is not deletable, that recursion would trip the assertion in {@code deleteWorkflowArtefact}. This replays that
     * reachability on the already-traversed graph, without further database queries.
     */
    @CompileDynamic
    private static void assertGraphDeletionTouchesNoRetainedArtefact(ArtefactGraph graph, List<ArtefactDeletionEntry> deletableEntries) {
        Set<Long> deletedRunIds = deletableEntries.collect { it.workflowArtefact.producedBy?.id }.findAll { it } as Set<Long>
        Map<Long, ArtefactDeletionEntry> entryByArtefactId = graph.entriesInDeletionOrder.collectEntries {
            [(it.workflowArtefact.id): it]
        }

        // Seed with all outputs of the runs whose metadata will be deleted, then follow successor edges downstream.
        Set<Long> touchedArtefactIds = graph.entriesInDeletionOrder.findAll {
            it.workflowArtefact.producedBy?.id in deletedRunIds
        }*.workflowArtefact*.id as Set<Long>
        List<Long> frontier = touchedArtefactIds as List<Long>
        while (frontier) {
            frontier = frontier.collectMany { Long artefactId ->
                graph.successorArtefactIdsByArtefactId[artefactId] ?: []
            }.findAll { Long successorId ->
                touchedArtefactIds.add(successorId)
            }
        }

        List<Artefact> retained = touchedArtefactIds.collect { entryByArtefactId[it].concreteArtefact }.findAll {
            it && !isDeletableArtefact(it)
        }
        if (retained) {
            throw new NotSupportedException("Deleting this seqTrack's processing results would also remove retained " +
                    "artefacts still present in the workflow graph: ${retained.unique { it.id }.join(', ')}")
        }
    }

    @CompileDynamic
    private static String artefactKey(Artefact artefact) {
        // Keys the artefact families this method deletes, for the coverage cross-check only. The type prefixes the id
        // because ids are unique per table, so a RoddyBamFile and a SingleCellBamFile (or an analysis) sharing an id
        // must not collide.
        Class<?> artefactClass = getClassWithoutInitializingProxy(artefact)
        if (BamFilePairAnalysis.isAssignableFrom(artefactClass)) {
            return "analysis:${artefact.id}"
        }
        if (RoddyBamFile.isAssignableFrom(artefactClass)) {
            return "roddyBam:${artefact.id}"
        }
        if (SingleCellBamFile.isAssignableFrom(artefactClass)) {
            return "singleCellBam:${artefact.id}"
        }
        throw new NotSupportedException("Cannot key artefact of unsupported type: ${artefact}")
    }

    @CompileDynamic
    private List<File> deleteConcreteArtefact(Artefact artefact) {
        Class<?> artefactClass = getClassWithoutInitializingProxy(artefact)
        if (BamFilePairAnalysis.isAssignableFrom(artefactClass)) {
            List<File> dirs = analysisDeletionService.deleteInstance(artefact).collect { new File(it.toString()) }
            deleteProcessParametersForArtefact(artefact)
            return dirs
        }
        if (RoddyBamFile.isAssignableFrom(artefactClass)) {
            deleteProcessParametersForArtefact(artefact)
            return deleteRoddyBamFile(artefact)
        }
        if (SingleCellBamFile.isAssignableFrom(artefactClass)) {
            deleteProcessParametersForArtefact(artefact)
            return deleteSingleCellBamFile(artefact)
        }
        throw new NotSupportedException("Cannot delete artefact of unsupported type: ${artefact}")
    }

    @CompileDynamic
    private void deleteProcessParametersForArtefact(Artefact artefact) {
        deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(artefact.id.toString(), getClassWithoutInitializingProxy(artefact).name))
    }

    @CompileDynamic
    private List<File> deleteRoddyBamFile(RoddyBamFile bamFile) {
        List<File> dirs = []
        MergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = null
        mergingWorkPackage.save(flush: true, validate: false) // since object is deleted later, no validation is necessary
        deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
        Path baseDir = panCancerLinkFileService.getDirectoryPath(bamFile)
        if (Files.exists(baseDir) && bamFile.isMostRecentBamFile()) {
            Files.list(baseDir).findAll {
                it.fileName.toString() != ExternallyProcessedBamFile.NON_OTP
            }.each {
                dirs << new File(it.toString())
            }
        }
        if (bamFile.workflowArtefact?.producedBy?.workFolder) {
            Path workFolder = filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
            if (Files.exists(workFolder)) {
                dirs << new File(workFolder.toString())
            }
        }
        bamFile.delete(flush: true)
        // The MergingWorkPackage can only be deleted if all corresponding RoddyBamFiles are removed already
        if (!RoddyBamFile.findAllByWorkPackage(mergingWorkPackage)) {
            mergingWorkPackage.delete(flush: true)
        }
        return dirs
    }

    @CompileDynamic
    private List<File> deleteSingleCellBamFile(SingleCellBamFile bamFile) {
        List<File> dirs = []
        CellRangerMergingWorkPackage crmwp = bamFile.mergingWorkPackage
        crmwp.bamFileInProjectFolder = null
        crmwp.save(flush: true, validate: false)
        deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
        Path baseDirectory = abstractBamFileService.getBaseDirectory(bamFile)
        if (Files.exists(baseDirectory)) {
            Files.list(baseDirectory).findAll {
                it.fileName.toString() != ExternallyProcessedBamFile.NON_OTP
            }.each {
                dirs << new File(it.toString())
            }
        }
        if (bamFile.workflowArtefact?.producedBy?.workFolder) {
            Path workFolder = filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
            if (Files.exists(workFolder)) {
                dirs << new File(workFolder.toString())
            }
        }
        bamFile.delete(flush: true)
        if (!SingleCellBamFile.findAllByWorkPackage(crmwp)) {
            crmwp.delete(flush: true)
        }
        return dirs
    }

    /**
     * Walks the workflow graph downstream of the given SeqTrack's artefact and returns one entry
     * per reachable artefact, ordered so that consumers appear before the producers they consume
     * (e.g. an analysis before the bam file it was computed from). Entries are deduped by
     * artefact. The SeqTrack's own artefact is never included. Returns an empty list if the
     * SeqTrack has no workflow artefact.
     */
    @CompileDynamic
    List<ArtefactDeletionEntry> collectArtefactsInDeletionOrder(SeqTrack seqTrack) {
        return collectArtefactGraph(seqTrack).entriesInDeletionOrder
    }

    /**
     * Same traversal as {@link #collectArtefactsInDeletionOrder}, additionally exposing the successor edges
     * (consumed artefact to the outputs of its consuming runs) so callers can replay run-deletion reachability
     * in memory.
     */
    @CompileDynamic
    private ArtefactGraph collectArtefactGraph(SeqTrack seqTrack) {
        WorkflowArtefact rootArtefact = seqTrack.workflowArtefact
        if (!rootArtefact) {
            return new ArtefactGraph([], [:])
        }

        Map<Long, WorkflowArtefact> discoveredArtefactsById = [:]
        Map<Long, List<Long>> successorIdsByArtefactId = [:]
        Set<Long> visitedArtefactIds = [rootArtefact.id] as Set
        List<WorkflowArtefact> frontier = [rootArtefact]

        while (frontier) {
            List<WorkflowArtefact> nextFrontier = []
            frontier.collate(ARTEFACT_QUERY_CHUNK_SIZE).each { List<WorkflowArtefact> chunk ->
                List<WorkflowRunInputArtefact> inputArtefacts = WorkflowRunInputArtefact.findAllByWorkflowArtefactInList(chunk)
                List<WorkflowRun> consumingRuns = inputArtefacts*.workflowRun.unique { it.id }
                Map<Long, List<WorkflowArtefact>> producedArtefactsByRunId = findOutputArtefactsOf(consumingRuns).groupBy { it.producedBy.id }

                inputArtefacts.each { WorkflowRunInputArtefact inputArtefact ->
                    Long consumedArtefactId = inputArtefact.workflowArtefact.id
                    (producedArtefactsByRunId[inputArtefact.workflowRun.id] ?: []).each { WorkflowArtefact producedArtefact ->
                        successorIdsByArtefactId.computeIfAbsent(consumedArtefactId) { [] } << producedArtefact.id
                        if (visitedArtefactIds.add(producedArtefact.id)) {
                            discoveredArtefactsById[producedArtefact.id] = producedArtefact
                            nextFrontier << producedArtefact
                        }
                    }
                }
            }
            frontier = nextFrontier
        }

        // Resolve all concrete artefacts in bulk: WorkflowArtefact.getArtefact() runs one polymorphic query per
        // call, which would otherwise fire once per discovered artefact inside the emit recursion below.
        Map<Long, Artefact> concreteArtefactsByWorkflowArtefactId = discoveredArtefactsById.values()
                .collate(ARTEFACT_QUERY_CHUNK_SIZE)
                .collectMany { List<WorkflowArtefact> chunk ->
                    WorkflowArtefact.executeQuery(
                            "FROM de.dkfz.tbi.otp.workflowExecution.Artefact WHERE workflowArtefact IN (:chunk)", [chunk: chunk])
                }
                .collectEntries { [(it.workflowArtefact.id): it] }

        List<ArtefactDeletionEntry> orderedEntries = []
        Set<Long> emittedArtefactIds = [] as Set
        Closure emitDescendantsFirst
        emitDescendantsFirst = { Long artefactId ->
            (successorIdsByArtefactId[artefactId] ?: []).each { Long successorId ->
                if (emittedArtefactIds.add(successorId)) {
                    emitDescendantsFirst(successorId)
                    orderedEntries << new ArtefactDeletionEntry(discoveredArtefactsById[successorId], concreteArtefactsByWorkflowArtefactId[successorId])
                }
            }
        }
        emitDescendantsFirst(rootArtefact.id)

        return new ArtefactGraph(orderedEntries, successorIdsByArtefactId)
    }

    @CompileDynamic
    void deleteProcessParameters(List<ProcessParameter> processParameters) {
        Set<ProcessParameter> processParametersSet = collectProcessParametersRecursively([] as Set<ProcessParameter>, processParameters)
        Set<Process> processSet = processParametersSet*.process
        processParametersSet.each {
            it.delete(flush: true)
        }
        // delete all associations between processes
        // processes can only be safely deleted if no associations between them left
        processSet.each {
            it.restarted = null
            it.save(flush: true, validate: false)
        }.each {
            deleteProcess(it)
        }
    }

    @CompileDynamic
    private void deleteProjectDependencies(Project project) {
        // Deletes the connection of the project to the reference genome
        ReferenceGenomeProjectSeqType.findAllByProject(project)*.delete(flush: true)

        MergingCriteria.findAllByProject(project)*.delete(flush: true)

        SampleTypePerProject.findAllByProject(project)*.delete(flush: true)

        List configPerProjectAndSeqTypes = ConfigPerProjectAndSeqType.findAllByProject(project)
        configPerProjectAndSeqTypes*.previousConfig = null
        configPerProjectAndSeqTypes*.delete(flush: true)

        UserProjectRole.findAllByProject(project)*.delete(flush: true)
        QcThreshold.findAllByProject(project)*.delete(flush: true)
        ProjectInfo.findAllByProject(project)*.delete(flush: true)

        DataTransferAgreement.findAllByProject(project)*.delete(flush: true)
    }

    @CompileDynamic
    private void deleteProcess(Process process) {
        assert process.finished: "process with id ${process.id} not finished"
        assert !ProcessParameter.findAllByProcess(process): "process with id ${process.id} has ProcessParameter attached to it. Delete association first."

        deleteProcessingSteps(ProcessingStep.findAllByProcess(process))
        process.delete(flush: true)
    }

    private void deleteProcessingSteps(List<ProcessingStep> processingSteps) {
        processingSteps*.next = null
        processingSteps.sort { -it.id }
        processingSteps.each {
            deleteProcessingStep(it)
        }
    }

    @CompileDynamic
    private void deleteProcessingStep(ProcessingStep processingStep) {
        deleteClusterJobs(ClusterJob.findAllByProcessingStep(processingStep))
        deleteProcessingStepUpdates(ProcessingStepUpdate.findAllByProcessingStep(processingStep))
        processingStep.delete(flush: true)
    }

    @CompileDynamic
    private void deleteClusterJobs(List<ClusterJob> clusterJobs) {
        clusterJobs*.dependencies = [] as Set
        clusterJobs.each {
            it.delete(flush: true)
        }
    }

    private void deleteProcessingStepUpdates(List<ProcessingStepUpdate> processingStepUpdates) {
        processingStepUpdates*.previous = null
        processingStepUpdates.each {
            deleteProcessingStepUpdate(it)
        }
    }

    @CompileDynamic
    private void deleteProcessingStepUpdate(ProcessingStepUpdate processingStepUpdate) {
        ProcessingError error = processingStepUpdate.error
        if (error) {
            processingStepUpdate.error = null
            error.delete(flush: true)
        }
        processingStepUpdate.delete(flush: true)
    }

    @CompileDynamic
    private Set<ProcessParameter> collectProcessParametersRecursively(
            Set<ProcessParameter> processParametersRecursionSet,
            List<ProcessParameter> processParameters
    ) {
        processParametersRecursionSet.addAll(processParameters)
        processParameters.each {
            Process.findAllByRestarted(it.process).each {
                collectProcessParametersRecursively(processParametersRecursionSet, ProcessParameter.findAllByProcess(it)).each {
                    processParametersRecursionSet << it
                }
            }
        }
        return processParametersRecursionSet
    }

    /**
     * Deletes one SeqTrack from the DB
     *
     * !! Be aware that the run information are not deleted.
     * There is always more than one seqTrack which belongs to one Run, which is why the run is not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    @CompileDynamic
    List<File> deleteSeqTrack(SeqTrack seqTrack, boolean check = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteSeqTrack is null")
        assert seqTrack.project.state != Project.State.ARCHIVED

        if (check) {
            seqTrackService.throwExceptionInCaseOfExternallyProcessedBamFileIsAttached([seqTrack])
            seqTrackService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }

        // keep ilse reference for later deletion
        IlseSubmission ilseSubmission = seqTrack.ilseSubmission

        List<File> dirsToDelete = []
        List<File> seqTrackDelete = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, check)
        dirsToDelete.addAll(seqTrackDelete)

        List<RawSequenceFile> rawSequenceFiles = seqTrack.sequenceFiles

        RawSequenceFile.findAllBySeqTrack(seqTrack).each { RawSequenceFile df ->
            dirsToDelete.addAll(deleteRawSequenceFile(df))
        }

        Sample sample1 = seqTrack.sample
        SeqType seqType = seqTrack.seqType
        Individual individual = seqTrack.individual
        AntibodyTarget antibodyTarget = seqTrack.antibodyTarget

        if (SeqTrack.exists(seqTrack.id)) {
            seqTrack.delete(flush: true)
        }

        List<SeqTrack> leftOverSeqTracks
        if (seqType.hasAntibodyTarget) {
            leftOverSeqTracks = SeqTrack.findAllBySampleAndAntibodyTarget(sample1, antibodyTarget)
        } else {
            leftOverSeqTracks = SeqTrack.findAllBySample(sample1)
        }
        List<ExternallyProcessedBamFile> leftOverBamFiles = ExternallyProcessedBamFile.withCriteria {
            'workPackage' {
                eq('sample', sample1)
                eq('seqType', seqType)
            }
        } as List<ExternallyProcessedBamFile>

        if (!leftOverSeqTracks && !leftOverBamFiles) {
            rawSequenceFiles.collect {
                dirsToDelete.add(fileService.toFile(rawSequenceDataViewFileService.getSampleTypeDirectoryPath(it)))
            }
            if (!SeqTrack.findAllBySample(sample1)) {
                deleteSample(sample1)
            }
        } else {
            List<SeqTrack> seqTrackSampleList = SeqTrack.createCriteria().list {
                eq('sample', sample1)
                eq('seqType', seqType)
                if (seqType.hasAntibodyTarget) {
                    eq('antibodyTarget', antibodyTarget)
                }
            } as List<SeqTrack>
            if (seqTrackSampleList.empty) {
                rawSequenceFiles.collect {
                    dirsToDelete.add(fileService.toFile(rawSequenceDataViewFileService.getSampleTypeDirectoryPath(it)))
                }
            }
        }

        List<SeqTrack> seqTrackIndividualList = SeqTrack.createCriteria().list {
            sample {
                eq('individual', individual)
            }
            eq('seqType', seqType)
        } as List<SeqTrack>
        if (seqTrackIndividualList.empty) {
            dirsToDelete.add(fileService.toFile(individualService.getViewByPidPath(individual, seqType)))
            if (!leftOverBamFiles && !SeqTrack.createCriteria().list {
                sample {
                    eq('individual', individual)
                }
            }) {
                if (Individual.exists(individual.id)) {
                    deleteClusterJobs(ClusterJob.findAllByIndividual(individual))
                    Sample.findAllByIndividual(individual).each { deleteSample(it) }
                    individual.delete(flush: true)
                }
            }
        }

        // delete ilseSubmission if it is not used by other seqTracks and is not blacklisted
        if (ilseSubmission && !ilseSubmission.warning && SeqTrack.countByIlseSubmission(ilseSubmission) == 0) {
            ilseSubmission.delete(flush: true)
        }

        if (runService.isRunEmpty(seqTrack.run)) {
            deleteEmptyRun(seqTrack.run)
        }

        return dirsToDelete
    }

    @CompileDynamic
    private void deleteSample(Sample sample) {
        if (Sample.exists(sample.id)) {
            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.delete(flush: true)
        }
    }

    /**
     * Removes all metadata-entries, which belong to the sequenceFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    @CompileDynamic
    private void deleteMetaDataEntryForRawSequenceFile(RawSequenceFile rawSequenceFile) {
        notNull(rawSequenceFile, "The input dataFiles is null")
        MetaDataEntry.findAllBySequenceFile(rawSequenceFile)*.delete(flush: true)
    }

    /**
     * Removes all QA-Information & the MarkDuplicate-metrics for an AbstractBamFile.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    @CompileDynamic
    private void deleteQualityAssessmentInfoForAbstractBamFile(AbstractBamFile abstractBamFile) {
        notNull(abstractBamFile, "The input AbstractBamFile is null")
        if (abstractBamFile instanceof RoddyBamFile) {
            RoddyQualityAssessment.findAllByAbstractBamFile(abstractBamFile)*.delete(flush: true)
        } else if (abstractBamFile instanceof SingleCellBamFile) {
            CellRangerQualityAssessment.findAllByAbstractBamFile(abstractBamFile)*.delete(flush: true)
        } else {
            throw new NotSupportedException("This BamFile type " + abstractBamFile + " is not supported")
        }
    }

    /**
     * Deletes a sequenceFile and all corresponding information
     */
    @SuppressWarnings('JavaIoPackageAccess')
    @CompileDynamic
    private List<File> deleteRawSequenceFile(RawSequenceFile rawSequenceFile) {
        notNull(rawSequenceFile, "The dataFile input of method deleteDataFile is null")

        String fileFinalPath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        List<File> dirs = [
                fileFinalPath,
                "${fileFinalPath}.md5sum",
                rawSequenceDataViewFileService.getFilePath(rawSequenceFile).toString(),
        ].collect { new File(it) }

        dirs.addAll(deleteFastQCInformationFromRawSequenceFile(rawSequenceFile))
        deleteMetaDataEntryForRawSequenceFile(rawSequenceFile)
        rawSequenceFile.delete(flush: true)
        return dirs
    }

    /**
     * Removes all fastQC information about the sequenceFile
     */
    @SuppressWarnings('JavaIoPackageAccess')
    @CompileDynamic
    private List<File> deleteFastQCInformationFromRawSequenceFile(RawSequenceFile rawSequenceFile) {
        notNull(rawSequenceFile, "The input dataFile is null")
        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile)
        List<File> filesToDelete = []

        if (fastqcProcessedFiles) {
            Path fastqFile = fastqcLinkFileService.fastqcOutputPath(fastqcProcessedFiles.first())
            File folder = new File(fastqFile.parent.toString())

            if (folder.exists()) {
                filesToDelete.add(folder)
            }
            fastqcProcessedFiles*.delete(flush: true)
        }
        return filesToDelete
    }

    /**
     * Either remove project from all dependent external workflow config selectors or delete selectors completely
     * if selectors are only dependent on this one project.
     *
     * @param project which should be remove from the selectors.
     */
    @CompileDynamic
    void deleteProjectsExternalWorkflowConfigSelector(Project project) {
        if (project) {
            ExternalWorkflowConfigSelector.withCriteria {
                projects {
                    eq('id', project.id)
                }
            }.each {
                if (it.projects.size() > 1) {
                    it.projects.remove(project)
                } else {
                    it.delete(flush: true)
                }
            }
        }
    }
}

@Canonical
class ArtefactDeletionEntry {
    WorkflowArtefact workflowArtefact
    Artefact concreteArtefact
}

@Canonical
class ArtefactGraph {
    List<ArtefactDeletionEntry> entriesInDeletionOrder
    Map<Long, List<Long>> successorArtefactIdsByArtefactId
}
