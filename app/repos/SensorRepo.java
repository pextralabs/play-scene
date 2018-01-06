package repos;

import db.DatabaseExecutionContext;
import models.sensing.Sensor;
import models.sensing.Stream;
import models.sensing.StreamData;
import org.torpedoquery.jpa.OnGoingLogicalCondition;
import org.torpedoquery.jpa.Query;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.torpedoquery.jpa.Torpedo.*;

public class SensorRepo extends BaseRepo {

    @Inject
    public SensorRepo(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        super(jpaApi, executionContext);
    }

    private Long fetchStreamSize(EntityManager em, Long streamId) {
        return (Long) em.createQuery("SELECT COUNT(*) FROM StreamData D WHERE D.stream.id=:streamId")
                .setParameter("streamId", streamId).getSingleResult();
    }

    public List<Sensor> getSensors(EntityManager em) {
        List<Sensor> list = em.createQuery("SELECT s FROM models.sensing.Sensor s").getResultList();
        list.forEach(
                sensor -> sensor.getStreams().forEach(
                        stream -> stream.setTotalSize(fetchStreamSize(em, stream.getId()))
                )
        );
        return list;
    }

    public Optional<Sensor> getSensorBy(EntityManager em, Long sensorId) {
        return Optional.ofNullable(em.find(Sensor.class, sensorId)).map(
                sensor -> {
                    sensor.getStreams().forEach(
                            stream -> stream.setTotalSize(fetchStreamSize(em, stream.getId() ) )
                    );
                    return sensor;
                }
            );
    }

    public Optional<Stream> getStreamBy(EntityManager em, Long sensorId, Long streamId) {
        return getStreamBy(em, sensorId, streamId, 0);
    }

    public Optional<Stream> getStreamBy(EntityManager em, Long sensorId, Long streamId, int maxData) {
        return getUniqueOrEmpty(
                em.createQuery("select stm " +
                        "from Stream stm " +
                        "where stm.id=:streamId and " +
                        "stm.sensor.id=:sensorId")
                        .setParameter("streamId", streamId)
                        .setParameter("sensorId", sensorId)

        );
    }

    public Optional<Stream> getStreamBy(EntityManager em, String sensorKey, String streamKey) {
        return getStreamBy(em, sensorKey, streamKey, 0);
    }

    public Optional<Stream> getStreamBy(EntityManager em, String sensorKey, String streamKey, int maxData) {
           return getUniqueOrEmpty(em.createQuery("select stm " +
                            "from Stream stm " +
                            "where stm.key=:streamKey and " +
                            "stm.sensor.key=:sensorKey")
                            .setParameter("streamKey", streamKey)
                            .setParameter("sensorKey", sensorKey));
    }

    public Optional<Stream> getStreamBy(EntityManager em, String streamKey) {
        return getUniqueOrEmpty(em.createQuery("SELECT st FROM Stream st WHERE st.key=:streamKey")
                                .setParameter("streamKey", streamKey));
    }

    public List<StreamData> getStreamDataBy(EntityManager em, String sensorKey, String streamKey, Map<String, Long> queryParams) {
            StreamData from = from(StreamData.class);
            OnGoingLogicalCondition condition = condition(from.getStream().getKey()).eq(streamKey)
                    .and(from.getStream().getSensor().getKey()).eq(streamKey);
            Long start = queryParams.get("start");
            Long end = queryParams.get("end");
            Long length = queryParams.get("length");

            if (start != null) condition.and(condition(from.getServerTimestamp()).gte(start));
            if (end != null) condition.and(condition(from.getServerTimestamp()).lte(end));

            where(condition);
            Query<StreamData> select = select(from);

            return (length != null) ? select.setMaxResults(length.intValue()).list(em) : select.list(em);
    }

    public List<StreamData> getStreamDataBy(EntityManager em, Long sensorId, Long streamId, Map<String, Long> queryParams) {
            StreamData from = from(StreamData.class);
            OnGoingLogicalCondition condition = condition(from.getStream().getId()).eq(streamId)
                    .and(from.getStream().getSensor().getId()).eq(sensorId);
            Long start = queryParams.get("start");
            Long end = queryParams.get("end");
            Long length = queryParams.get("length");

            if (start != null) condition.and(condition(from.getServerTimestamp()).gte(start));
            if (end != null) condition.and(condition(from.getServerTimestamp()).lte(end));

            where(condition);
            Query<StreamData> select = select(from);

            return (length != null) ? select.setMaxResults(length.intValue()).list(em) : select.list(em);
    }


}
