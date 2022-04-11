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
import org.junit.Test

import de.dkfz.tbi.TestCase

@Rollback
@Integration
class SeqPlatformServiceTests {

    static final String PLATFORM_NAME = 'Some platform name'

    SeqPlatformService seqPlatformService

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
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndModelGivenAndKitIsNull() {
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameAndKitGivenAndModelIsNull() {
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameGivenAndModelAndKitIsNull() {
        createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, null, null)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert null == seqPlatform.seqPlatformModelLabel
        assert null == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnSeqplatform_PlatformNameInOtherCaseAndModelAndKitGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, kit)
        assert seqPlatform
        assert PLATFORM_NAME == seqPlatform.name
        assert model == seqPlatform.seqPlatformModelLabel
        assert kit == seqPlatform.sequencingKitLabel
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownPlatformNameGiven() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit('Some other name', model, kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownModelGiven() {
        SequencingKitLabel kit = createDataFor_findForNameAndModelAndSequencingKit()[1] as SequencingKitLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, DomainFactory.createSeqPlatformModelLabel(), kit)
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldReturnNull_UnknownKitGiven() {
        SeqPlatformModelLabel model = createDataFor_findForNameAndModelAndSequencingKit()[0] as SeqPlatformModelLabel

        SeqPlatform seqPlatform = seqPlatformService.findForNameAndModelAndSequencingKit(PLATFORM_NAME, model, DomainFactory.createSequencingKitLabel())
        assert null == seqPlatform
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsNull() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit(null, model, kit)
        }
    }

    @Test
    void test_findForNameAndModelAndSequencingKit_shouldFail_PlatformNameIsEmpty() {
        def (model, kit) = createDataFor_findForNameAndModelAndSequencingKit()

        TestCase.shouldFail(AssertionError) {
            seqPlatformService.findForNameAndModelAndSequencingKit('', model, kit)
        }
    }

    @Test
    void testCreateNewSeqPlatform_SeqPlatformExistsAlready_shouldFail() {
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
