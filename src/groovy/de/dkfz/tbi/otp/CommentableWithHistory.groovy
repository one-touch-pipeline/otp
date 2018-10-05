package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.*

interface CommentableWithHistory {
    List<Comment> getComments()
    void setComments(List<Comment> comments)
}
