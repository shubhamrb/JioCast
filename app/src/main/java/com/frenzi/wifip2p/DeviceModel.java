package com.frenzi.wifip2p;

public class DeviceModel {
    private String location = "";
    private String name = "";

    public DeviceModel(String location, String name) {
        this.location = location;
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "{" +
                "location='" + location + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
