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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@Rollback
@Integration
class SeqPlatformServiceTests {

    static final String PLATFORM_NAME = 'Some platform name'

    SeqPlatformService seqPlatformService

    CheckedLogger checkedLogger

    void setupData() {
        checkedLogger = new CheckedLogger()
        LogThreadLocal.threadLog = checkedLogger
    }

    @After
    void tearDown() {
        LogThreadLocal.removeThreadLog()
        checkedLogger.assertAllMessagesConsumed()
        checkedLogger = null
    }


    private static List createDataFor_findForNameAndModelAndSequencingKit() {
        final String OTHER_PLATFORM_NAME = 'Some other platform name'
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel()
        SeqPlatformModelLabel seqPlatformModelLabel2 = DomainFactory.createSeqPlatformModelLabel()
        SequencingKitLabel sequencingKitLabel = DomainFactory.createSequencingKitLabel()
        SequencingKitLabel sequencingKitLabel2 = DomainFactory.createSequencingKitLabel()
        [
                PLATFORM_NAME,
                OTHER_PLATFORM_NAME,
        ].each { name ->
            [
                    seqPlatformModelLabel,
                    seqPlatformModelLabel2,
                    null,
            ].each { model ->
                [
                        sequencingKitLabel,
                        sequencingKitLabel2,
                        null,
                ].each { kit ->
                    DomainFactory.createSeqPlatformWithSeqPlatformGroup([
                            name                 : name,
                            seqPlatformModelLabel: model,
                            sequencingKitLabel   : kit,
                    ])
                }
            }
        }
        return [
                seqPlatformModelLabel,
                sequencingKitLabel,
        ]
    }


    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndModelAndKitGiven() {
        setupData()
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndModelGivenAndKitIsNull() {
        setupData()
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndKitGivenAndModelIsNull() {
        setupData()
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameGivenAndModelAndKitIsNull() {
        setupData()
        createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameInOtherCaseAndModelAndKitGiven() {
        setupData()
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownPlatformNameGiven() {
        setupData()
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit('Some other name', model, kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownModelGiven() {
        setupData()
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, DomainFactory.createSeqPlatformModelLabel(), kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownKitGiven() {
        setupData()
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, DomainFactory.createSequencingKitLabel())
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsNull() {
        setupData()
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit(null, model, kit)
        }
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsEmpty() {
        setupData()
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit('', model, kit)
        }
    }

    @Test
    void testCreateNewSeqPlatform_SeqPlatformExistsAlready_shouldFail() {
        setupData()
        DomainFactory.createSeqPlatformWithSeqPlatformGroup(
                name: PLATFORM_NAME,
                seqPlatformGroups: null,
                seqPlatformModelLabel: null,
                sequencingKitLabel: null,
        )
        TestCase.shouldFail(AssertionError) {
            SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)
        }
    }
}
