package de.dkfz.tbi.ngstools.qualityAssessment

public class OnlineStatistics {

    private int size
    private double median
    private double mean
    private double standardDeviation
    private double rms

    // helper variables used in the calculations
    private double absoluteSum
    private double tempS2

    private Map<Integer, Integer> histogram = new HashMap<Integer, Integer>()

    public void add(int value) {
        value = Math.abs(value)
        Integer count = histogram.get(value)
        if (!count) {
            histogram.put(value, 1)
        } else {
            histogram.put(value, ++count)
        }
        meanAndStandardDeviationAndRMSUpdate(value)
    }

    public double getMedian() {
        int countOfNumber = histogram.values()?histogram.values().sum():0
        if (countOfNumber == 0) {
            throw new ArithmeticException()
        }
        if (countOfNumber % 2 == 0) {
            int position = countOfNumber/2 as int
            return (getElementAt(position) + getElementAt(position + 1)) / 2
        } else {
            return getElementAt(Math.ceil(countOfNumber/2) as int)
        }
    }

    public int getElementAt(int position) {
        int total = histogram.values().sum()
        List list = histogram.keySet().sort()
        int counter = 0
        for (key in list) {
            counter = counter + histogram.get(key)
            if (counter >= position) {
                return key
            }
        }
    }

    public double getMean() {
        if (!size) {
            throw new ArithmeticException()
        }
        return mean
    }

    /**
     * Retrieves the standard deviation of the sample (also know as uncorrected sample standard deviation)
     * @return the standard deviation of the sample
     */
    public double getStandardDeviation() {
        if (!size) {
            throw new ArithmeticException()
        }
        return standardDeviation
    }

    /**
     * Retrieves the root mean square
     * @return the root mean square of the sample
     */
    public double getRms() {
        if (!size) {
            throw new ArithmeticException()
        }
        return rms
    }

    /**
     * Retrieves the histogram of the data
     * @returns The histogram
     */
    public Map getHistogram() {
        return histogram
    }

    /**
     * Retrieves the histogram in form of a Map where the value are the counts per bin (going from the maximum bin that contains any value and with step of binSize)
     * @returns A Map where the keys are sorted by bin
     */
    public Map getHistogram(int binSize) {
        if (binSize == 0) {
            return histogram
        }
        Map<Integer, Integer> histogramBin = new TreeMap<Integer, Integer>()
        List keys = histogram.keySet().sort()
        // first bin of the histogram
        int bin = keys.first() - keys.first() % binSize
        keys.each { int key ->
            while (key >= (bin + binSize)) {
                bin += binSize
                histogramBin.put(bin, 0)
            }
            int count = (!histogramBin.get(bin)) ? 0 : histogramBin.get(bin)
            histogramBin.put(bin, count + histogram.get(key))
        }
        return histogramBin
    }

    /**
     * Helper method to convert the added data to a list
     */
    public List toList(Map map) {
        List list = []
        map.each { key, value ->
            value.times {
            list << key
            }
        }
        return list
    }

    public List toList() {
        return toList(histogram)
    }

    private void meanAndStandardDeviationAndRMSUpdate(int newValue) {
        size++
        absoluteSum += newValue
        mean = absoluteSum / size

        tempS2 += Math.pow(newValue, 2)
        double temp = tempS2 / size
        rms = Math.sqrt(temp)
        standardDeviation = Math.sqrt(temp - Math.pow(mean, 2))
    }
}
