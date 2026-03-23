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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*

class CellRangerMergingWorkPackageSpec extends Specification implements CellRangerFactory, WorkflowSystemDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                FastqImportInstance,
                FastqFile,
                Individual,
                Pipeline,
                Project,
                ReferenceGenome,
                ReferenceGenomeIndex,
                ReferenceGenomeProjectSeqType,
                SeqType,
                SingleCellBamFile,
                WorkflowArtefact,
        ]
    }

    void "referenceGenome and referenceGenomeIndex.referenceGenome have to be the same object"() {
        given:
        ReferenceGenome referenceGenome = createReferenceGenome()
        ReferenceGenomeIndex referenceGenomeIndex = createReferenceGenomeIndex()

        when:
        createMergingWorkPackage(referenceGenome: referenceGenome, referenceGenomeIndex: referenceGenomeIndex)

        then:
        ValidationException e = thrown()
        e.message =~ "sync"
    }

    @Unroll
    void "test mutually exclusive validator, fails validation (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        when:
        createMergingWorkPackage(expectedCells: expectedCells, enforcedCells: enforcedCells)

        then:
        ValidationException e = thrown()
        e.message =~ "nand"

        where:
        expectedCells | enforcedCells
        5000          | 5000
    }

    @Unroll
    void "test nand validator, succeeds validation (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        when:
        createMergingWorkPackage(expectedCells: expectedCells, enforcedCells: enforcedCells)

        then:
        noExceptionThrown()

        where:
        expectedCells | enforcedCells
        null          | null
        5000          | null
        null          | 5000
    }

    void "expectedCells and enforcedCells are editable"() {
        given:
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage(expectedCells: null, enforcedCells: null)

        when:
        mwp.expectedCells = 5000
        mwp.enforcedCells = null
        mwp.save(flush: true)

        then:
        CellRangerMergingWorkPackage refreshedMwp = CellRangerMergingWorkPackage.get(mwp.id)
        refreshedMwp.expectedCells == 5000
        refreshedMwp.enforcedCells == null

        when:
        mwp.expectedCells = null
        mwp.enforcedCells = 5000
        mwp.save(flush: true)

        then:
        CellRangerMergingWorkPackage refreshedRefreshedMwp = CellRangerMergingWorkPackage.get(mwp.id)
        refreshedRefreshedMwp.expectedCells == null
        refreshedRefreshedMwp.enforcedCells == 5000

        when:
        mwp.expectedCells = 5000
        mwp.enforcedCells = 5000
        mwp.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message =~ "nand"
    }

    void "test status constraint, only one mwp with status FINAL and same sample, seqType, config and referenceGenomeIndex"() {
        given:
        CellRangerMergingWorkPackage mwp1 = createMergingWorkPackage(expectedCells: 1)
        CellRangerMergingWorkPackage mwp2 = createMergingWorkPackage(
                expectedCells: 2,
                sample: mwp1.sample,
                seqType: mwp1.seqType,
                config: mwp1.config,
                referenceGenomeIndex: mwp1.referenceGenomeIndex,
        )
        createMergingWorkPackage(status: CellRangerMergingWorkPackage.Status.FINAL)
        createMergingWorkPackage(config: mwp1.config, status: CellRangerMergingWorkPackage.Status.FINAL)
        createMergingWorkPackage(referenceGenomeIndex: mwp1.referenceGenomeIndex, status: CellRangerMergingWorkPackage.Status.FINAL)

        when:
        mwp1.status = CellRangerMergingWorkPackage.Status.FINAL
        mwp1.save(flush: true)

        then: 'no error'
        true

        when:
        mwp2.status = CellRangerMergingWorkPackage.Status.FINAL
        mwp2.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message =~ "unique.combination"
    }

    void "getProgramVersion returns config.programVersion when config is set"() {
        given:
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage()

        expect:
        mwp.programVersion == mwp.config.programVersion
    }

    void "getProgramVersion returns workflow version when config is null and artefact chain is complete"() {
        given:
        WorkflowVersion wfVersion = createWorkflowVersion()
        WorkflowArtefact artefact = createWorkflowArtefact(producedBy: createWorkflowRun(workflowVersion: wfVersion))
        SingleCellBamFile bamFile = createBamFile(workflowArtefact: artefact)
        CellRangerMergingWorkPackage mwp = bamFile.workPackage as CellRangerMergingWorkPackage
        mwp.config = null

        expect:
        mwp.programVersion == wfVersion.workflowVersion
    }

    void "getProgramVersion returns null when config is null and bamFileInProjectFolder is null"() {
        given:
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage()
        mwp.config = null

        expect:
        mwp.programVersion == null
    }
}
