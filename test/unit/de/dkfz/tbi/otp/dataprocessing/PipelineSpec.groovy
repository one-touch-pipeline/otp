package de.dkfz.tbi.otp.dataprocessing

import spock.lang.Specification
import spock.lang.Unroll


class PipelineSpec extends Specification {

    @Unroll
    void "test name type constraint"() {

        given:
        Pipeline pipeline = new Pipeline(
                name: name,
                type: type,
        )

        expect:
        valid == pipeline.validate()

        where:
        name                           | type                    || valid
        Pipeline.Name.OTP_SNV          | Pipeline.Type.SNV       || true
        Pipeline.Name.OTP_SNV          | Pipeline.Type.ALIGNMENT || false
        Pipeline.Name.RODDY_SNV        | Pipeline.Type.SNV       || true
        Pipeline.Name.RODDY_SNV        | Pipeline.Type.ALIGNMENT || false
        Pipeline.Name.PANCAN_ALIGNMENT | Pipeline.Type.SNV       || false
        Pipeline.Name.PANCAN_ALIGNMENT | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.DEFAULT_OTP      | Pipeline.Type.SNV       || false
        Pipeline.Name.DEFAULT_OTP      | Pipeline.Type.ALIGNMENT || true


    }
}
