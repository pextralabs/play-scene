package models.base;

import models.Persistent;

import javax.persistence.CascadeType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@javax.persistence.Entity
public class Entity extends Persistent {

    private String name;
    private String type;

    @OneToMany(mappedBy="bearer", cascade= CascadeType.ALL)
    private List<Sensor> sensors = new ArrayList<>();

    public Entity(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

}
