package models.sensing;

import models.Persistent;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Sensor extends Persistent {

    private String key;
    private String name;
    private String description;
    private String tag;

    @OneToMany(fetch = FetchType.EAGER, mappedBy="sensor", cascade= CascadeType.ALL)
    private List<Stream> streams = new ArrayList<>();

    public Sensor() {
        this.key = UUID.randomUUID().toString().replace("-", "");
    }


    public String getKey() {
        return key;
    }

    public Sensor setKey(String key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public Sensor setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Sensor setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public String getTag() {
        return tag;
    }

    public Sensor setTag(String tag) {
        this.tag = tag;
        return this;
    }
}
