package de.dkfz.tbi.otp

import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize


class CommentService {

    SpringSecurityService springSecurityService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#commentable?.project, read)")
    Comment saveComment(Commentable commentable, String message) {
        String userName = springSecurityService.principal.username
        return createOrUpdateComment(commentable, message, userName)
    }

    Comment saveCommentAsOtp(Commentable commentable, String message) {
        return createOrUpdateComment(commentable, message, "otp")
    }

    //only for otp internal use, not for gui
    Comment createOrUpdateComment(Commentable commentable, String message, String userName) {
        Comment comment = commentable.comment ?: new Comment()
        comment.comment = message
        comment.modificationDate = new Date()
        comment.author = userName
        assert comment.save(flush: true)
        commentable.comment = comment
        assert commentable.save(flush: true)
        return comment
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void saveComment(CommentableWithHistory commentableWithHistory, String message) {
        String userName = springSecurityService.principal.username
        addCommentToList(commentableWithHistory, message, userName)
    }

    void addCommentToList(CommentableWithHistory commentableWithHistory, String message, String userName) {
        Comment comment = new Comment(
                comment: message,
                modificationDate: new Date(),
                author: userName,
        )
        comment.save(flush: true)
        commentableWithHistory.comments.add(comment)
        assert commentableWithHistory.save(flush: true)
    }
}
