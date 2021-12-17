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
package de.dkfz.tbi.otp.config

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.*

class TypeValidatorsSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobExecutionPlan,
                JobDefinition,
                SeqCenter,
                SeqType,
        ]
    }

    private static final String CENTER_A = 'CenterA'
    private static final String CENTER_B = 'CenterB'

    private static final String SEQ_TYPE_RODDY_NAME_A = 'roddyNameA'
    private static final String SEQ_TYPE_RODDY_NAME_B = 'roddyNameB'

    private static final String SEQ_TYPE_DISPLAY_NAME_A = 'DisplayNameA'
    private static final String SEQ_TYPE_DISPLAY_NAME_B = 'DisplayNameB'

    private static final String WORKFLOW_NAME_A = 'WorkflowA'
    private static final String WORKFLOW_NAME_B = 'WorkflowB'

    @Unroll
    void "check #validator for value '#value' should return '#ret'"() {
        given:
        initClosure()

        expect:
        ret == validator.validate(value)

        where:
        validator                                    | value                             | initClosure                 || ret
        TypeValidators.BOOLEAN                       | 'true'                            | emptyInit                   || true
        TypeValidators.BOOLEAN                       | 'false'                           | emptyInit                   || true
        TypeValidators.BOOLEAN                       | 'True'                            | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'False'                           | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'TRUE'                            | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'FALSE'                           | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 't'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'f'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'T'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'F'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | null                              | emptyInit                   || false
        TypeValidators.BOOLEAN                       | ''                                | emptyInit                   || false
        TypeValidators.BOOLEAN                       | '0'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | '1'                               | emptyInit                   || false
        TypeValidators.BOOLEAN                       | 'text'                            | emptyInit                   || false

        TypeValidators.POSITIVE_NUMBER               | '1'                               | emptyInit                   || true
        TypeValidators.POSITIVE_NUMBER               | '2'                               | emptyInit                   || true
        TypeValidators.POSITIVE_NUMBER               | '10'                              | emptyInit                   || true
        TypeValidators.POSITIVE_NUMBER               | '0'                               | emptyInit                   || true
        TypeValidators.POSITIVE_NUMBER               | '+1'                              | emptyInit                   || true
        TypeValidators.POSITIVE_NUMBER               | '-1'                              | emptyInit                   || false
        TypeValidators.POSITIVE_NUMBER               | null                              | emptyInit                   || false
        TypeValidators.POSITIVE_NUMBER               | ''                                | emptyInit                   || false
        TypeValidators.POSITIVE_NUMBER               | 'text'                            | emptyInit                   || false
        TypeValidators.POSITIVE_NUMBER               | '1.0'                             | emptyInit                   || false
        TypeValidators.POSITIVE_NUMBER               | '1e3'                             | emptyInit                   || false

        TypeValidators.FLOAT                         | '0'                               | emptyInit                   || true
        TypeValidators.FLOAT                         | '1'                               | emptyInit                   || true
        TypeValidators.FLOAT                         | '2'                               | emptyInit                   || true
        TypeValidators.FLOAT                         | '+1'                              | emptyInit                   || true
        TypeValidators.FLOAT                         | '-1'                              | emptyInit                   || true
        TypeValidators.FLOAT                         | '1.'                              | emptyInit                   || true
        TypeValidators.FLOAT                         | '+1.'                             | emptyInit                   || true
        TypeValidators.FLOAT                         | '-1.'                             | emptyInit                   || true
        TypeValidators.FLOAT                         | '1.234'                           | emptyInit                   || true
        TypeValidators.FLOAT                         | '+1.234'                          | emptyInit                   || true
        TypeValidators.FLOAT                         | '-1.234'                          | emptyInit                   || true
        TypeValidators.FLOAT                         | '1e3'                             | emptyInit                   || false
        TypeValidators.FLOAT                         | null                              | emptyInit                   || false
        TypeValidators.FLOAT                         | ''                                | emptyInit                   || false
        TypeValidators.FLOAT                         | 'text'                            | emptyInit                   || false

        TypeValidators.SINGLE_WORD_TEXT              | 'one_word'                        | emptyInit                   || true
        TypeValidators.SINGLE_WORD_TEXT              | 'long_word_' * 10                 | emptyInit                   || true
        TypeValidators.SINGLE_WORD_TEXT              | null                              | emptyInit                   || false
        TypeValidators.SINGLE_WORD_TEXT              | ''                                | emptyInit                   || false
        TypeValidators.SINGLE_WORD_TEXT              | 'multi word'                      | emptyInit                   || false
        TypeValidators.SINGLE_WORD_TEXT              | 'multi\nline\ntext '              | emptyInit                   || false

        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'one_word'                        | emptyInit                   || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'long_word_' * 10                 | emptyInit                   || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | ''                                | emptyInit                   || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | null                              | emptyInit                   || false
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'multi word'                      | emptyInit                   || false
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'multi\nline\ntext '              | emptyInit                   || false

        TypeValidators.SINGLE_LINE_TEXT              | 'some text'                       | emptyInit                   || true
        TypeValidators.SINGLE_LINE_TEXT              | 'long text ' * 10                 | emptyInit                   || true
        TypeValidators.SINGLE_LINE_TEXT              | ''                                | emptyInit                   || false
        TypeValidators.SINGLE_LINE_TEXT              | null                              | emptyInit                   || false
        TypeValidators.SINGLE_LINE_TEXT              | 'multi\nline\ntext '              | emptyInit                   || false

        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'some text'                       | emptyInit                   || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'long text ' * 10                 | emptyInit                   || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | ''                                | emptyInit                   || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | null                              | emptyInit                   || false
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'multi\nline\ntext '              | emptyInit                   || false

        TypeValidators.MULTI_LINE_TEXT               | ''                                | emptyInit                   || true
        TypeValidators.MULTI_LINE_TEXT               | 'some text'                       | emptyInit                   || true
        TypeValidators.MULTI_LINE_TEXT               | 'long text ' * 10                 | emptyInit                   || true
        TypeValidators.MULTI_LINE_TEXT               | 'multi\nline\ntext '              | emptyInit                   || true
        TypeValidators.MULTI_LINE_TEXT               | 'long\nmulti\nline\ntext ' * 10   | emptyInit                   || true
        TypeValidators.MULTI_LINE_TEXT               | null                              | emptyInit                   || false

        TypeValidators.MAIL                          | 'a.b@c.de'                        | emptyInit                   || true
        TypeValidators.MAIL                          | 'a@b.cd'                          | emptyInit                   || true
        TypeValidators.MAIL                          | null                              | emptyInit                   || false
        TypeValidators.MAIL                          | ''                                | emptyInit                   || false
        TypeValidators.MAIL                          | 'text'                            | emptyInit                   || false
        TypeValidators.MAIL                          | 'a.b@c'                           | emptyInit                   || false
        TypeValidators.MAIL                          | 'a.b@cde'                         | emptyInit                   || false

        TypeValidators.PATH_COMPONENT                | 'valid'                           | emptyInit                   || true
        TypeValidators.PATH_COMPONENT                | null                              | emptyInit                   || false
        TypeValidators.PATH_COMPONENT                | ''                                | emptyInit                   || false
        TypeValidators.PATH_COMPONENT                | 'not valid'                       | emptyInit                   || false
        TypeValidators.PATH_COMPONENT                | 'not/valid'                       | emptyInit                   || false
        TypeValidators.PATH_COMPONENT                | '.'                               | emptyInit                   || false
        TypeValidators.PATH_COMPONENT                | '..'                              | emptyInit                   || false

        TypeValidators.ABSOLUTE_PATH                 | '/valid'                          | emptyInit                   || true
        TypeValidators.ABSOLUTE_PATH                 | '/valid/long/path'                | emptyInit                   || true
        TypeValidators.ABSOLUTE_PATH                 | null                              | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | ''                                | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | '/not valid'                      | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | 'notvalid'                        | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid//abc'                  | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid/./abc'                 | emptyInit                   || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid/../abc'                | emptyInit                   || false

        TypeValidators.PLUGIN                        | '1.2.3'                           | emptyInit                   || true
        TypeValidators.PLUGIN                        | '1.2.3-4'                         | emptyInit                   || true
        TypeValidators.PLUGIN                        | '1.2.345'                         | emptyInit                   || true
        TypeValidators.PLUGIN                        | '1.2345.3'                        | emptyInit                   || true
        TypeValidators.PLUGIN                        | '12345.2.3'                       | emptyInit                   || true
        TypeValidators.PLUGIN                        | '1.2'                             | emptyInit                   || false
        TypeValidators.PLUGIN                        | '1.2.3.4'                         | emptyInit                   || false
        TypeValidators.PLUGIN                        | 'a.2.3'                           | emptyInit                   || false
        TypeValidators.PLUGIN                        | '1.b.3'                           | emptyInit                   || false
        TypeValidators.PLUGIN                        | '1.2.c'                           | emptyInit                   || false
        TypeValidators.PLUGIN                        | '1.2.3-d'                         | emptyInit                   || false
        TypeValidators.PLUGIN                        | ''                                | emptyInit                   || false
        TypeValidators.PLUGIN                        | null                              | emptyInit                   || false

        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | ''                                | emptyInit                   || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{CORES: \'3\'}'                  | emptyInit                   || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{CORES: \'3\', MEMORY: \'100\'}' | emptyInit                   || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | null                              | emptyInit                   || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{}'                              | emptyInit                   || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '[\'1\', \'2\']'                  | emptyInit                   || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | 'text'                            | emptyInit                   || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{nojson}'                        | emptyInit                   || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{noKey: \'3\'}'                  | emptyInit                   || false

        TypeValidators.SSH_AUTH_METHOD               | 'sshagent'                        | emptyInit                   || true
        TypeValidators.SSH_AUTH_METHOD               | 'keyfile'                         | emptyInit                   || true
        TypeValidators.SSH_AUTH_METHOD               | 'password'                        | emptyInit                   || true
        TypeValidators.SSH_AUTH_METHOD               | ''                                | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | null                              | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'Sshagent'                        | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'Keyfile'                         | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'Password'                        | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'SSHAGENT'                        | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'KEYFILE'                         | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'PASSWORD'                        | emptyInit                   || false
        TypeValidators.SSH_AUTH_METHOD               | 'unknown'                         | emptyInit                   || false

        TypeValidators.PIPELINE_TYPE                 | 'ACESEQ'                          | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | 'ALIGNMENT'                       | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | 'INDEL'                           | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | 'MUTATIONAL_SIGNATURE'            | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | 'SNV'                             | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | 'SOPHIA'                          | emptyInit                   || true
        TypeValidators.PIPELINE_TYPE                 | ''                                | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | null                              | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'UNKNOWN'                         | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'aceseq'                          | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'alignment'                       | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'indel'                           | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'mutation_signature'              | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'snv'                             | emptyInit                   || false
        TypeValidators.PIPELINE_TYPE                 | 'sophia'                          | emptyInit                   || false

        TypeValidators.INSTANCE_LOGO                 | 'NONE'                            | emptyInit                   || true
        TypeValidators.INSTANCE_LOGO                 | 'CHARITE'                         | emptyInit                   || true
        TypeValidators.INSTANCE_LOGO                 | 'DKFZ'                            | emptyInit                   || true
        TypeValidators.INSTANCE_LOGO                 | ''                                | emptyInit                   || false
        TypeValidators.INSTANCE_LOGO                 | null                              | emptyInit                   || false
        TypeValidators.INSTANCE_LOGO                 | 'UNKNOWN'                         | emptyInit                   || false
        TypeValidators.INSTANCE_LOGO                 | 'None'                            | emptyInit                   || false
        TypeValidators.INSTANCE_LOGO                 | 'none'                            | emptyInit                   || false

        TypeValidators.JOB_SCHEDULER                 | 'PBS'                             | emptyInit                   || true
        TypeValidators.JOB_SCHEDULER                 | 'LSF'                             | emptyInit                   || true
        TypeValidators.JOB_SCHEDULER                 | ''                                | emptyInit                   || false
        TypeValidators.JOB_SCHEDULER                 | null                              | emptyInit                   || false
        TypeValidators.JOB_SCHEDULER                 | 'pbs'                             | emptyInit                   || false
        TypeValidators.JOB_SCHEDULER                 | 'lsf'                             | emptyInit                   || false
        TypeValidators.JOB_SCHEDULER                 | 'text'                            | emptyInit                   || false

        TypeValidators.SEQ_CENTER_NAME               | CENTER_A                          | initSeqCenter               || true
        TypeValidators.SEQ_CENTER_NAME               | CENTER_B                          | initSeqCenter               || true
        TypeValidators.SEQ_CENTER_NAME               | ''                                | initSeqCenter               || false
        TypeValidators.SEQ_CENTER_NAME               | null                              | initSeqCenter               || false
        TypeValidators.SEQ_CENTER_NAME               | 'OtherCenterName'                 | initSeqCenter               || false

        TypeValidators.SEQ_TYPE_RODDY_NAME           | SEQ_TYPE_RODDY_NAME_A             | initSeqTypes                || true
        TypeValidators.SEQ_TYPE_RODDY_NAME           | SEQ_TYPE_RODDY_NAME_B             | initSeqTypes                || true
        TypeValidators.SEQ_TYPE_RODDY_NAME           | ''                                | initSeqTypes                || false
        TypeValidators.SEQ_TYPE_RODDY_NAME           | null                              | initSeqTypes                || false
        TypeValidators.SEQ_TYPE_RODDY_NAME           | 'OtherSeqTypeName'                | initSeqTypes                || false

        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | SEQ_TYPE_DISPLAY_NAME_A           | initSeqTypesProcessingNames || true
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | SEQ_TYPE_DISPLAY_NAME_B           | initSeqTypesProcessingNames || true
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | ''                                | initSeqTypesProcessingNames || false
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | null                              | initSeqTypesProcessingNames || false
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | 'OtherSeqTypeName'                | initSeqTypesProcessingNames || false

        TypeValidators.WORKFLOW_NAME                 | WORKFLOW_NAME_A                   | initJobExecutionPlans       || true
        TypeValidators.WORKFLOW_NAME                 | WORKFLOW_NAME_B                   | initJobExecutionPlans       || true
        TypeValidators.WORKFLOW_NAME                 | ''                                | initJobExecutionPlans       || false
        TypeValidators.WORKFLOW_NAME                 | null                              | initJobExecutionPlans       || false
        TypeValidators.WORKFLOW_NAME                 | 'OtherWorkflowName'               | initJobExecutionPlans       || false
    }

    private static Closure emptyInit = { }

    private static Closure initSeqCenter = { ->
        [
                CENTER_A,
                CENTER_B,
        ].each {
            DomainFactory.createSeqCenter([
                    name: it,
            ])
        }
    }

    private static Closure initSeqTypes = { ->
        [
                SEQ_TYPE_RODDY_NAME_A,
                SEQ_TYPE_RODDY_NAME_B,
        ].each {
            DomainFactory.createSeqType([
                    roddyName: it,
            ])
        }
    }

    private static Closure initSeqTypesProcessingNames = { ->
        [
                SEQ_TYPE_DISPLAY_NAME_A,
                SEQ_TYPE_DISPLAY_NAME_B,
        ].each {
            DomainFactory.createSeqType([
                    displayName: it,
            ])
        }
    }

    private static Closure initJobExecutionPlans = { ->
        [
                WORKFLOW_NAME_A,
                WORKFLOW_NAME_B,
        ].each {
            DomainFactory.createJobExecutionPlan([
                    name: it,
            ])
        }
    }
}
