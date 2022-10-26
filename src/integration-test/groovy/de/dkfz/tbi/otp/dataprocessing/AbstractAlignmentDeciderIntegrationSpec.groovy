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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class AbstractAlignmentDeciderIntegrationSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    private AbstractAlignmentDecider decider

    final shouldFail = new GroovyTestCase().&shouldFail

    void setupData() {
        decider = newDecider()
        decider.otrsTicketService = new OtrsTicketService()
        decider.unalignableSeqTrackEmailCreator = new UnalignableSeqTrackEmailCreator()
        decider.unalignableSeqTrackEmailCreator.mailHelperService = Mock(MailHelperService)
        decider.unalignableSeqTrackEmailCreator.otrsTicketService = new OtrsTicketService()
    }

    void testDecideAndPrepareForAlignment_whenEverythingIsOkay_shouldReturnWorkPackages() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        exactlyOneElement(workPackages).seqTracks == [seqTrack] as Set<SeqTrack>
    }

    void testFindOrSaveWorkPackagesTwice_whenEverythingIsOkay_workPackageShouldContainBothSeqTracks() {
        given:
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: seqTrack.seqType, run: DomainFactory.createRun(seqPlatform: seqTrack.seqPlatform))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeProjectSeqType(project: seqTrack.project, referenceGenome: referenceGenome, seqType: seqTrack.seqType, statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)

        when:
        Collection<MergingWorkPackage> workPackages = decider.findOrSaveWorkPackages(seqTrack, seqTrack.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack))
        decider.findOrSaveWorkPackages(seqTrack2, seqTrack2.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack2))

        then:
        exactlyOneElement(workPackages).seqTracks == [seqTrack, seqTrack2] as Set<SeqTrack>
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

    void testDecideAndPrepareForAlignment_whenWrongReferenceGenome_shouldThrowAssertionError() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        expect:
        shouldFail(AssertionError, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    void testDecideAndPrepareForAlignment_whenWrongPipeline_shouldThrowAssertionError() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                pipeline: findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT),
        )

        expect:
        shouldFail(AssertionError, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    void testDecideAndPrepareForAlignment_whenDifferentSeqPlatformGroup_shouldReturnEmptyListAndSendMail() {
        given:
        setupData()
        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.dataFiles*.fastqImportInstance = fastqImportInstance
        seqTrack.dataFiles*.save(flush: true)

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        decider.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _) >> { String subject, String content ->
                assert subject.contains(prefix)
                assert subject.contains(ticket.ticketNumber)
                assert subject.contains(seqTrack.sample.toString())
            }
        }

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        workPackages.empty
    }

    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithTicket() {
        given:
        setupData()
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        seqTrack.dataFiles*.fastqImportInstance = fastqImportInstance
        seqTrack.dataFiles*.save(flush: true)

        decider.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _) >> { String subject, String content ->
                assert subject.contains(prefix)
                assert subject.contains(ticket.ticketNumber)
                assert subject.contains(seqTrack.sample.toString())
                assert content.contains(seqTrack.libraryPreparationKit.name)
                assert content.contains(workPackage.libraryPreparationKit.name)
            }
        }

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        workPackages.empty
    }

    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithoutTicket() {
        given:
        setupData()
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        decider.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _) >> { String subject, String content ->
                assert subject.contains(seqTrack.sample.toString())
                assert content.contains(seqTrack.libraryPreparationKit.name)
                assert content.contains(workPackage.libraryPreparationKit.name)
            }
        }

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        workPackages.empty
    }

    void testDecideAndPrepareForAlignment_whenMergingWorkPackageExists_shouldReturnIt() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                pipeline: findOrSaveByNameAndType(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )
        workPackage.save(flush: true)

        when:
        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        then:
        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        exactlyOneElement(workPackages).seqTracks.contains(seqTrack)
        exactlyOneElement(workPackages).seqTracks.size() == 1
    }

    void testDecideAndPrepareForAlignment_callsEnsureConfigurationIsComplete() {
        given:
        setupData()
        SeqTrack st = buildSeqTrack()
        int callCount = 0
        decider = newDecider(
                ensureConfigurationIsComplete: { SeqTrack seqTrack ->
                    assert seqTrack == st
                    callCount++
                }
        )

        when:
        decider.decideAndPrepareForAlignment(st, true)

        then:
        callCount == 1
    }

    void testDecideAndPrepareForAlignment_callsPrepareForAlignment() {
        given:
        setupData()
        SeqTrack st = buildSeqTrack()
        Collection<MergingWorkPackage> calledForMergingWorkPackages = []
        decider = newDecider(
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign ->
                    assert seqTrack == st
                    assert forceRealign == true
                    calledForMergingWorkPackages.add(workPackage)
                }
        )

        expect:
        TestCase.containSame(calledForMergingWorkPackages, decider.decideAndPrepareForAlignment(st, true))
        calledForMergingWorkPackages.size() > 0
    }

    void testEnsureConfigurationIsComplete_whenReferenceGenomeNull_shouldThrowRuntimeException() {
        given:
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        exactlyOneElement(ReferenceGenomeProjectSeqType.list()).delete(flush: true)

        expect:
        shouldFail(RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
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

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(flush: true)

        expect:
        shouldFail(RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
    }

    void testCanPipelineAlign_whenEverythingIsOkay_shouldReturnTrue() {
        given:
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(flush: true)

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
                    return findOrSaveByNameAndType(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
                },
        ] + methods) as AbstractAlignmentDecider
        decider.applicationContext = applicationContext
        decider.mailHelperService = applicationContext.mailHelperService
        return decider
    }

    private List<Entity> prepareDifferentLibraryPreparationKit() {
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.save(flush: true)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )
        workPackage.save(flush: true)

        return [seqTrack, workPackage]
    }

    private SeqTrack buildSeqTrack() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(flush: true)
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(flush: true)

        return seqTrack
    }

    private void assertSeqTrackProperties(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    private static Pipeline findOrSaveByNameAndType(Pipeline.Name name, Pipeline.Type type) {
        return CollectionUtils.atMostOneElement(Pipeline.findAllByNameAndType(name, type)) ?: DomainFactory.createPipeline(name, type)
    }
}
