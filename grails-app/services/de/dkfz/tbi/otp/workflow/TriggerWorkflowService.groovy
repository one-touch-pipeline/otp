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
package de.dkfz.tbi.otp.workflow

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflowExecution.ReferenceGenomeSelector
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelector

@Transactional
class TriggerWorkflowService {

    @CompileDynamic
    List<AbstractBamFile> getBamFiles(List<Long> seqTrackIds) {
        if (!seqTrackIds) {
            return []
        }

        return RoddyBamFile.createCriteria().list {
            seqTracks {
                'in'('id', seqTrackIds)
            }
            eq('withdrawn', false)
        } as List<RoddyBamFile>
    }

    @CompileDynamic
    List<SeqTrack> getSeqTracks(Collection<Long> bamFileIds) {
        if (!bamFileIds) {
            return []
        }

        return AbstractBamFile.executeQuery('''
            SELECT DISTINCT bf.seqTracks
            FROM AbstractBamFile bf
            WHERE bf.id IN (:bamFileIds)
        ''', [bamFileIds: bamFileIds]) as List<SeqTrack>
    }

    @CompileDynamic
    List<WorkflowVersionAndReferenceGenomeSelector> getInfo(Collection<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return []
        }

        List<WorkflowVersionSelector> wvs = WorkflowVersionSelector.createCriteria().listDistinct {
            or {
                seqTracks.each { st ->
                    and {
                        eq('project', st.project)
                        eq('seqType', st.seqType)
                    }
                }
            }
            isNull('deprecationDate')
        } as List<WorkflowVersionSelector>
        return wvs.collect {
            new WorkflowVersionAndReferenceGenomeSelector(it,
                    ReferenceGenomeSelector.findAllByProjectAndSeqTypeAndWorkflow(it.project, it.seqType, it.workflowVersion.workflow))
        }
    }
}

@TupleConstructor
class WorkflowVersionAndReferenceGenomeSelector {
    WorkflowVersionSelector workflowVersionSelector
    List<ReferenceGenomeSelector> referenceGenomeSelectors
}
