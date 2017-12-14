package repos;

import db.DatabaseExecutionContext;
import models.Entity;
import models.Sensor;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class EntityRepo extends BaseRepo {

    @Inject
    public EntityRepo(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        super(jpaApi, executionContext);
    }

    public CompletionStage<List<Entity>> getEntities() {
        return supplyAsync(() -> withTransaction(
            (em) -> em.createQuery("select e from Entity e", Entity.class).getResultList()
        ), executionContext);
    }

    public CompletionStage<Optional<Sensor>> getSensorByEntity(Long entityId, Long sensorId) {
        return supplyAsync(() -> withTransaction(
                (em) -> Optional.ofNullable(
                        em.createQuery("select s from Sensor s where s.id=:sensorId and s.bearer=:entityId", Sensor.class)
                        .setParameter("sensorId", sensorId)
                        .setParameter("entityId", entityId)
                        .getSingleResult()
                )
        ), executionContext);
    }





}
