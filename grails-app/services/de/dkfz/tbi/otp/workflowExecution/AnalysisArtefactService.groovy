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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.decider.analysis.*

@Slf4j
@Transactional
class AnalysisArtefactService {

    private static final int INDEX_0 = 0
    private static final int INDEX_1 = 1
    private static final int INDEX_2 = 2

    private final static String HQL_FIND_BAM_FILES_FOR_WORKFLOW_ARTEFACTS = """
        select
            new ${AnalysisBamFileArtefactData.name}(
                wa,
                bf,
                project,
                seqType,
                individual,
                sampleType,
                sample,
                wp,
                seqPlatformGroup
            )
        from
            AbstractBamFile bf
            join bf.workflowArtefact wa
            join bf.workPackage wp
            join wp.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join wp.seqType seqType
            left outer join wp.seqPlatformGroup seqPlatformGroup
        where
            wa in (:workflowArtefacts)
            and bf.withdrawn = false
            and wp.seqType in (:seqTypes)
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.SKIPPED}'
            and wa.withdrawnDate is null
            and bf.identifier = (
                select
                    max(identifier)
                from
                    AbstractBamFile bf2
                where
                    bf2.workPackage = bf.workPackage
            )
        """

    private final static String HQL_FIND_RELATED_BAM_FILES_FOR_BAM_FILES = """
        select distinct
            new ${AnalysisBamFileArtefactData.name}(
                wa,
                bf,
                project,
                seqType,
                individual,
                sampleType,
                sample,
                wp,
                seqPlatformGroup
            )
        from
            AbstractBamFile bf
            join bf.workflowArtefact wa
            join bf.workPackage wp
            join wp.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join wp.seqType seqType
            left outer join wp.seqPlatformGroup seqPlatformGroup,
            AbstractBamFile bf2
            join bf2.workPackage wp2
        where
            bf2 in (:bamFiles)
            and bf not in (:bamFiles)
            and wp.seqType = wp2.seqType
            and sample.individual = wp2.sample.individual
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.SKIPPED}'
            and wa.withdrawnDate is null
            and bf.withdrawn = false
            and bf.identifier = (
                select
                    max(bf2.identifier)
                from
                    AbstractBamFile bf2
                where
                    bf2.workPackage = bf.workPackage
            )
        """

    private final static String HQL_FIND_RELATED_ANALYSIS_FOR_BAM_FILES = """
        select distinct
            new ${AnalysisAnalysisArtefactData.name}(
                wa,
                analysis,
                project,
                seqType,
                samplePair,
                individual,
                sampleType1,
                sampleType2,
                sample1,
                sample2,
                bf1,
                bf2
            )
        from
            BamFilePairAnalysis analysis
            join analysis.workflowArtefact wa
            join analysis.samplePair samplePair
            join analysis.sampleType1BamFile bf1
            join analysis.sampleType2BamFile bf2
            join bf1.workPackage wp1
            join bf2.workPackage wp2
            join wp1.sample sample1
            join wp2.sample sample2
            join sample1.sampleType sampleType1
            join sample2.sampleType sampleType2
            join sample1.individual individual
            join individual.project project
            join wp1.seqType seqType
        where
            (
                bf1 in (:bamFiles)
                or bf2 in (:bamFiles)
            )
            and analysis.class = :clazz
            and analysis.withdrawn = false
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.SKIPPED}'
            and wa.withdrawnDate is null
        """

    private final static String HQL_WORKFLOW_VERSION_SELECTOR = """
        select distinct
            selector
        from
            WorkflowVersionSelector selector,
            AbstractBamFile bf
        where
            bf in (:bamFiles)
            and selector.project = bf.workPackage.sample.individual.project
            and selector.seqType = bf.workPackage.seqType
            and selector.workflowVersion.apiVersion.workflow = :workflow
            and selector.deprecationDate is null
        """

    private final static String HQL_FETCH_SAMPLE_PAIRS = """
        select distinct
            mwp1,
            mwp2,
            samplePair
        from
            SamplePair samplePair
            join samplePair.mergingWorkPackage1 mwp1
            join samplePair.mergingWorkPackage2 mwp2,
            AbstractBamFile bf
            join bf.workPackage mwp
        where
            bf in (:bamFiles)
            and (
                mwp1 = mwp
                or mwp2 = mwp
            )
        """

    private final static String HQL_FETCH_SAMPLE_TYPE_PER_PROJECT = """
        select distinct
            project,
            sampleType,
            stpp.category
        from
            SampleTypePerProject stpp
            join stpp.project project
            join stpp.sampleType sampleType,
            AbstractBamFile bf
            join bf.workPackage.sample sample
        where
            bf in (:bamFiles)
            and sampleType = sample.sampleType
            and project = sample.individual.project
        """

