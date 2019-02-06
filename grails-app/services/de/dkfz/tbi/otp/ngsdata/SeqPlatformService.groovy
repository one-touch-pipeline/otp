package de.dkfz.tbi.otp.ngsdata

import org.springframework.context.annotation.Scope
import org.springframework.security.access.prepost.PreAuthorize

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Scope("prototype")
class SeqPlatformService {

    SeqPlatformModelLabelService seqPlatformModelLabelService

    SequencingKitLabelService sequencingKitLabelService

    static SeqPlatform findForNameAndModelAndSequencingKit(
            String platformName, SeqPlatformModelLabel seqPlatformModelLabel, SequencingKitLabel sequencingKitLabel) {
        assert platformName
        return SeqPlatform.findByNameIlikeAndSeqPlatformModelLabelAndSequencingKitLabel(platformName, seqPlatformModelLabel, sequencingKitLabel)
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
        return atMostOneElement(SeqPlatform.findAllByNameAndSeqPlatformModelLabelAndSequencingKitLabel(
                seqPlatformName,
                seqPlatformModelLabel,
                sequencingKitLabel)
        )
    }
}
