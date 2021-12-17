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
package de.dkfz.tbi.otp.analysis.pair.roddy

import org.junit.Assume
import spock.lang.Shared

import de.dkfz.tbi.otp.analysis.pair.AbstractBamFilePairAnalysisWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.utils.SessionUtils

import java.nio.file.Path

abstract class AbstractRoddyBamFilePairAnalysisWorkflowTests<Instance extends BamFilePairAnalysis> extends AbstractBamFilePairAnalysisWorkflowTests {

    /**
     * These two options have been added to speedup testing.
     * It is not possible to automatically execute multiple test methods in parallel
     * if they are located in the same class.
     * Therefore you have to create two classes and only activate one of the two tests in each.
     **/
    @Shared
    boolean ignoreRoddyBamFileTest = false

    @Shared
    boolean ignoreExternalBamFileTest = false

    void setupData() {
        createConfig()
    }

    void "testWholeWorkflowWithRoddyBamFile"() {
        given:
        Assume.assumeFalse(ignoreRoddyBamFileTest)
        SessionUtils.withNewSession {
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
        Assume.assumeFalse(ignoreExternalBamFileTest)
        SessionUtils.withNewSession {
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
        SessionUtils.withNewSession {
            Instance createdInstance = BamFilePairAnalysis.listOrderById().last()
            assert createdInstance.processingState == AnalysisProcessingStates.FINISHED
            assert createdInstance.config == config
            assert createdInstance.sampleType1BamFile == bamFileTumor
            assert createdInstance.sampleType2BamFile == bamFileControl

            filesToCheck(createdInstance).flatten().each { Path file ->
                FileService.ensureFileIsReadableAndNotEmpty(file)
            }
            checkAnalysisSpecific(createdInstance)
        }
    }

    abstract List<Path> filesToCheck(Instance instance)

    @SuppressWarnings("UnusedMethodParameter")
    void checkAnalysisSpecific(Instance instance) { }
}
