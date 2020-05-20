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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class RoddySnvCallingInstanceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                AlignmentPass,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingPass,
                MergingSet,
                MergingSetAssignment,
                MergingWorkPackage,
                BamFilePairAnalysis,
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
                FastqImportInstance,
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
                SoftwareTool,
        ]
    }

    SamplePair samplePair
    String samplePairPath

    void setup() {
        samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        samplePairPath = "${samplePair.sampleType1.name}_${samplePair.sampleType2.name}"

        samplePair.metaClass.getSnvSamplePairPath = {
            return new OtpPath(samplePair.project, samplePairPath)
        }
    }

    void "test getSnvInstancePath"() {
        given:
        RoddySnvCallingInstance instance = createSnvCallingInstance()

        when:
        OtpPath snvInstancePath = instance.getInstancePath()

        then:
        instance.project == snvInstancePath.project
        new File(getSnvInstancePathHelper(instance)) == snvInstancePath.relativePath
    }

    void "test getConfigFilePath"() {
        given:
        RoddySnvCallingInstance instance = createSnvCallingInstance()

        when:
        OtpPath configFilePath = instance.getConfigFilePath()

        then:
        instance.project == configFilePath.project
        new File("${getSnvInstancePathHelper(instance)}/config.txt") == configFilePath.relativePath
    }

    private String getSnvInstancePathHelper(RoddySnvCallingInstance instance) {
        return "${samplePairPath}/${instance.instanceName}"
    }

    private RoddySnvCallingInstance createSnvCallingInstance() {
        return DomainFactory.createRoddySnvCallingInstance([
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair        : samplePair,
        ])
    }
}
