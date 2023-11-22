package rs.raf.schedulemanagerimplone;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import rs.raf.Place;
import rs.raf.Schedule;
import rs.raf.ScheduleImplManager;
import rs.raf.Term;


import java.io.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleImpl extends Schedule {

    static {
        ScheduleImplManager.setScheduleSpecification(new ScheduleImpl());
    }
    private List<String> headersOrder;
    private List<String> headers;

    @Override
    public void loadPlaces(File file) throws IOException {
        FileReader fr = new FileReader(file);
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(true).build();
        CSVReader reader = new CSVReaderBuilder(fr).withCSVParser(parser).build();

        this.setPlaces(new ArrayList<>());

        String[] line;
        while ((line = reader.readNext()) != null) {
            Place newPlace = new Place(line[0]);
            newPlace.setLocation(line[1]);
            if(line.length > 2){
                String[] inputProp = line[2].replace(" ", "").split(";");
                Map<String, Integer> properties = new HashMap<>();
                for (String property : inputProp) {
                    properties.put(property.substring(0, property.indexOf(":")),
                            Integer.parseInt(property.substring(property.indexOf(":")+1)));
                }
                newPlace.setProperties(properties);
            }

            this.getPlaces().add(newPlace);
        }
    }

    @Override
    public void makeSchedule(File file) throws IOException {
        FileReader fr = new FileReader(file);
        CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(true).build();
        CSVReader reader = new CSVReaderBuilder(fr).withCSVParser(parser).build();

        this.setTerms(new ArrayList<>());
        if(this.getPlaces() == null)
            this.setPlaces(new ArrayList<>());
        if(this.getFreeDays() == null)
            this.setFreeDays(new ArrayList<>());

        this.headersOrder = new ArrayList<>();
        this.headers = new ArrayList<>();
        for(String e : reader.readNext())
           headersOrder.add(e.replace("\"", ""));
        for(String e : reader.readNext())
            headers.add(e.replace("\"", ""));

        String[] line;
        while ((line = reader.readNext()) != null) {
            int headersOrderIndex = 0;
            Map<String, String> terminfo = new HashMap<>();
            LocalDate date = LocalDate.now();
            LocalTime timeStart = LocalTime.now();
            LocalTime timeEnd = LocalTime.now();
            Place place = new Place();
            for (String cell : line) {
                cell = cell.replace("\"", "");
                switch (headersOrder.get(headersOrderIndex)) {
                    case "terminfo" -> terminfo.put(headers.get(headersOrderIndex), cell);
                    case "date" -> {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        date = LocalDate.parse(cell, formatter);
                        if(this.getScheduleStartDate() == null || date.isBefore(this.getScheduleStartDate()))
                            this.setScheduleStartDate(date);
                        else if(this.getScheduleEndDate() == null || date.isAfter(this.getScheduleEndDate()))
                            this.setScheduleEndDate(date);
                    }
                    case "time" -> {
                        String start = cell.substring(0, cell.indexOf('-'));
                        String end = cell.substring(cell.indexOf('-') + 1);
                        if (start.length() == 2)
                            timeStart = LocalTime.of(Integer.parseInt(start), 0);
                        else
                            timeStart = LocalTime.of(Integer.parseInt(start.substring(0, start.indexOf(":"))),
                                    Integer.parseInt(start.substring(start.indexOf(":") + 1)));

                        if (end.length() == 2)
                            timeEnd = LocalTime.of(Integer.parseInt(end), 0);
                        else
                            timeEnd = LocalTime.of(Integer.parseInt(start.substring(0, end.indexOf(":"))),
                                    Integer.parseInt(start.substring(end.indexOf(":") + 1)));
                    }
                    case "place" -> {
                        boolean exists = false;
                        for (Place p : this.getPlaces()) {
                            if (p.getName().equals(cell)) {
                                place = p;
                                exists = true;
                                break;
                            }
                        }
                        if(!exists){
                            place = new Place(cell);
                            this.getPlaces().add(place);
                        }
                    }
                }
                headersOrderIndex++;
            }
            this.addTerm(new Term(date, date, date.getDayOfWeek(), timeStart, timeEnd, place, terminfo));
        }
    }

    @Override
    public void addTerm(Term term) {
        if(this.getFreeDays().contains(term.getDate()) && this.getTerms().contains(term))
            return;

        if(searchTerm(term.getPlace(), term.getDate(), term.getDate(), term.getTimeStart(), term.getTimeEnd()).size() != 0)
            return;

        this.getTerms().add(term);
        addPlaceIfNeeded(term.getPlace()).getTerms().add(term);
    }

    @Override
    public void addTerm(LocalDate localDate, LocalTime localTime, LocalTime localTime1, Place place) {
        this.addTerm(new Term(localDate, localTime, localTime1, place, new HashMap<>()));
    }

    public void addTerm(LocalDate localDate, LocalTime localTime, LocalTime localTime1, Place place, Map<String, String> info) {
        this.addTerm(new Term(localDate, localTime, localTime1, place, info));
    }

    @Override
    public void deleteTerm(Term term) {
        Iterator<Term> iterator = this.getTerms().iterator();
        while (iterator.hasNext()) {
            Term t = iterator.next();
            if (t.getPlace().equals(term.getPlace()) && t.getDate().equals(term.getDate())
                    && t.getTimeStart().equals(term.getTimeStart()) && t.getTimeEnd().equals(term.getTimeEnd())) {
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public void deleteTermInDateSpan(LocalDate localDate, LocalDate localDate1) {
        this.getTerms().removeIf(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1));
    }

    @Override
    public void deleteTermInDateAndTimeSpan(LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        this.getTerms().removeIf(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1)
                                && !t.getTimeStart().isBefore(localTime) && !t.getTimeStart().isAfter(localTime1));
    }

    @Override
    public void deleteTerm(LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        this.getTerms().removeIf(t -> t.getDate().equals(localDate) && !t.getTimeStart().isBefore(localTime)
                                                                && !t.getTimeStart().isAfter(localTime1));
    }

    @Override
    public void deleteTerm(Place place) {
        this.getTerms().removeIf(t -> place.getTerms().contains(t));
    }

    @Override
    public void deleteTerm(Place place, LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        this.deleteTerm(new Term(localDate, localTime, localTime1, place, null));
    }

    @Override
    public void deleteTerm(Place place, LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        this.getTerms().removeIf(t -> t.getPlace().equals(place) && !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1)
                                                && !t.getTimeStart().isBefore(localTime) && !t.getTimeStart().isAfter(localTime1));
    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime) {
        if(this.getFreeDays().contains(localDate))
            return null;

        Term newTerm = null;
        Duration timeDifference = Duration.between(term.getTimeStart(), localTime);
        LocalTime newTimeEnd = term.getTimeEnd().plus(timeDifference);
        for(Term t : this.getTerms()){
            if(t.equals(term)){
                newTerm = t;
                break;
            }
        }
        if (newTerm != null){
            boolean available = true;
            for(Term t : term.getPlace().getTerms()){
                if(t.getDate().equals(localDate)){
                    if((!localTime.isBefore(t.getTimeStart()) && !localTime.isAfter(t.getTimeEnd())) ||
                            (!localTime.isBefore(newTimeEnd) && !localTime.isAfter(newTimeEnd))){
                        available = false;
                        break;
                    }
                }
            }
            if (available){
                this.getTerms().remove(newTerm);
                newTerm.setDate(localDate);
                newTerm.setDateEnd(localDate);
                newTerm.setDay(localDate.getDayOfWeek());
                newTerm.setTimeStart(localTime);
                newTerm.setTimeEnd(newTimeEnd);
                this.getTerms().add(newTerm);
                return newTerm;
            }
            else
                System.out.println("Term not available.");
        }
        else
            System.out.println("Term doesn't exists.");
        return null;
    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime, Place place) {
        if(this.getFreeDays().contains(localDate))
            return null;

        Term newTerm = null;
        Duration timeDifference = Duration.between(term.getTimeStart(), localTime);
        LocalTime newTimeEnd = term.getTimeEnd().plus(timeDifference);
        for(Term t : this.getTerms()){
            if(t.equals(term)){
                newTerm = t;
                break;
            }
        }
        if (newTerm != null){
            boolean available = true;
            for(Term t : place.getTerms()){
                if(t.getDate().equals(localDate)){
                    if((!localTime.isBefore(t.getTimeStart()) && !localTime.isAfter(t.getTimeEnd())) ||
                            (!localTime.isBefore(newTimeEnd) && !localTime.isAfter(newTimeEnd))){
                        available = false;
                        break;
                    }
                }
            }
            if(available){
                this.getTerms().remove(newTerm);
                newTerm.setDate(localDate);
                newTerm.setDateEnd(localDate);
                newTerm.setDay(localDate.getDayOfWeek());
                newTerm.setTimeStart(localTime);
                newTerm.setPlace(place);
                this.getTerms().add(newTerm);
            }
            else
                System.out.println("Term not available.");
        }
        else
            System.out.println("Term doesn't exists.");
        return null;
    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        if(this.getFreeDays().contains(localDate))
            return null;

        Term newTerm = null;
        for(Term t : this.getTerms()){
            if(t.equals(term)){
                newTerm = t;
                break;
            }
        }
        if (newTerm != null){
            boolean available = true;
            for(Term t : term.getPlace().getTerms()){
                if(t.getDate().equals(localDate)){
                    if((!localTime.isBefore(t.getTimeStart()) && !localTime.isAfter(t.getTimeEnd())) ||
                            (!localTime.isBefore(localTime1) && !localTime.isAfter(localTime1))){
                        available = false;
                        break;
                    }
                }
            }
            if (available){
                this.getTerms().remove(newTerm);
                newTerm.setDate(localDate);
                newTerm.setDateEnd(localDate);
                newTerm.setDay(localDate.getDayOfWeek());
                newTerm.setTimeStart(localTime);
                newTerm.setTimeEnd(localTime1);
                this.getTerms().add(newTerm);
                return newTerm;
            }
            else
                System.out.println("Term not available.");
        }
        else
            System.out.println("Term doesn't exists.");
        return null;
    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime, LocalTime localTime1, Place place) {
        if(this.getFreeDays().contains(localDate))
            return null;

        Term newTerm = null;
        for(Term t : this.getTerms()){
            if(t.equals(term)){
                newTerm = t;
                break;
            }
        }
        if (newTerm != null){
            boolean available = true;
            for(Term t : place.getTerms()){
                if(t.getDate().equals(localDate)){
                    if((!localTime.isBefore(t.getTimeStart()) && !localTime.isAfter(t.getTimeEnd())) ||
                            (!localTime.isBefore(localTime1) && !localTime.isAfter(localTime1))){
                        available = false;
                        break;
                    }
                }
            }
            if(available){
                this.getTerms().remove(newTerm);
                newTerm.setDate(localDate);
                newTerm.setDateEnd(localDate);
                newTerm.setDay(localDate.getDayOfWeek());
                newTerm.setTimeStart(localTime);
                newTerm.setTimeEnd(localTime1);
                newTerm.setPlace(place);
                this.getTerms().add(newTerm);
            }
            else
                System.out.println("Term not available.");
        }
        else
            System.out.println("Term doesn't exists.");
        return null;
    }

    @Override
    public List<Term> searchTerm(Place place) {
        Collections.sort(place.getTerms()); ;
        return place.getTerms();
    }

    @Override
    public List<Term> searchTerm(LocalDate localDate, LocalDate localDate1) {
        return this.getTerms().stream()
                .filter(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1))
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTerm(Place place, LocalDate localDate, LocalDate localDate1) {
        return place.getTerms().stream()
                .filter(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1))
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTerm(Place place, Map<String, String> map) {
        return place.getTerms().stream().filter(t -> {
            for (Map.Entry<String, String> property : map.entrySet()) {
                String key = property.getKey();
                String value = property.getValue();

                if (t.getInfo().containsKey(key) && t.getInfo().get(key).contains(value))
                    return true;
            }
            return false;
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTerm(Place place, LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        return place.getTerms().stream()
                .filter(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1)
                        && !t.getTimeStart().isBefore(localTime) && !t.getTimeStart().isAfter(localTime1))
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTerm(Map<String, Integer> map) {
        return this.getTerms().stream().filter(t -> {
            for (Map.Entry<String, Integer> property : map.entrySet()) {
                String key = property.getKey();
                Integer value = property.getValue();
                if(t.getPlace().getProperties().containsKey(key) && (t.getPlace().getProperties().get(key) >= value))
                    return true;
            }
            return false;
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTermByTermInfo(Map<String, String> map) {
        return this.getTerms().stream().filter(t -> {
            for (Map.Entry<String, String> property : map.entrySet()) {
                String key = property.getKey();
                String value = property.getValue();
                if(t.getInfo().containsKey(key) && (t.getInfo().get(key).contains(value)))
                    return true;
            }
            return false;
        }).sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchTerm(LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        return this.getTerms().stream()
                .filter(t -> !t.getDate().isBefore(localDate) && !t.getDate().isAfter(localDate1)
                        && !t.getTimeStart().isBefore(localTime) && !t.getTimeStart().isAfter(localTime1))
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate date) {
        if(this.getFreeDays().contains(date))
            return new ArrayList<>();

        List<Term> availableTerms = new ArrayList<>();
        for(Place p : this.getPlaces()){
            Collections.sort(p.getTerms());
            LocalTime start = LocalTime.of(0,0);
            for(Term t : p.getTerms()){
                if(t.getDate().equals(date)){
                    availableTerms.add(new Term(date, start, t.getTimeStart(), p, new HashMap<>()));
                    start = t.getTimeEnd();
                }
            }
            availableTerms.add(new Term(date, date, date.getDayOfWeek(), start, LocalTime.of(23, 59), p, new HashMap<>()));
        }

        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate localDate, LocalDate localDate1) {
        List<Term> availableTerms = new ArrayList<>();
        for(; !localDate.isAfter(localDate1); localDate = localDate.plusDays(1)){
            if(this.getFreeDays().contains(localDate))
                continue;
            availableTerms.addAll(searchAvailableTerms(localDate));
        }

        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate date, LocalTime localTime, LocalTime localTime1) {
        if(this.getFreeDays().contains(date))
            return new ArrayList<>();

        List<Term> availableTerms = new ArrayList<>();
        for(Place p : this.getPlaces()){
            Arrays.sort(p.getTerms().toArray());
            Collections.sort(p.getTerms());
            LocalTime start = localTime;
            for(Term t : p.getTerms()){
                if(t.getDate().equals(date)){
                    if(!t.getTimeStart().isAfter(localTime))
                        start = t.getTimeEnd();
                    if(!t.getTimeEnd().isBefore(localTime1))
                        availableTerms.add(new Term(date, date, date.getDayOfWeek(), start, localTime1, p, new HashMap<>()));
                    else
                        availableTerms.add(new Term(date, date, date.getDayOfWeek(), start, t.getTimeEnd(), p, new HashMap<>()));
                }
            }
            availableTerms.add(new Term(date, date, date.getDayOfWeek(), start, localTime1, p, new HashMap<>()));
        }

        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(Place place) {
        List<Term> availableTerms = new ArrayList<>();
        for(LocalDate date = this.getScheduleStartDate(); !date.isAfter(this.getScheduleEndDate()); date = date.plusDays(1)){
            if(this.getFreeDays().contains(date))
                continue;
            availableTerms.addAll(searchAvailableTerms(place, date));
            Collections.sort(availableTerms);
        }

        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(Place place, LocalDate localDate, LocalDate localDate1) {
        List<Term> availableTerms = new ArrayList<>();
        for(; !localDate.isAfter(localDate1); localDate = localDate.plusDays(1)){
            if(this.getFreeDays().contains(localDate))
                continue;
            availableTerms.addAll(searchAvailableTerms(place, localDate));
        }

        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(Place place, LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        return searchAvailableTerms(localDate, localDate1, localTime, localTime1).stream()
                .filter(t -> t.getPlace().equals(place))
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<Term> searchAvailableTerms(Map<String, Integer> map) {
        List<Term> availableTerms = new ArrayList<>();
        for(Place p : this.getPlaces()){
            for (Map.Entry<String, Integer> property : map.entrySet()){
                String key = property.getKey();
                Integer value = property.getValue();
                if(p.getProperties().containsKey(key) && p.getProperties().get(key) > value)
                    availableTerms.addAll(searchAvailableTerms(p));
            }
        }
        Collections.sort(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        List<Term> availableTerms = new ArrayList<>();
        for(; !localDate.isAfter(localDate1); localDate = localDate.plusDays(1)){
            if(this.getFreeDays().contains(localDate))
                continue;
            availableTerms.addAll(searchAvailableTerms(localDate, localTime, localTime1));
        }
        Collections.sort(availableTerms);
        return availableTerms;
    }

    public List<Term> searchAvailableTerms(Place place, LocalDate localDate) {
        if(this.getFreeDays().contains(localDate))
            return new ArrayList<>();

        List<Term> availableTerms = new ArrayList<>();
        Arrays.sort(place.getTerms().toArray());
        Collections.sort(place.getTerms());

        LocalTime start = LocalTime.of(0,0);
        for(Term t : place.getTerms()){
            if(t.getDate().equals(localDate)){
                availableTerms.add(new Term(localDate, localDate, localDate.getDayOfWeek(), start, t.getTimeStart(), place, new HashMap<>()));
                start = t.getTimeEnd();
            }
        }
        availableTerms.add(new Term(localDate, localDate, localDate.getDayOfWeek(), start, LocalTime.of(23, 59), place, new HashMap<>()));

        Collections.sort(availableTerms);
        return availableTerms;
    }

    private Place addPlaceIfNeeded(Place place){
        for(Place p : this.getPlaces()){
            if (p.getName().equals(place.getName()))
                return p;
        }
        this.getPlaces().add(place);
        return place;
    }
}
