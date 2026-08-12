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
package de.dkfz.tbi.otp.workflow.alignment.roddy.rna

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.infrastructure.alignment.RoddyBamFileNames
import de.dkfz.tbi.otp.workflow.alignment.*
import de.dkfz.tbi.otp.workflow.alignment.roddy.RoddyAlignmentCheckQcJob
import de.dkfz.tbi.otp.workflow.alignment.roddy.RoddyAlignmentConditionalFailJob
import de.dkfz.tbi.otp.workflow.alignment.roddy.RoddyAlignmentFinishJob
import de.dkfz.tbi.otp.workflow.alignment.roddy.RoddyAlignmentPrepareJob
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.*

@Component
@Slf4j
class RnaAlignmentWorkflow extends AlignmentWorkflow implements LinearWorkflow {

    public static final String WORKFLOW = "RNA alignment"

    final String userDocumentation = null

    @Override
    List<Class<? extends Job>> getJobList() {
        return [
                SkipForEmptyRawSequenceFileJob,
                AlignmentFragmentJob,
//                RnaAlignmentCheckFragmentKeysJob,
                // will be uncommented after default fragments have been adapted
                // Keep the order, since it is important
//                RnaAlignmentCreateNotificationJob,
                RoddyAlignmentConditionalFailJob,
                AttachUuidJob,
                RoddyAlignmentPrepareJob,
                RnaAlignmentExecuteJob,
                RnaAlignmentValidationJob,
                RnaAlignmentParseJob,
                RoddyCleanupJob,
                RnaAlignmentCleanUpJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                RnaAlignmentLinkJob,
                RoddyAlignmentCheckQcJob,
                RoddyAlignmentFinishJob,
        ]
    }

    @Override
    String buildWorkDirectoryName(AbstractMergingWorkPackage mergingWorkPackage, int identifier) {
        return "${RoddyBamFileNames.WORK_DIR_PREFIX}_${identifier}"
    }

    @Override
    void reconnectDependencies(Artefact artefact, Artefact newArtefact, String role) {
    }
}
