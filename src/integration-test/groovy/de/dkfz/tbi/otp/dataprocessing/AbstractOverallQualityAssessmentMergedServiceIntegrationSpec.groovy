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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

@Rollback
@Integration
abstract class AbstractOverallQualityAssessmentMergedServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    AbstractQualityAssessment qualityAssessment

    void setupData() {
        createUserAndRoles()

        qualityAssessment = createQualityAssessment()
        AbstractMergedBamFile abstractBamFile = qualityAssessment.bamFile
        abstractBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        abstractBamFile.md5sum = "12345678901234567890123456789012"
        abstractBamFile.fileSize = 10000
        abstractBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        abstractBamFile.withdrawn = false
        abstractBamFile.workPackage.bamFileInProjectFolder = abstractBamFile
        abstractBamFile.save(flush: true)
    }

    abstract AbstractQualityAssessment createQualityAssessment()

    void "findAllByProjectAndSeqType, when call by user with role #user, then return values"() {
        given:
        setupData()
        List expected = [
                qualityAssessment,
        ]
        List<OverallQualityAssessmentMerged> result

        when:
        SpringSecurityUtils.doWithAuth(user) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        assert expected == result

        where:
        user << [
                ADMIN,
                OPERATOR,
        ]
    }

    void "findAllByProjectAndSeqType, when call by user with project access, then return values"() {
        given:
        setupData()
        List expected = [
                qualityAssessment,
        ]
        List<OverallQualityAssessmentMerged> result
        addUserWithReadAccessToProject(CollectionUtils.atMostOneElement(User.findAllByUsername(TESTUSER)), qualityAssessment.project)

        when:
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        assert expected == result
    }

    void "findAllByProjectAndSeqType, when call by user without access or needed role, then throw AccessDeniedException"() {
        given:
        setupData()

        when:
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        thrown(AccessDeniedException)
    }

    void "findAllByProjectAndSeqType, when project is wrong, then return empty list"() {
        given:
        setupData()
        List<OverallQualityAssessmentMerged> result

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(createProject(), qualityAssessment.seqType)
        }

        then:
        result == []
    }

    void "findAllByProjectAndSeqType, when seqType is wrong, then return empty list"() {
        given:
        setupData()
        List<OverallQualityAssessmentMerged> result

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, createSeqType([name: 'otherSeqType', dirName: 'otherDirName']))
        }

        then:
        result == []
    }

    void "findAllByProjectAndSeqType, when operation state is wrong, then return empty list"() {
        given:
        setupData()
        List<OverallQualityAssessmentMerged> result

        qualityAssessment.bamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        qualityAssessment.bamFile.md5sum = null
        qualityAssessment.bamFile.save(flush: true)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        result == []
    }

    void "findAllByProjectAndSeqType, when bam file is withdrawn, then return empty list"() {
        given:
        setupData()
        List<OverallQualityAssessmentMerged> result
        qualityAssessment.bamFile.withdrawn = true
        qualityAssessment.bamFile.save(flush: true)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        result == []
    }

    void "findAllByProjectAndSeqType, when QA has wrong state, then return empty list"() {
        given:
        setupData()
        List<OverallQualityAssessmentMerged> result
        qualityAssessment.bamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.IN_PROGRESS
        qualityAssessment.bamFile.save(flush: true)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            result = overallQualityAssessmentMergedService.findAllByProjectAndSeqType(qualityAssessment.project, qualityAssessment.seqType)
        }

        then:
        result == []
    }
}
