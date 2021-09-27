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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class AbstractAlignmentDeciderIntegrationTests {

    @Autowired
    ApplicationContext applicationContext

    private AbstractAlignmentDecider decider

    final shouldFail = new GroovyTestCase().&shouldFail

    void setupData() {
        decider = newDecider()
        decider.otrsTicketService = new OtrsTicketService()
        decider.processingOptionService = new ProcessingOptionService()
        DomainFactory.createProcessingOptionForNotificationRecipient()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, decider.mailHelperService)
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

    @Test
    void testDecideAndPrepareForAlignment_whenEverythingIsOkay_shouldReturnWorkPackages() {
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        assert exactlyOneElement(workPackages).seqTracks == [seqTrack] as Set<SeqTrack>
    }

    @Test
    void testFindOrSaveWorkPackagesTwice_whenEverythingIsOkay_workPackageShouldContainBothSeqTracks() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: seqTrack.seqType, run: DomainFactory.createRun(seqPlatform: seqTrack.seqPlatform))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeProjectSeqType(project: seqTrack.project, referenceGenome: referenceGenome, seqType: seqTrack.seqType, statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)

        Collection<MergingWorkPackage> workPackages = decider.findOrSaveWorkPackages(seqTrack, seqTrack.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack))
        decider.findOrSaveWorkPackages(seqTrack2, seqTrack2.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack2))

        assert exactlyOneElement(workPackages).seqTracks == [seqTrack, seqTrack2] as Set<SeqTrack>
    }

    @Test
    void testDecideAndPrepareForAlignment_noDataFile_shouldReturnEmptyList() {
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save()

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)
        assert workPackages.empty
    }

    @Test
    void testDecideAndPrepareForAlignment_whenCanPipelineAlignReturnsFalse_shouldReturnEmptyList() {
        setupData()
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.seqType = DomainFactory.createSeqType(name: "Invalid")
        seqTrack.save()

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)
        assert workPackages.empty
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongReferenceGenome_shouldThrowAssertionError() {
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongPipeline_shouldThrowAssertionError() {
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                pipeline: findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT),
        )

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentSeqPlatformGroup_shouldReturnEmptyListAndSendMail() {
        setupData()
        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.dataFiles*.fastqImportInstance = fastqImportInstance
        seqTrack.dataFiles*.save()

        boolean emailIsSent = false

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert subject.contains(prefix)
            assert subject.contains(ticket.ticketNumber)
            assert subject.contains(seqTrack.sample.toString())
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    private List<Entity> prepareDifferentLibraryPreparationKit() {
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.save()

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )
        workPackage.save()

        return [seqTrack, workPackage]
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithTicket() {
        setupData()
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance(otrsTicket: ticket)
        seqTrack.dataFiles*.fastqImportInstance = fastqImportInstance
        seqTrack.dataFiles*.save()

        boolean emailIsSent = false

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert subject.contains(prefix)
            assert subject.contains(ticket.ticketNumber)
            assert subject.contains(seqTrack.sample.toString())
            assert content.contains(seqTrack.libraryPreparationKit.name)
            assert content.contains(workPackage.libraryPreparationKit.name)
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithoutTicket() {
        setupData()
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        boolean emailIsSent = false

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert subject.contains(seqTrack.sample.toString())
            assert content.contains(seqTrack.libraryPreparationKit.name)
            assert content.contains(workPackage.libraryPreparationKit.name)
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    @Test
    void testDecideAndPrepareForAlignment_whenMergingWorkPackageExists_shouldReturnIt() {
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
        workPackage.save()

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        assert exactlyOneElement(workPackages).seqTracks.contains(seqTrack)
        assert exactlyOneElement(workPackages).seqTracks.size() == 1
    }

    @Test
    void testDecideAndPrepareForAlignment_callsEnsureConfigurationIsComplete() {
        setupData()
        SeqTrack st = buildSeqTrack()
        int callCount = 0
        decider = newDecider(
                ensureConfigurationIsComplete: { SeqTrack seqTrack ->
                    assert seqTrack == st
                    callCount++
                }
        )

        decider.decideAndPrepareForAlignment(st, true)

        assert callCount == 1
    }

    @Test
    void testDecideAndPrepareForAlignment_callsPrepareForAlignment() {
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

        assert TestCase.containSame(calledForMergingWorkPackages, decider.decideAndPrepareForAlignment(st, true))
        assert calledForMergingWorkPackages.size() > 0
    }

    @Test
    void testEnsureConfigurationIsComplete_whenReferenceGenomeNull_shouldThrowRuntimeException() {
        setupData()
        SeqTrack seqTrack = buildSeqTrack()

        exactlyOneElement(ReferenceGenomeProjectSeqType.list()).delete()

        shouldFail(RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
    }

    @Test
    void testEnsureConfigurationIsComplete_whenLibraryPreparationKitIsMissing_shouldThrowRuntimeException() {
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createExomeSeqTrack(testData.run)
        seqTrack.libraryPreparationKit = null
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
        seqTrack.save()

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save()

        shouldFail(RuntimeException, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
    }

    @Test
    void testCanPipelineAlign_whenEverythingIsOkay_shouldReturnTrue() {
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save()

        assert decider.canPipelineAlign(seqTrack)
    }

    @Test
    void testCanPipelineAlign_whenWrongSeqType_shouldReturnFalse() {
        setupData()
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = DomainFactory.createWholeGenomeSeqType(SequencingReadType.MATE_PAIR)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save()

        assert !decider.canPipelineAlign(seqTrack)
    }

    private SeqTrack buildSeqTrack() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save()

        return seqTrack
    }

    private void assertSeqTrackProperties(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    private static Pipeline findOrSaveByNameAndType(Pipeline.Name name, Pipeline.Type type) {
        return Pipeline.findByNameAndType(name, type) ?: DomainFactory.createPipeline(name, type)
    }
}
