/*
 * Copyright 2011-2025 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.RnaAlignmentWorkFileService

@Transactional
class RoddyResultWorkFileServiceFactoryService {

    AceseqWorkFileService aceseqWorkFileService
    IndelWorkFileService indelWorkFileService
    PanCancerWorkFileService panCancerWorkFileService
    RnaAlignmentWorkFileService rnaAlignmentWorkFileService
    SnvWorkFileService snvWorkFileService
    SophiaWorkFileService sophiaWorkFileService

    @CompileDynamic
    RoddyResultServiceTrait<? extends RoddyResult> getService(RoddyResult rr) {
        Map<Class<? extends RoddyResult>, RoddyResultServiceTrait<? extends RoddyResult>> map = [
                (AceseqInstance)            : aceseqWorkFileService,
                (IndelCallingInstance)      : indelWorkFileService,
                (RnaRoddyBamFile)           : rnaAlignmentWorkFileService,
                (RoddyBamFile)              : panCancerWorkFileService,
                (RoddySnvCallingInstance)   : snvWorkFileService,
                (SophiaInstance)            : sophiaWorkFileService,
        ]
        RoddyResultServiceTrait<? extends RoddyResult> result = map[rr.class]
        if (!result) {
            throw new IllegalArgumentException("No service exists for ${rr.class.simpleName}")
        }
        return result
    }
}
