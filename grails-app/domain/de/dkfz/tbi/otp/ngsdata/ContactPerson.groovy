package de.dkfz.tbi.otp.ngsdata

class ContactPerson {

    String fullName
    String email

    static belongsTo = [seqCenter : SeqCenter]

    static constraints = {
        fullName(blank: false)
        email(email:true)
    }

    static mapping = {
        seqCenter index: "contact_person_seq_center_idx"
    }
}
