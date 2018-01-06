package models.sensing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import models.Persistent;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@javax.persistence.Entity
@Table(name="stream_data")
@JsonPropertyOrder({ "oid", "sensorTimestamp", "serverTimestamp", "values" })
public class StreamData extends Persistent {

    @ManyToOne(optional=false)
    @JsonBackReference
    private Stream stream;
    @Column(name="server_timestamp")
    private long serverTimestamp;
    @Column(name="sensor_timestamp")
    private long sensorTimestamp;
    @JsonIgnore
    private String raw;

    @OneToMany(fetch = FetchType.EAGER, mappedBy="data", cascade= CascadeType.ALL)
    private List<Tuple> values = new ArrayList<Tuple>();

    public StreamData () {

    }

    public StreamData(Stream stream, long serverTimestamp) {
        this.stream = stream;
        stream.getData().add(this);
        this.serverTimestamp = serverTimestamp;
    }

    public StreamData(Stream stream, long serverTimestamp, long sensorTimestamp) {
        this(stream, serverTimestamp);
        this.sensorTimestamp = sensorTimestamp;
    }

    public long getServerTimestamp() {
        return serverTimestamp;
    }

    public long getSensorTimestamp() {
        return sensorTimestamp;
    }

    public StreamData addTuple(String key, String value) {
        values.add(new Tuple(this, key, value));
        return this;
    }

    public String getRaw() {
        return raw;
    }

    public List<Tuple> getValues() {
        return values;
    }

    public Double get(String key) {
        for (Tuple v: values) {
            if (v.getKey().equals(key)) {
                return new Double(v.getValue());
            }
        }
        return null;
    }

    public Stream getStream() {
        return stream;
    }
}
