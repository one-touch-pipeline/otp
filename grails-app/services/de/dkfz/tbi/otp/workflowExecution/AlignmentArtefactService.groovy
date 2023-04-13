/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.decider.ProjectSeqTypeGroup
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactData
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentWorkPackageGroup

@Slf4j
@Transactional
class AlignmentArtefactService {

    static final int INDEX_0 = 0
    static final int INDEX_1 = 1
    static final int INDEX_2 = 2
    static final int INDEX_3 = 3
    static final int INDEX_4 = 4
    static final int INDEX_5 = 5
    static final int INDEX_6 = 6
    static final int INDEX_7 = 7
    static final int INDEX_8 = 8
    static final int INDEX_9 = 9

    final static String HQL_FIND_SEQ_TRACKS_FOR_WORKFLOW_ARTEFACTS = """
        select
            wa,
            st,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatform
        from
            SeqTrack st
            join st.workflowArtefact wa
            join st.sample sample
            left outer join fetch sample.mixedInSpecies
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            join st.run run
            join run.seqPlatform seqPlatform
            left outer join st.antibodyTarget antibodyTarget
            left outer join st.libraryPreparationKit libraryPreparationKit
        where
            wa in (:workflowArtefacts)
            and st.seqType in (:seqTypes)
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
            and not exists (
                select
                    id
                from
                    DataFile df
                where
                    df.seqTrack = st
                    and df.fileWithdrawn = true
            )
        """

    final static String HQL_FIND_FAST_QC_FOR_WORKFLOW_ARTEFACTS = """
        select
            wa,
            fastqc,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatform
        from
            FastqcProcessedFile fastqc
            join fastqc.workflowArtefact wa
            join fetch fastqc.dataFile df
            join df.seqTrack st
            join st.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            join st.run run
            join run.seqPlatform seqPlatform
            left outer join st.antibodyTarget antibodyTarget
            left outer join st.libraryPreparationKit libraryPreparationKit
        where
            wa in (:workflowArtefacts)
            and st.seqType in (:seqTypes)
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
            and df.fileWithdrawn = false
        """

    final static String HQL_FIND_BAM_FILES_FOR_WORKFLOW_ARTEFACTS = """
        select
            wa,
            bamFile,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatformGroup
        from
            AbstractMergedBamFile bamFile
            join bamFile.workflowArtefact wa
            join bamFile.workPackage mergingWorkPackage
            join mergingWorkPackage.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join mergingWorkPackage.seqType seqType
            left outer join mergingWorkPackage.seqPlatformGroup seqPlatformGroup
            left outer join mergingWorkPackage.antibodyTarget antibodyTarget
            left outer join mergingWorkPackage.libraryPreparationKit libraryPreparationKit
        where
            wa in (:workflowArtefacts)
            and mergingWorkPackage.seqType in (:seqTypes)
            and bamFile.withdrawn = false
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
        """

    final static String HQL_FIND_RELATED_SEQ_TRACKS_FOR_SEQ_TRACKS = """
        select distinct
            wa,
            st,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatform
        from
            SeqTrack st
            join st.workflowArtefact wa
            join st.workflowArtefact wa
            join st.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            join st.run run
            join run.seqPlatform seqPlatform
            left outer join st.antibodyTarget antibodyTarget
            left outer join st.libraryPreparationKit libraryPreparationKit,
            SeqTrack st2
        where
            st2 in (:seqTracks)
            and st.sample = st2.sample
            and st.seqType = st2.seqType
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
            and not exists (
                select
                    id
                from
                    DataFile df
                where
                    df.seqTrack = st
                    and df.fileWithdrawn = true
            )
        """

    final static String HQL_FIND_RELATED_FAST_QC_FOR_SEQ_TRACKS = """
        select distinct
            wa,
            fastqc,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatform
        from
            FastqcProcessedFile fastqc
            join fastqc.workflowArtefact wa
            join fetch fastqc.dataFile df
            join fetch df.fastqImportInstance
            join fetch df.fileType
            join df.seqTrack st
            join st.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            join st.run run
            join run.seqPlatform seqPlatform
            left outer join st.antibodyTarget antibodyTarget
            left outer join st.libraryPreparationKit libraryPreparationKit,
            SeqTrack st2
        where
            st2 in (:seqTracks)
            and st.sample = st2.sample
            and st.seqType = st2.seqType
            and df.fileWithdrawn = false
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
        """

