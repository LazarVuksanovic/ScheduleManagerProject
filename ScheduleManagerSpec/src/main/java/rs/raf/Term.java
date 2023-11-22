package rs.raf;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class Term implements Comparable<Term> {

    private DayOfWeek day;
    private LocalDate date;
    private LocalDate dateEnd;
    private LocalTime timeStart;
    private LocalTime timeEnd;
    private Place place;
    private Map<String, String> info;

    /**
     * Creates new instance of Term
     */
    public Term(){

    }

    /**
     * Creates new instance of Term
     * @param date date of term
     * @param timeStart time start of term
     * @param timeEnd time end of term
     * @param place term palce
     * @param info info about term
     */
    public Term(LocalDate date, LocalTime timeStart, LocalTime timeEnd, Place place, Map<String, String> info) {
        this.date = date;
        this.dateEnd = null;
        this.day = null;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.place = place;
        this.info = info;
    }

    /**
     * Creates new instance of Term
     * @param date start date of term
     * @param dateEnd end date of term
     * @param day day of week
     * @param timeStart time start of term
     * @param timeEnd time end of term
     * @param place term palce
     * @param info info about term
     */
    public Term(LocalDate date, LocalDate dateEnd, DayOfWeek day, LocalTime timeStart, LocalTime timeEnd, Place place, Map<String, String> info) {
        this.date = date;
        this.dateEnd = dateEnd;
        this.day = day;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.place = place;
        this.info = info;
    }

    /**
     * Creates new instance of Term
     * @param term Term to be copied from
     */
    public Term(Term term){
        this.date = term.getDate();
        this.dateEnd = term.getDateEnd();
        this.day = term.getDay();
        this.timeStart = term.getTimeStart();
        this.timeEnd = term.getTimeEnd();
        this.place = term.getPlace();
        this.info = term.getInfo();
    }

    /**
     * Compares two Terms
     * @param o object to be compared
     * @return compared value
     */
    @Override
    public int compareTo(Term o) {
        int dateComparison = this.getDate().compareTo(o.getDate());

        if (dateComparison == 0){
            int timeStartComparison = this.getTimeStart().compareTo(o.getTimeStart());
            if (timeStartComparison == 0) {
                int timeEndComparison = this.getTimeEnd().compareTo(o.getTimeEnd());
                if(timeEndComparison == 0)
                    return this.getPlace().getName().compareTo(o.getPlace().getName());
            }
            else
                return timeStartComparison;
        }

        return dateComparison;
    }

    /**
     * Returns Term values
     * @return Returns String that contains term values
     */
    @Override
    public String toString() {
        return this.place.getName()+" | "+this.getDay()+" | "+this.getTimeStart()+" | "+ this.getTimeEnd()+" | "+this.getDate()+" | "+this.getDateEnd()+" | "+
                this.getInfo();
    }
}
