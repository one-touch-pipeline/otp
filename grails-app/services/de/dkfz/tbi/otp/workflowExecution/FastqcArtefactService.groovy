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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LogUsedTimeUtils
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactDataWithSeqTrack
import de.dkfz.tbi.otp.workflowExecution.decider.fastqc.FastqcArtefactDataWithFastqcProcessedFile

@Slf4j
@CompileStatic
@Transactional
class FastqcArtefactService {

    private static final int INDEX_0 = 0
    private static final int INDEX_1 = 1

    private final static String HQL_FIND_SEQ_TRACKS_FOR_WORKFLOW_ARTEFACTS = """
        select
            new ${FastqcArtefactDataWithSeqTrack.name}(
                wa,
                st,
                version.workflowVersion,
                project,
                seqType,
                individual,
                sampleType,
                sample,
                run
            )
        from
            SeqTrack st
            join st.workflowArtefact wa
            join st.sample sample
            join sample.sampleType sampleType
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            join st.run run
            left outer join wa.producedBy workflowRun
            left outer join workflowRun.workflowVersion version
        where
            wa in (:workflowArtefacts)
            and wa.state <> '${WorkflowArtefact.State.FAILED}'
            and wa.state <> '${WorkflowArtefact.State.SKIPPED}'
            and wa.withdrawnDate is null
            and not exists (
                select
                    1
                from
                    RawSequenceFile df
                where
                    df.seqTrack = st
                    and df.fileWithdrawn = true
            )
        """

    private final static String HQL_FIND_RELATED_FAST_QC_FOR_SEQ_TRACKS = """
        select distinct
            new ${FastqcArtefactDataWithFastqcProcessedFile.name}(
                wa,
                fastqc,
                workflowVersion.workflowVersion,
                project,
                seqType,
                df,
                st
            )
        from
            FastqcProcessedFile fastqc
            join fastqc.workflowArtefact wa
            join fastqc.sequenceFile df
            join df.seqTrack st
            join st.sample sample
            join sample.individual individual
            join individual.project project
            join st.seqType seqType
            left outer join wa.producedBy run
            left outer join run.workflowVersion workflowVersion
        where
            st in (:seqTracks)
            and df.fileWithdrawn = false
            and wa.state <> '${WorkflowArtefact.State.SKIPPED}'
            and wa.withdrawnDate is null
        """

    private final static String HQL_WORKFLOW_VERSION_SELECTOR = """
        select distinct
            selector
        from
            WorkflowVersionSelector selector,
            SeqTrack st
        where
            st in (:seqTracks)
            and selector.project = st.sample.individual.project
            and selector.seqType is null
            and selector.workflowVersion.apiVersion.workflow in (:workflows)
            and selector.deprecationDate is null
        """

    private final static String HQL_FETCH_DATA_FILES = """
        select distinct
            df,
            st
        from
            RawSequenceFile df
            left outer join fetch df.comment
            join df.seqTrack st
        where
            st in (:seqTracks)
        """

    List<FastqcArtefactDataWithSeqTrack> fetchSeqTrackArtefacts(Collection<WorkflowArtefact> workflowArtefacts) {
        return LogUsedTimeUtils.logUsedTime(log, "        fetchSeqTrackArtefacts") {
            if (!workflowArtefacts) {
                return []
            }

            return SeqTrack.executeQuery(HQL_FIND_SEQ_TRACKS_FOR_WORKFLOW_ARTEFACTS, [
                    workflowArtefacts: workflowArtefacts
            ]) as List<FastqcArtefactDataWithSeqTrack>
        }
    }

    List<FastqcArtefactDataWithFastqcProcessedFile> fetchRelatedFastqcArtefactsForSeqTracks(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "        fetchRelatedFastqcArtefactsForSeqTracks") {
            return SeqTrack.executeQuery(HQL_FIND_RELATED_FAST_QC_FOR_SEQ_TRACKS, [
                    seqTracks: seqTracks,
            ]) as List<FastqcArtefactDataWithFastqcProcessedFile>
        }
    }

    List<WorkflowVersionSelector> fetchWorkflowVersionSelectorForSeqTracks(Collection<SeqTrack> seqTracks, Collection<Workflow> workflows) {
        return LogUsedTimeUtils.logUsedTime(log, "        fetchWorkflowVersionSelectorForSeqTracks") {
            if (!seqTracks) {
                return []
            }

            return SeqTrack.executeQuery(HQL_WORKFLOW_VERSION_SELECTOR, [
                    seqTracks: seqTracks,
                    workflows: workflows,
            ]) as List<WorkflowVersionSelector>
        }
    }

    Map<SeqTrack, List<RawSequenceFile>> fetchRawSequenceFiles(Collection<SeqTrack> seqTracks) {
        return LogUsedTimeUtils.logUsedTime(log, "        fetchDataFile") {
            return (SeqTrack.executeQuery(HQL_FETCH_DATA_FILES, [
                    seqTracks: seqTracks,
            ]) as List<List<?>>).groupBy {
                it[INDEX_1] as SeqTrack
            }.collectEntries {
                [(it.key): (it.value.collect {
                    it[INDEX_0] as RawSequenceFile
                })]
            } as Map<SeqTrack, List<RawSequenceFile>>
        }
    }

}
