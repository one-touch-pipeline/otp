package de.dkfz.tbi.otp.ngsdata

import spock.lang.*

@Mock([
        SeqPlatformModelLabel,
])
class SeqPlatformModelLabelServiceSpec extends MetadataFieldsServiceSpec<SeqPlatformModelLabel> {
    SeqPlatformModelLabelService seqPlatformModelLabelService = new SeqPlatformModelLabelService()

    @Override
    protected MetadataFieldsService getService() {
        return seqPlatformModelLabelService
    }
}
