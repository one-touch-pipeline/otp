/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

@ToString(includeNames = true, includePackage = false)
@ManagedEntity
class MergingCriteria implements Entity {

    Project project
    SeqType seqType
    boolean useLibPrepKit = true
    SpecificSeqPlatformGroups useSeqPlatformGroup = SpecificSeqPlatformGroups.USE_OTP_DEFAULT

    enum SpecificSeqPlatformGroups {
        USE_OTP_DEFAULT,
        USE_PROJECT_SEQ_TYPE_SPECIFIC,
        IGNORE_FOR_MERGING,
    }

    static constraints = {
        useLibPrepKit validator: { val, obj ->
            if (obj.seqType.needsBedFile && !val) {
                return "exome"
            }

            Workflow wgbsWorkflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(WgbsWorkflow.WORKFLOW))
            Set<SeqType> supportedSeqTypes = WorkflowVersion.findAllByWorkflow(wgbsWorkflow).collectMany { wv -> wv.supportedSeqTypes }
            if (obj.seqType in supportedSeqTypes && val) {
                return "wgbs"
            }
        }
        project unique: 'seqType'
    }
}
