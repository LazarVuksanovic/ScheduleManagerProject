package rs.raf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.Getter;
import lombok.Setter;
import rs.raf.adapters.LocalDateAdapter;
import rs.raf.adapters.LocalTimeAdapter;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
@Setter
public abstract class Schedule {

    private List<Term> terms;
    private List<Place> places;
    private List<LocalDate> freeDays;
    private LocalDate scheduleEndDate;
    private LocalDate scheduleStartDate;

    /**
     * Imports data from file
     * @param file File from which places will be loaded
     * @throws IOException File does not exist
     */
    public abstract void loadPlaces(File file) throws IOException;

    /**
     * Imports data from CSV file
     * @param file File from which free days will be loaded
     * @throws IOException File does not exist
     */
    public void loadFreeDays(File file) throws IOException {
        List<LocalDate> freeDays = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        FileReader fr = new FileReader(file);
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(true).build();
        CSVReader reader = new CSVReaderBuilder(fr).withCSVParser(parser).build();

        String[] line;
        while ((line = reader.readNext()) != null) {
            for(String cell : line){
                freeDays.add(LocalDate.parse(cell, formatter));
            }

        }
        this.setFreeDays(freeDays);
    }

    /**
     * Imports data from file
     * @param file File from which terms will be loaded
     * @throws IOException File does not exist
     */
    public abstract void makeSchedule(File file) throws IOException;

    /**
     * Exports data within a date span to JSON file
     * @param file File in which data will be exported
     * @param dateStart Date from, if null sets to scheduleStartDate
     * @param dateEnd Date to, if null sets to scheduleStartDate
     * @param header Header in which value is placed, if null ignored
     * @param value Select terms with this value, if null ignored
     * @throws IOException File does not exist
     */
    public void exportScheduleJSON(File file, LocalDate dateStart, LocalDate dateEnd, String header, String value) throws IOException{
        List<Term> termsToExport = new ArrayList<>();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .setPrettyPrinting()
                .create();

        if(dateStart == null) dateStart = this.getScheduleStartDate();
        if(dateEnd == null) dateEnd = this.getScheduleEndDate();

        if(header == null || value == null)
            termsToExport.addAll(searchTerm(dateStart, dateEnd));
        else{
            Map<String, String> val = new HashMap<>();
            val.put(header, value);
            termsToExport.addAll(searchTermByTermInfo(val));
        }

        PrintStream writer = new PrintStream(file);
        gson.toJson(termsToExport, writer);
    }