    final static String HQL_FIND_RELATED_BAM_FILES_FOR_SEQ_TRACKS = """
        select distinct
            wa,
            bamFile,
            project,
            seqType,
            individual,
            sampleType,
            sample,
            antibodyTarget,
            libraryPreparationKit,
            seqPlatformGroup
        from
            AbstractMergedBamFile bamFile
            join bamFile.workflowArtefact wa
            join bamFile.workPackage mergingWorkPackage
            join mergingWorkPackage.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join mergingWorkPackage.seqType seqType
            left outer join mergingWorkPackage.seqPlatformGroup seqPlatformGroup
            left outer join mergingWorkPackage.antibodyTarget antibodyTarget
            left outer join mergingWorkPackage.libraryPreparationKit libraryPreparationKit,
            SeqTrack st
        where
            st in (:seqTracks)
            and mergingWorkPackage.sample = st.sample
            and mergingWorkPackage.seqType = st.seqType
            and bamFile.withdrawn = false
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.OMITTED}'
            and wa.withdrawnDate is null
            and bamFile.identifier = (
                select
                    max(identifier)
                from
                    AbstractMergedBamFile bamFile2
                where
                    bamFile2.workPackage = bamFile.workPackage
            )
        """

    final static String HQL_WORKFLOW_VERSION_SELECTOR = """
        select distinct
            selector
        from
            WorkflowVersionSelector selector,
            SeqTrack st
        where
            st in (:seqTracks)
            and selector.project = st.sample.individual.project
            and selector.seqType = st.seqType
            and selector.workflowVersion.workflow = :workflow
            and selector.deprecationDate is null
        """

    final static String HQL_REFERENCE_GENOME = """
        select distinct
            selector
        from
            ReferenceGenomeSelector selector
            join fetch selector.referenceGenome referenceGenome
            left outer join fetch referenceGenome.species
            left outer join fetch referenceGenome.speciesWithStrain,
            SeqTrack st
        where
            st in (:seqTracks)
            and selector.project = st.sample.individual.project
            and selector.seqType = st.seqType
            and selector.workflow = :workflow
        """

    final static String HQL_MERGING_CRITERIA = """
        select distinct
            mergingCriteria
        from
            MergingCriteria mergingCriteria,
            SeqTrack st
        where
            st in (:seqTracks)
            and mergingCriteria.project = st.sample.individual.project
            and mergingCriteria.seqType = st.seqType
        """

    final static String HQL_SPECIFIC_SEQ_PLATFORM_GROUP = """
        select distinct
            mergingCriteria.project,
            mergingCriteria.seqType,
            seqPlatform,
            seqPlatformGroup
        from
            SeqPlatformGroup seqPlatformGroup
            join seqPlatformGroup.mergingCriteria mergingCriteria
            join seqPlatformGroup.seqPlatforms seqPlatform,
            SeqTrack st
        where
            st in (:seqTracks)
            and mergingCriteria.project = st.sample.individual.project
            and mergingCriteria.seqType = st.seqType
            and mergingCriteria.useSeqPlatformGroup = '${MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}'
        """

    final static String HQL_DEFAULT_SEQ_PLATFORM_GROUP = """
        select distinct
            seqPlatform,
            seqPlatformGroup
        from
            SeqPlatformGroup seqPlatformGroup
            join seqPlatformGroup.seqPlatforms seqPlatform
            left outer join fetch seqPlatform.seqPlatformModelLabel
            left outer join fetch seqPlatform.sequencingKitLabel
        where
            seqPlatformGroup.mergingCriteria is null
        """

    final static String HQL_FETCH_WORK_PACKAGES = """
        select distinct
            sample,
            seqType,
            antibodyTarget,
            mergingWorkPackage
        from
            MergingWorkPackage mergingWorkPackage
            join mergingWorkPackage.sample sample
            join mergingWorkPackage.seqType seqType
            left outer join mergingWorkPackage.antibodyTarget antibodyTarget,
            SeqTrack st
        where
            st in (:seqTracks)
            and mergingWorkPackage.sample = st.sample
            and mergingWorkPackage.seqType = st.seqType
        """

