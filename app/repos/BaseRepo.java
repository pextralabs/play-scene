package repos;

import db.DatabaseExecutionContext;
import models.Persistent;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class BaseRepo {

    protected final JPAApi jpaApi;
    protected final DatabaseExecutionContext executionContext;

    @Inject
    public BaseRepo(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    protected <T> T withTransaction(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }

    public CompletionStage<Optional<? extends Persistent>> getById(Class<? extends Persistent> clazz, Long oid) {
        return supplyAsync(() -> withTransaction(em -> Optional.ofNullable(em.find(clazz, oid)) ), executionContext);
    }

    public CompletionStage<? extends Persistent> saveAsync(Persistent obj) {
        return supplyAsync(() -> withTransaction((em) -> {
            em.persist(obj);
            return obj;
        }), executionContext);
    }

    public Persistent save(Persistent obj) {
        return withTransaction((em) -> {
            em.persist(obj);
            return obj;
        });
    }

}