    /**
     * Exports data within a date span to CSV file
     * @param file File in which data will be exported
     * @param dateStart Date from, if null sets to scheduleStartDate
     * @param dateEnd Date to, if null sets to scheduleStartDate
     * @param header Header in which value is placed, if null ignored
     * @param value Select terms with this value, if null ignored
     * @throws IOException File does not exist
     */
    public void exportScheduleCSV(File file, LocalDate dateStart, LocalDate dateEnd, String header, String value) throws IOException {
        List<Term> termsToExport = new ArrayList<>();
        if(dateStart == null) dateStart = this.getScheduleStartDate();
        if(dateEnd == null) dateEnd = this.getScheduleEndDate();
        if(header == null || value == null)
            termsToExport.addAll(searchTerm(dateStart, dateEnd));
        else{
            Map<String, String> val = new HashMap<>();
            val.put(header, value);
            termsToExport.addAll(searchTermByTermInfo(val));
        }
        try (FileWriter fileWriter = new FileWriter(file);
             com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(fileWriter)) {
             String[] headerString = new String[6+getTerms().get(0).getInfo().size()];
             headerString[0] = "Mesto";
             headerString[1] = "Pocetni_Datum";
             headerString[2] = "Krajnji_Datum";
             headerString[3] = "Dan";
             headerString[4] = "Pocetak_Termina";
             headerString[5] = "Kraj_Termina";
            int ii = 6;
            for(Map.Entry<String,String> entry : getTerms().get(0).getInfo().entrySet())
                headerString[ii++] = entry.getKey().replace("\"", "");
            csvWriter.writeNext(headerString);
            for(Term term : termsToExport){
                String[] elements = new String[6+term.getInfo().size()];
                elements[0] = term.getPlace().getName();
                elements[1] = term.getDate().toString();
                elements[2] = term.getDateEnd().toString();
                elements[3] = term.getDay().toString();
                elements[4] = term.getTimeStart().toString();
                elements[5] = term.getTimeEnd().toString();
                int i = 6;
                for(Map.Entry<String,String> entry : term.getInfo().entrySet())
                    elements[i++] = entry.getValue();

                csvWriter.writeNext(elements);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Adds new Term
     * @param term new Term
     */
    public abstract void addTerm(Term term);

    /**
     * Adds new Term
     * @param date date of new Term
     * @param timeStart time start of new Term
     * @param timeEnd time end of new Term
     * @param place Place of new Term
     */
    public abstract void addTerm(LocalDate date, LocalTime timeStart, LocalTime timeEnd, Place place);

    /**
     * Deletes Term from Schedule
     * @param term term to be deleted from Schedule
     */
    public abstract void deleteTerm(Term term);

    /**
     * Deletes Terms within date span from Schedule
     * @param dateStart date from
     * @param dateEnd date to
     */
    public abstract void deleteTermInDateSpan(LocalDate dateStart, LocalDate dateEnd);

    /**
     * Deletes Terms within date and time span from Schedule
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     */
    public abstract void deleteTermInDateAndTimeSpan(LocalDate dateStart,LocalDate dateEnd,LocalTime timeStart,LocalTime timeEnd);

    /**
     * Deletes Terms on given date within time span from Schedule
     * @param date date
     * @param timeStart time from
     * @param timeEnd time to
     */
    public abstract void deleteTerm(LocalDate date, LocalTime timeStart, LocalTime timeEnd);

    /**
     * Deletes Terms at given Place
     * @param place Terms at this Place will be deleted
     */
    public abstract void deleteTerm(Place place);

    /**
     * Deletes Terms at given Place and date within time span from Schedule
     * @param place Place
     * @param date Date
     * @param timeStart time from
     * @param timeEnd time to
     */
    public abstract void deleteTerm(Place place,LocalDate date,LocalTime timeStart,LocalTime timeEnd);

    /**
     * Deletes Terms at given Place within date and time span from Schedule
     * @param place Place
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     */
    public abstract void deleteTerm(Place place,LocalDate dateStart,LocalDate dateEnd,LocalTime timeStart,LocalTime timeEnd);

    /**
     * Reschedules a Term
     * @param term Term to be rescheduled
     * @param newDate new date
     * @param newTime new time
     * @return Instance of a moved Term
     */
    public abstract Term moveTerm(Term term, LocalDate newDate, LocalTime newTime);

    /**
     * Reschedules a Term
     * @param term Term to be rescheduled
     * @param newDate new date
     * @param newTime new time
     * @param newPlace new place
     * @return Instance of a moved Term
     */
    public abstract Term moveTerm(Term term, LocalDate newDate, LocalTime newTime,Place newPlace);

    /**
     * Reschedules a Term
     * @param term Term to be rescheduled
     * @param newDate new date
     * @param newTimeStart new start time
     * @param newTimeEnd new end time
     * @return Instance of a moved Term
     */
    public abstract Term moveTerm(Term term, LocalDate newDate, LocalTime newTimeStart, LocalTime newTimeEnd);

    /**
     * Reschedules a Term
     * @param term Term to be rescheduled
     * @param newDate new date
     * @param newTimeStart new start time
     * @param newTimeEnd new end time
     * @param newPlace new place
     * @return Instance of a moved Term
     */
    public abstract Term moveTerm(Term term, LocalDate newDate, LocalTime newTimeStart, LocalTime newTimeEnd, Place newPlace);


    /**
     * Searches existing Terms
     * @param place Terms at this place
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(Place place);

    /**
     * Searches existing Terms within date span
     * @param startDate date from
     * @param endDate date to
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(LocalDate startDate, LocalDate endDate);

    /**
     * Searches existing Terms within date span at given Place
     * @param place Terms at this place
     * @param dateStart date from
     * @param dateEnd date to
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(Place place, LocalDate dateStart, LocalDate dateEnd);

    /**
     * Searches existing Terms with given info and place
     * @param place Terms at this place
     * @param properties Term info
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(Place place, Map<String ,String> properties);

    /**
     * Searches existing Terms within date and time span at given Place
     * @param place Terms at this place
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(Place place, LocalDate dateStart, LocalDate dateEnd, LocalTime timeStart, LocalTime timeEnd);

    /**
     * Searches existing Terms with given place info
     * @param properties Place info
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(Map<String ,Integer> properties);

    /**
     * Searches existing Terms with given term info
     * @param properties Term info
     * @return List of Terms
     */
    public abstract List<Term> searchTermByTermInfo(Map<String ,String> properties);

    /**
     * Searches existing Terms within date and time span
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     * @return List of Terms
     */
    public abstract List<Term> searchTerm(LocalDate dateStart,LocalDate dateEnd, LocalTime timeStart, LocalTime timeEnd);

    /**
     * Searches available Terms at given date
     * @param date date
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(LocalDate date);

    /**
     * Searches available Terms within date span
     * @param startDate date from
     * @param endDate date to
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(LocalDate startDate, LocalDate endDate);

    /**
     * Searches available Terms at given date within time span
     * @param date date
     * @param timeStart time from
     * @param timeEnd time to
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(LocalDate date, LocalTime timeStart, LocalTime timeEnd);

    /**
     * Searches available Terms at given Place
     * @param place Place
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(Place place);

    /**
     * Searches available Terms at given place within date span
     * @param place Place
     * @param dateStart date from
     * @param dateEnd date to
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(Place place, LocalDate dateStart, LocalDate dateEnd);

    /**
     * Searches available Terms at given place within date and time span
     * @param place Place
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(Place place, LocalDate dateStart, LocalDate dateEnd, LocalTime timeStart, LocalTime timeEnd);

    /**
     * Searches available Terms at places that match given properties
     * @param properties Place properties
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(Map<String ,Integer> properties);

    /**
     * Searches available Terms within date and time span
     * @param dateStart date from
     * @param dateEnd date to
     * @param timeStart time from
     * @param timeEnd time to
     * @return List of Terms
     */
    public abstract List<Term> searchAvailableTerms(LocalDate dateStart,LocalDate dateEnd, LocalTime timeStart, LocalTime timeEnd);

}
