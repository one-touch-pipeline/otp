/*
 * Copyright 2011-2024 The OTP authors
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
        TypeValidators.BOOLEAN                       | 'true'                            | EMPTY_INIT || true
        TypeValidators.BOOLEAN                       | 'false'                           | EMPTY_INIT || true
        TypeValidators.BOOLEAN                       | 'True'                            | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'False'                           | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'TRUE'                            | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'FALSE'                           | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 't'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'f'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'T'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'F'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | null                              | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | ''                                | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | '0'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | '1'                               | EMPTY_INIT || false
        TypeValidators.BOOLEAN                       | 'text'                            | EMPTY_INIT || false

        TypeValidators.POSITIVE_NUMBER               | '1'                               | EMPTY_INIT || true
        TypeValidators.POSITIVE_NUMBER               | '2'                               | EMPTY_INIT || true
        TypeValidators.POSITIVE_NUMBER               | '10'                              | EMPTY_INIT || true
        TypeValidators.POSITIVE_NUMBER               | '0'                               | EMPTY_INIT || true
        TypeValidators.POSITIVE_NUMBER               | '+1'                              | EMPTY_INIT || true
        TypeValidators.POSITIVE_NUMBER               | '-1'                              | EMPTY_INIT || false
        TypeValidators.POSITIVE_NUMBER               | null                              | EMPTY_INIT || false
        TypeValidators.POSITIVE_NUMBER               | ''                                | EMPTY_INIT || false
        TypeValidators.POSITIVE_NUMBER               | 'text'                            | EMPTY_INIT || false
        TypeValidators.POSITIVE_NUMBER               | '1.0'                             | EMPTY_INIT || false
        TypeValidators.POSITIVE_NUMBER               | '1e3'                             | EMPTY_INIT || false

        TypeValidators.FLOAT                         | '0'                               | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '1'                               | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '2'                               | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '+1'                              | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '-1'                              | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '1.'                              | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '+1.'                             | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '-1.'                             | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '1.234'                           | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '+1.234'                          | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '-1.234'                          | EMPTY_INIT || true
        TypeValidators.FLOAT                         | '1e3'                             | EMPTY_INIT || false
        TypeValidators.FLOAT                         | null                              | EMPTY_INIT || false
        TypeValidators.FLOAT                         | ''                                | EMPTY_INIT || false
        TypeValidators.FLOAT                         | 'text'                            | EMPTY_INIT || false

        TypeValidators.SINGLE_WORD_TEXT              | 'one_word'                        | EMPTY_INIT || true
        TypeValidators.SINGLE_WORD_TEXT              | 'long_word_' * 10                 | EMPTY_INIT || true
        TypeValidators.SINGLE_WORD_TEXT              | null                              | EMPTY_INIT || false
        TypeValidators.SINGLE_WORD_TEXT              | ''                                | EMPTY_INIT || false
        TypeValidators.SINGLE_WORD_TEXT              | 'multi word'                      | EMPTY_INIT || false
        TypeValidators.SINGLE_WORD_TEXT              | 'multi\nline\ntext '              | EMPTY_INIT || false

        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'one_word'                        | EMPTY_INIT || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'long_word_' * 10                 | EMPTY_INIT || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | ''                                | EMPTY_INIT || true
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | null                              | EMPTY_INIT || false
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'multi word'                      | EMPTY_INIT || false
        TypeValidators.SINGLE_WORD_TEXT_OPTIONAL     | 'multi\nline\ntext '              | EMPTY_INIT || false

        TypeValidators.SINGLE_LINE_TEXT              | 'some text'                       | EMPTY_INIT || true
        TypeValidators.SINGLE_LINE_TEXT              | 'long text ' * 10                 | EMPTY_INIT || true
        TypeValidators.SINGLE_LINE_TEXT              | ''                                | EMPTY_INIT || false
        TypeValidators.SINGLE_LINE_TEXT              | null                              | EMPTY_INIT || false
        TypeValidators.SINGLE_LINE_TEXT              | 'multi\nline\ntext '              | EMPTY_INIT || false

        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'some text'                       | EMPTY_INIT || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'long text ' * 10                 | EMPTY_INIT || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | ''                                | EMPTY_INIT || true
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | null                              | EMPTY_INIT || false
        TypeValidators.SINGLE_LINE_TEXT_OPTIONAL     | 'multi\nline\ntext '              | EMPTY_INIT || false

        TypeValidators.MULTI_LINE_TEXT               | ''                                | EMPTY_INIT || true
        TypeValidators.MULTI_LINE_TEXT               | 'some text'                       | EMPTY_INIT || true
        TypeValidators.MULTI_LINE_TEXT               | 'long text ' * 10                 | EMPTY_INIT || true
        TypeValidators.MULTI_LINE_TEXT               | 'multi\nline\ntext '              | EMPTY_INIT || true
        TypeValidators.MULTI_LINE_TEXT               | 'long\nmulti\nline\ntext ' * 10   | EMPTY_INIT || true
        TypeValidators.MULTI_LINE_TEXT               | null                              | EMPTY_INIT || false

        TypeValidators.MAIL                          | 'a.b@c.de'                        | EMPTY_INIT || true
        TypeValidators.MAIL                          | 'a@b.cd'                          | EMPTY_INIT || true
        TypeValidators.MAIL                          | null                              | EMPTY_INIT || false
        TypeValidators.MAIL                          | ''                                | EMPTY_INIT || false
        TypeValidators.MAIL                          | 'text'                            | EMPTY_INIT || false
        TypeValidators.MAIL                          | 'a.b@c'                           | EMPTY_INIT || false
        TypeValidators.MAIL                          | 'a.b@cde'                         | EMPTY_INIT || false

        TypeValidators.PATH_COMPONENT                | 'valid'                           | EMPTY_INIT || true
        TypeValidators.PATH_COMPONENT                | null                              | EMPTY_INIT || false
        TypeValidators.PATH_COMPONENT                | ''                                | EMPTY_INIT || false
        TypeValidators.PATH_COMPONENT                | 'not valid'                       | EMPTY_INIT || false
        TypeValidators.PATH_COMPONENT                | 'not/valid'                       | EMPTY_INIT || false
        TypeValidators.PATH_COMPONENT                | '.'                               | EMPTY_INIT || false
        TypeValidators.PATH_COMPONENT                | '..'                              | EMPTY_INIT || false

        TypeValidators.ABSOLUTE_PATH                 | '/valid'                          | EMPTY_INIT || true
        TypeValidators.ABSOLUTE_PATH                 | '/valid/long/path'                | EMPTY_INIT || true
        TypeValidators.ABSOLUTE_PATH                 | null                              | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | ''                                | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | '/not valid'                      | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | 'notvalid'                        | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid//abc'                  | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid/./abc'                 | EMPTY_INIT || false
        TypeValidators.ABSOLUTE_PATH                 | '/notValid/../abc'                | EMPTY_INIT || false

        TypeValidators.PLUGIN                        | '1.2.3'                           | EMPTY_INIT || true
        TypeValidators.PLUGIN                        | '1.2.3-4'                         | EMPTY_INIT || true
        TypeValidators.PLUGIN                        | '1.2.345'                         | EMPTY_INIT || true
        TypeValidators.PLUGIN                        | '1.2345.3'                        | EMPTY_INIT || true
        TypeValidators.PLUGIN                        | '12345.2.3'                       | EMPTY_INIT || true
        TypeValidators.PLUGIN                        | '1.2'                             | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | '1.2.3.4'                         | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | 'a.2.3'                           | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | '1.b.3'                           | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | '1.2.c'                           | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | '1.2.3-d'                         | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | ''                                | EMPTY_INIT || false
        TypeValidators.PLUGIN                        | null                              | EMPTY_INIT || false

        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | ''                                | EMPTY_INIT || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{CORES: \'3\'}'                  | EMPTY_INIT || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{CORES: \'3\', MEMORY: \'100\'}' | EMPTY_INIT || true
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | null                              | EMPTY_INIT || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{}'                              | EMPTY_INIT || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '[\'1\', \'2\']'                  | EMPTY_INIT || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | 'text'                            | EMPTY_INIT || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{nojson}'                        | EMPTY_INIT || false
        TypeValidators.CLUSTER_JOB_SUBMISSION_OPTION | '{noKey: \'3\'}'                  | EMPTY_INIT || false

        TypeValidators.SSH_AUTH_METHOD               | 'sshagent'                        | EMPTY_INIT || true
        TypeValidators.SSH_AUTH_METHOD               | 'keyfile'                         | EMPTY_INIT || true
        TypeValidators.SSH_AUTH_METHOD               | 'password'                        | EMPTY_INIT || true
        TypeValidators.SSH_AUTH_METHOD               | ''                                | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | null                              | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'Sshagent'                        | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'Keyfile'                         | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'Password'                        | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'SSHAGENT'                        | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'KEYFILE'                         | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'PASSWORD'                        | EMPTY_INIT || false
        TypeValidators.SSH_AUTH_METHOD               | 'unknown'                         | EMPTY_INIT || false

        TypeValidators.PIPELINE_TYPE                 | 'ACESEQ'                          | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | 'ALIGNMENT'                       | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | 'INDEL'                           | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | 'MUTATIONAL_SIGNATURE'            | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | 'SNV'                             | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | 'SOPHIA'                          | EMPTY_INIT || true
        TypeValidators.PIPELINE_TYPE                 | ''                                | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | null                              | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'UNKNOWN'                         | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'aceseq'                          | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'alignment'                       | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'indel'                           | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'mutation_signature'              | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'snv'                             | EMPTY_INIT || false
        TypeValidators.PIPELINE_TYPE                 | 'sophia'                          | EMPTY_INIT || false

        TypeValidators.INSTANCE_LOGO                 | 'NONE'                            | EMPTY_INIT || true
        TypeValidators.INSTANCE_LOGO                 | 'CHARITE'                         | EMPTY_INIT || true
        TypeValidators.INSTANCE_LOGO                 | 'DKFZ'                            | EMPTY_INIT || true
        TypeValidators.INSTANCE_LOGO                 | ''                                | EMPTY_INIT || false
        TypeValidators.INSTANCE_LOGO                 | null                              | EMPTY_INIT || false
        TypeValidators.INSTANCE_LOGO                 | 'UNKNOWN'                         | EMPTY_INIT || false
        TypeValidators.INSTANCE_LOGO                 | 'None'                            | EMPTY_INIT || false
        TypeValidators.INSTANCE_LOGO                 | 'none'                            | EMPTY_INIT || false

        TypeValidators.JOB_SCHEDULER                 | 'PBS'                             | EMPTY_INIT || true
        TypeValidators.JOB_SCHEDULER                 | 'LSF'                             | EMPTY_INIT || true
        TypeValidators.JOB_SCHEDULER                 | ''                                | EMPTY_INIT || false
        TypeValidators.JOB_SCHEDULER                 | null                              | EMPTY_INIT || false
        TypeValidators.JOB_SCHEDULER                 | 'pbs'                             | EMPTY_INIT || false
        TypeValidators.JOB_SCHEDULER                 | 'lsf'                             | EMPTY_INIT || false
        TypeValidators.JOB_SCHEDULER                 | 'text'                            | EMPTY_INIT || false

        TypeValidators.SEQ_CENTER_NAME               | CENTER_A                          | INIT_SEQ_CENTER || true
        TypeValidators.SEQ_CENTER_NAME               | CENTER_B                          | INIT_SEQ_CENTER || true
        TypeValidators.SEQ_CENTER_NAME               | ''                                | INIT_SEQ_CENTER || false
        TypeValidators.SEQ_CENTER_NAME               | null                              | INIT_SEQ_CENTER || false
        TypeValidators.SEQ_CENTER_NAME               | 'OtherCenterName'                 | INIT_SEQ_CENTER || false

        TypeValidators.SEQ_TYPE_RODDY_NAME           | SEQ_TYPE_RODDY_NAME_A             | INIT_SEQ_TYPES || true
        TypeValidators.SEQ_TYPE_RODDY_NAME           | SEQ_TYPE_RODDY_NAME_B             | INIT_SEQ_TYPES || true
        TypeValidators.SEQ_TYPE_RODDY_NAME           | ''                                | INIT_SEQ_TYPES || false
        TypeValidators.SEQ_TYPE_RODDY_NAME           | null                              | INIT_SEQ_TYPES || false
        TypeValidators.SEQ_TYPE_RODDY_NAME           | 'OtherSeqTypeName'                | INIT_SEQ_TYPES || false

        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | SEQ_TYPE_DISPLAY_NAME_A           | INIT_SEQ_TYPES_PROCESSING_NAMES || true
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | SEQ_TYPE_DISPLAY_NAME_B           | INIT_SEQ_TYPES_PROCESSING_NAMES || true
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | ''                                | INIT_SEQ_TYPES_PROCESSING_NAMES || false
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | null                              | INIT_SEQ_TYPES_PROCESSING_NAMES || false
        TypeValidators.SEQ_TYPE_PROCESSING_NAME      | 'OtherSeqTypeName'                | INIT_SEQ_TYPES_PROCESSING_NAMES || false

        TypeValidators.WORKFLOW_NAME                 | WORKFLOW_NAME_A                   | INIT_JOB_EXECUTION_PLANS || true
        TypeValidators.WORKFLOW_NAME                 | WORKFLOW_NAME_B                   | INIT_JOB_EXECUTION_PLANS || true
        TypeValidators.WORKFLOW_NAME                 | ''                                | INIT_JOB_EXECUTION_PLANS || false
        TypeValidators.WORKFLOW_NAME                 | null                              | INIT_JOB_EXECUTION_PLANS || false
        TypeValidators.WORKFLOW_NAME                 | 'OtherWorkflowName'               | INIT_JOB_EXECUTION_PLANS || false
    }

    private final static Closure EMPTY_INIT = { }

    private final static Closure INIT_SEQ_CENTER = { ->
        [
                CENTER_A,
                CENTER_B,
        ].each {
            DomainFactory.createSeqCenter([
                    name: it,
            ])
        }
    }

    private final static Closure INIT_SEQ_TYPES = { ->
        [
                SEQ_TYPE_RODDY_NAME_A,
                SEQ_TYPE_RODDY_NAME_B,
        ].each {
            DomainFactory.createSeqType([
                    roddyName: it,
            ])
        }
    }

    private final static Closure INIT_SEQ_TYPES_PROCESSING_NAMES = { ->
        [
                SEQ_TYPE_DISPLAY_NAME_A,
                SEQ_TYPE_DISPLAY_NAME_B,
        ].each {
            DomainFactory.createSeqType([
                    displayName: it,
            ])
        }
    }

    private final static Closure INIT_JOB_EXECUTION_PLANS = { ->
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
