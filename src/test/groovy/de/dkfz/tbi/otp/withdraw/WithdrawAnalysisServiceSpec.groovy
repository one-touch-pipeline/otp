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
package de.dkfz.tbi.otp.withdraw

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Paths

class WithdrawAnalysisServiceSpec extends Specification implements ServiceUnitTest<WithdrawAnalysisService>, DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AceseqInstance,
                AceseqQc,
                RawSequenceFile,
                FastqFile,
                IndelCallingInstance,
                IndelQualityControl,
                IndelSampleSwapDetection,
                MergingWorkPackage,
                RoddyBamFile,
                RoddyWorkflowConfig,
                RoddySnvCallingInstance,
                RunYapsaConfig,
                RunYapsaInstance,
                SampleTypePerProject,
                SophiaInstance,
                SophiaQc,
        ]
    }

    @Unroll
    void "collectObjects, when called for #name bamFiles, then return analysis run on the bamFiles"() {
        given:
        List<BamFilePairAnalysis> analyses = createAnalysisList()
        List<AbstractBamFile> bamFiles = firstBamFile ? analyses*.sampleType1BamFile : analyses*.sampleType2BamFile

        when:
        List<BamFilePairAnalysis> result = service.collectObjects(bamFiles)

        then:
        result == analyses

        where:
        name      | firstBamFile
        "disease" | true
        "control" | false
    }

    void "collectPaths, when called for analysis, then return path of each analysis"() {
        given:
        new TestConfigService()

        List<BamFilePairAnalysis> analyses = createAnalysisList()
        createFactoryService(service)

        when:
        List<String> result = service.collectPaths(analyses)

        then:
        result.size() == 5
        result.each {
            assert new File(it).absolute
        }
    }

    void "withdrawObjects, when called for analysis, then mark each as withdrawn"() {
        given:
        List<BamFilePairAnalysis> analyses = createAnalysisList()

        when:
        service.withdrawObjects(analyses)

        then:
        analyses.each {
            assert it.withdrawn
        }
    }

    void "deleteObjects, when called for analysis, then delete each of them"() {
        given:
        List<BamFilePairAnalysis> analyses = createAnalysisList()
        createFactoryService(service)
        service.analysisDeletionService = new AnalysisDeletionService()
        service.analysisDeletionService.bamFileAnalysisServiceFactoryService = service.bamFileAnalysisServiceFactoryService
        service.analysisDeletionService.fileService = new FileService()

        when:
        service.deleteObjects(analyses)

        then:
        analyses.each {
            assert !BamFilePairAnalysis.get(it.id)
        }
    }

    private List<BamFilePairAnalysis> createAnalysisList() {
        return [
                DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(),
                DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(),
                DomainFactory.createSophiaInstanceWithRoddyBamFiles(),
                DomainFactory.createAceseqInstanceWithRoddyBamFiles(),
                DomainFactory.createRunYapsaInstanceWithRoddyBamFiles(),
        ]
    }

    private void createFactoryService(WithdrawAnalysisService service) {
        IndividualService individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> Paths.get("/")
        }
        service.bamFileAnalysisServiceFactoryService = new BamFileAnalysisServiceFactoryService()
        service.bamFileAnalysisServiceFactoryService.aceseqService = new AceseqService(individualService: individualService)
        service.bamFileAnalysisServiceFactoryService.indelCallingService = new IndelCallingService(individualService: individualService)
        service.bamFileAnalysisServiceFactoryService.runYapsaService = new RunYapsaService(individualService: individualService)
        service.bamFileAnalysisServiceFactoryService.snvCallingService = new SnvCallingService(individualService: individualService)
        service.bamFileAnalysisServiceFactoryService.sophiaService = new SophiaService(individualService: individualService)
    }
}
