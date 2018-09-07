package de.dkfz.tbi.otp.config

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.validator.routines.*
import org.springframework.context.*

import java.time.*

enum TypeValidators {

    BOOLEAN({ it ==~ /true|false/ }, { ["true", "false"] }),

    POSITIVE_NUMBER({ it ==~ /^[\+]?\d+$/ }, null),

    FLOAT({ it ==~ /^[\+\-]?\d+\.?\d*$/ }, null),

    SINGLE_WORD_TEXT({ it ==~ /\S+/ }, null),

    SINGLE_WORD_TEXT_OPTIONAL({ it ==~ /\S*/ }, null),

    SINGLE_LINE_TEXT({ it ==~ /[^\n]*/ }, null),

    MULTI_LINE_TEXT({ it ==~ /[\s\S]*/ }, null),

    MAIL({ EmailValidator.getInstance().isValid(it) }, null),

    PATH_COMPONENT({ OtpPath.isValidPathComponent(it) }, null),

    ABSOLUTE_PATH({ OtpPath.isValidAbsolutePath(it) }, null),

    PLUGIN({ it ==~ /^\d+\.\d+\.\d+(\-\d+)?$/ }, null),

    CLUSTER_JOB_SUBMISSION_OPTION({ ClusterJobSubmissionOptionsService.validateJsonString(it) }, null),

    SSH_AUTH_METHOD({ SshAuthMethod.getByConfigName(it) }, { SshAuthMethod.values()*.name() }),

    PIPELINE_TYPE({ Pipeline.Type.findByName(it) }, { Pipeline.Type.values()*.name() }),

    JOB_SCHEDULER({ Realm.JobScheduler.findByName(it) }, { Realm.JobScheduler.values()*.name() }),

    INSTANCE_LOGO({ InstanceLogo.findByName(it) }, { InstanceLogo.values()*.name() }),

    SEQ_CENTER_NAME({ SeqCenter.findByName(it) }, { SeqCenter.all*.name }),

    SEQ_TYPE_RODDY_NAME({ SeqType.findByRoddyName(it) }, { SeqType.all*.roddyName.findAll().unique() }),

    SEQ_TYPE_PROCESSING_NAME({ SeqType.findByDisplayName(it) }, { SeqType.all*.displayName.unique() }),

    WORKFLOW_NAME({ JobExecutionPlan.findByName(it) }, { JobExecutionPlan.findAllByObsoleted(false)*.name }),

    JOB_NAME_SEQ_TYPE({ validateTypeForClusterSubmission(it) }, { allowedValuesForClusterSubmission() }),

    TIME_ZONE({ try { ZoneId.of(it); return true } catch (DateTimeException ignored) { return false } }, { ZoneId.getAvailableZoneIds().sort() }),

    private Closure validator
    private final Closure<List<String>> allowedValues

    TypeValidators(Closure validator, Closure<List<String>> allowedValues) {
        this.validator = validator
        this.allowedValues = allowedValues
    }

    boolean validate(String value) {
        value != null && validator(value)
    }

    List<String> getAllowedValues() {
        if (allowedValues) {
            return allowedValues.call()
        } else {
            return []
        }
    }

    private static boolean validateTypeForClusterSubmission(String name) {
        if (!name) {
            return false
        }
        String[] split = name.split('_', 2)
        if (split.size() == 2 && !SEQ_TYPE_PROCESSING_NAME.validate(split[1])) {
            return false
        }

        ApplicationContext context = StaticApplicationContextWrapper.context
        List<String> jobNames = context.getBeanNamesForType(Job.class)

        return jobNames.any {
            context.getType(it).getSimpleName() == split[0]
        }
    }

    private static List<String> allowedValuesForClusterSubmission() {
        ApplicationContext context = StaticApplicationContextWrapper.context
        List<String> beanNames = context.getBeanNamesForType(Job.class)
        List<String> jobNames = beanNames.collect {
            context.getType(it).getSimpleName()
        }
        List<String> seqTypes = SEQ_TYPE_PROCESSING_NAME.allowedValues
        List<String> result = jobNames.collectMany { String jobName ->
            [jobName] + seqTypes.collect { String seqType -> "${jobName}_${seqType}" }
        }
        result.sort()
    }
}
