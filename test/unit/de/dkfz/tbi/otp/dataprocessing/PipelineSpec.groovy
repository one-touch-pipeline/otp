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
        name                               | type                    || valid
        Pipeline.Name.RODDY_SNV            | Pipeline.Type.SNV       || true
        Pipeline.Name.RODDY_SNV            | Pipeline.Type.ALIGNMENT || false
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Type.SNV       || false
        Pipeline.Name.PANCAN_ALIGNMENT     | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.RODDY_RNA_ALIGNMENT  | Pipeline.Type.SNV       || false
        Pipeline.Name.RODDY_RNA_ALIGNMENT  | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Type.ALIGNMENT || true
        Pipeline.Name.EXTERNALLY_PROCESSED | Pipeline.Type.SNV       || false

    }
}
