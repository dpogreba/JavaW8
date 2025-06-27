package com.antbear.javaw8.map;

/**
 * Provider-agnostic representation of a place/POI.
 * Used to standardize place data across different map providers.
 */
public class PlaceInfo {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private String address;
    private String phoneNumber;
    private Float rating;
    private boolean isSampleData;

    /**
     * Create a new PlaceInfo object.
     * 
     * @param id Unique identifier for this place
     * @param name Name of the place
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param address Address of the place
     * @param phoneNumber Phone number (can be null)
     * @param rating Rating (can be null)
     * @param isSampleData Whether this is sample/fallback data
     */
    public PlaceInfo(String id, String name, double latitude, double longitude, 
                    String address, String phoneNumber, Float rating, boolean isSampleData) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.rating = rating;
        this.isSampleData = isSampleData;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public Float getRating() { return rating; }
    public boolean isSampleData() { return isSampleData; }

    /**
     * Create a snippet string for display in an info window.
     * 
     * @return Formatted string with place details
     */
    public String createSnippet() {
        StringBuilder snippet = new StringBuilder();
        
        // Add address if available
        if (address != null && !address.isEmpty()) {
            snippet.append(address);
        }
        
        // Add phone number if available
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("Phone: ").append(phoneNumber);
        }
        
        // Add rating if available
        if (rating != null) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("Rating: ").append(rating).append(" ★");
        }
        
        // Add sample data indicator if appropriate
        if (isSampleData) {
            if (snippet.length() > 0) {
                snippet.append("\n");
            }
            snippet.append("(Sample Data)");
        }
        
        // Add directions instruction
        if (snippet.length() > 0) {
            snippet.append("\n");
        }
        snippet.append("Tap to get directions");
        
        return snippet.toString();
    }
}
