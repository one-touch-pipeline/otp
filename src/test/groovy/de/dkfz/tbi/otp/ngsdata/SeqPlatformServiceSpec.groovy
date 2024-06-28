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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SeqPlatformServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SequencingKitLabel,
        ]
    }

    SeqPlatformService service = new SeqPlatformService()

    static final String PLATFORM_NAME = "platform_name"

    void "test createNewSeqPlatform, when seq platform name is null, should fail"() {
        when:
        SeqPlatformService.createNewSeqPlatform(null)

        then:
        thrown(AssertionError)
    }

    void "test createNewSeqPlatform, when only name is provided"() {
        when:
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)

        then:
        seqPlatform.name == PLATFORM_NAME
        seqPlatform.seqPlatformModelLabel == null
        seqPlatform.sequencingKitLabel == null
    }

    void "test createNewSeqPlatform, when name and seq platform group are provided"() {
        when:
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)

        then:
        seqPlatform.name == PLATFORM_NAME
        seqPlatform.seqPlatformModelLabel == null
        seqPlatform.sequencingKitLabel == null
    }

    void "test createNewSeqPlatform, when name and seq platform group and seq platform model label are provided"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = DomainFactory.createSeqPlatformModelLabel()

        when:
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel
        )

        then:
        seqPlatform.name == PLATFORM_NAME
        seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        seqPlatform.sequencingKitLabel == null
    }

    void "test createNewSeqPlatform, when everything is provided"() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = createSeqPlatformModelLabel()
        SequencingKitLabel sequencingKitLabel = createSequencingKitLabel()

        when:
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel,
                sequencingKitLabel
        )

        then:
        seqPlatform.name == PLATFORM_NAME
        seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        seqPlatform.sequencingKitLabel == sequencingKitLabel
    }

    void "test changeLegacyState"() {
        given:
        SeqPlatform seqPlatform = createSeqPlatformWithSeqPlatformGroup()
        boolean legacy = true

        when:
        service.changeLegacyState(seqPlatform, legacy)

        then:
        seqPlatform.legacy == legacy
    }

    void "test displayableMetadata"() {
        given:
        List<String> importAlias = ['alias1', 'alias2']
        List<String> seqKitImportAlias = ['seqKitAlias1', 'seqKitAlias2']
        SeqPlatform seqPlatform = createSeqPlatformWithSeqPlatformGroup([
                sequencingKitLabel   : createSequencingKitLabel([
                        importAlias: seqKitImportAlias,
                ]),
                seqPlatformModelLabel: createSeqPlatformModelLabel([
                        importAlias: importAlias,
                ]),
        ])

        expect:
        service.displayableMetadata() == [
                [
                        id                 : seqPlatform.id,
                        name               : seqPlatform.name,
                        modelId            : seqPlatform.seqPlatformModelLabel?.id,
                        model              : seqPlatform.seqPlatformModelLabel?.name,
                        modelImportAliases : importAlias.join(AbstractMetadataFieldsService.MULTILINE_JOIN_STRING),
                        hasModel           : seqPlatform.seqPlatformModelLabel ? true : false,
                        seqKitId           : seqPlatform.sequencingKitLabel?.id,
                        seqKit             : seqPlatform.sequencingKitLabel?.name,
                        seqKitImportAliases: seqKitImportAlias.join(AbstractMetadataFieldsService.MULTILINE_JOIN_STRING),
                        hasSeqKit          : seqPlatform.sequencingKitLabel?.name ? true : false,
                        legacy             : seqPlatform.legacy,
                ]
        ]
    }
}
