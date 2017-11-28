package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import br.ufes.inf.lprm.scene.SceneApplication;
import com.google.inject.assistedinject.Assisted;
import play.api.Environment;
import scene.PersistenceListener;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.drools.compiler.kproject.models.KieModuleModelImpl;
import org.drools.core.ClockType;
import org.kie.api.KieBase;
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

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SceneActor extends AbstractActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private Set<ActorRef> subscribers;
    private final KieSession kSession;
    private final ExecutorService ex;

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
        kieBaseModel.addInclude("sceneKieBase");

        File userpath = env.getFile("conf/scene/" + sceneId);

        File[] files = userpath.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".drl"));

        if (files != null) {
            for (File file : files) {
                kFileSystem.write(
                        kServices.getResources().newFileSystemResource(file).setResourceType(ResourceType.DRL)
                );
            }
        }

        KieSessionModel kieSessionModel = kieBaseModel.newKieSessionModel(sceneId + ".session");
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
            e1.printStackTrace();
        }

        kbuilder.setDependencies(dependencies.toArray(new Resource[0]));
        kbuilder.buildAll();
        if (kbuilder.getResults().hasMessages()) {
            throw new IllegalArgumentException("Couldn't build knowledge module " + kbuilder.getResults());
        }

        KieModule kModule = kbuilder.getKieModule();
        KieContainer kContainer = kServices.newKieContainer(kModule.getReleaseId());

        /*KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        KieBase kieBase = kContainer.newKieBase("temporal-operators", config);*/

        KieSession kSession = kContainer.newKieSession(sceneId + ".session");
        kSession.addEventListener(new PersistenceListener(self(), subscribers));
        return kSession;

    }

    @Inject
    public SceneActor(Environment env) {

        Logger.debug("Starting scene actor");

        this.subscribers = new HashSet<>();

        kSession = newSession(env, "scene-actor");

        ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new LoaderClassPath(Enhancer.class.getClassLoader()));
        classPool.appendClassPath(new LoaderClassPath(env.classLoader()));

        SceneApplication app = new SceneApplication(classPool, kSession, "scene-actor");

        //kSession.setGlobal("publisher", publisher);
        ex = Executors.newSingleThreadExecutor();
        ex.submit((Runnable) kSession::fireUntilHalt);
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
                            kSession.submit(session -> collection.forEach(session::insert));
                            break;
                        case UPDATE:
                            kSession.submit(session -> collection.forEach(item -> {
                                session.update(session.getFactHandle(item), item);
                            }));
                            break;
                        case DELETE:
                            kSession.submit(session -> collection.forEach(item -> {
                                session.delete(session.getFactHandle(item));
                            }));
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
