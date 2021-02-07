package me.tazadejava.vaccinescraper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Stores information related to where a vaccine is potentially available.
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineLocation {

    private final String url, county, city, name, address, phoneNumber, email;
    private final String additionalDetails;

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

    public String getCounty() {
        return county;
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

    public String getKey() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        String val = name + " " + address;
        val = val.toLowerCase();

        md.update(val.getBytes(StandardCharsets.UTF_8));
        return String.format("%032x", new BigInteger(1, md.digest()));
    }

    public String getName() {
        return name;
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
