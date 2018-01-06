package models.sensing;

import models.Persistent;

import javax.persistence.*;

@Entity
public class Tuple extends Persistent {

    private String key;
    private String value;

    @ManyToOne(cascade= CascadeType.ALL)
    private StreamData data;

    public Tuple() {

    }

    public Tuple(StreamData data, String key, String value) {
        this.data = data;
        this.key = key;
        this.value = value;
    }

    public StreamData getData() {
        return data;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
