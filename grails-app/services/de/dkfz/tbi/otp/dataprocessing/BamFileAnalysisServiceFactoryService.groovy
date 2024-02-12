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
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService

@Transactional
class BamFileAnalysisServiceFactoryService {

    AceseqService aceseqService
    IndelCallingService indelCallingService
    RunYapsaService runYapsaService
    SnvCallingService snvCallingService
    SophiaService sophiaService

    private Map<Class<? extends BamFilePairAnalysis>, AbstractBamFileAnalysisService<? extends BamFilePairAnalysis>> serviceMap

    /**
     * @deprecated use {@link AnalysisLinkFileServiceFactoryService#getService()} or {@link AnalysisWorkFileServiceFactoryService#getService()}
     */
    @Deprecated
    AbstractBamFileAnalysisService<? extends BamFilePairAnalysis> getService(Class<? extends BamFilePairAnalysis> clazz) {
        AbstractBamFileAnalysisService<? extends BamFilePairAnalysis> result = map[clazz]
        if (!result) {
            throw new IllegalArgumentException("No service exists for ${clazz.simpleName}")
        }
        return result
    }

    /**
     * @deprecated use {@link AnalysisLinkFileServiceFactoryService#getService()} or {@link AnalysisWorkFileServiceFactoryService#getService()}
     */
    @Deprecated
    AbstractBamFileAnalysisService<? extends BamFilePairAnalysis> getService(BamFilePairAnalysis bfpa) {
        return getService(bfpa.class)
    }

    private Map<Class<? extends BamFilePairAnalysis>, AbstractBamFileAnalysisService<? extends BamFilePairAnalysis>> getMap() {
        serviceMap = serviceMap ?: Collections.unmodifiableMap([
                (AceseqInstance)         : aceseqService,
                (IndelCallingInstance)   : indelCallingService,
                (RoddySnvCallingInstance): snvCallingService,
                (RunYapsaInstance)       : runYapsaService,
                (SnvCallingInstance)     : snvCallingService,
                (SophiaInstance)         : sophiaService,
        ])
        return serviceMap
    }
}
