package models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import play.libs.Json;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@MappedSuperclass
public class Persistent {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

}
