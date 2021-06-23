package com.idealista.domain;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Ad {

    private Integer id;
    private Typology typology;
    private String description;
    private List<Picture> pictures;
    private Integer houseSize;
    private Integer gardenSize;
    private Integer score;
    private Date irrelevantSince;

    public Ad(Integer id,
              Typology typology,
              String description,
              List<Picture> pictures,
              Integer houseSize,
              Integer gardenSize,
              Integer score,
              Date irrelevantSince) {
        this.id = id;
        this.typology = typology;
        this.description = description;
        this.pictures = pictures;
        this.houseSize = houseSize;
        this.gardenSize = gardenSize;
        this.score = score;
        this.irrelevantSince = irrelevantSince;
    }

    public Ad(Integer id,
              Typology typology,
              String description,
              List<Picture> pictures,
              Integer houseSize,
              Integer gardenSize) {
        this.id = id;
        this.typology = typology;
        this.description = description;
        this.pictures = pictures;
        this.houseSize = houseSize;
        this.gardenSize = gardenSize;
    }

    public boolean isComplete() {
        return (Typology.GARAGE.equals(typology) && !pictures.isEmpty())
                || (Typology.FLAT.equals(typology) && !pictures.isEmpty() && description != null && !description.isEmpty() && houseSize != null)
                || (Typology.CHALET.equals(typology) && !pictures.isEmpty() && description != null && !description.isEmpty() && houseSize != null && gardenSize != null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Typology getTypology() {
        return typology;
    }

    public void setTypology(Typology typology) {
        this.typology = typology;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    public Integer getHouseSize() {
        return houseSize;
    }

    public void setHouseSize(Integer houseSize) {
        this.houseSize = houseSize;
    }

    public Integer getGardenSize() {
        return gardenSize;
    }

    public void setGardenSize(Integer gardenSize) {
        this.gardenSize = gardenSize;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Date getIrrelevantSince() {
        return irrelevantSince;
    }

    public void setIrrelevantSince(Date irrelevantSince) {
        this.irrelevantSince = irrelevantSince;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ad ad = (Ad) o;
        return Objects.equals(id, ad.id) && typology == ad.typology && Objects.equals(description, ad.description) && Objects.equals(pictures, ad.pictures) && Objects.equals(houseSize, ad.houseSize) && Objects.equals(gardenSize, ad.gardenSize) && Objects.equals(score, ad.score) && Objects.equals(irrelevantSince, ad.irrelevantSince);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, typology, description, pictures, houseSize, gardenSize, score, irrelevantSince);
    }

    @Override
    public String toString() {
        return "Ad{" +
                "id=" + id +
                ", typology=" + typology +
                ", description='" + description + '\'' +
                ", pictures=" + pictures +
                ", houseSize=" + houseSize +
                ", gardenSize=" + gardenSize +
                ", score=" + score +
                ", irrelevantSince=" + irrelevantSince +
                '}';
    }
}
