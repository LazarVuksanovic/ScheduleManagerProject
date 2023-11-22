package rs.raf;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class Place {

    private String name;
    private String location;
    private transient List<Term> terms;
    private Map<String, Integer> properties;

    /**
     * Creates new instance of Place
     */
    public Place() {
        this.terms = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    /**
     * Creates new instance of Place
     * @param name name of place
     */
    public Place(String name) {
        this.name = name;
        this.terms = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    /**
     * Creates new instance of Place
     * @param name name of place
     * @param location location of palce
     * @param properties properties of place
     */
    public Place(String name, String location, Map<String, Integer> properties) {
        this.name = name;
        this.location = location;
        this.properties = properties;
    }

    /**
     * Compares two Places
     * @param o object to be compared
     * @return compared value
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return name.equals(place.name);
    }

    /**
     * Returns Place values
     * @return Returns String that contains place values
     */
    @Override
    public String toString() {
        return this.name + " | " + this.location + " | " + this.properties;
    }
}
