package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class SeqPlatform implements Entity {

    String name   // eg. solid, illumina
    SeqPlatformModelLabel seqPlatformModelLabel
    SequencingKitLabel sequencingKitLabel

    String identifierInRunName

    static hasMany = [
            seqPlatformGroups: SeqPlatformGroup,
    ]

    static constraints = {
        name(blank: false, unique: ['seqPlatformModelLabel','sequencingKitLabel'])
        seqPlatformModelLabel(nullable: true)
        sequencingKitLabel(nullable: true)
        identifierInRunName(nullable: true, matches: /^[A-Z]{4}$/)
    }

    String toString() {
        return fullName()
    }

    SeqPlatformGroup getSeqPlatformGroup(ProjectSeqType projectSeqType) {
        if (!projectSeqType) {
            return null
        }

        List<SeqPlatformGroup> seqPlatformGroups = SeqPlatformGroup.withCriteria() {
            seqPlatforms {
                eq("id", this.id)
            }
            if (projectSeqType.useSeqPlatformGroups == ProjectSeqType.SpecificSeqPlatformGroups.USE_OTP_DEFAULT) {
                isNull("projectSeqType")
            } else {
                eq("projectSeqType", projectSeqType)
            }
        }
        return atMostOneElement(seqPlatformGroups)
    }

    String fullName() {
        return [
            name,
            seqPlatformModelLabel?.name,
            sequencingKitLabel?.name ?: 'unknown kit'
            ].findAll().join(' ')
    }

    static mapping = {
        sequencingKitLabel index: "seq_platform_sequencing_kit_label_idx"
        seqPlatformModelLabel index: "seq_platform_seq_platform_model_label_idx"
    }
}
