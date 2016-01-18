package de.dkfz.tbi.util.spreadsheet.validation

class Level extends java.util.logging.Level {

    static final Level ERROR = new Level('ERROR', 1000)

    Level(String name, int value) {
        super(name, value)
    }

    Level(String name, int value, String resourceBundleName) {
        super(name, value, resourceBundleName)
    }
}
