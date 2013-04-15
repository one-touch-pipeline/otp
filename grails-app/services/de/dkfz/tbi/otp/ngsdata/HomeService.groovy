package de.dkfz.tbi.otp.ngsdata

class HomeService {

    Map projectQuery() {
         List query = Sequence.createCriteria().listDistinct() {
             projections {
                 groupProperty("projectName")
             }
             order ("projectName")
         }

         Map queryMap = [:]

         query.each { iter ->
             List seq = Sequence.createCriteria().listDistinct {
                 eq("projectName", iter)
                 projections {
                     groupProperty("seqTypeName")
                 }
                 order ("seqTypeName")
             }
             queryMap[iter] = seq.toListString().replace("[", "").replace("]", "")
         }
         return queryMap
    }
}
