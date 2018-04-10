package de.dkfz.tbi.otp.ngsdata

import spock.lang.*

@Mock([
        SequencingKitLabel,
])
class SequencingKitLabelServiceSpec extends MetadataFieldsServiceSpec<SequencingKitLabel> {
    SequencingKitLabelService sequencingKitLabelService = new SequencingKitLabelService()

    @Override
    protected MetadataFieldsService getService() {
        return sequencingKitLabelService
    }
}
