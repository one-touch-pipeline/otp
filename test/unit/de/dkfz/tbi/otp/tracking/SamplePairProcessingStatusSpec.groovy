package de.dkfz.tbi.otp.tracking

import spock.lang.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*


class SamplePairProcessingStatusSpec extends Specification {

    @Unroll
    void "getVariantCallingProcessingStatus"() {
        given:
        SamplePairProcessingStatus status = new SamplePairProcessingStatus(null, values.snv, null, values.indel, null, values.sophia, null, values.aceseq, null, values.runYapsa, null)
        def result = TrackingService.combineStatuses([values.snv, values.indel, values.sophia, values.aceseq, values.runYapsa], Closure.IDENTITY)

        expect:
        result == status.getVariantCallingProcessingStatus()

        where:
        //generates a Permutation List for indel, snv, aceseq and runYapsa.
        values << {
            List ret = []
            List list = [NOTHING_DONE_WONT_DO, NOTHING_DONE_MIGHT_DO, PARTLY_DONE_WONT_DO_MORE, PARTLY_DONE_MIGHT_DO_MORE, ALL_DONE]

            list.each { snv ->
                list.each { indel ->
                    list.each { aceseq ->
                        list.each { sophia ->
                            list.each { runYapsa ->
                                ret << [
                                        snv     : snv,
                                        indel   : indel,
                                        sophia  : sophia,
                                        aceseq  : aceseq,
                                        runYapsa: runYapsa,
                                ]
                            }
                        }
                    }
                }
            }
            ret
        }()
    }
}