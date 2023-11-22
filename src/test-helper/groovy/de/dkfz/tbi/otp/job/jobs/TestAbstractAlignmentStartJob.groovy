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
package de.dkfz.tbi.otp.job.jobs

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.*

@Component('testAbstractAlignmentStartJob')
@Scope('singleton')
@Slf4j
class TestAbstractAlignmentStartJob extends AbstractAlignmentStartJob {

    JobExecutionPlan jep

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        return jep
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.wholeGenomePairedSeqType]
    }

    @Override
    AbstractBamFile reallyCreateBamFile(MergingWorkPackage mergingWorkPackage, int identifier, Set<SeqTrack> seqTracks, ConfigPerProjectAndSeqType config) {
        return new RoddyBamFile(
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFileService.WORK_DIR_PREFIX}_${identifier}",
                seqTracks: seqTracks,
                config: config,
        )
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForIndividual(mergingWorkPackage.individual, mergingWorkPackage.seqType, mergingWorkPackage.pipeline)
        assert config: "Could not find one RoddyWorkflowConfig for ${mergingWorkPackage.project}, ${mergingWorkPackage.seqType} and ${mergingWorkPackage.pipeline}"
        return config
    }
}
