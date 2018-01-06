package scene;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import br.ufes.inf.lprm.scene.model.events.Activation;
import br.ufes.inf.lprm.situation.model.events.SituationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.base.Entity;
import models.Persistent;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import br.ufes.inf.lprm.situation.model.Situation;
import play.libs.Json;

import java.io.IOException;
import java.util.Set;

public class SituationBroadcaster extends DefaultRuleRuntimeEventListener {

    ActorRef owner;
    Set<ActorRef> subscribers;
    LoggingAdapter logger;

    public SituationBroadcaster(ActorRef owner, Set<ActorRef> subscribers, LoggingAdapter logger) {
        this.logger = logger;
        this.owner = owner;
        this.subscribers = subscribers;
    }

    public String beautify(JsonNode jsonNode) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(jsonNode.toString(), Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    public JsonNode toJson(SituationEvent event) {
        ObjectNode json = Json.newObject();

        if ( event instanceof Activation) {
            json.put("type", "activation");
        } else{
            json.put("type", "deactivation");
        }

        json.put("timestamp", event.getTimestamp());

        ObjectNode situationNode = Json.newObject();
        json.set("situation", situationNode);

        situationNode.put("rid", event.getSituation().getUID());
        situationNode.put("type", event.getSituation().getType().getName());
        situationNode.put("active", event.getSituation().isActive());
        situationNode.put("started", event.getSituation().getActivation().getTimestamp());
        if (!event.getSituation().isActive()) {
            situationNode.put("finished", event.getSituation().getDeactivation().getTimestamp());
        }

        ArrayNode participationsNode = situationNode.putArray("participations");

        event.getSituation().getParticipations().forEach(
                participation -> {
                    ObjectNode participationNode = Json.newObject();
                    if (participation.getActor() instanceof Persistent) {
                        participationNode.put("id", ((Persistent) participation.getActor()).getId());
                    }

                    if (participation.getActor() instanceof Entity) {
                        participationNode.put("type", ((Entity) participation.getActor()).getType());
                    }
                    else participationNode.put("type", participation.getActor().getClass().getCanonicalName());

                    participationNode.put("as", participation.getPart().getLabel());
                    participationsNode.add(participationNode);
                }
        );

        return json;
    }

    @Override
    public void objectInserted(ObjectInsertedEvent event) {
        try {
            Object in = event.getObject();
            if (in instanceof Situation) {
                logger.info( beautify(toJson(((Situation) in).getActivation())));
                subscribers.forEach(
                        subscriber -> subscriber.tell(toJson(((Situation) in).getActivation()), owner)
                );
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void objectUpdated(ObjectUpdatedEvent event) {
        try {
            Object in = event.getObject();
            if (in instanceof Situation) {
                logger.info( beautify(toJson(((Situation) in).getDeactivation())));

                subscribers.forEach(
                        subscriber -> subscriber.tell(toJson(((Situation) in).getDeactivation()), owner)
                );
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
