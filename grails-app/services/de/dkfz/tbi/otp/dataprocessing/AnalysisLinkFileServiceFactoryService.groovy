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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqLinkFileService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelLinkFileService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaLinkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaLinkFileService

@Transactional
class AnalysisLinkFileServiceFactoryService {

    AceseqLinkFileService aceseqLinkFileService
    IndelLinkFileService indelLinkFileService
    RunYapsaLinkFileService runYapsaLinkFileService
    SnvLinkFileService snvLinkFileService
    SophiaLinkFileService sophiaLinkFileService

    private Map<Class<? extends BamFilePairAnalysis>, AbstractAnalysisLinkFileService<? extends BamFilePairAnalysis>> serviceMap

    def <T extends BamFilePairAnalysis> AbstractAnalysisLinkFileService<T> getService(Class<T> clazz) {
        AbstractAnalysisLinkFileService<? extends BamFilePairAnalysis> result = map[clazz]
        if (!result) {
            throw new IllegalArgumentException("No service exists for ${clazz.simpleName}")
        }
        return result
    }

    def <T extends BamFilePairAnalysis> AbstractAnalysisLinkFileService<T> getService(T instance) {
        return getService(instance.class)
    }

    private Map<Class<? extends BamFilePairAnalysis>, AbstractAnalysisLinkFileService<? extends BamFilePairAnalysis>> getMap() {
        serviceMap = serviceMap ?: Collections.unmodifiableMap([
                (AceseqInstance)         : aceseqLinkFileService,
                (IndelCallingInstance)   : indelLinkFileService,
                (RoddySnvCallingInstance): snvLinkFileService,
                (RunYapsaInstance)       : runYapsaLinkFileService,
                (SnvCallingInstance)     : snvLinkFileService,
                (SophiaInstance)         : sophiaLinkFileService,
        ])
        return serviceMap
    }
}
