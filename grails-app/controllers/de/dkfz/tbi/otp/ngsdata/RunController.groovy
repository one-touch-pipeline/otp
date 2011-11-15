package de.dkfz.tbi.otp.ngsdata

class RunController {

    static scaffold = Run

    //static list = {
    // render "test "
    //}


    def display = {

        //render params.id
        Run run = Run.findByName(params.id)
        long id = run.id

        redirect(action: "show", id:id)
    }
}
