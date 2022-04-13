/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.sophia

import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.IndividualService

import java.nio.file.Path

class SophiaServiceSpec extends AbstractBamFileAnalysisServiceSpec implements ServiceUnitTest<SophiaService> {

    final String pathPart = 'sv_results'

    @Override
    BamFilePairAnalysis getNewInstance() {
        return DomainFactory.createSophiaInstanceWithRoddyBamFiles()
    }

    @Rule
    TemporaryFolder temporaryFolder

    SophiaInstance instance
    Path instancePath

    /**
     * Creates Temporary File for Data Management path
     * so later on temp files can be generated and paths tested
     */
    void setup() {
        Path temporaryFile = temporaryFolder.newFolder().toPath()

        this.instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        Path vbpPath = temporaryFile.resolve("${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/${instance.individual.pid}")

        service.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> vbpPath
        }

        instancePath = vbpPath.resolve("sv_results/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}")
    }

    void "getFinalAceseqInputFile, tests if path is in a valid form"() {
        given:
        Path expectedPath = instancePath.resolve("svs_${instance.individual.pid}_${SophiaService.SOPHIA_OUTPUT_FILE_SUFFIX}")

        expect:
        service.getFinalAceseqInputFile(instance) == expectedPath
    }
}
