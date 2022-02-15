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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

/**
 * @deprecated class is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
 */
@Deprecated
class SnvConfig extends ConfigPerProjectAndSeqType {

    /**
     * In this String the complete content of the config file is stored.
     * This solution was chosen to be as flexible as possible in case the style of the config file changes.
     */
    @Deprecated
    String configuration

    static constraints = {
        configuration blank: false
        seqType validator: { SeqType seqType, SnvConfig snvConfig ->
            SnvConfig resultSnvConfig =
                    CollectionUtils.atMostOneElement(SnvConfig.findAllBySeqTypeAndProjectAndObsoleteDate(seqType, snvConfig.project, snvConfig.obsoleteDate))
            if (resultSnvConfig && resultSnvConfig.id != snvConfig.id) {
                return 'unique'
            }
        }
        pipeline validator: { pipeline ->
            pipeline?.name == Pipeline.Name.OTP_SNV
        }
    }

    static mapping = {
        configuration type: 'text'
    }
}
