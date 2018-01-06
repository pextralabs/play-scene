package repos;

import db.DatabaseExecutionContext;
import models.Persistent;
import models.sensing.Stream;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    protected Optional getUniqueOrEmpty(Query query) {
        List results = query.getResultList();
        return !results.isEmpty() ? Optional.of( results.get(0)) : Optional.empty();
    }

    public <T> CompletableFuture<T> withinAsyncTransaction(Function<EntityManager, T> function) {
        return supplyAsync(() -> jpaApi.withTransaction(function), executionContext);
    }

    public Optional<? extends Persistent> getById(EntityManager em, Class<? extends Persistent> clazz, Long oid) {
        return Optional.ofNullable(em.find(clazz, oid));
    }

    public Persistent save(EntityManager em, Persistent obj) {
        em.persist(obj);
        return obj;
    }

    public Persistent delete(EntityManager em, Persistent obj) {
        em.remove(obj);
        return obj;
    }

}
