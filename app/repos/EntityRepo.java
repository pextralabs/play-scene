package repos;

import db.DatabaseExecutionContext;
import models.base.Entity;
import models.base.Sensor;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class EntityRepo extends BaseRepo {

    @Inject
    public EntityRepo(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        super(jpaApi, executionContext);
    }

    public List<Entity> getEntities(EntityManager em) {
        return em.createQuery("select e from Entity e", Entity.class).getResultList();
    }

    public Optional<Sensor> getSensorByEntity(EntityManager em, Long entityId, Long sensorId) {
        return getUniqueOrEmpty(
                em.createQuery("select s from models.base.Sensor s where s.id=:sensorId and s.bearer=:entityId", models.base.Sensor.class)
                        .setParameter("sensorId", sensorId)
                        .setParameter("entityId", entityId)
        );

    }





}
