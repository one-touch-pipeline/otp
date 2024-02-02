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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class AbstractAlignmentDeciderIntegrationSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    private AbstractAlignmentDecider decider

    @Deprecated
    final shouldFail = new GroovyTestCase().&shouldFail

    void setupData() {
        TicketService ticketService = new TicketService(
                processingOptionService: new ProcessingOptionService(),
        )

        decider = newDecider()
        decider.ticketService = ticketService
        decider.unalignableSeqTrackEmailCreator = new UnalignableSeqTrackEmailCreator(
                ticketService: ticketService
        )
        decider.unalignableSeqTrackEmailCreator.mailHelperService = Mock(MailHelperService)
        decider.unalignableSeqTrackEmailCreator.ticketService = ticketService
        DomainFactory.createRoddyAlignableSeqTypes()
    }

    void testFindOrSaveWorkPackagesTwice_whenEverythingIsOkay_workPackageShouldContainBothSeqTracks() {
        given:
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: seqTrack.seqType, run: DomainFactory.createRun(seqPlatform: seqTrack.seqPlatform))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeProjectSeqType(project: seqTrack.project, referenceGenome: referenceGenome, seqType: seqTrack.seqType)

        when:
        Collection<MergingWorkPackage> workPackages = decider.findOrSaveWorkPackages(seqTrack, seqTrack.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack))
        decider.findOrSaveWorkPackages(seqTrack2, seqTrack2.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack2))

        then:
        TestCase.assertContainSame(exactlyOneElement(workPackages).seqTracks, [seqTrack, seqTrack2])
    }

    void testDecideAndPrepareForAlignment_noDataFile_shouldReturnEmptyList() {
        given:
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(flush: true)

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        workPackages.empty
    }

    void testDecideAndPrepareForAlignment_whenCanPipelineAlignReturnsFalse_shouldReturnEmptyList() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.seqType = DomainFactory.createSeqType(name: "Invalid")
        seqTrack.save(flush: true)

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        workPackages.empty
    }

    void testEnsureConfigurationIsComplete_whenReferenceGenomeNull_shouldThrowRuntimeException() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        exactlyOneElement(ReferenceGenomeProjectSeqType.list()).delete(flush: true)

        expect:
        shouldFail RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        }
    }

    void testEnsureConfigurationIsComplete_whenLibraryPreparationKitIsMissing_shouldThrowRuntimeException() {
        given:
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createExomeSeqTrack(testData.run)
        seqTrack.libraryPreparationKit = null
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
        seqTrack.save(flush: true)

        RawSequenceFile rawSequenceFile = testData.createDataFile(seqTrack: seqTrack)
        rawSequenceFile.save(flush: true)

        expect:
        shouldFail RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        }
    }

    void testCanPipelineAlign_whenEverythingIsOkay_shouldReturnTrue() {
        given:
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(flush: true)
        DomainFactory.createRoddyWorkflowConfig(project: seqTrack.project, seqType: seqTrack.seqType,
                pipeline: findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT))

        expect:
        decider.canPipelineAlign(seqTrack)
    }

    void testCanPipelineAlign_whenWrongSeqType_shouldReturnFalse() {
        given:
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = DomainFactory.createWholeGenomeSeqType(SequencingReadType.MATE_PAIR)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(flush: true)

        expect:
        !decider.canPipelineAlign(seqTrack)
    }

    void "test isLibraryPreparationKitOrBedFileMissing, with null"() {
        when:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(null)
        then:
        AssertionError e = thrown()
        e.message.contains("The input seqTrack of method hasLibraryPreparationKitAndBedFile is null")
    }

    void "test isLibraryPreparationKitOrBedFileMissing, with normal seqTrack"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        expect:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(seqTrack)
    }

    @SuppressWarnings('SpaceAfterOpeningBrace')
    void "isLibraryPreparationKitOrBedFileMissing, with exome seqTrack"() {
        given:
        LibraryPreparationKit libraryPreparationKit = libraryPreparationKitMethod()
        ReferenceGenome referenceGenome = referenceGenomeMethod()
        SeqTrack seqTrack = DomainFactory.createExomeSeqTrack([
                libraryPreparationKit: libraryPreparationKit,
        ])
        DomainFactory.createReferenceGenomeProjectSeqType([
                referenceGenome: referenceGenome ?: DomainFactory.createReferenceGenome(),
                project        : seqTrack.project,
                seqType        : seqTrack.seqType,
        ])
        DomainFactory.createBedFile([
                libraryPreparationKit: libraryPreparationKit ?: DomainFactory.createLibraryPreparationKit(),
                referenceGenome      : referenceGenome ?: DomainFactory.createReferenceGenome(),
        ])

        expect:
        AbstractAlignmentDecider.hasLibraryPreparationKitAndBedFile(seqTrack) == result

        where:
        libraryPreparationKitMethod                       | referenceGenomeMethod                       || result
        ({ null })                                        | ({ null })                                  || false
        ({ null })                                        | ({ DomainFactory.createReferenceGenome() }) || false
        ({ DomainFactory.createLibraryPreparationKit() }) | ({ null })                                  || false
        ({ DomainFactory.createLibraryPreparationKit() }) | ({ DomainFactory.createReferenceGenome() }) || true
    }

    private AbstractAlignmentDecider newDecider(Map methods = [:]) {
        AbstractAlignmentDecider decider = ([
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign -> },
                getPipeline        : {
                    return findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
                },
        ] + methods) as PanCanAlignmentDecider
        decider.applicationContext = applicationContext
        decider.mailHelperService = applicationContext.mailHelperService
        decider.seqTypeService = new SeqTypeService()
        return decider
    }

    private SeqTrack buildSeqTrack() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(flush: true)
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        RawSequenceFile rawSequenceFile = testData.createDataFile(seqTrack: seqTrack)
        rawSequenceFile.save(flush: true)

        DomainFactory.createRoddyWorkflowConfig(project: seqTrack.project, seqType: seqTrack.seqType,
                pipeline: findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT))
        return seqTrack
    }

    private static Pipeline findOrSaveByNameAndType(Pipeline.Name name, Pipeline.Type type) {
        return CollectionUtils.atMostOneElement(Pipeline.findAllByNameAndType(name, type)) ?: DomainFactory.createPipeline(name, type)
    }
}
