package de.dkfz.tbi.otp.seed

import seedme.SeedService
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.CollectionUtils

class SeedSpec extends Specification {

    SeedService seedService

    void "seed, ensure that all needed pipeline are created"() {
        when:
        seedService.installSeedData('application.seed-Pipeline')
        List<Pipeline.Name> unknownPipelines = Pipeline.Name.values().findAll {
            !it.name().contains('OTP') && CollectionUtils.atMostOneElement(Pipeline.findAllByName(it)) == null
        }

        then:
        unknownPipelines.empty
    }

    void "seed, ensure that all needed seqTypes are created"() {
        when:
        seedService.installSeedData('application.seed-SeqType')

        then:
        SeqTypeService.allAlignableSeqTypes
    }

    void "seed, ensure that all needed acl Roles are created"() {
        when:
        seedService.installSeedData('application.seed-Role')
        List<String> unknownRoles = Role.IMPORTANT_ROLES.findAll {
            CollectionUtils.atMostOneElement(Role.findAllByAuthority(it)) == null
        }

        then:
        unknownRoles.empty
    }

    void "seed, ensure that all needed FileTypes are created"() {
        when:
        seedService.installSeedData('application.seed-FileType')

        FileType fileType = FileType.findByTypeAndSubTypeAndSignature(type, subType, signature)

        then:
        fileType

        where:
        type                   | subType | signature
        FileType.Type.SEQUENCE | 'fastq' | '.fastq'
        FileType.Type.SEQUENCE | 'fastq' | '_fastq'
    }
}
