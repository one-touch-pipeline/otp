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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractBamFileAnalysisServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AceseqInstance,
                AceseqQc,
                AlignmentPass,
                DataFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingPass,
                MergingSet,
                MergingSetAssignment,
                MergingWorkPackage,
                Pipeline,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                ProcessingOption,
                ProcessingThresholds,
                Project,
                Realm,
                ReferenceGenome,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                Run,
                RunYapsaConfig,
                RunYapsaInstance,
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
                SequencingKitLabel,
                SnvConfig,
                SoftwareTool,
                SophiaInstance,
        ]
    }

    void "tests if the instance path is valid"() {
        given:
        BamFilePairAnalysis instance = newInstance
        service.configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): "/asdf"])
        service.fileSystemService = new TestFileSystemService()

        when:
        Path result = service.getWorkDirectory(instance)

        then:
        result == Paths.get(
                "/asdf", "${instance.project.dirName}/sequencing/${instance.seqType.dirName}/view-by-pid/" +
                "${instance.individual.pid}/${pathPart}/${instance.seqType.libraryLayoutDirName}/" +
                "${instance.sampleType1BamFile.sampleType.dirName}_${instance.sampleType2BamFile.sampleType.dirName}/" +
                "${instance.instanceName}")
    }

    void "test withdraw"() {
        given:
        AbstractBamFileAnalysisService service = new SnvCallingService()
        RoddySnvCallingInstance snvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()

        when:
        service.withdraw(snvCallingInstance)

        then:
        snvCallingInstance.withdrawn
    }

    abstract BamFilePairAnalysis getNewInstance()
    abstract AbstractBamFileAnalysisService getService()
    abstract String getPathPart()
}
