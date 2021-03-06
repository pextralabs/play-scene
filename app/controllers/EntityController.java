package controllers;

import actors.Protocols;
import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.name.Named;
import models.base.Entity;
import models.base.Sensor;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import repos.EntityRepo;
import utils.JsonResults;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static actors.Protocols.Operation.Type.INSERT;
import static actors.Protocols.Operation.Type.UPDATE;

public class EntityController extends Controller {

    private final ActorRef sceneActor;
    private final HttpExecutionContext httpExecutionContext;
    private final EntityRepo entityRepo;

    @Inject
    public EntityController(EntityRepo entityRepo,
                            HttpExecutionContext httpExecutionContext,
                            @Named("scene") ActorRef sceneActor) {
        this.entityRepo = entityRepo;
        this.httpExecutionContext = httpExecutionContext;
        this.sceneActor = sceneActor;
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> addEntity() {

        JsonNode entityNode = request().body().asJson();
        if (!entityNode.has("name"))
            return CompletableFuture.completedFuture(JsonResults.badRequest("missing name"));

        if (!entityNode.has("type"))
            return CompletableFuture.completedFuture(JsonResults.badRequest("missing type"));

        Entity entity = new Entity(entityNode.findPath("type").textValue(), entityNode.findPath("name").textValue());

        return entityRepo.withinAsyncTransaction(
           em -> {
               entityRepo.save(em, entity);
               sceneActor.tell(new Protocols.Operation(entity, Protocols.Operation.Type.INSERT), null );
               return ok(Json.toJson(entity));
           }
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> addSensor(Long entityId) {

        return entityRepo.withinAsyncTransaction(
            em -> entityRepo.getById(em, Entity.class, entityId).map(
                entity -> {
                    JsonNode sensorNode = request().body().asJson();

                    if (!sensorNode.has("label"))
                        return JsonResults.badRequest("missing label");

                    if (!sensorNode.has("initValue"))
                        return JsonResults.badRequest("missing default value");

                    Sensor sensor = new Sensor((Entity) entity, sensorNode.findPath("label").textValue(), sensorNode.findPath("initValue").doubleValue() );
                    entityRepo.save(em, sensor);
                    sceneActor.tell(new Protocols.Operation(sensor, INSERT), null );
                    return ok(Json.toJson(sensor));
                    }
            ).orElse(
                    JsonResults.notFound("entity not found")
            )
        );

    }

    public CompletionStage<Result> updateSensor(Long entityId, Long sensorId) {

        return entityRepo.withinAsyncTransaction(
                em -> entityRepo.getSensorByEntity(em, entityId, sensorId).map(
                        sensor -> {
                            JsonNode sensorNode = request().body().asJson();
                            if (!sensorNode.has("value"))
                                return JsonResults.badRequest("missing value");

                            entityRepo.save(em, sensor.setValue(
                                    sensorNode.findPath("value").doubleValue()
                            ));

                            sceneActor.tell(new Protocols.Operation(sensor, UPDATE), null );
                            return ok(Json.toJson(sensor));
                        }
                ).orElse(
                        JsonResults.notFound("sensor not found")
                )
        );

    }

}
            
