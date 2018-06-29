package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import grails.validation.*
import org.springframework.security.access.prepost.*
import org.springframework.validation.*

class MergingCriteriaService {

    CommentService commentService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, read)")
    MergingCriteria findMergingCriteria(Project project, SeqType seqType) {
        return MergingCriteria.findByProjectAndSeqType(project, seqType) ?: new MergingCriteria()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createOrUpdateMergingCriteria(Project project, SeqType seqType, boolean libPrepKit, MergingCriteria.SpecificSeqPlatformGroups seqPlatformGroups) {
        MergingCriteria mergingCriteria = MergingCriteria.findOrCreateWhere(project: project, seqType: seqType)
        mergingCriteria.libPrepKit = libPrepKit
        mergingCriteria.seqPlatformGroup = seqPlatformGroups
        try {
            mergingCriteria.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    @PreAuthorize("hasRole('ROLE_USER')")
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, read)")
    List<SeqPlatformGroup> findSeqPlatformGroupsForProjectAndSeqType(Project project, SeqType seqType) {
        return SeqPlatformGroup.createCriteria().list {
            mergingCriteria {
                eq("project", project)
                eq("seqType", seqType)
            }
            isNotEmpty("seqPlatforms")
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
    void deleteSeqPlatformGroup(SeqPlatformGroup group) {
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
