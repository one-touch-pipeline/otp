package de.dkfz.tbi.otp.ngsdata

class WorkPackagesDSL {

    List<SeqType> wp = null

    def static make (closure) {
        WorkPackagesDSL wpDSL = new WorkPackagesDSL()
        wpDSL.wp = []
        closure.delegate = wpDSL
        closure()
        return wpDSL.wp
    }

    def seqType (String type, String library) {
        SeqType s = SeqType.findByNameAndLibraryLayout(type, library)
        wp << s
    }
}
