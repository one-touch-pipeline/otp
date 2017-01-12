package de.dkfz.tbi.otp.monitor


abstract class PipelinesChecker<E> {

    abstract List handle(List<E> objectsToCheck, MonitorOutputCollector output)
}
