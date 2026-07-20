/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.notification

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

import java.nio.file.Paths

class WorkflowNotificationContentServiceSpec extends Specification implements DomainFactoryCore, AlignmentPipelineFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                AntibodyTarget,
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                RawSequenceFile,
                FastqFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                ReferenceGenome,
                ReferenceGenomeIndex,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyBamFile,
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
                SingleCellBamFile,
                SoftwareTool,
                SoftwareToolIdentifier,
                ToolName,
        ]
    }

    private WorkflowNotificationContentService service

    void setup() {
        service = new WorkflowNotificationContentService()
    }

    void "buildSampleNotificationText, when rows are empty, returns empty set"() {
        expect:
        service.buildSampleNotificationText([]) == [] as Set
    }

    void "buildSamplePairNotificationText, when rows are empty, returns empty set"() {
        expect:
        service.buildSamplePairNotificationText([]) == [] as Set
    }

    void "buildSampleNotificationText, groups by PID, sample type and seqType with unique and sorted sample names"() {
        given:
        List<SampleNotificationRow> rows = [
                new SampleNotificationRow("pidA", "tumor1", "WGS PAIRED bulk", "sampleName2", 1L, "projectA"),
                new SampleNotificationRow("pidA", "tumor1", "WGS PAIRED bulk", "sampleName1", 1L, "projectA"),
                new SampleNotificationRow("pidA", "tumor1", "WGS PAIRED bulk", "sampleName1", 1L, "projectA"),
                new SampleNotificationRow("pidA", "tumor2", "WGS PAIRED bulk", "sampleName3", 1L, "projectA"),
                new SampleNotificationRow("pidB", "tumor1", "WGS PAIRED bulk", "sampleName4", 2L, "projectB"),
        ]

        Set<String> expected = [
                "pidA tumor1 WGS PAIRED bulk (sampleName1, sampleName2)",
                "pidA tumor2 WGS PAIRED bulk (sampleName3)",
                "pidB tumor1 WGS PAIRED bulk (sampleName4)",
        ] as Set

        expect:
        service.buildSampleNotificationText(rows) == expected
    }

    void "buildSamplePairNotificationText, returns unique and sorted PIDs, with both sample types and seqTypes per sample pair"() {
        given:
        List<SamplePairNotificationRow> rows = [
                new SamplePairNotificationRow("pidB", "tumor", "control", "WGS PAIRED bulk", 2L, "projectB"),
                new SamplePairNotificationRow("pidA", "tumor", "control", "WGS PAIRED bulk", 1L, "projectA"),
                new SamplePairNotificationRow("pidA", "tumor", "control", "WGS PAIRED bulk", 1L, "projectA"),
        ]

        Set<String> expected = [
                "pidA tumor control WGS PAIRED bulk",
                "pidB tumor control WGS PAIRED bulk",
        ] as Set

        expect:
        service.buildSamplePairNotificationText(rows) == expected
    }

    void "getMergingDirectories, when bamFiles is empty, returns empty set"() {
        expect:
        service.getMergingDirectories([]) == [] as Set
    }

    @SuppressWarnings('GStringExpressionWithinString')
    void "getMergingDirectories, builds the merged alignment patterns with PID and sample type as variables"() {
        given:
        TestConfigService configService = new TestConfigService()
        service.projectService = new ProjectService(
                configService: configService,
                fileSystemService: new TestFileSystemService(),
        )
        RoddyBamFile roddyBamFile = RoddyPanCancerFactoryInstance.INSTANCE.createBamFile()

        String expected = Paths.get(configService.rootPath.toString(), roddyBamFile.project.dirName, "sequencing",
                roddyBamFile.seqType.dirName, "view-by-pid", "\${PID}", "\${SAMPLE_TYPE}",
                roddyBamFile.seqType.libraryLayoutDirName, "merged-alignment").toAbsolutePath()

        when:
        Set<String> result = service.getMergingDirectories([roddyBamFile])

        then:
        result == [expected] as Set
    }
}
