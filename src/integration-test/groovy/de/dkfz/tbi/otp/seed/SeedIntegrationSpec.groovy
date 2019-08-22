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
package de.dkfz.tbi.otp.seed

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import seedme.SeedService
import spock.lang.Shared
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
class SeedIntegrationSpec extends Specification {

    SeedService seedService

    @Shared
    GrailsApplication grailsApplication

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

    void cleanupSpec() {
        Realm.withNewSession {
            grailsApplication.domainClasses*.clazz.sort {
                it.simpleName
            }.each {
                Realm.executeUpdate("delete ${it.simpleName} ".toString())
            }
            grailsApplication.domainClasses*.clazz.each {
                assert it.count() == 0
            }
        }
    }
}
