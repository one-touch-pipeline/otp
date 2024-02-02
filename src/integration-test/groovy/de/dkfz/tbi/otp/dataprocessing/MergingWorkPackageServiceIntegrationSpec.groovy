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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
class MergingWorkPackageServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    @Unroll
    void "findAllBySampleAndSeqTypeAndAntibodyTarget, when no workPackage exist and #name, then return empty list"() {
        given:
        SeqType seqType = createSeqType([
                hasAntibodyTarget: hasAntibodyTarget,
        ])
        Sample sample = createSample()
        DomainFactory.proxyRoddy.createMergingWorkPackage([seqType: seqType])
        DomainFactory.proxyRoddy.createMergingWorkPackage([sample: sample])
        AntibodyTarget antibodyTarget = hasAntibodyTarget ? createAntibodyTarget() : null

        MergingWorkPackageService service = new MergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.findAllBySampleAndSeqTypeAndAntibodyTarget(
                sample, seqType, antibodyTarget)

        then:
        mergingWorkPackages.empty

        where:
        hasAntibodyTarget | _
        true              | _
        false             | _

        name = "hasAntibodyTarget: ${hasAntibodyTarget}"
    }

    @Unroll
    void "findAllBySampleAndSeqTypeAndAntibodyTarget, when mergingWorkPackage exist and #name, then return the correct one"() {
        given:
        SeqType seqType = createSeqType([
                hasAntibodyTarget: hasAntibodyTarget
        ])
        AbstractMergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackageForPipeline(pipeline, [seqType: seqType])
        DomainFactory.createMergingWorkPackageForPipeline(pipeline, [seqType: seqType])
        DomainFactory.createMergingWorkPackageForPipeline(pipeline, [sample: mergingWorkPackage.sample])

        MergingWorkPackageService service = new MergingWorkPackageService()

        when:
        List<AbstractMergingWorkPackage> mergingWorkPackages = service.findAllBySampleAndSeqTypeAndAntibodyTarget(
                mergingWorkPackage.sample, mergingWorkPackage.seqType, mergingWorkPackage.antibodyTarget)

        then:
        mergingWorkPackages == [mergingWorkPackage]

        where:
        hasAntibodyTarget | pipeline
        true              | Pipeline.Name.PANCAN_ALIGNMENT
        true              | Pipeline.Name.DEFAULT_OTP
        false             | Pipeline.Name.PANCAN_ALIGNMENT
        false             | Pipeline.Name.DEFAULT_OTP

        name = "hasAntibodyTarget: ${hasAntibodyTarget}, pipeline is ${pipeline}"
    }

    @Unroll
    void "getStatusWithProcessingDate, should return #result, when MergingWorkPackage contains BamFile is #containsBamFile"() {
        given:
        MergingWorkPackageService service = new MergingWorkPackageService()
        AbstractMergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage()
        if (containsBamFile) {
            AbstractBamFile bamFile = DomainFactory.proxyRoddy.createBamFile([
                    fileOperationStatus: AbstractBamFile.FileOperationStatus.INPROGRESS,
                    workPackage        : mergingWorkPackage,
            ])
            bamFile.dateCreated = new Date(dateCreated)
            bamFile.save(flush: true)
            mergingWorkPackage.bamFileInProjectFolder = bamFile
            mergingWorkPackage.save(flush: true)
        }

        expect:
        service.getStatusWithProcessingDate(mergingWorkPackage) == result

        where:
        containsBamFile | dateCreated   | result
        true            | 1700000162849 | "INPROGRESS (2023-11-14)"
        false           | null          | "N/A"
    }
}
