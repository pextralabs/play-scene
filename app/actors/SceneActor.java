package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import br.ufes.inf.lprm.scene.SceneApplication;
import models.base.Entity;
import models.sensing.Sensor;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.rule.FactHandle;
import play.api.Environment;
import repos.EntityRepo;
import repos.SensorRepo;
import scala.Option;
import scene.SituationBroadcaster;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.drools.compiler.kproject.models.KieModuleModelImpl;
import org.drools.core.ClockType;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.springframework.cglib.proxy.Enhancer;
import play.Logger;
import utils.JsonViews;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static actors.Protocols.Operation.Type.INSERT;

public class SceneActor extends AbstractActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final Set<ActorRef> subscribers;
    private final Environment env;
    private final EntityRepo entityRepo;
    private final SensorRepo sensorRepo;

    private KieSession kSession;
    private CompletableFuture engine;

    private KieSession newSession(Environment env, String sceneId) {
        // Getting KieServices
        KieServices kServices = KieServices.Factory.get();
        // Instantiating a KieFileSystem and KieModuleModel
        KieFileSystem kFileSystem = kServices.newKieFileSystem();
        KieModuleModel kieModuleModel = kServices.newKieModuleModel();

        ReleaseId releaseId = kServices.newReleaseId("br.ufes.inf.lprm.scene", sceneId, "0.1.0");
        kFileSystem.generateAndWritePomXML(releaseId);

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel(sceneId);
        kieBaseModel.setEventProcessingMode(EventProcessingOption.STREAM);
        kieBaseModel.setEqualsBehavior(EqualityBehaviorOption.EQUALITY);
        kieBaseModel.addInclude("sceneKieBase");

        Option<URL> scenePath = env.resource("scene");

        if (scenePath.isEmpty()) throw new IllegalStateException("couldn't find scene's root dir");

        File userpath;
        try {
            userpath = new File(scenePath.get().toURI());
        } catch(URISyntaxException e) {
            userpath = new File(scenePath.get().getPath());
        }

        File[] files = userpath.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".drl"));

        if (files != null) {
            for (File file : files) {
                kFileSystem.write(
                        kServices.getResources().newFileSystemResource(file).setResourceType(ResourceType.DRL)
                );
            }
        }

        KieSessionModel kieSessionModel = kieBaseModel.newKieSessionModel(sceneId + ".kSession");
        kieSessionModel.setClockType(ClockTypeOption.get(ClockType.REALTIME_CLOCK.getId()))
                .setType(KieSessionModel.KieSessionType.STATEFUL);

        kFileSystem.writeKModuleXML(kieModuleModel.toXML());
        KieBuilder kbuilder = kServices.newKieBuilder(kFileSystem);
        ArrayList<Resource> dependencies = new ArrayList();
        try {
            Enumeration<URL> e = env.classLoader().getResources(KieModuleModelImpl.KMODULE_JAR_PATH);
            while ( e.hasMoreElements() ) {
                URL url = e.nextElement();
                String path;
                if (url.getPath().contains(".jar!")) {
                    path = url.getPath().replace("!/" + KieModuleModelImpl.KMODULE_JAR_PATH, "");
                    dependencies.add(kServices.getResources().newUrlResource(path));
                } else {
                    path = url.getPath().replace(KieModuleModelImpl.KMODULE_JAR_PATH, "");
                    dependencies.add(kServices.getResources().newFileSystemResource(path));
                }

            }
        } catch (IOException e1) {
            logger.error(e1.getMessage());
        }

        kbuilder.setDependencies(dependencies.toArray(new Resource[0]));
        kbuilder.buildAll();
        if (kbuilder.getResults().hasMessages()) {
            logger.error("Couldn't build knowledge module " + kbuilder.getResults());
            throw new IllegalArgumentException("Couldn't build knowledge module " + kbuilder.getResults());
        }

        KieModule kModule = kbuilder.getKieModule();
        KieContainer kContainer = kServices.newKieContainer(kModule.getReleaseId());

        KieSession session = kContainer.newKieSession(sceneId + ".kSession");
        session.addEventListener(new SituationBroadcaster(self(), subscribers, logger));
        return session;

    }

    @Override
    public void preStart() {

        Logger.debug("Starting scene actor");

        kSession = newSession(env, "scene-actor");

        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(Enhancer.class.getClassLoader()));
        classPool.appendClassPath(new LoaderClassPath(env.classLoader()));

        new SceneApplication(classPool, kSession, "scene-actor");

        sensorRepo.withinAsyncTransaction(
                em -> {
                    List<Entity> entities = entityRepo.getEntities(em);
                    self().tell(new Protocols.Operation(entities, INSERT), self() );
                    entities.forEach(
                            entity -> self().tell(new Protocols.Operation(entity.getSensors(), INSERT), self() )
                    );
                    return entities;
                }
        );

        sensorRepo.withinAsyncTransaction(
            em -> {
                List<Sensor> sensors = sensorRepo.getSensors(em);
                self().tell(new Protocols.Operation(sensors, INSERT), self() );
                sensors.forEach(
                        sensor -> self().tell(new Protocols.Operation(sensor.getStreams(), INSERT), self() )
                );
                return sensors;
            }
        );

        kSession.setGlobal("logger", logger);

        engine = CompletableFuture.runAsync(kSession::fireUntilHalt)
              .exceptionally(
                err -> {
                    logger.error(err.toString());
                    return null;
                }
        );
    }

    @Override
    public void postStop() {
        logger.info("Shutting down SCENE engine...");
        engine.cancel(true);
        kSession.dispose();
    }

    @Inject
    public SceneActor(Environment env, EntityRepo entityRepo, SensorRepo sensorRepo) {
        this.env = env;
        this.sensorRepo = sensorRepo;
        this.entityRepo = entityRepo;

        this.subscribers = new HashSet<>();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Protocols.Operation.class, (Protocols.Operation operation) -> {
                    Collection collection;
                    if (operation.obj instanceof Collection) {
                        collection = (Collection) operation.obj;
                    } else {
                        collection = new ArrayList();
                        collection.add(operation.obj);
                    }
                    switch (operation.type) {
                        case INSERT:
                            kSession.submit(session -> collection.forEach(item -> {
                                if (session.getFactHandle(item) == null) {
                                    logger.info("inserting object... {}", item);
                                    session.insert(item);
                                    logger.info("object inserted...");
                                }
                            } ));
                            break;
                        case UPDATE:
                            kSession.submit(session -> collection.forEach(item -> {
                                FactHandle handle = session.getFactHandle(item);
                                if (handle == null) {
                                    logger.info("fact handle for `{}` not found... inserting it", item);
                                    logger.info("inserting object... {}", JsonViews.toJson(item, JsonViews.Default.class ));
                                    session.insert(item);
                                    logger.info("object inserted...");
                                } else {
                                    logger.info("updating object... {}",  JsonViews.toJson(item, JsonViews.Default.class ));
                                    session.update(handle, item);
                                    logger.info("object updated...");
                                }
                            }));
                            break;
                        case DELETE:
                            kSession.submit(session -> collection.forEach(item -> session.delete(session.getFactHandle(item))));
                    }
                } )
                .match(Protocols.Subscribe.class, subscribe -> {
                    logger.info("adding a new subscriber for `{}`", self().path());
                    subscribers.add(sender());
                })
                .match(Protocols.Unsubscribe.class, unsubscribe -> {
                    logger.info("removing subscriber for `{}`", self().path());
                    subscribers.remove(sender());
                })
                .build();
    }

}
