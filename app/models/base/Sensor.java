package models.base;

import models.Persistent;

import javax.persistence.ManyToOne;
import javax.persistence.Table;

@javax.persistence.Entity
@Table(name="entity_sensor")
public class Sensor extends Persistent {

    @ManyToOne(optional=false)
    private Entity bearer;
    private String label;
    private Double value;

    public Sensor(Entity bearer,  String name, Double defaultValue) {
        this.bearer = bearer;
        this.label = name;
        this.value = defaultValue;
    }

    public Entity getBearer() {
        return bearer;
    }

    public String getLabel() {
        return label;
    }

    public Double getValue() {
        return value;
    }

    public Sensor setValue(Double value) {
        this.value = value;
        return this;
    }
}
