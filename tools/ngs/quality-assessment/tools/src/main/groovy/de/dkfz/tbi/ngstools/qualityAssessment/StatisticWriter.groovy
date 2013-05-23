package de.dkfz.tbi.ngstools.qualityAssessment

import com.google.gson.Gson
import groovy.json.JsonOutput

class StatisticWriter implements FileWriter {

    @Override
    public void setParameters(Parameters parameters) {
        //parameters are not needed here, so setting ignored
    }

    public void write(File file, GenomeStatistic<?> genomeStatistic) {
        file << toJsonString(genomeStatistic)
    }

    private String toJsonString(GenomeStatistic<?> genomeStatistic) {
        Map<String, ChromosomeStatistic> map = genomeStatistic.getChromosomes()
        // using a pretty printer to make it easier to debug
        String jsonString = JsonOutput.prettyPrint(new Gson().toJson(map))
        return jsonString
    }
}
