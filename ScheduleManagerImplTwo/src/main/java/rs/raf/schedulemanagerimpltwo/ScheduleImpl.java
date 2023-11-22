package rs.raf.schedulemanagerimpltwo;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import rs.raf.Place;
import rs.raf.Schedule;
import rs.raf.ScheduleImplManager;
import rs.raf.Term;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleImpl extends Schedule {
    static {
        ScheduleImplManager.setScheduleSpecification(new ScheduleImpl());
    }
    @Override
    public void loadPlaces(File file) throws IOException{

        FileReader fileReader = new FileReader(file);
        CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build();

        List<String[]>    stringList = csvReader.readAll();
        setPlaces(new ArrayList<>());
        for(String[] string : stringList){
            Place place = new Place();
            place.setProperties(new HashMap<>());
            place.setName(string[0]);
            place.setLocation(string[1]);
            place.setTerms(new ArrayList<>());
            if(string.length > 1){
                for(int i = 2; i < string.length;i++){
                    String[] properyParams = string[i].split(":");
                    place.getProperties().put(properyParams[0], Integer.valueOf(properyParams[1]));
                }
            }
            this.getPlaces().add(place);
        }
    }

    @Override
    public void makeSchedule(File file) throws IOException {
        if(this.getPlaces() == null)
            loadPlaces(new File("places2.csv"));
        if(this.getFreeDays() == null)
            loadFreeDays(new File("freeDays.csv"));

        file = new File("terms2.csv");
        this.setScheduleStartDate(null);
        this.setScheduleEndDate(null);
        if (this.getFreeDays() == null)
            this.setFreeDays(new ArrayList<>());

        Scanner scanner = new Scanner(file);
        String firstLine = scanner.nextLine();
        scanner.close();
        List<String> additionalTypes = new ArrayList<>();
        String[] params = firstLine.split(",");
        for(int i = 5;i<params.length;i++)
            additionalTypes.add(params[i]);
        FileReader fileReader = new FileReader(file);
        CSVReader csvReader = new CSVReaderBuilder(fileReader).withSkipLines(1).build();

        List<String[]> stringList;
        stringList = csvReader.readAll();
        this.setTerms(new ArrayList<>());
        for(String[] string : stringList){
            Term term = new Term();
            term.setDate(LocalDate.parse(string[1],DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            term.setDateEnd(LocalDate.parse(string[2],DateTimeFormatter.ofPattern("dd.MM.yyyy")));

            switch (string[3]) {
                case "PON" -> term.setDay(DayOfWeek.MONDAY);
                case "UTO" -> term.setDay(DayOfWeek.TUESDAY);
                case "SRE" -> term.setDay(DayOfWeek.WEDNESDAY);
                case "CET" -> term.setDay(DayOfWeek.THURSDAY);
                case "PET" -> term.setDay(DayOfWeek.FRIDAY);
                case "SUB" -> term.setDay(DayOfWeek.SATURDAY);
                case "NED" -> term.setDay(DayOfWeek.SUNDAY);

            }
            String[] paramsTime = string[4].split("-");
            term.setTimeStart(LocalTime.parse(paramsTime[0], DateTimeFormatter.ofPattern("HH:mm")));
            term.setTimeEnd(LocalTime.parse(paramsTime[1], DateTimeFormatter.ofPattern("HH")));
            Place place = null;
            for(Place currPlace : this.getPlaces()){
                if(currPlace.getName().equals(string[0])){
                    place = currPlace;
                    break;
                }
            }
            if(place!=null){
                term.setPlace(place);
                place.getTerms().add(term);
            }else{
                System.out.println("Location not recognized");
                return;
            }
            int j = 5;
            term.setInfo(new HashMap<>());
            for(int i = 0 ; i < additionalTypes.size();i++)
                term.getInfo().put(additionalTypes.get(i),string[j++]);
            this.getTerms().add(term);
            if(this.getScheduleStartDate() == null || term.getDate().isBefore(this.getScheduleStartDate()))
                this.setScheduleStartDate(term.getDate());
            if(this.getScheduleEndDate() == null || term.getDateEnd().isAfter(this.getScheduleEndDate()))
                this.setScheduleEndDate(term.getDateEnd());
        }
//sortByPlaceName(this.getTerms());
    }






    @Override
    public void addTerm(Term term) {
        List<Term> searched = searchAvailableTerms(term.getPlace(),term.getDate(),term.getDateEnd(),term.getTimeStart(),term.getTimeEnd());
        for (LocalDate date = term.getDate(); !date.isAfter(term.getDateEnd()); date = date.plusDays(1)) {
            if(date.getDayOfWeek().equals(term.getDay()) && !getFreeDays().contains(date)){
                boolean flag = false;
                for(Term term1 : searched){
                    if(term1.getDate().equals(date)){
                        flag = true;
                        break;
                    }
                }
                if(!flag){
                    return;
                }
            }
        }
        getTerms().add(term);
        term.getPlace().getTerms().add(term);
    }

    @Override
    public void addTerm(LocalDate localDate, LocalTime localTime, LocalTime localTime1, Place place) {
        if(getFreeDays().contains(localDate)) return;
        List<Term> searched = searchAvailableTerms(place,localDate,localDate,localTime,localTime1);
        if(searched.size() == 1){
            Term term = new Term();
            term.setDate(localDate);
            term.setDateEnd(localDate);
            term.setTimeStart(localTime);
            term.setTimeEnd(localTime1);
            term.setPlace(place);
            place.getTerms().add(term);
            this.getTerms().add(term);
        }else{
            System.out.println("Nije moguce zakazati dogadjaj u tom terminu.");
        }
    }

    @Override
    public void deleteTerm(Term term) {
        this.getTerms().remove(term);
        for(Place place : this.getPlaces()){
            if(place.getTerms().remove(term)) {
                break;
            }
        }
    }

    @Override
    public void deleteTermInDateSpan(LocalDate localDate, LocalDate localDate1) {
        List<Term> toBeDeleted = new ArrayList<>();
        for(Term term : this.getTerms()){
            if(term.getDate().isAfter(localDate) && term.getDateEnd().isBefore(localDate1)) {
                toBeDeleted.add(term);
                term.getPlace().getTerms().remove(term);
            }
        }
        this.getTerms().removeAll(toBeDeleted);
    }

    @Override
    public void deleteTermInDateAndTimeSpan(LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeDeleted = new ArrayList<>();
        for(Term term : this.getTerms()) {
            if ((term.getDate().isAfter(localDate) && term.getDateEnd().isBefore(localDate1)) && (term.getTimeStart().isAfter(localTime) && term.getTimeEnd().isBefore(localTime1))){
                toBeDeleted.add(term);
                term.getPlace().getTerms().remove(term);
            }
        }
        this.getTerms().removeAll(toBeDeleted);
    }

    @Override
    public void deleteTerm(LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeDeleted = new ArrayList<>();
        for(Term term : this.getTerms()) {
            if ((localDate.isAfter(term.getDate()) && localDate.isBefore(term.getDateEnd())) && (term.getTimeStart().isAfter(localTime) && term.getTimeEnd().isBefore(localTime1))){
                toBeDeleted.add(term);
                term.getPlace().getTerms().remove(term);
            }
        }

        this.getTerms().removeAll(toBeDeleted);
    }



    @Override
    public void deleteTerm(Place place) {
        this.getTerms().removeAll(place.getTerms());
        place.getTerms().clear();
    }

    @Override
    public void deleteTerm(Place place, LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeDeleted = new ArrayList<>();
        for(Term term : place.getTerms()){
            if((localDate.isAfter(term.getDate()) && localDate.isBefore(term.getDateEnd())) && (term.getTimeEnd().isAfter(localTime) && term.getTimeStart().isBefore(localTime1)) ) {
                toBeDeleted.add(term);
                term.getPlace().getTerms().remove(term);
            }
        }
        this.getTerms().removeAll(toBeDeleted);
    }

    @Override
    public void deleteTerm(Place place, LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeDeleted = new ArrayList<>();
        for(Term term : place.getTerms()) {
            if ((term.getDate().isAfter(localDate) && term.getDateEnd().isBefore(localDate1)) && (term.getTimeEnd().isAfter(localTime) && term.getTimeStart().isBefore(localTime1))){
                toBeDeleted.add(term);
                term.getPlace().getTerms().remove(term);
            }
        }
        this.getTerms().removeAll(toBeDeleted);
    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime) {
        if(getFreeDays().contains(localDate)) return null;
        List<Term> termsSearch = searchAvailableTerms(term.getPlace(),localDate,localDate,localTime,localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())));
        if(termsSearch.size() == 1 && termsSearch.get(0).getTimeStart().equals(localTime) && termsSearch.get(0).getTimeEnd().equals(localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())))){
            term.setTimeEnd(localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())));
            term.setDate(localDate);
            term.setDateEnd(localDate);
            term.setTimeStart(localTime);
            return term;
        }else  return null;

    }

    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime, Place place) {
        if(getFreeDays().contains(localDate)) return null;
        List<Term> termsSearch = searchAvailableTerms(place,localDate,localDate,localTime,localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())));
        if(termsSearch.size() == 1 && termsSearch.get(0).getTimeStart().equals(localTime) && termsSearch.get(0).getTimeEnd().equals(localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())))){
            term.setDate(localDate);
            term.setDateEnd(localDate);
            term.setTimeStart(localTime);
            term.setTimeEnd(localTime.plus(Duration.between(term.getTimeStart(),term.getTimeEnd())));
            ////???
            term.getPlace().getTerms().remove(term);
            term.setPlace(place);
            return term;
        }else  return null;
    }


    @Override
    public Term moveTerm(Term term, LocalDate localDate, LocalTime localTime, LocalTime localTime1) {
        if(getFreeDays().contains(localDate)) return null;
        List<Term> termsSearch = searchAvailableTerms(term.getPlace(),localDate,localDate,localTime,localTime1);
        if(termsSearch.size() == 1 && termsSearch.get(0).getTimeStart().equals(localTime) && termsSearch.get(0).getTimeEnd().equals(localTime1)){
            term.setDate(localDate);
            term.setDateEnd(localDate);
            term.setTimeStart(localTime);
            term.setTimeEnd(localTime1);
            return term;
        }else  return null;
    }

    @Override
    public Term moveTerm(Term term, LocalDate dateStart, LocalTime timeStart, LocalTime timeEnd, Place place) {
        if(getFreeDays().contains(dateStart)) return null;
        List<Term> termsSearch = searchAvailableTerms(place,dateStart,dateStart,timeStart,timeEnd);
        if(termsSearch.size() == 1 && termsSearch.get(0).getTimeStart().equals(timeStart) && termsSearch.get(0).getTimeEnd().equals(timeEnd)){
            term.setDate(dateStart);
            term.setDateEnd(dateStart);
            term.setTimeStart(timeStart);
            term.setTimeEnd(timeEnd);
            ////???
            term.getPlace().getTerms().remove(term);
            return term;
        }else  return null;
    }

    @Override
    public List<Term> searchTerm(Place place) {
        return place.getTerms();
    }

    @Override
    public List<Term> searchTerm(LocalDate localDate, LocalDate localDate1) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : getTerms()){
            if(term.getDate().isAfter(localDate) && term.getDateEnd().isBefore(localDate1)) {
                toBeReturned.add(term);
            }
        }
        return toBeReturned;    }

    @Override
    public List<Term>  searchTerm(Place place, LocalDate localDate, LocalDate localDate1) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : place.getTerms()){
            if(term.getDate().isAfter(localDate) && term.getDateEnd().isBefore(localDate1)) {
                toBeReturned.add(term);
            }
        }
        return toBeReturned;
    }

    @Override
    public List<Term>  searchTerm(Place place, Map<String, String> map) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : place.getTerms()){
            boolean flag = true;
            for(Map.Entry<String,String> entry : map.entrySet()) {
                if (term.getInfo().containsKey("\""+entry.getKey()+"\"")) {
                    String termInfo = term.getInfo().get("\""+entry.getKey()+"\"");
                    if (!termInfo.equals(entry.getValue())) flag = false;
                } else flag = false;
            }
            if(flag)
                toBeReturned.add(term);
        }
        return toBeReturned;
    }

    @Override
    public List<Term>  searchTerm(Place place, LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : place.getTerms()) {
            if ((term.getDateEnd().isAfter(localDate) && term.getDate().isBefore(localDate1)) && (term.getTimeEnd().isAfter(localTime) && term.getTimeStart().isBefore(localTime1))){
                toBeReturned.add(term);
            }
        }
        return toBeReturned;
    }



    @Override
    public List<Term>  searchTerm(Map<String, Integer> map) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : getTerms()){
            boolean flag = true;
            for(Map.Entry<String,Integer> entry : map.entrySet()){
                if(term.getPlace().getProperties().containsKey(entry.getKey())){
                    int currCapacity = term.getPlace().getProperties().get(entry.getKey());
                    if(currCapacity < entry.getValue()) flag = false;
                }else flag = false;
            }
            if(flag)
                toBeReturned.add(term);
        }
        return toBeReturned;
    }

    @Override
    public List<Term> searchTermByTermInfo(Map<String, String> map) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : getTerms()){
            boolean flag = true;
            for(Map.Entry<String,String> entry : map.entrySet()) {
                if (term.getInfo().containsKey("\""+entry.getKey()+"\"")) {
                    String termInfo = term.getInfo().get("\""+entry.getKey()+"\"");
                    if (!termInfo.equals(entry.getValue())) flag = false;
                } else flag = false;
            }
            if(flag)
                toBeReturned.add(term);
        }
        return toBeReturned;
    }

    @Override
    public List<Term>  searchTerm(LocalDate localDate, LocalDate localDate1, LocalTime localTime, LocalTime localTime1) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : this.getTerms()) {
            if ((term.getDateEnd().isAfter(localDate) && term.getDate().isBefore(localDate1)) && (term.getTimeEnd().isAfter(localTime) && term.getTimeStart().isBefore(localTime1))){
                toBeReturned.add(term);
            }
        }
        return toBeReturned;
    }

    public static void sortTermsByTime(List<Term> terms) {
        terms.sort(Comparator.comparing(Term::getTimeStart).thenComparing(Term::getTimeEnd));
    }
    public static void sortByPlaceName(List<Term> terms) {
        Comparator<Term> placeNameComparator = Comparator.comparing(term -> term.getPlace().getName());
        terms.sort(placeNameComparator);
    }
    @Override
    public List<Term> searchAvailableTerms(LocalDate localDate) {
        List<Term> availableTerms = new ArrayList<>();
        List<Term> termsSearch = new ArrayList<>();
        if(getFreeDays().contains(localDate))
            return availableTerms;
        for(Term term : getTerms()){
            if(localDate.isAfter(term.getDate()) && localDate.isBefore(term.getDateEnd()))
                termsSearch.add(term);
        }
        for(Place p : getPlaces()){
            LocalTime start = LocalTime.of(0,1);
            List<Term> filtered = new ArrayList<>();
            for(Term term : termsSearch)
                if(term.getPlace().equals(p) && term.getDay().equals(localDate.getDayOfWeek()))
                    filtered.add(term);

            sortTermsByTime(filtered);
            for(Term term : filtered){
                availableTerms.add(new Term(localDate,localDate,localDate.getDayOfWeek(),start,term.getTimeStart(),p,new HashMap<>()));
                start = term.getTimeEnd();
            }
            availableTerms.add(new Term(localDate,localDate,localDate.getDayOfWeek(),start,LocalTime.of(23,58),p,new HashMap<>()));
        }
        sortByPlaceName(availableTerms);
        return availableTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate start, LocalDate end) {
        List<Term>  freeTerms = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            freeTerms.addAll(searchAvailableTerms(date));
        }
        sortByPlaceName(freeTerms);
        return freeTerms;
    }




    @Override
    public List<Term> searchAvailableTerms(Place place) {
        List<Term>  freeTerms;
        freeTerms = searchAvailableTerms(getScheduleStartDate(),getScheduleEndDate());
        List<Term> toBeRemoved = new ArrayList<>();
        for(Term term : freeTerms)
            if(!term.getPlace().getName().equals(place.getName()))
                toBeRemoved.add(term);

        freeTerms.removeAll(toBeRemoved);
        return freeTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(Place place, LocalDate beginDate, LocalDate endDate) {
        List<Term>  freeTerms;
        freeTerms = searchAvailableTerms(beginDate,endDate);
        List<Term> toBeRemoved = new ArrayList<>();
        for(Term term : freeTerms)
            if(!term.getPlace().getName().equals(place.getName()))
                toBeRemoved.add(term);
        freeTerms.removeAll(toBeRemoved);
        return freeTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate localDate, LocalTime timeStart, LocalTime timeEnd) {
        List<Term>  freeTerms = searchAvailableTerms(localDate);
        List<Term> toBeReturned = new ArrayList<>();

        for(Place place : getPlaces()) {
            for (Term term : freeTerms) {
                if (timeStart.isAfter(term.getTimeStart().minusMinutes(1)) && timeEnd.isBefore(term.getTimeEnd().plusMinutes(1)) && place.getName().equals(term.getPlace().getName())) {
                    term.setTimeStart(timeStart);
                    term.setTimeEnd(timeEnd);
                    toBeReturned.add(term);
                    break;
                }
            }
        }
        sortByPlaceName(toBeReturned);
        return toBeReturned;
    }
    @Override
    public List<Term> searchAvailableTerms(Place place, LocalDate beginDate, LocalDate endDate, LocalTime beginTime, LocalTime endTime) {
        List<Term>  freeTerms = new ArrayList<>();
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1))
            freeTerms.addAll(searchAvailableTerms(date,beginTime,endTime));;

        List<Term> toBeRemoved = new ArrayList<>();
        for(Term term : freeTerms)
            if(!term.getPlace().getName().equals(place.getName()))
                toBeRemoved.add(term);
        freeTerms.removeAll(toBeRemoved);

        return freeTerms;
    }

    @Override
    public List<Term> searchAvailableTerms(Map<String, Integer> map) {
        List<Term> toBeReturned = new ArrayList<>();
        for(Term term : searchAvailableTerms(getScheduleStartDate(),getScheduleEndDate())){
            boolean flag = true;
            for(Map.Entry<String,Integer> entry : map.entrySet()){
                if(term.getPlace().getProperties().containsKey(entry.getKey())){
                    int currCapacity = term.getPlace().getProperties().get(entry.getKey());
                    if(currCapacity < entry.getValue()) flag = false;
                }else flag = false;
            }
            if(flag)
                toBeReturned.add(term);
        }
        return toBeReturned;
    }

    @Override
    public List<Term> searchAvailableTerms(LocalDate beginDate, LocalDate endDate, LocalTime beginTime, LocalTime endTime) {
        List<Term>  freeTerms = new ArrayList<>();
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1))
            freeTerms.addAll(searchAvailableTerms(date,beginTime,endTime));;
        return freeTerms;
    }


}
