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
package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.ValidatorUtil

/**
 * For each individual disease/control pairs are compared in the analysis pipelines. These pairs are defined in the GUI and stored in this domain.
 * The sampleTypes which have to be compared differ between the individuals, which is why it has to be specific for an individual.
 * It can happen that not only disease and control shall be compared but also disease&disease.
 * This is why the properties are not call disease and control.
 * The sample pairs can also be used for other purposes i.e. coverage combination between disease and control
 */
class SamplePair implements Entity {

    final static String SNV_RESULTS_PATH_PART = 'snv_results'
    final static String INDEL_RESULTS_PATH_PART = 'indel_results'
    final static String SOPHIA_RESULTS_PATH_PART = 'sv_results'
    final static String ACESEQ_RESULTS_PATH_PART = 'cnv_results'
    final static String RUN_YAPSA_RESULTS_PATH_PART = 'mutational_signatures_results'

    static enum ProcessingStatus {

        /**
         * The sample pair needs to be processed as soon as conditions are satisfied (coverage is high enough etc).
         */
        NEEDS_PROCESSING,

        /**
         * The sample pair does not have to be processed, because there already is an {@link BamFilePairAnalysis} which
         * is up-to-date.
         */
        NO_PROCESSING_NEEDED,

        /**
         * The sample pair shall not be processed (again), for example because there was a notice from the responsible
         * bioinformatician to exclude it.
         */
        DISABLED,
    }

    AbstractMergingWorkPackage mergingWorkPackage1
    AbstractMergingWorkPackage mergingWorkPackage2

    /**
     * not used, only to check whether the path is unique
     */

    ProcessingStatus snvProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus indelProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus sophiaProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus aceseqProcessingStatus = ProcessingStatus.NEEDS_PROCESSING
    ProcessingStatus runYapsaProcessingStatus = ProcessingStatus.NEEDS_PROCESSING

    boolean isProcessingDisabled() {
        return  snvProcessingStatus      == ProcessingStatus.DISABLED &&
                indelProcessingStatus    == ProcessingStatus.DISABLED &&
                sophiaProcessingStatus   == ProcessingStatus.DISABLED &&
                aceseqProcessingStatus   == ProcessingStatus.DISABLED &&
                runYapsaProcessingStatus == ProcessingStatus.DISABLED
    }

    static constraints = {
        mergingWorkPackage2 unique: 'mergingWorkPackage1', validator: ValidatorUtil.messageArgs("mergingWorkPackage2") {
            AbstractMergingWorkPackage val, SamplePair obj ->
            if (val == obj.mergingWorkPackage1) {
                reject("equal")
            }
            // For one sample pair the individual, the seqType and the pipeline must be the same.
            // To provide the possibility to create sample pairs manually other properties are ignored here.
            ['individual', 'seqType'].each {
                def mwp1Value = obj.mergingWorkPackage1."${it}"
                def mwp2Value = val."${it}"
                if (mwp1Value != mwp2Value) {
                    reject("different", [it, mwp1Value, mwp2Value])
                }
            }
        }
    }

    static mapping = {
        /**
         * sample_pair_snv_idx1 is used by SnvCallingService.samplePairForProcessing.
         * processing_status must be the first column in this index! Grails does not provide a means to specify this, so
         * this must be done via SQL.
         */
        mergingWorkPackage1 index: 'sample_pair_snv_idx1,sample_pair_indel_idx1,sample_pair_sophia_idx1,sample_pair_aceseq_idx1,sample_pair_runyapsa_idx1'
        mergingWorkPackage2 index: 'sample_pair_snv_idx1,sample_pair_indel_idx1,sample_pair_sophia_idx1,sample_pair_aceseq_idx1,sample_pair_runyapsa_idx1'
        snvProcessingStatus index: 'sample_pair_snv_idx1'
        indelProcessingStatus index: 'sample_pair_indel_idx1'
        sophiaProcessingStatus index: 'sample_pair_sophia_idx1'
        aceseqProcessingStatus index: 'sample_pair_aceseq_idx1'
        runYapsaProcessingStatus index: 'sample_pair_runyapsa_idx1'
    }

    Project getProject() {
        return individual.project
    }

    Individual getIndividual() {
        assert mergingWorkPackage1.individual.id == mergingWorkPackage2.individual.id
        return mergingWorkPackage1.individual
    }

    SeqType getSeqType() {
        assert mergingWorkPackage1.seqType.id == mergingWorkPackage2.seqType.id
        return mergingWorkPackage1.seqType
    }

    SampleType getSampleType1() {
        return mergingWorkPackage1.sampleType
    }

    SampleType getSampleType2() {
        return mergingWorkPackage2.sampleType
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control
     */
    OtpPath getSnvSamplePairPath() {
        return buildPath(SNV_RESULTS_PATH_PART)
    }

    OtpPath getIndelSamplePairPath() {
        return buildPath(INDEL_RESULTS_PATH_PART)
    }

    OtpPath getSophiaSamplePairPath() {
        return buildPath(SOPHIA_RESULTS_PATH_PART)
    }

    OtpPath getAceseqSamplePairPath() {
        return buildPath(ACESEQ_RESULTS_PATH_PART)
    }

    OtpPath getRunYapsaSamplePairPath() {
        return buildPath(RUN_YAPSA_RESULTS_PATH_PART)
    }

    private OtpPath buildPath(String analysisPath) {
        return new OtpPath(individual.getViewByPidPath(seqType), analysisPath, seqType.libraryLayoutDirName, "${sampleType1.dirName}_${sampleType2.dirName}")
    }

    String toStringWithoutId() {
        return "${individual.mockPid} ${sampleType1.name} ${sampleType2.name} ${seqType.displayName} ${seqType.libraryLayout}"
    }

    @Override
    String toString() {
        return "SP ${id}: ${toStringWithoutId()}"
    }

    AbstractSnvCallingInstance findLatestSnvCallingInstance() {
        return findLatestInstance(AbstractSnvCallingInstance) as AbstractSnvCallingInstance
    }

    IndelCallingInstance findLatestIndelCallingInstance() {
        return findLatestInstance(IndelCallingInstance) as IndelCallingInstance
    }

    SophiaInstance findLatestSophiaInstance() {
        return findLatestInstance(SophiaInstance) as SophiaInstance
    }

    AceseqInstance findLatestAceseqInstance() {
        return findLatestInstance(AceseqInstance) as AceseqInstance
    }

    RunYapsaInstance findLatestRunYapsaInstance() {
        return findLatestInstance(RunYapsaInstance) as RunYapsaInstance
    }

    private BamFilePairAnalysis findLatestInstance(Class instanceClass) {
        BamFilePairAnalysis criteria = instanceClass.createCriteria().get {
            eq ('samplePair', this)
            order('id', 'desc')
            maxResults(1)
        }
        if (!criteria?.withdrawn) {
            return criteria
        }
        return null
    }
}
