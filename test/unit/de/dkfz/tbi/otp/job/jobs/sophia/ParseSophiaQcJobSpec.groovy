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

package de.dkfz.tbi.otp.job.jobs.sophia

import grails.test.mixin.Mock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService

@Mock([
        AbstractMergedBamFile,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        QcThreshold,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        SophiaInstance,
        SophiaQc
])
class ParseSophiaQcJobSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void "test execute"() {
        given:
        File temporaryFile = temporaryFolder.newFolder()
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFile.path])

        SophiaInstance instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        DomainFactory.createSophiaQcFileOnFileSystem(instance.getQcJsonFile())

        ParseSophiaQcJob job = [
                getProcessParameterObject: { -> instance },
        ] as ParseSophiaQcJob
        job.qcTrafficLightService = new QcTrafficLightService()

        when:
        job.execute()

        then:
        SophiaQc.findAllBySophiaInstance(instance).size() == 1
        SophiaQc qc = SophiaQc.findAllBySophiaInstance(instance).first()
        qc.controlMassiveInvPrefilteringLevel == 0
        qc.tumorMassiveInvFilteringLevel == 0
        qc.rnaContaminatedGenesMoreThanTwoIntron == "PRKRA;ACTG2;TYRO3;COL18A1;"
        qc.rnaContaminatedGenesCount == 4
        qc.rnaDecontaminationApplied == false

        instance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }
}
