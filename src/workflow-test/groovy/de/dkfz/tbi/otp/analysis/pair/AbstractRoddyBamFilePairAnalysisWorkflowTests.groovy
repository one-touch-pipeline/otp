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

package de.dkfz.tbi.otp.analysis.pair

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractRoddyBamFilePairAnalysisWorkflowTests<Instance extends BamFilePairAnalysis> extends AbstractBamFilePairAnalysisWorkflowTests {

    void setupData() {
        createConfig()
    }

    void "testWholeWorkflowWithRoddyBamFile"() {
        given:
        Realm.withNewSession {
            setupRoddyBamFile()
            setupData()
        }

        when:
        execute()

        then:
        checkInstance()
    }

    void "testWholeWorkflowWithExternalBamFile"() {
        given:
        Realm.withNewSession {
            setupExternalBamFile()
            setupData()
        }

        when:
        execute()

        then:
        checkInstance()
    }


    @Override
    ReferenceGenome createReferenceGenome() {
        return createAndSetup_Bwa06_1K_ReferenceGenome()
    }

    void checkInstance() {
        Realm.withNewSession {
            Instance createdInstance = BamFilePairAnalysis.listOrderById().last()
            assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
            assert createdInstance.config == config
            assert createdInstance.sampleType1BamFile == bamFileTumor
            assert createdInstance.sampleType2BamFile == bamFileControl

            filesToCheck(createdInstance).flatten().each { File file ->
                FileService.ensureFileIsReadableAndNotEmpty(file.toPath())
            }
            checkAnalysisSpecific(createdInstance)
        }
    }

    abstract List<File> filesToCheck(Instance instance)

    @SuppressWarnings("UnusedMethodParameter")
    void checkAnalysisSpecific(Instance instance) { }

}
