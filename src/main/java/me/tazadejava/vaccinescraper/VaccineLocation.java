package me.tazadejava.vaccinescraper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Stores information related to where a vaccine is potentially available.
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineLocation {

    private String url, county, city, name, address, phoneNumber, email;
    private String additionalDetails;

    public VaccineLocation(String url, String county, String city, String name, String address, String phoneNumber, String email, String additionalDetails) {
        this.url = url;
        this.county = county;
        this.city = city;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.additionalDetails = additionalDetails;
    }

    public JsonElement save(Gson gson) {
        return gson.toJsonTree(this);
    }

    public static VaccineLocation createInstance(Gson gson, JsonObject data) {
        return gson.fromJson(data, VaccineLocation.class);
    }

    public String getAddress() {
        return address;
    }

    public String getUrl() {
        return url;
    }

    public String getID() {
        return name + " " + url + " " + address + " " + city + " " + county;
    }

    @Override
    public String toString() {
        return name + " " + url + " " + address + " " + city + " " + county;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccineLocation that = (VaccineLocation) o;
        return Objects.equals(url, that.url) && Objects.equals(county, that.county) && Objects.equals(city, that.city) && Objects.equals(name, that.name) && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, county, city, name, address);
    }
}
