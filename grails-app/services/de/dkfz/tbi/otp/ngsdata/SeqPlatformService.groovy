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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class SeqPlatformService {

    SeqPlatformModelLabelService seqPlatformModelLabelService

    SequencingKitLabelService sequencingKitLabelService

    static SeqPlatform findForNameAndModelAndSequencingKit(
            String platformName, SeqPlatformModelLabel seqPlatformModelLabel, SequencingKitLabel sequencingKitLabel) {
        assert platformName
        return CollectionUtils.atMostOneElement(
                SeqPlatform.findAllByNameIlikeAndSeqPlatformModelLabelAndSequencingKitLabel(platformName, seqPlatformModelLabel, sequencingKitLabel))
    }

    static SeqPlatform createNewSeqPlatform(String seqPlatformName,
                                            SeqPlatformModelLabel seqPlatformModelLabel = null,
                                            SequencingKitLabel sequencingKitLabel = null) {
        assert seqPlatformName: "The input seqPlatformName must not be null"

        assert !findForNameAndModelAndSequencingKit(seqPlatformName, seqPlatformModelLabel, sequencingKitLabel):
                "The seqPlatform for this name, model and kit exists already"

        SeqPlatform seqPlatform = new SeqPlatform(
                name: seqPlatformName,
                seqPlatformModelLabel: seqPlatformModelLabel,
                sequencingKitLabel: sequencingKitLabel,
        )
        assert seqPlatform.save(flush: true)
        return seqPlatform
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<Map> displayableMetadata() {
        return SeqPlatform.list().collect {
            [
                    id                 : it.id,
                    name               : it.name,
                    modelId            : it.seqPlatformModelLabel?.id,
                    model              : it.seqPlatformModelLabel?.name,
                    modelImportAliases : it.seqPlatformModelLabel?.importAlias?.sort()?.join(AbstractMetadataFieldsService.MULTILINE_JOIN_STRING),
                    hasModel           : it.seqPlatformModelLabel ? true : false,
                    seqKitId           : it.sequencingKitLabel?.id,
                    seqKit             : it.sequencingKitLabel?.name,
                    seqKitImportAliases: it.sequencingKitLabel?.importAlias?.sort()?.join(AbstractMetadataFieldsService.MULTILINE_JOIN_STRING),
                    hasSeqKit          : it.sequencingKitLabel?.name ? true : false,
                    legacy             : it.legacy,
            ]
        }.sort { "${it.name}, ${it.model}, ${it.seqKit}" }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SeqPlatform createNewSeqPlatform(String seqPlatformName, String seqPlatformModelLabelName, String sequencingKitLabelName) {
        assert seqPlatformName: "the input seqplatformname '${seqPlatformName}' must not be null"
        SeqPlatformModelLabel seqPlatformModelLabel = null
        SequencingKitLabel sequencingKitLabel = null

        if (seqPlatformModelLabelName) {
            seqPlatformModelLabel = seqPlatformModelLabelService.findByNameOrImportAlias(seqPlatformModelLabelName) ?:
                    seqPlatformModelLabelService.create(seqPlatformModelLabelName)
        }
        if (sequencingKitLabelName) {
            sequencingKitLabel = sequencingKitLabelService.findByNameOrImportAlias(sequencingKitLabelName) ?:
                    sequencingKitLabelService.create(sequencingKitLabelName)
        }
        SeqPlatform seqPlatform = createNewSeqPlatform(seqPlatformName, seqPlatformModelLabel, sequencingKitLabel)
        return seqPlatform
    }

    SeqPlatform findSeqPlatform(String seqPlatformName, String seqPlatformModelLabelNameOrAlias, String sequencingKitLabelNameOrAlias) {
        SeqPlatformModelLabel seqPlatformModelLabel = null
        if (seqPlatformModelLabelNameOrAlias) {
            seqPlatformModelLabel = seqPlatformModelLabelService.findByNameOrImportAlias(seqPlatformModelLabelNameOrAlias)
            if (seqPlatformModelLabel == null) {
                return null
            }
        }
        SequencingKitLabel sequencingKitLabel = null
        if (sequencingKitLabelNameOrAlias) {
            sequencingKitLabel = sequencingKitLabelService.findByNameOrImportAlias(sequencingKitLabelNameOrAlias)
            if (sequencingKitLabel == null) {
                return null
            }
        }
        return findForNameAndModelAndSequencingKit(
                seqPlatformName,
                seqPlatformModelLabel,
                sequencingKitLabel
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    SeqPlatform changeLegacyState(SeqPlatform seqPlatform, boolean legacy) {
        seqPlatform.legacy = legacy
        assert seqPlatform.save(flush: true)
        return seqPlatform
    }
}
