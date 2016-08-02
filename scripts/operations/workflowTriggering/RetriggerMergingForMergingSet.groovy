//Retrigger merging for specified (merging set) id(s)

//Re-trigger "merging" workflow for the merging sets given by id.

import de.dkfz.tbi.otp.dataprocessing.*

println "\n\n rerun merging: "

List<MergingSet> d = MergingSet.withCriteria {
    eq("status", MergingSet.State.INPROGRESS)
    'in'("id", [

        ] as long[])
    }

println d.size()

d = d.findAll { set->
    set.identifier == MergingSet.createCriteria().get {
        eq("mergingWorkPackage", set.mergingWorkPackage)
        projections { max("identifier") }
    }
}

d.each {
  println "id: ${it.id}, set: ${it.identifier} on ${it.mergingWorkPackage.seqType} ${it.mergingWorkPackage.sample} ${it.mergingWorkPackage.sample.individual.project}"
}
println d.size()

/*
d.each {
  it.status = MergingSet.State.NEEDS_PROCESSING
  println it.save(flush: true)
}
// */
print ""
''
