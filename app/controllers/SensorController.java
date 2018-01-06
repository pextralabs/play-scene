package controllers;

import actors.Protocols;
import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import models.sensing.*;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import repos.SensorRepo;
import scala.Option;
import utils.JsonResults;
import utils.JsonViews;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static actors.Protocols.Operation.Type.INSERT;

public class SensorController extends Controller {

    private final SensorRepo sensorRepo;
    private final ActorRef sceneActor;
    private final HttpExecutionContext httpExecutionContext;

    @Inject
    public SensorController(SensorRepo sensorRepo,
                            @Named("scene") ActorRef sceneActor,
                            HttpExecutionContext httpExecutionContext) {
        this.sensorRepo = sensorRepo;
        this.sceneActor = sceneActor;
        this.httpExecutionContext = httpExecutionContext;
    }

    public CompletionStage<Result> getSensors() {
        return sensorRepo.withinAsyncTransaction(
          em -> JsonResults.ok(JsonViews.toString( sensorRepo.getSensors(em) ) )
        );
    }

    public CompletionStage<Result> getSensor(Long sensorId) {
        return sensorRepo.withinAsyncTransaction(
            em -> sensorRepo.getSensorBy(em, sensorId).map(
                   sensor -> JsonResults.ok(JsonViews.toString(sensor))
                ).orElse(
                    JsonResults.notFound("sensor not found")
                )
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> createSensor() {

        JsonNode sensorNode = request().body().asJson();
        if (!sensorNode.has("label"))
            return CompletableFuture.completedFuture(JsonResults.badRequest("missing label"));

        Sensor sensor = new Sensor().setName(sensorNode.findPath("label").textValue());

        if (sensorNode.has("description"))
            sensor.setDescription(sensorNode.findPath("description").textValue());

        if (sensorNode.has("tag"))
            sensor.setTag(sensorNode.findPath("tag").textValue());

        return sensorRepo.withinAsyncTransaction(
                em -> {
                    sensorRepo.save(em, sensor);
                    sceneActor.tell(new Protocols.Operation(sensor, INSERT), null);
                    return JsonResults.ok(JsonViews.toString(sensor));
                }
        );

    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> updateSensor(Long sensorId) {
        return sensorRepo.withinAsyncTransaction(
            em -> {
                Optional sensorOpt = sensorRepo.getById(em, Sensor.class, sensorId);

                if (sensorOpt.isPresent()) {
                    Sensor updating = (Sensor) sensorOpt.get();
                    JsonNode sensorNode = request().body().asJson();

                    if (!sensorNode.has("label"))
                        return JsonResults.badRequest("missing label");

                    updating.setName(sensorNode.findPath("label").textValue());

                    if (sensorNode.has("description"))
                        updating.setDescription(sensorNode.findPath("description").textValue());

                    if (sensorNode.has("tag"))
                        updating.setTag(sensorNode.findPath("tag").textValue());

                    sensorRepo.save(em, updating);

                    return JsonResults.ok(JsonViews.toString(updating));
                } else return JsonResults.notFound("sensor not found");
            }
        );

    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> deleteSensor(Long sensorId) {
        return sensorRepo.withinAsyncTransaction(
            em -> {
                Optional sensorOpt = sensorRepo.getById(em, Sensor.class, sensorId);
                if (sensorOpt.isPresent()) {
                    Sensor deleting = (Sensor) sensorOpt.get();
                    sensorRepo.delete(em, deleting);
                    return ok();
                } else return JsonResults.notFound("sensor not found");
            }
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> createStream(Long sensorId) {
        JsonNode streamNode = request().body().asJson();
        return sensorRepo.withinAsyncTransaction(
              em -> {
                  Optional sensorOpt = sensorRepo.getById(em, Sensor.class, sensorId);

                  if (sensorOpt.isPresent()) {

                      Sensor sensor = (Sensor) sensorOpt.get();



                      if (!streamNode.has("label"))
                          return JsonResults.badRequest("missing label");

                      Stream stream = new Stream(sensor).setName(streamNode.findPath("label").textValue());

                      if (streamNode.has("tag"))
                          stream.setTag(streamNode.findPath("tag").textValue());

                      sensor.getStreams().add(stream);

                      sensorRepo.save(em, stream);
                      sceneActor.tell(new Protocols.Operation(stream, INSERT), null);
                      return JsonResults.ok(JsonViews.toString(stream));
                  } else return JsonResults.notFound("sensor not found");
              }
        );

    }

    public CompletionStage<Result> getStream(Long sensorId, Long streamId) {
        return sensorRepo.withinAsyncTransaction(
                em -> {
                    Optional streamOpt = sensorRepo.getStreamBy(em, sensorId, streamId, 5);
                    return streamOpt.isPresent() ?
                            JsonResults.ok(JsonViews.toString(streamOpt.get(), JsonViews.Complete.class))
                            : JsonResults.notFound("stream not found");
                }
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> updateStream(Long sensorId, Long streamId) {
        JsonNode streamNode = request().body().asJson();
        return sensorRepo.withinAsyncTransaction(
            em ->
                sensorRepo.getStreamBy(em, sensorId, streamId).map(
                    stream -> {
                        if (!streamNode.has("label"))
                            return JsonResults.badRequest("missing label");

                        stream.setName(streamNode.findPath("label").textValue());
                        stream.getSensor().getStreams().add(stream);
                        sensorRepo.save(em, stream);
                        return JsonResults.ok(JsonViews.toString(stream));
                    }
                ).orElse(
                        JsonResults.notFound("stream not found")
                )
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> deleteStream(Long sensorId, Long streamId) {
        return sensorRepo.withinAsyncTransaction(
            em -> sensorRepo.getStreamBy(em, sensorId, streamId).map(
                stream -> {
                    Sensor sensor = stream.getSensor();
                    sensorRepo.delete(em, stream);
                    return JsonResults.ok(JsonViews.toString(sensor));
                }
            ).orElse(JsonResults.notFound("stream not found"))
        );
    }

    @BodyParser.Of(BodyParser.Json.class)
    public CompletionStage<Result> putData(String sensorKey, String streamKey) {
        JsonNode dataNode = request().body().asJson();
        return sensorRepo.withinAsyncTransaction(
                em -> sensorRepo.getStreamBy(em, sensorKey, streamKey).map(

                        stream -> {
                            StreamData data = !dataNode.has("timestamp") ?
                                    new StreamData(stream, System.currentTimeMillis()) :
                                    new StreamData(stream, System.currentTimeMillis(), dataNode.get("timestamp").asLong());

                            if (!dataNode.has("values"))
                                return JsonResults.badRequest("missing values");

                            JsonNode valuesNode = dataNode.get("values");
                            Iterator<String> itr = valuesNode.fieldNames();
                            while (itr.hasNext()) {
                                String key = itr.next();
                                data.addTuple(key, valuesNode.get(key).asText());
                            }

                            sensorRepo.save(em, data);
                            sceneActor.tell(new Protocols.Operation(data, INSERT), null);
                            return ok(Json.toJson(data));
                        }

                ).orElse(JsonResults.notFound("stream not found"))
        );
    }

    public CompletionStage<Result> getStreamData(Long sensorId, Long streamId, Option<String> start, Option<String> end, Option<String> length) {
        Map<String, Long> params = new HashMap<>();
        start.map(p -> params.put("start", new Long(p)));
        end.map(p -> params.put("end", new Long(p)));
        length.map(p -> params.put("length", new Long(p)));
        return sensorRepo.withinAsyncTransaction(
                em -> ok(Json.toJson(sensorRepo.getStreamDataBy(em, sensorId, streamId, params)))
        );
    }

}
