package de.dkfz.tbi.otp.config

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.validator.routines.*
import org.springframework.context.*

enum TypeValidators {

    BOOLEAN({ it ==~ /true|false/ }),

    POSITIVE_NUMBER({ it ==~ /^[\+]?\d+$/ }),

    FLOAT({ it ==~ /^[\+\-]?\d+\.?\d*$/ }),

    SINGLE_WORD_TEXT({ it ==~ /\S+/ }),

    SINGLE_WORD_TEXT_OPTIONAL({ it ==~ /\S*/ }),

    SINGLE_LINE_TEXT({ it ==~ /[^\n]*/ }),

    MULTI_LINE_TEXT({ it ==~ /[\s\S]*/ }),

    MAIL({ EmailValidator.getInstance().isValid(it) }),

    PATH_COMPONENT({ OtpPath.isValidPathComponent(it) }),

    ABSOLUTE_PATH({ OtpPath.isValidAbsolutePath(it) }),

    PLUGIN({ it ==~ /^\d+\.\d+\.\d+(\-\d+)?$/ }),

    CLUSTER_JOB_SUBMISSION_OPTION({ ClusterJobSubmissionOptionsService.validateJsonString(it) }),

    SSH_AUTH_METHOD({ SshAuthMethod.getByConfigName(it) }),

    PIPELINE_TYPE({ Pipeline.Type.findByName(it) }),

    JOB_SCHEDULER({ Realm.JobScheduler.findByName(it) }),

    INSTANCE_LOGO({ InstanceLogo.findByName(it) }),

    SEQ_CENTER_NAME({ SeqCenter.findByName(it) }),

    SEQ_TYPE_RODDY_NAME({ SeqType.findByRoddyName(it) }),

    SEQ_TYPE_PROCESSING_NAME({ SeqType.findByDisplayName(it) }),

    WORKFLOW_NAME({ JobExecutionPlan.findByName(it) }),

    JOB_NAME_SEQ_TYPE({ validateTypeForClusterSubmission(it) }),

    private Closure validator

    TypeValidators(Closure validator) {
        this.validator = validator
    }

    boolean validate(String value) {
        value != null && validator(value)
    }

    private static boolean validateTypeForClusterSubmission(String name) {
        if (!name) {
            return false
        }
        String[] split = name.split('_')
        if (split.size() > 2) {
            return false
        }
        if (split.size() == 2 && !SEQ_TYPE_PROCESSING_NAME.validate(split[2])) {
            return false
        }

        ApplicationContext context = StaticApplicationContextWrapper.context
        List<String> jobNames = context.getBeanNamesForType(Job.class)

        return jobNames.any {
            context.getType(it).getSimpleName() == split[0]
        }
    }
}
