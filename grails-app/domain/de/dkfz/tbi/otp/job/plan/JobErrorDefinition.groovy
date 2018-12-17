package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.utils.Entity

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class JobErrorDefinition implements Entity {
    String errorExpression
    Type type
    Action action

    /**
     * This enum defines the source of the error message.
     */
    enum Type {
        MESSAGE,
        STACKTRACE,
        CLUSTER_LOG
    }
    /**
     * This enum defines how to handle a failed job.
     */
    enum Action {
        RESTART_WF,
        RESTART_JOB,
        STOP,
        CHECK_FURTHER,
    }

   static constraints = {
       errorExpression(validator: { val, obj ->
           try {
               Pattern.compile(val)
           }
           catch (PatternSyntaxException e) {
               return "${val} is not a valid REGEX"
           }
           return true
       })
    }
    static hasMany = [
            checkFurtherJobErrors: JobErrorDefinition,
            jobDefinitions: JobDefinition,
    ]

    static mapping = {
        errorExpression type: 'text'
    }

    @Override
    String toString() {
        return "JobErrorDefinition(id=${id}, type=${type}, action=${action}, errorExpression=${errorExpression})"
    }
}
