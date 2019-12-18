package com.victu.foodatory.detail;


public class FoodNutritionData {

    private float CHOLE, CHOTDF, ENERC, FASATF, FAT, FATRNF, NA, PROCNP, SUGAR, WEIGHT;
    private String FOOD_GROUPS, FOOD_NAME;


    public FoodNutritionData(float CHOLE, float CHOTDF, float ENERC, float FASATF, float FAT, float FATRNF, float NA, float PROCNP, float SUGAR, float WEIGHT, String FOOD_GROUPS, String FOOD_NAME) {
        this.CHOLE = CHOLE;
        this.CHOTDF = CHOTDF;
        this.ENERC = ENERC;
        this.FASATF = FASATF;
        this.FAT = FAT;
        this.FATRNF = FATRNF;
        this.NA = NA;
        this.PROCNP = PROCNP;
        this.SUGAR = SUGAR;
        this.WEIGHT = WEIGHT;
        this.FOOD_GROUPS = FOOD_GROUPS;
        this.FOOD_NAME = FOOD_NAME;
    }

    public float getCHOLE() {
        return CHOLE;
    }

    public float getCHOTDF() {
        return CHOTDF;
    }

    public float getENERC() {
        return ENERC;
    }

    public float getFASATF() {
        return FASATF;
    }

    public float getFAT() {
        return FAT;
    }

    public float getFATRNF() {
        return FATRNF;
    }

    public float getNA() {
        return NA;
    }

    public float getPROCNP() {
        return PROCNP;
    }

    public float getSUGAR() {
        return SUGAR;
    }

    public float getWEIGHT() {
        return WEIGHT;
    }

    public String getFOOD_GROUPS() {
        return FOOD_GROUPS;
    }

    public String getFOOD_NAME() {
        return FOOD_NAME;
    }
}