    final static String HQL_FETCH_DATA_FILES = """
        select distinct
            df,
            st
        from
            DataFile df
            left outer join fetch df.comment
            join df.seqTrack st,
            SeqTrack st2
        where
            st2 in (:seqTracks)
            and st.sample = st2.sample
            and st.seqType = st2.seqType
        """

    List<AlignmentArtefactData<SeqTrack>> fetchSeqTrackArtefacts(Collection<WorkflowArtefact> workflowArtefacts,
                                                                 Collection<SeqType> seqTypes = SeqType.list()) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchSeqTrackArtefacts") {
            return this.<SeqTrack> executeHelper(HQL_FIND_SEQ_TRACKS_FOR_WORKFLOW_ARTEFACTS, workflowArtefacts, seqTypes, false)
        }
    }

    List<AlignmentArtefactData<FastqcProcessedFile>> fetchFastqcProcessedFileArtefacts(Collection<WorkflowArtefact> workflowArtefacts,
                                                                                       Collection<SeqType> seqTypes = SeqType.list()) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchFastqcProcessedFileArtefacts") {
            return this.<FastqcProcessedFile> executeHelper(HQL_FIND_FAST_QC_FOR_WORKFLOW_ARTEFACTS, workflowArtefacts, seqTypes, false)
        }
    }

    List<AlignmentArtefactData<RoddyBamFile>> fetchBamArtefacts(Collection<WorkflowArtefact> workflowArtefacts,
                                                                Collection<SeqType> seqTypes = SeqType.list()) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchBamArtefacts") {
            return this.<RoddyBamFile> executeHelper(HQL_FIND_BAM_FILES_FOR_WORKFLOW_ARTEFACTS, workflowArtefacts, seqTypes, true)
        }
    }

    List<AlignmentArtefactData<SeqTrack>> fetchRelatedSeqTrackArtefactsForSeqTracks(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchRelatedSeqTrackArtefactsForSeqTracks") {
            return this.<SeqTrack> executeHelper(HQL_FIND_RELATED_SEQ_TRACKS_FOR_SEQ_TRACKS, seqTracks, false)
        }
    }

    List<AlignmentArtefactData<FastqcProcessedFile>> fetchRelatedFastqcArtefactsForSeqTracks(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchRelatedFastqcArtefactsForSeqTracks") {
            return this.<FastqcProcessedFile> executeHelper(HQL_FIND_RELATED_FAST_QC_FOR_SEQ_TRACKS, seqTracks, false)
        }
    }

    List<AlignmentArtefactData<RoddyBamFile>> fetchRelatedBamFileArtefactsForSeqTracks(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchRelatedBamFileArtefactsForSeqTracks") {
            return this.<RoddyBamFile> executeHelper(HQL_FIND_RELATED_BAM_FILES_FOR_SEQ_TRACKS, seqTracks, true)
        }
    }

    List<WorkflowVersionSelector> fetchWorkflowVersionSelectorForSeqTracks(Workflow workflow, Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchWorkflowVersionSelectorForSeqTracks") {
            if (!seqTracks) {
                return []
            }

            return AbstractMergedBamFile.executeQuery(HQL_WORKFLOW_VERSION_SELECTOR, [
                    seqTracks: seqTracks,
                    workflow : workflow,
            ]) as List<WorkflowVersionSelector>
        }
    }

    Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>> fetchReferenceGenome(Workflow workflow, Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchReferenceGenome") {
            return (AbstractMergedBamFile.executeQuery(HQL_REFERENCE_GENOME, [
                    seqTracks: seqTracks,
                    workflow : workflow,
            ]) as List<ReferenceGenomeSelector>).groupBy {
                new ProjectSeqTypeGroup(it.project, it.seqType)
            }.collectEntries {
                [(it.key): it.value.collectEntries {
                    [(it.species): it.referenceGenome]
                }]
            } as Map<ProjectSeqTypeGroup, Map<Set<SpeciesWithStrain>, ReferenceGenome>>
        }
    }

    Map<ProjectSeqTypeGroup, MergingCriteria> fetchMergingCriteria(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchMergingCriteria") {
            return (AbstractMergedBamFile.executeQuery(HQL_MERGING_CRITERIA, [
                    seqTracks: seqTracks,
            ]) as List<MergingCriteria>).collectEntries {
                [(new ProjectSeqTypeGroup(it.project, it.seqType)): it]
            } as Map<ProjectSeqTypeGroup, MergingCriteria>
        }
    }

    Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>> fetchSpecificSeqPlatformGroup(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchSeqPlatformGroup") {
            return (AbstractMergedBamFile.executeQuery(HQL_SPECIFIC_SEQ_PLATFORM_GROUP, [
                    seqTracks: seqTracks,
            ]) as List<List<?>>).groupBy {
                new ProjectSeqTypeGroup(it[INDEX_0] as Project, it[INDEX_1] as SeqType)
            }.collectEntries {
                [(it.key): it.value.collectEntries {
                    [(it[INDEX_2] as SeqPlatform): it[INDEX_3] as SeqPlatformGroup]
                }]
            } as Map<ProjectSeqTypeGroup, Map<SeqPlatform, SeqPlatformGroup>>
        }
    }

    Map<SeqPlatform, SeqPlatformGroup> fetchDefaultSeqPlatformGroup() {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchDefaultSeqPlatformGroup") {
            return (AbstractMergedBamFile.executeQuery(HQL_DEFAULT_SEQ_PLATFORM_GROUP) as List<List<?>>).collectEntries {
                [(it[INDEX_0] as SeqPlatform): it[INDEX_1] as SeqPlatformGroup]
            } as Map<SeqPlatform, SeqPlatformGroup>
        }
    }

    Map<AlignmentWorkPackageGroup, MergingWorkPackage> fetchMergingWorkPackage(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchMergingWorkPackage") {
            return (AbstractMergedBamFile.executeQuery(HQL_FETCH_WORK_PACKAGES, [
                    seqTracks: seqTracks,
            ]) as List<List<?>>).collectEntries {
                [(new AlignmentWorkPackageGroup(it[INDEX_0] as Sample, it[INDEX_1] as SeqType, it[INDEX_2] as AntibodyTarget)):
                 it[INDEX_3] as MergingWorkPackage]
            } as Map<AlignmentWorkPackageGroup, MergingWorkPackage>
        }
    }

    Map<SeqTrack, List<DataFile>> fetchDataFiles(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchDataFile") {
            return (AbstractMergedBamFile.executeQuery(HQL_FETCH_DATA_FILES, [
                    seqTracks: seqTracks,
            ]) as List<List<?>>).groupBy {
                it[INDEX_1] as SeqTrack
            }.collectEntries {
                [(it.key): (it.value.collect {
                    it[INDEX_0] as DataFile
                })]
            } as Map<SeqTrack, List<DataFile>>
        }
    }

    private <T extends Artefact> List<AlignmentArtefactData<T>> executeHelper(String hql, Collection<WorkflowArtefact> workflowArtefacts,
                                                                              Collection<SeqType> seqTypes, boolean haveGroup) {
        if (!workflowArtefacts || !seqTypes) {
            return []
        }

        return executeHelper(hql, [
                workflowArtefacts: workflowArtefacts,
                seqTypes         : seqTypes
        ], haveGroup)
    }

    private <T extends Artefact> List<AlignmentArtefactData<T>> executeHelper(String hql, Collection<SeqTrack> seqTracks, boolean haveGroup) {
        if (!seqTracks) {
            return []
        }

        return executeHelper(hql, [
                seqTracks: seqTracks,
        ], haveGroup)
    }

    private <T extends Artefact> List<AlignmentArtefactData<T>> executeHelper(String hql, Map<String, ?> parameters, boolean haveGroup) {
        return SeqTrack.executeQuery(hql, parameters).collect {
            List<?> list = it as List<?>

            new AlignmentArtefactData<T>(
                    list[INDEX_0] as WorkflowArtefact,
                    list[INDEX_1] as T,
                    list[INDEX_2] as Project,
                    list[INDEX_3] as SeqType,
                    list[INDEX_4] as Individual,
                    list[INDEX_5] as SampleType,
                    list[INDEX_6] as Sample,
                    list[INDEX_7] as AntibodyTarget,
                    list[INDEX_8] as LibraryPreparationKit,
                    (haveGroup ? null : list[INDEX_9]) as SeqPlatform,
                    (haveGroup ? list[INDEX_9] : null) as SeqPlatformGroup
            )
        }
    }
}

