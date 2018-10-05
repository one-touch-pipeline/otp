package de.dkfz.tbi.otp.utils

import grails.validation.Validateable

@Validateable
class DataTableCommand implements Serializable {

    int iDisplayStart = 0
    int iDisplayLength = 10
    int iSortCol_0 = 0
    String sSortDir_0 = "asc"
    String sEcho
    String sSearch = ""

    static constraints = {
    }

    void setIDisplayStart(int start) {
        this.iDisplayStart = start
    }

    void setIDisplayLength(int length) {
        if (length <= 100) {
            this.iDisplayLength = length
        } else {
            this.iDisplayLength = 100
        }
    }

    Map dataToRender() {
        return [
            sEcho: this.sEcho,
            offset: this.iDisplayStart,
            iSortCol_0: this.iSortCol_0,
            sSortDir_0: this.sSortDir_0,
            aaData: [],
            ]
    }

    boolean getSortOrder() {
        return this.sSortDir_0 == "asc"
    }

}
