package rs.raf;

public class ScheduleImplManager {
    private static Schedule scheduleImplementation;

    public static Schedule getScheduleSpecification() {
        return scheduleImplementation;
    }

    /**
     * Changes implemention
     * @param scheduleSpecification sets new specification
     */
    public static void setScheduleSpecification(Schedule scheduleSpecification) {
        ScheduleImplManager.scheduleImplementation = scheduleSpecification;
    }
}
