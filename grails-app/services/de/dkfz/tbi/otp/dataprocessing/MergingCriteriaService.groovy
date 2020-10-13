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

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Transactional
class MergingCriteriaService {

    CommentService commentService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    MergingCriteria findMergingCriteria(Project project, SeqType seqType) {
        return MergingCriteria.findByProjectAndSeqType(project, seqType) ?: new MergingCriteria()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createOrUpdateMergingCriteria(
            Project project, SeqType seqType, boolean useLibPrepKit, MergingCriteria.SpecificSeqPlatformGroups useSeqPlatformGroups
    ) {
        MergingCriteria mergingCriteria = MergingCriteria.findOrCreateWhere(project: project, seqType: seqType)
        mergingCriteria.useLibPrepKit = useLibPrepKit
        mergingCriteria.useSeqPlatformGroup = useSeqPlatformGroups
        try {
            mergingCriteria.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    void createDefaultMergingCriteria(Project project, SeqType seqType) {
        if (seqType in SeqTypeService.allAlignableSeqTypes && !MergingCriteria.findAllByProjectAndSeqType(project, seqType)) {
            new MergingCriteria(
                    project: project,
                    seqType: seqType,
                    useLibPrepKit: !seqType.isWgbs(),
                    useSeqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT,
            ).save(flush: true)
        }
    }

    List<SeqPlatformGroup> findDefaultSeqPlatformGroups() {
        return SeqPlatformGroup.createCriteria().list {
            isNull("mergingCriteria")
            isNotEmpty("seqPlatforms")
        }.sort { it.seqPlatforms.sort { it.fullName() }.first().fullName() }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<SeqPlatformGroup> findDefaultSeqPlatformGroupsOperator() {
        findDefaultSeqPlatformGroups()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List<SeqPlatformGroup> findSeqPlatformGroupsForProjectAndSeqType(Project project, SeqType seqType, boolean sortOrder = true) {
        return SeqPlatformGroup.createCriteria().list {
            mergingCriteria {
                eq("project", project)
                eq("seqType", seqType)
            }
            isNotEmpty("seqPlatforms")
            order("id", sortOrder ? "asc" : "desc")
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void removePlatformFromSeqPlatformGroup(SeqPlatformGroup group, SeqPlatform platform) {
        commentService.saveComment(group, group.seqPlatforms.collect { it.fullName() }.join("\n"))
        group.removeFromSeqPlatforms(platform)
        assert group.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addPlatformToExistingSeqPlatformGroup(SeqPlatformGroup group, SeqPlatform platform) {
        commentService.saveComment(group, group.seqPlatforms.collect { it.fullName() }.join("\n"))
        group.addToSeqPlatforms(platform)
        assert group.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void createNewGroupAndAddPlatform(SeqPlatform platform, MergingCriteria mergingCriteria = null) {
        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup(
                mergingCriteria: mergingCriteria,
        )
        seqPlatformGroup.addToSeqPlatforms(platform)
        assert seqPlatformGroup.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    // the group will not be deleted internally for reproducibility reasons, but the user will think the group is deleted
    void emptySeqPlatformGroup(SeqPlatformGroup group) {
        // code necessary because of grails behaviour with many-to-many relationships
        Set<SeqPlatform> seqPlatforms = new HashSet<SeqPlatform>(group.seqPlatforms ?: [])
        commentService.saveComment(group, seqPlatforms.collect { it.fullName() }.join("\n"))
        seqPlatforms.each {
            group.removeFromSeqPlatforms(it)
        }
        assert group.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void copyDefaultToSpecific(SeqPlatformGroup seqPlatformGroup, MergingCriteria mergingCriteria) {
        SeqPlatformGroup seqPlatformGroup1  = new SeqPlatformGroup(
                mergingCriteria: mergingCriteria,
        )
        seqPlatformGroup.seqPlatforms?.each { SeqPlatform seqPlatform ->
            seqPlatformGroup1.addToSeqPlatforms(seqPlatform)
        }
        assert seqPlatformGroup1.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void copyAllDefaultToSpecific(MergingCriteria mergingCriteria) {
        SeqPlatformGroup.findAllByMergingCriteriaIsNull().each { SeqPlatformGroup seqPlatformGroup ->
            copyDefaultToSpecific(seqPlatformGroup, mergingCriteria)
        }
    }
}
