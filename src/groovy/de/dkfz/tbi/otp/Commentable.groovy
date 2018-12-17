package de.dkfz.tbi.otp

interface Commentable {
    Comment getComment()
    void setComment(Comment comment)
    abstract getProject()
}