    List<AnalysisBamFileArtefactData> fetchBamFileArtefacts(Collection<WorkflowArtefact> workflowArtefacts,
                                                            Collection<SeqType> seqTypes) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchBamFileArtefacts") {
            return this.executeHelperBam(HQL_FIND_BAM_FILES_FOR_WORKFLOW_ARTEFACTS, workflowArtefacts, seqTypes)
        }
    }

    List<AnalysisBamFileArtefactData> fetchRelatedBamFilesArtefactsForBamFiles(Collection<AbstractBamFile> bamFiles) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchRelatedBamFileArtefactsForBamFiles") {
            return this.executeHelperBam(HQL_FIND_RELATED_BAM_FILES_FOR_BAM_FILES, bamFiles)
        }
    }

    List<AnalysisAnalysisArtefactData> fetchRelatedAnalysisArtefactsForBamFiles(
            Collection<AbstractBamFile> bamFiles, Class<? extends BamFilePairAnalysis> clazz) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchRelatedAnalysisArtefactsForBamFile") {
            return this.<BamFilePairAnalysis> executeHelperAnalysis(HQL_FIND_RELATED_ANALYSIS_FOR_BAM_FILES, bamFiles, clazz)
        }
    }

    List<WorkflowVersionSelector> fetchWorkflowVersionSelectorForBamFiles(Workflow workflow, Collection<AbstractBamFile> bamFiles) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchWorkflowVersionSelectorForBamFiles") {
            if (!bamFiles) {
                return []
            }

            return BamFilePairAnalysis.executeQuery(HQL_WORKFLOW_VERSION_SELECTOR, [
                    bamFiles: bamFiles,
                    workflow: workflow,
            ]) as List<WorkflowVersionSelector>
        }
    }

    Map<AnalysisGroup, SamplePair> fetchSamplePairs(Collection<AbstractBamFile> bamFiles) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchSamplePair") {
            if (!bamFiles) {
                return [:]
            }

            return (AbstractBamFile.executeQuery(HQL_FETCH_SAMPLE_PAIRS, [
                    bamFiles: bamFiles,
            ]) as List<List<?>>).collectEntries {
                AnalysisGroup analysisGroup = new AnalysisGroup(it[INDEX_0] as MergingWorkPackage, it[INDEX_1] as MergingWorkPackage)
                [(analysisGroup): it[INDEX_2] as SamplePair]
            } as Map<AnalysisGroup, SamplePair>
        }
    }

    Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> fetchCategoryPerSampleTypeAndProject(Collection<AbstractBamFile> bamFiles) {
        return LogUsedTimeUtils.logUsedTime(log, "          fetchCategoryPerSampleTypeAndProject") {
            if (!bamFiles) {
                return [:]
            }

            return (AbstractBamFile.executeQuery(HQL_FETCH_SAMPLE_TYPE_PER_PROJECT, [
                    bamFiles: bamFiles,
            ]) as List<List<?>>).collectEntries {
                [(new ProjectSampleTypeGroup(it[INDEX_0] as Project, it[INDEX_1] as SampleType)): it[INDEX_2] as SampleTypePerProject.Category]
            } as Map<ProjectSampleTypeGroup, SampleTypePerProject.Category>
        }
    }

    private List<AnalysisBamFileArtefactData> executeHelperBam(String hql, Collection<WorkflowArtefact> workflowArtefacts, Collection<SeqType> seqTypes) {
        if (!workflowArtefacts || !seqTypes) {
            return []
        }

        return executeHelperBam(hql, [
                workflowArtefacts: workflowArtefacts,
                seqTypes         : seqTypes,
        ])
    }

    private List<AnalysisBamFileArtefactData> executeHelperBam(String hql, Collection<AbstractBamFile> bamFiles) {
        if (!bamFiles) {
            return []
        }

        return executeHelperBam(hql, [
                bamFiles: bamFiles,
        ])
    }

    private List<AnalysisBamFileArtefactData> executeHelperBam(String hql, Map<String, ?> parameters) {
        return BamFilePairAnalysis.executeQuery(hql, parameters) as List<AnalysisBamFileArtefactData>
    }

    private <T extends BamFilePairAnalysis> List<AnalysisAnalysisArtefactData<T>> executeHelperAnalysis(
            String hql, Collection<AbstractBamFile> bamFiles, Class<T> clazz) {
        if (!bamFiles) {
            return []
        }
        Map<String, ?> parameters = [
                bamFiles: bamFiles,
                clazz   : clazz.name,
        ]

        return BamFilePairAnalysis.executeQuery(hql, parameters) as List<AnalysisAnalysisArtefactData<T>>
    }
}
