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
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

class ProcessingPrioritySpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AceseqInstance,
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                DataFile,
                IndelCallingInstance,
                MergingCriteria,
                MergingSetAssignment,
                ProcessedMergedBamFile,
                QualityAssessmentPass,
                QualityAssessmentMergedPass,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RunYapsaConfig,
                RunYapsaInstance,
                SingleCellBamFile,
                SampleTypePerProject,
                SophiaInstance,
        ]
    }

    @Unroll
    void "processingPriority, when priority change in project, then priority of #className should return that value"() {
        given:
        Entity domainObject = createClousure()

        expect:
        ProcessingPriority.NORMAL.priority == domainObject.processingPriority

        when:
        domainObject.project.processingPriority = ProcessingPriority.FAST_TRACK.priority

        then:
        ProcessingPriority.FAST_TRACK.priority == domainObject.processingPriority

        where:
        clazz                       | createClousure
        SeqTrack                    | { DomainFactory.createSeqTrack() }
        // old OTP alignment
        AlignmentPass               | { DomainFactory.createAlignmentPass() }
        MergingPass                 | { DomainFactory.createMergingPass() }
        QualityAssessmentPass       | { DomainFactory.createQualityAssessmentMergedPass() }
        QualityAssessmentMergedPass | { DomainFactory.createQualityAssessmentMergedPass() }
        ProcessedBamFile            | { DomainFactory.createProcessedBamFile() }
        ProcessedMergedBamFile      | { DomainFactory.createProcessedMergedBamFile() }
        // alignment
        RoddyBamFile                | { DomainFactory.createRoddyBamFile() }
        RnaRoddyBamFile             | { AlignmentPipelineFactory.RoddyRnaFactoryInstance.INSTANCE.createBamFile() }
        SingleCellBamFile           | { AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile() }
        // analysis
        RoddySnvCallingInstance     | { DomainFactory.createRoddySnvInstanceWithRoddyBamFiles() }
        IndelCallingInstance        | { DomainFactory.createIndelCallingInstanceWithRoddyBamFiles() }
        SophiaInstance              | { DomainFactory.createSophiaInstanceWithRoddyBamFiles() }
        AceseqInstance              | { DomainFactory.createAceseqInstanceWithRoddyBamFiles() }
        RunYapsaInstance            | { DomainFactory.createRunYapsaInstanceWithRoddyBamFiles() }

        className = clazz.getSimpleName()
    }
}
