package de.dkfz.tbi.otp

import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize


class CommentService {

    SpringSecurityService springSecurityService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public Comment saveComment(Commentable commentable, String message) {
        Comment comment = commentable.comment ?: new Comment()
        comment.comment = message
        comment.modificationDate = new Date()
        comment.author = springSecurityService.principal.username
        assert comment.save(flush: true)
        commentable.comment = comment
        assert commentable.save(flush: true)
        return comment
    }
}
