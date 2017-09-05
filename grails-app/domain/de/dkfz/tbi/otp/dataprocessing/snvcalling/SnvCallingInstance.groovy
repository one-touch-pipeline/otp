package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * For each tumor-control pair the snv pipeline will be called.
 * The SnvCallingInstance symbolizes one call of the pipeline.
 */
class SnvCallingInstance extends BamFilePairAnalysis implements ProcessParameterObject, Entity {

    static constraints = {
        processingState validator: { val, obj ->
            // there must be at least one withdrawn {@link SnvJobResult}
            // if {@link this#withdrawn} is true
            if (obj.withdrawn == true) {
                return !SnvJobResult.findAllBySnvCallingInstanceAndWithdrawn(obj, true).empty || SnvJobResult.findAllBySnvCallingInstance(obj).empty
            } else {
                return true
            }
        }
        config validator: { val, obj ->
            SnvConfig.isAssignableFrom(Hibernate.getClass(val))
        }
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32
     */
    @Override
    OtpPath getInstancePath() {
        return new OtpPath(samplePair.snvSamplePairPath, instanceName)
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32/config.txt
     */
    OtpPath getConfigFilePath() {
        return new OtpPath(instancePath, "config.txt")
    }


    File getCombinedPlotPath() {
        return new File(getWorkDirectory(), "snvs_${getIndividual().pid}_allSNVdiagnosticsPlots.pdf")
    }


    /**
     * Returns the non-withdrawn, finished {@link SnvJobResult} for the specified {@link SnvCallingStep} belonging to
     * the latest (even {@link SnvCallingInstance#withdrawn}) {@link SnvCallingInstance} that has such a result and is based on the same BAM files as this instance;
     * <code>null</code> if no such {@link SnvCallingInstance} exists.
     */
    SnvJobResult findLatestResultForSameBamFiles(final SnvCallingStep step) {
        assert step
        final SnvJobResult result = atMostOneElement(SnvJobResult.createCriteria().list {
            eq 'step', step
            eq 'withdrawn', false
            eq 'processingState', AnalysisProcessingStates.FINISHED
            snvCallingInstance {
                sampleType1BamFile {
                    eq 'id', sampleType1BamFile.id
                }
                sampleType2BamFile {
                    eq 'id', sampleType2BamFile.id
                }
            }
            order('snvCallingInstance.id', 'desc')
            maxResults(1)
        })
        if (result != null) {
            assert result.step == step
            assert !result.withdrawn
            assert result.processingState == AnalysisProcessingStates.FINISHED
            assert result.sampleType1BamFile.id == sampleType1BamFile.id
            assert result.sampleType2BamFile.id == sampleType2BamFile.id
        }
        return result
    }

    @Override
    public String toString() {
        return "SCI ${id} ${withdrawn ? ' (withdrawn)': ''}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }

    void withdraw() {
        withTransaction {
            SnvJobResult.findByInputResultIsNullAndSnvCallingInstance(this).each {
                it.withdraw()
            }
            super.withdraw()
        }
    }

}
