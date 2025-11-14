package com.example.rc.models;

public class King {
    private int imageRes;
    private String name;
    private String description;
    private String faction;
    private String ability;

    public King(int imageRes, String name, String description, String faction) {
        this.imageRes = imageRes;
        this.name = name;
        this.description = description;
        this.faction = faction;
        this.ability = "";
    }

    public King(int imageRes, String name, String description, String faction, String ability) {
        this.imageRes = imageRes;
        this.name = name;
        this.description = description;
        this.faction = faction;
        this.ability = ability;
    }

    public int getImageRes() { return imageRes; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFaction() { return faction; }
    public String getAbility() { return ability; }
}