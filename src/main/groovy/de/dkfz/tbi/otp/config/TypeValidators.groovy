/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.config

import org.apache.commons.validator.routines.EmailValidator
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.cron.ScheduledJob
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractOtpJob
import de.dkfz.tbi.otp.job.processing.ClusterJobSubmissionOptionsService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StaticApplicationContextWrapper
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.time.DateTimeException
import java.time.ZoneId

enum TypeValidators {

    BOOLEAN({ it ==~ /true|false/ }, { ["true", "false"] }),

    POSITIVE_NUMBER({ it ==~ /^[\+]?\d+$/ }, null),

    FLOAT({ it ==~ /^[\+\-]?\d+\.?\d*$/ }, null),

    SINGLE_WORD_TEXT({ it ==~ /\S+/ }, null),

    SINGLE_WORD_TEXT_OPTIONAL({ it ==~ /\S*/ }, null),

    SINGLE_LINE_TEXT({ it ==~ /[^\n]+/ }, null),

    SINGLE_LINE_TEXT_OPTIONAL({ it ==~ /[^\n]*/ }, null),

    MULTI_LINE_TEXT({ it ==~ /[\s\S]*/ }, null),

    MAIL({ EmailValidator.instance.isValid(it) }, null),

    PATH_COMPONENT({ OtpPathValidator.isValidPathComponent(it) }, null),

    ABSOLUTE_PATH({ OtpPathValidator.isValidAbsolutePath(it) }, null),

    PLUGIN({ it ==~ /^\d+\.\d+\.\d+(\-\d+)?$/ }, null),

    CLUSTER_JOB_SUBMISSION_OPTION({ !ClusterJobSubmissionOptionsService.validateJsonString(it) }, null),

    SSH_AUTH_METHOD({ SshAuthMethod.getByConfigName(it) }, { SshAuthMethod.values()*.name() }),

    PIPELINE_TYPE({ Pipeline.Type.findByName(it) }, { Pipeline.Type.values()*.name() }),

    JOB_SCHEDULER({ Realm.JobScheduler.findByName(it) }, { Realm.JobScheduler.values()*.name() }),

    INSTANCE_LOGO({ InstanceLogo.findByName(it) }, { InstanceLogo.values()*.name() }),

    SEQ_CENTER_NAME({ SeqCenter.findByName(it) }, { SeqCenter.all*.name }),

    SEQ_TYPE_RODDY_NAME({ SeqType.findByRoddyName(it) }, { SeqType.all*.roddyName.findAll().unique() }),
    SEQ_TYPE_RODDY_NAME_SNV({ SeqType.findByRoddyName(it) }, { SeqTypeService.snvPipelineSeqTypes*.roddyName.findAll().unique() }),
    SEQ_TYPE_RODDY_NAME_INDEL({ SeqType.findByRoddyName(it) }, { SeqTypeService.indelPipelineSeqTypes*.roddyName.findAll().unique() }),
    SEQ_TYPE_RODDY_NAME_SOPHIA({ SeqType.findByRoddyName(it) }, { SeqTypeService.sophiaPipelineSeqTypes*.roddyName.findAll().unique() }),
    SEQ_TYPE_RODDY_NAME_ACESEQ({ SeqType.findByRoddyName(it) }, { SeqTypeService.aceseqPipelineSeqTypes*.roddyName.findAll().unique() }),

    SEQ_TYPE_PROCESSING_NAME({ SeqType.findByDisplayName(it) }, { SeqType.all*.displayName.unique() }),

    WORKFLOW_NAME({ JobExecutionPlan.findByName(it) }, { JobExecutionPlan.findAllByObsoleted(false)*.name }),

    PROCESSING_PRIORITY_NAME({ CollectionUtils.atMostOneElement(ProcessingPriority.findAllByName(it)) }, { ProcessingPriority.list([sort: 'name'])*.name }),

    JOB_NAME_SEQ_TYPE({ validateTypeForClusterSubmission(it) }, { allowedValuesForClusterSubmission() }),

    TIME_ZONE({ try { ZoneId.of(it); return true } catch (DateTimeException ignored) { return false } }, { ZoneId.availableZoneIds.sort() }),

    GUI_ANNOTATION({ GuiAnnotation.findByName(it) }, { GuiAnnotation.values()*.name() }),

    CRONJOB_CLASS({ it in ScheduledJob.ALL_JOB_CLASSES*.canonicalName }, { ScheduledJob.ALL_JOB_CLASSES*.canonicalName }),

    private final Closure validator
    private final Closure<List<String>> allowedValues

    TypeValidators(Closure validator, Closure<List<String>> allowedValues) {
        this.validator = validator
        this.allowedValues = allowedValues
    }

    boolean validate(String value) {
        value != null && validator(value)
    }

    /**
     * @return a list of allowed values, or null if everything is allowed
     */
    @SuppressWarnings("ReturnsNullInsteadOfEmptyCollection")
    List<String> getAllowedValues() {
        if (allowedValues) {
            return allowedValues.call()
        }
        return null
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
        List<String> jobNames = context.getBeanNamesForType(AbstractOtpJob)

        return jobNames.any {
            context.getType(it).simpleName == split[0]
        }
    }

    private static List<String> allowedValuesForClusterSubmission() {
        ApplicationContext context = StaticApplicationContextWrapper.context
        List<String> beanNames = context.getBeanNamesForType(AbstractOtpJob)
        List<String> jobNames = beanNames.collect {
            context.getType(it).simpleName
        }
        List<String> seqTypes = SEQ_TYPE_PROCESSING_NAME.allowedValues
        List<String> result = jobNames.collectMany { String jobName ->
            [jobName] + seqTypes.collect { String seqType -> "${jobName}_${seqType}" }
        }
        result.sort()
    }
}
