import rs.raf.Place;
import rs.raf.Schedule;
import rs.raf.ScheduleImplManager;
import rs.raf.Term;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {

        try {
            Class.forName("rs.raf.schedulemanagerimplone.ScheduleImpl");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Schedule schedule = ScheduleImplManager.getScheduleSpecification();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("H:mm");

        while(true){
            System.out.println("Available commands:\n" +
                    "0 : Exit.\n" +
                    "1 : Load places.\n" +
                    "2 : Load free days.\n" +
                    "3 : Load terms.\n" +
                    "4 : Add term.\n" +
                    "5 : Delete term.\n" +
                    "6 : Move term.\n" +
                    "7 : Search term.\n" +
                    "8 : Search available terms.\n" +
                    "9 : Export schedule.\n"+
                    "10: Print all terms.\n");

            System.out.print("$: ");
            Scanner input = new Scanner(System.in);

            switch (input.nextLine()){
                case "0" -> { return; }
                case "1" -> {
                    System.out.println("Enter file path:");
                    File places = new File(input.nextLine());
                    schedule.loadPlaces(places);
                    for(Place place : schedule.getPlaces())
                        System.out.println(place);
                    System.out.println("Done.");
                }
                case "2" -> {
                    System.out.println("Enter file path:");
                    File freeDays = new File(input.nextLine());
                    schedule.loadFreeDays(freeDays);
                    System.out.println("Done.");
                }
                case "3" -> {
                    System.out.println("Enter file path:");
                    File terms = new File(input.nextLine());
                    schedule.makeSchedule(terms);
                    System.out.println("Done.");
                }

                case "4" -> {
                    System.out.println("Enter: <date> <dateEnd> <timeStart> <timeEnd> <placeName> [<prop:val>]");
                    String[] p = input.nextLine().split(" ");
                    String[] info = p[5].split(",");
                    Map<String, String> props = new HashMap<>();
                    for(String s : info)
                        props.put(s.substring(0, s.indexOf(":")), s.substring(s.indexOf(":") + 1));

                    Term newTerm = new Term(LocalDate.parse(p[0], df), LocalDate.parse(p[1], df),LocalDate.parse(p[0], df).getDayOfWeek(),
                            LocalTime.parse(p[2], tf), LocalTime.parse(p[3], tf), findPlace(schedule, p[4]), props);

                    schedule.addTerm(newTerm);
                    System.out.println(newTerm);
                }

                case "5" -> {
                    System.out.println("Choose parameter input:\n" +
                            "1 : <dateFrom> <dateTo>\n" +
                            "2 : <dateFrom> <dateTo> <timeFrom> <timeTo>\n" +
                            "3 : <date> <timeFrom> <timeTo>\n" +
                            "4 : <placeName> <date> <timeFrom> <timeTo>\n" +
                            "5 : <placeName> <dateFrom> <dateTo> <timeFrom> <timeTo>\n"+
                            "6 : <placeName>\n"+
                            "7 : <termIndex>");
                    String[] p = input.nextLine().split(" ");
                    switch(p[0]){
                        case "1" -> schedule.deleteTermInDateSpan(LocalDate.parse(p[1], df), LocalDate.parse(p[2], df));
                        case "2" -> schedule.deleteTermInDateAndTimeSpan(LocalDate.parse(p[1], df), LocalDate.parse(p[2], df), LocalTime.parse(p[3], tf), LocalTime.parse(p[4], tf));
                        case "3" -> schedule.deleteTerm(LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf), LocalTime.parse(p[2], tf));
                        case "4" -> schedule.deleteTerm(findPlace(schedule, p[1]),LocalDate.parse(p[2], df), LocalTime.parse(p[3], tf), LocalTime.parse(p[4], tf));
                        case "5" -> schedule.deleteTerm(findPlace(schedule, p[1]),LocalDate.parse(p[2], df), LocalDate.parse(p[3], df), LocalTime.parse(p[4], tf), LocalTime.parse(p[5], tf));
                        case "6" -> schedule.deleteTerm(findPlace(schedule,p[1]));
                        case "7"->  schedule.deleteTerm(schedule.getTerms().get(Integer.parseInt(p[1])));
                    }
                    System.out.println("\nDone.");
                }

                case "6" -> {
                    System.out.println("Move first term. Choose parameter input:\n" +
                            "1 : <newDate> <newTime>\n" +
                            "2 : <newDate> <newTime> <newPlaceName>\n" +
                            "3 : <newDate> <newTimeStart> <newTimeEnd>\n" +
                            "5 : <newDate> <newTimeStart> <newTimeEnd> <newPlaceName>\n");
                    String[] p = input.nextLine().split(" ");
                    switch(p[0]){
                        case "1" -> schedule.moveTerm(schedule.getTerms().get(0), LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf));
                        case "2" -> schedule.moveTerm(schedule.getTerms().get(0), LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf), findPlace(schedule, p[3]));
                        case "3" -> schedule.moveTerm(schedule.getTerms().get(0), LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf), LocalTime.parse(p[3], tf));
                        case "4" -> schedule.moveTerm(schedule.getTerms().get(0), LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf), LocalTime.parse(p[3], tf), findPlace(schedule, p[4]));
                    }
                    System.out.println("\nDone.");
                }

                case "7" -> {
                    System.out.println("Search terms by:\n" +
                            "1 : <placeName>\n" +
                            "2 : <dateFrom> <dateTo>\n" +
                            "3 : <placeName> <dateFrom> <dateTo>\n" +
                            "4 : <placeName> (grupa 101)\n" +
                            "5 :  (Racunari > 10)\n"+
                            "6 : <placeName> <dateFrom> <dateTo> <timeFrom> <timeTo>\n"+
                            "7 : <dateFrom> <dateTo> <timeFrom> <timeTo>\n"+
                            "8 : (grupa 101)\n");

                    String[] p = input.nextLine().split(" ");
                    switch(p[0]){
                        case "1" -> printTermsList(schedule.searchTerm(findPlace(schedule,p[1])));
                        case "2" -> printTermsList(schedule.searchTerm(LocalDate.parse(p[1], df), LocalDate.parse(p[2], df)));
                        case "3" -> printTermsList(schedule.searchTerm(findPlace(schedule, p[1]), LocalDate.parse(p[2], df), LocalDate.parse(p[3], df)));
                        case "4" -> {
                            Map<String, String> props = new HashMap<>();
                            props.put("Grupe", "101");
                            printTermsList(schedule.searchTerm(findPlace(schedule, p[1]),props));
                        }
                        case "5" -> {
                            Map<String, Integer> props = new HashMap<>();
                            props.put("Racunari", 10);
                            printTermsList(schedule.searchTerm(props));
                        }
                        case "6" -> printTermsList(schedule.searchTerm(findPlace(schedule,p[1]),LocalDate.parse(p[2],df),LocalDate.parse(p[3],df),LocalTime.parse(p[4], tf),LocalTime.parse(p[5], tf)));
                        case "7" -> printTermsList(schedule.searchTerm(LocalDate.parse(p[2],df),LocalDate.parse(p[3],df),LocalTime.parse(p[4], tf),LocalTime.parse(p[5], tf)));
                        case "8" ->{
                            Map<String, String> props = new HashMap<>();
                            props.put("Grupe", "101");
                            printTermsList(schedule.searchTermByTermInfo(props));                        }
                    }
                    System.out.println("\nDone.");
                }

                case "8" -> {
                    System.out.println("Search available terms by:\n" +
                            "1 : <date>\n" +
                            "2 : <placeName>\n" +
                            "3 : <dateFrom> <dateTo> <timeFrom> <timeTo>\n" +
                            "4 : <date> <timeFrom> <timeTo>\n" +
                            "5 : (Racunari > 10)\n"+
                            "6 : <dateFrom> <dateTo>\n"+
                            "7 : <placeName> <dateFrom> <dateTo>\n"+
                            "8 : <placeName> <dateFrom> <dateTo> <timeFrom> <timeTo>\n");

                    String[] p = input.nextLine().split(" ");
                    switch(p[0]){
                        case "1" -> printTermsList(schedule.searchAvailableTerms(LocalDate.parse(p[1], df)));
                        case "2" -> printTermsList(schedule.searchAvailableTerms(findPlace(schedule, p[1])));
                        case "3" -> printTermsList(schedule.searchAvailableTerms(LocalDate.parse(p[1], df), LocalDate.parse(p[2], df), LocalTime.parse(p[3], tf), LocalTime.parse(p[4], tf)));
                        case "4" -> printTermsList(schedule.searchAvailableTerms(LocalDate.parse(p[1], df), LocalTime.parse(p[2], tf), LocalTime.parse(p[3], tf)));
                        case "5" ->{
                            Map<String, Integer> props = new HashMap<>();
                            props.put("Racunari", 10);
                            printTermsList(schedule.searchAvailableTerms(props));
                        }
                        case "6" -> printTermsList(schedule.searchAvailableTerms(LocalDate.parse(p[1], df),LocalDate.parse(p[2], df)));
                        case "7" -> printTermsList(schedule.searchAvailableTerms(findPlace(schedule,p[1]),LocalDate.parse(p[2], df),LocalDate.parse(p[3], df)));
                        case "8" -> printTermsList(schedule.searchAvailableTerms(findPlace(schedule,p[1]),LocalDate.parse(p[2], df),LocalDate.parse(p[3], df),LocalTime.parse(p[4], tf),LocalTime.parse(p[5], tf)));
                    }
                    System.out.println("Done.");
                }

                case "9" ->{
                    System.out.println("Enter file type and path:");
                    String[] p = input.nextLine().split(" ");
                    File exportFile = new File(p[1]);
                    if(p[0].equalsIgnoreCase("json"))
                        schedule.exportScheduleJSON(exportFile,null, null, "Grupe", "101");
                    else
                        schedule.exportScheduleCSV(exportFile, null, null, null, null);
                    System.out.println("Done.");
                }
                case "10"-> printTermsList(schedule.getTerms());
            }
        }
    }
    static public void printTermsList(List<Term> terms){
        for(Term term : terms)
            System.out.println(term);
    }
    static public Place findPlace(Schedule s, String name){
        for(Place p : s.getPlaces())
            if(p.getName().equals(name.replace("_", " ")))
                return p;
        return null;
    }
}