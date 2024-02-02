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
package de.dkfz.tbi.otp.monitor.alignment

import grails.gorm.transactions.Rollback

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType

@Rollback
class RnaRoddyAlignmentCheckerIntegrationSpec extends AbstractAlignmentCheckerIntegrationSpec implements RoddyRnaFactory {

    @Override
    AbstractAlignmentChecker createAlignmentChecker() {
        return new RnaRoddyAlignmentChecker()
    }

    @Override
    @SuppressWarnings("GetterMethodCouldBeProperty")
    String getWorkflowName() {
        return null
    }

    @Override
    Set<SeqType> getSupportedSeqTypes() {
        return DomainFactory.createRnaAlignableSeqTypes() as Set
    }

    @Override
    Pipeline createPipeLine() {
        return RoddyRnaFactory.super.findOrCreatePipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    @SuppressWarnings("GetterMethodCouldBeProperty")
    String getWorkflowNameForCrosschecking() {
        return null
    }

    @Override
    RoddyBamFile createBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createBamFile([
                workPackage: mergingWorkPackage,
        ] + properties)
    }

    void "workflowName, should return RnaAlignmentWorkflow"() {
        expect:
        'RnaAlignmentWorkflow' == createAlignmentChecker().workflowName
    }

    void "pipeLineName, should return RODDY_RNA_ALIGNMENT"() {
        expect:
        Pipeline.Name.RODDY_RNA_ALIGNMENT == createAlignmentChecker().pipeLineName
    }
}
