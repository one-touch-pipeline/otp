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
package de.dkfz.tbi.otp

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.security.access.prepost.PreAuthorize

@Transactional
class CommentService {

    SpringSecurityService springSecurityService

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#commentable?.project, 'OTP_READ_ACCESS')")
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
        assert comment.save()
        commentable.comment = comment
        assert commentable.save()
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
        comment.save()
        commentableWithHistory.comments.add(comment)
        assert commentableWithHistory.save()
    }
}
