package org.jetbrains.mps.tool.builder;

import jetbrains.mps.baseLanguage.closures.runtime.Wrappers;
import jetbrains.mps.classloading.ClassLoaderManager;
import jetbrains.mps.core.tool.environment.classloading.UrlClassLoader;
import jetbrains.mps.core.tool.environment.util.FileMPSProject;
import jetbrains.mps.extapi.persistence.FileDataSource;
import jetbrains.mps.generator.GenerationFacade;
import jetbrains.mps.generator.GenerationSettingsProvider;
import jetbrains.mps.generator.IModifiableGenerationSettings;
import jetbrains.mps.internal.collections.runtime.*;
import jetbrains.mps.internal.make.cfg.TextGenFacetInitializer;
import jetbrains.mps.library.LibraryInitializer;
import jetbrains.mps.library.ModulesMiner;
import jetbrains.mps.make.MPSCompilationResult;
import jetbrains.mps.make.MakeSession;
import jetbrains.mps.make.ModuleMaker;
import jetbrains.mps.make.facet.IFacet;
import jetbrains.mps.make.facet.ITarget;
import jetbrains.mps.make.resources.IResource;
import jetbrains.mps.make.script.*;
import jetbrains.mps.make.service.AbstractMakeService;
import jetbrains.mps.messages.IMessage;
import jetbrains.mps.messages.IMessageHandler;
import jetbrains.mps.progress.EmptyProgressMonitor;
import jetbrains.mps.project.DevKit;
import jetbrains.mps.project.MPSExtentions;
import jetbrains.mps.project.Project;
import jetbrains.mps.project.io.DescriptorIOFacade;
import jetbrains.mps.smodel.*;
import jetbrains.mps.smodel.persistence.def.ModelPersistence;
import jetbrains.mps.smodel.persistence.def.ModelReadException;
import jetbrains.mps.smodel.resources.MResource;
import jetbrains.mps.smodel.resources.ModelsToResources;
import jetbrains.mps.tool.builder.make.BuildMakeService;
import jetbrains.mps.tool.common.ScriptProperties;
import jetbrains.mps.tool.environment.Environment;
import jetbrains.mps.tool.environment.EnvironmentConfig;
import jetbrains.mps.tool.environment.MpsEnvironment;
import jetbrains.mps.util.Computable;
import jetbrains.mps.util.FileUtil;
import jetbrains.mps.util.IterableUtil;
import jetbrains.mps.util.PathManager;
import jetbrains.mps.vfs.FileSystem;
import jetbrains.mps.vfs.IFile;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.mps.openapi.model.SModel;
import org.jetbrains.mps.openapi.model.SModelReference;
import org.jetbrains.mps.openapi.module.SModule;
import org.jetbrains.mps.openapi.persistence.PersistenceFacade;
import org.jetbrains.mps.openapi.util.Consumer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GeneratorWorker {
    protected final List<String> myErrors = new ArrayList<String>();
    protected final List<String> myWarnings = new ArrayList<String>();
    protected final Script myWhatToDo;
    private final UrlClassLoader myClassLoader;
    private final MyMessageHandler myMessageHandler = new MyMessageHandler();
    private final Log myLogger;
    protected Environment myEnvironment;

    public GeneratorWorker(Script whatToDo, Log logger) {
        myWhatToDo = whatToDo;
        myLogger = logger;
        myClassLoader = createClassloader();
    }

    public static EnvironmentConfig createEnvConfig(Script whatToDo) throws IOException {
        EnvironmentConfig config = EnvironmentConfig.emptyConfig();
        for (IMapping<String, String> macro : MapSequence.fromMap(whatToDo.getMacro())) {
            config = config.addMacro(macro.key(), new File(macro.value()));
        }

        for (String jar : whatToDo.getLibraryJars()) {
            config = config.addLib(jar);
        }

        if (whatToDo.isLoadBootstrapLibraries()) {
            config = config.withBootstrapLibraries();
        }
        return config;
    }

    public static StringBuffer extractStackTrace(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.getBuffer();
    }

    public void workFromBaseGenerator() throws IOException {
        setupEnvironment();
        boolean doneSomething = false;

        if (makeProjects(myWhatToDo.getMPSProjectFiles().keySet())) {
            doneSomething = true;
        }

        // the rest -- using dummy project
        LinkedHashSet<SModule> modules = new LinkedHashSet<SModule>();
        LinkedHashSet<SModel> models = new LinkedHashSet<SModel>();
        collectFromModuleFiles(myWhatToDo.getModules(), modules);
        collectFromModelFiles(models);
        ObjectsToProcess go = new ObjectsToProcess(Collections.EMPTY_SET, modules, models);
        if (go.hasAnythingToGenerate()) {
            Project project = createDummyProject();
            //executeTask(project, go);
            generate(project, go);
            doneSomething = true;
        }

        if (!(doneSomething)) {
            error("Could not find anything to generate.");
        }

        dispose();
        showStatistic();
    }

    private boolean makeProjects(Set<File> projects) {
        for (File file : projects) {
            makeProject(file);
        }
        return !projects.isEmpty();
    }

    private void makeProject(File file) {
        FileMPSProject p = new FileMPSProject(file);
        makeProject();
        p.projectOpened();

        info("Loaded project " + p);

        executeTask(p, new ObjectsToProcess(Collections.singleton(p), new HashSet<SModule>(), new HashSet<SModel>()));

        p.projectClosed();
    }

    public void work() throws IOException {
        EnvironmentConfig config = createEnvConfig(myWhatToDo);

        Environment environment = new GeneratorWorker.MyEnvironment(config);
        environment.init();
        setupEnvironment();
        setGenerationProperties();
        boolean doneSomething = false;

        Project project = createDummyProject();

        for (IMapping<List<String>, Boolean> chunk : MapSequence.fromMap(myWhatToDo.getChunks())) {
            final List<String> modulePaths = chunk.key();
            final Set<SModule> modules = new LinkedHashSet<SModule>();
            project.getModelAccess().runWriteAction(new Runnable() {
                public void run() {
                    for (String modulePath : modulePaths) {
                        processModuleFile(new File(modulePath), modules);
                    }
                }
            });
            Boolean bootstrap = chunk.value();
            if (bootstrap) {
                warning("Found bootstrap chunk " + chunk.key() + ". Generation may be impossible.");
            }
            ObjectsToProcess go = new ObjectsToProcess(Collections.EMPTY_SET, modules, Collections.EMPTY_SET);
            if (go.hasAnythingToGenerate()) {
                generate(project, go);
                doneSomething = true;
            }
        }

        if (!(doneSomething)) {
            error("Could not find anything to generate.");
        }

        dispose();
        showStatistic();
    }

    private UrlClassLoader createClassloader() {
        String pluginsPath = myWhatToDo.getProperty(ScriptProperties.PLUGIN_PATHS);
        Set<File> pluginsClasspath = SetSequence.fromSet(new LinkedHashSet<File>());
        if (pluginsPath != null) {
            for (String plugin : pluginsPath.split(File.pathSeparator)) {
                File lib = new File(plugin + File.separator + "lib");
                if (lib.exists() && lib.isDirectory()) {
                    SetSequence.fromSet(pluginsClasspath).addSequence(Sequence.fromIterable(Sequence.fromArray(lib.listFiles(PathManager.JAR_FILE_FILTER))));
                }
            }
        }
        if ((pluginsPath == null || pluginsPath.length() == 0)) {
            return null;
        }
        return new UrlClassLoader(SetSequence.fromSet(pluginsClasspath).select(new ISelector<File, URL>() {
            public URL select(File it) {
                try {
                    return it.toURI().toURL();
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }).where(new IWhereFilter<URL>() {
            public boolean accept(URL it) {
                return it != null;
            }
        }).toGenericArray(URL.class), LibraryInitializer.class.getClassLoader());
    }

    protected void executeTask(final Project project, ObjectsToProcess go) {
        setGenerationProperties();
        if (go.hasAnythingToGenerate()) {
            generate(project, go);
        }
    }

    protected void setGenerationProperties() {
        GeneratorProperties gp = new GeneratorProperties(myWhatToDo);
        IModifiableGenerationSettings settings = GenerationSettingsProvider.getInstance().getGenerationSettings();
        boolean strictMode = gp.isStrictMode();
        boolean parallelMode = gp.isParallelMode();
        boolean inplace = gp.isInplaceTransform();
        boolean warnings = !(gp.isHideWarnings());
        int threadCount = gp.getParallelThreads();
        final boolean useStaticRefs = gp.isCreateStaticRefs();
        settings.setStrictMode(strictMode);
        if (strictMode) {
            settings.setParallelGenerator(parallelMode);
            if (parallelMode) {
                settings.setNumberOfParallelThreads(threadCount);
            }
        }
        String[] onoff = new String[]{"on", "off"};
        settings.enableInplaceTransformations(inplace);
        settings.setShowBadChildWarning(warnings);
        settings.setCreateStaticReferences(useStaticRefs);
        // incremental generation for Ant build doesn't make sense as we have no way to ensure 'unchanged' artifacts are still there
        settings.setIncremental(false);
        settings.setIncrementalUseCache(false);
        settings.setCheckModelsBeforeGeneration(false);
        info(String.format("Generating: strict mode is %s, parallel generation is %s (%d threads), in-place is %s, warnings are %s, static references to replace dynamic is %s", onoff[(strictMode ? 0 : 1)], onoff[(parallelMode ? 0 : 1)], (parallelMode ? threadCount : 1), onoff[(inplace ? 0 : 1)], onoff[(warnings ? 0 : 1)], onoff[(useStaticRefs ? 0 : 1)]));
    }

    protected void showStatistic() {
        failBuild("generation");
    }

    protected void generate(Project project, ObjectsToProcess go) {
        StringBuilder s = new StringBuilder("Generating:");
        for (Project p : go.getProjects()) {
            s.append("\n    ");
            s.append(p);
        }
        for (SModule m : go.getModules()) {
            s.append("\n    ");
            s.append(m);
        }
        for (SModel m : go.getModels()) {
            s.append("\n    ");
            s.append(m);
        }
        info(s.toString());
        Iterable<MResource> resources = Sequence.fromIterable(collectResources(project, go)).toListSequence();
        ModelAccess.instance().flushEventQueue();
        final MakeSession session = new MakeSession(project, myMessageHandler, true);
        AbstractMakeService.DefaultMonitor defaultMonitor = new AbstractMakeService.DefaultMonitor(session);
        IScriptController.Stub controller = new IScriptController.Stub(defaultMonitor, defaultMonitor) {
            @Override
            public void setup(IPropertiesPool ppool) {
                super.setup(ppool);
                new TextGenFacetInitializer(session).populate(ppool);
            }
        };

        IScript script = new ScriptBuilder()
                .withFacetNames(
                        new IFacet.Name("jetbrains.mps.lang.core.Generate"),
                        new IFacet.Name("jetbrains.mps.lang.core.TextGen"),
                        new IFacet.Name("jetbrains.mps.make.facets.Make"))
                .withFinalTarget(new ITarget.Name("jetbrains.mps.make.facets.Make.make"))
                .toScript();
        Future<IResult> res = new BuildMakeService().make(session, resources, script, controller, new EmptyProgressMonitor());

        try {
            if (!(res.get().isSucessful())) {
                myErrors.add("Make was not successful");
            }
        } catch (InterruptedException e) {
            myErrors.add(e.toString());
        } catch (ExecutionException e) {
            myErrors.add(e.toString());
        }
        ModelAccess.instance().flushEventQueue();
    }

    protected void makeProject() {
        final MPSCompilationResult mpsCompilationResult = ModelAccess.instance().runReadAction(new Computable<MPSCompilationResult>() {
            public MPSCompilationResult compute() {
                return new ModuleMaker().make(IterableUtil.asCollection(MPSModuleRepository.getInstance().getModules()), new EmptyProgressMonitor(), null);
            }
        });
        if (mpsCompilationResult.isReloadingNeeded()) {
            ModelAccess.instance().runWriteAction(new Runnable() {
                public void run() {
                    ClassLoaderManager.getInstance().reloadModules(mpsCompilationResult.getChangedModules());
                }
            });
        }
    }

    private Iterable<SModule> withGenerators(Iterable<SModule> modules) {
        return Sequence.fromIterable(modules).concat(Sequence.fromIterable(modules).where(new IWhereFilter<SModule>() {
            public boolean accept(SModule it) {
                return it instanceof Language;
            }
        }).translate(new ITranslator2<SModule, SModule>() {
            public Iterable<SModule> translate(SModule it) {
                return (List<SModule>) (List) ((Language) it).getGenerators();
            }
        }));
    }

    private Iterable<SModel> getModelsToGenerate(SModule mod) {
        return Sequence.fromIterable(((Iterable<SModel>) mod.getModels())).where(new IWhereFilter<SModel>() {
            public boolean accept(SModel it) {
                return GenerationFacade.canGenerate(it);
            }
        }).select(new ISelector<SModel, SModel>() {
            public SModel select(SModel it) {
                return (SModel) it;
            }
        });
    }

    protected Iterable<MResource> collectResources(Project project, final ObjectsToProcess go) {
        final Wrappers._T<Iterable<SModel>> models = new Wrappers._T<Iterable<SModel>>(null);
        project.getModelAccess().runReadAction(new Runnable() {
            public void run() {
                for (Project p : go.getProjects()) {
                    for (SModule mod : withGenerators((Iterable<SModule>) p.getModules())) {
                        models.value = Sequence.fromIterable(models.value).concat(Sequence.fromIterable((getModelsToGenerate(mod))));

                    }
                }
                for (SModule mod : withGenerators(go.getModules())) {
                    models.value = Sequence.fromIterable(models.value).concat(Sequence.fromIterable(getModelsToGenerate(mod)));
                }
                if (go.getModels() != null) {
                    models.value = Sequence.fromIterable(models.value).concat(SetSequence.fromSet(go.getModels()));
                }
            }
        });
        return Sequence.fromIterable(new ModelsToResources(Sequence.fromIterable(models.value).where(new IWhereFilter<SModel>() {
            public boolean accept(SModel smd) {
                return GenerationFacade.canGenerate(smd);
            }
        })).resources(false)).select(new ISelector<IResource, MResource>() {
            public MResource select(IResource r) {
                return (MResource) r;
            }
        });
    }

    private Environment createDefaultEnvironment() throws IOException {
        Environment env = MpsEnvironment.getOrCreate(createEnvConfig(myWhatToDo));
//        org.apache.log4j.Logger.getRootLogger().setLevel(myWhatToDo.getLogLevel());
        return env;
    }

    protected Project createDummyProject() {
        return myEnvironment.createEmptyProject();
    }

    protected void dispose() {
        if (myEnvironment != null) {
            myEnvironment.dispose();
            myEnvironment = null;
        }
    }

    public void setupEnvironment() throws IOException {
        if (myEnvironment == null) {
            myEnvironment = createDefaultEnvironment();

            final List<String> errors = new ArrayList<String>();
            Consumer<String> addToErrors = new Consumer<String>() {
                @Override
                public void consume(String s) {
                    errors.add(s);
                }
            };

            make();
        }
    }

    protected void make() {
        MPSCompilationResult mpsCompilationResult = ModelAccess.instance().runReadAction(new Computable<MPSCompilationResult>() {
            public MPSCompilationResult compute() {
                ModuleMaker maker = new ModuleMaker();
                return maker.make(IterableUtil.asCollection(MPSModuleRepository.getInstance().getModules()), new EmptyProgressMonitor(), null);
            }
        });
        reload(mpsCompilationResult);
    }

    protected void reload(final MPSCompilationResult mpsCompilationResult) {
        if (mpsCompilationResult.isReloadingNeeded()) {
            ModelAccess.instance().runWriteAction(new Runnable() {
                public void run() {
                    ClassLoaderManager.getInstance().reloadModules(mpsCompilationResult.getChangedModules());
                }
            });
        }
    }

    protected StringBuffer formatErrorsReport(String taskName) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 100; i++) {
            sb.append('*');
        }
        sb.append("\n");
        sb.append(myErrors.size());
        sb.append(" errors during " + taskName + ":\n");
        for (String error : myErrors) {
            sb.append(error);
            sb.append("\n");
        }
        for (int i = 0; i < 100; i++) {
            sb.append('*');
        }
        return sb;
    }

    protected void failBuild(String name) {
        if (!(myErrors.isEmpty()) && myWhatToDo.getFailOnError()) {
            throw new RuntimeException(this.formatErrorsReport(name).toString());
        }
    }

    public void collectModelsToGenerate(ObjectsToProcess go) {
        collectFromProjects(go.getProjects());
        collectFromModuleFiles(myWhatToDo.getModules(), go.getModules());
        collectFromModelFiles(go.getModels());
    }

    private void collectFromProjects(Set<Project> projects) {
        for (File projectFile : myWhatToDo.getMPSProjectFiles().keySet()) {
            if (projectFile.getAbsolutePath().endsWith(MPSExtentions.DOT_MPS_PROJECT)) {
                Project project = myEnvironment.openProject(projectFile);
                info("Loaded project " + project);
                projects.add(project);
            }
        }
    }

    protected void extractModels(Set<SModel> result, Project project) {
        for (SModule module : project.getModulesWithGenerators()) {
            for (SModel model : module.getModels()) {
                if (includeModel(model)) {
                    result.add(model);
                }
            }
        }
    }

    private boolean includeModel(SModel model) {
        return SModelStereotype.isUserModel(model) && GenerationFacade.canGenerate(model);
    }

    protected void extractModels(Collection<SModel> modelsList, SModule m) {
        for (SModel d : m.getModels()) {
            if (includeModel(d)) {
                modelsList.add(d);
            }
        }
    }

    protected void collectFromModuleFiles(final Set<File> inputModules, final Set<SModule> outputModules) {
        ModelAccess.instance().runWriteAction(new Runnable() {
            public void run() {
                for (File moduleFile : inputModules) {
                    processModuleFile(moduleFile, outputModules);
                }
            }
        });
    }

    protected void processModuleFile(final File moduleFile, final Set<SModule> modules) {
        if (DescriptorIOFacade.getInstance().fromFileType(FileSystem.getInstance().getFileByPath(moduleFile.getPath())) == null) {
            return;
        }
        List<SModule> tmpmodules = new ArrayList<SModule>();
        SModule moduleByFile = ModuleFileTracker.getInstance().getModuleByFile(FileSystem.getInstance().getFileByPath(moduleFile.getAbsolutePath()));
        if (moduleByFile != null) {
            tmpmodules = Collections.singletonList(moduleByFile);
        } else {
            IFile file = FileSystem.getInstance().getFileByPath(moduleFile.getPath());
            BaseMPSModuleOwner owner = new BaseMPSModuleOwner();
            for (ModulesMiner.ModuleHandle moduleHandle : new ModulesMiner().collectModules(file).getCollectedModules()) {
                SModule module = ModuleRepositoryFacade.createModule(moduleHandle, owner);
                if (module != null) {
                    tmpmodules.add(module);
                }
            }
        }
        for (SModule module : tmpmodules) {
            info("Loaded module " + module);
            if (module.isReadOnly()) {
                continue;
            }
            if (module instanceof DevKit) {
                continue;
            }
            modules.add(module);
            if (module instanceof Language) {
                Language language = (Language) module;
                for (Generator gen : language.getGenerators()) {
                    modules.add(gen);
                }
            }
        }
    }

    protected void collectFromModelFiles(Set<SModel> model) {
        for (File f : myWhatToDo.getModels()) {
            if (f.getPath().endsWith(MPSExtentions.DOT_MODEL)) {
                processModelFile(model, f);
            }
        }
    }

    private void processModelFile(Set<SModel> models, File f) {
        final IFile ifile = FileSystem.getInstance().getFileByPath(f.getAbsolutePath());
        //  try to find if model is loaded
        SModel model = SModelFileTracker.getInstance().findModel(ifile);
        if (model != null) {
            models.add(model);
            info("Found model " + model);
            return;
        }
        //  if model is not loaded, read it
        try {
            SModelHeader dr = ModelPersistence.loadDescriptor(new FileDataSource(ifile));
            SModelReference modelReference = dr.getModelReference();
            if (modelReference == null) {
                String modelName = FileUtil.getNameWithoutExtension(ifile.getName());
                modelReference = PersistenceFacade.getInstance().createModelReference(modelName);
            }
            info("Read model " + modelReference);
            SModel existingDescr = SModelRepository.getInstance().getModelDescriptor(modelReference);
            if (existingDescr == null) {
                error("Module for " + ifile.getPath() + " was not found. Use \"library\" tag to load required modules.");
            } else {
                models.add(existingDescr);
            }
        } catch (ModelReadException e) {
            error("Error reading model", e);
        }
    }

    public void info(String text) {
        myLogger.info(text);
    }

    public void warning(String text) {
        myLogger.warn(text);
        myWarnings.add(text);
    }

    public void debug(String text) {
        myLogger.debug(text);
    }

    public void error(String text) {
        myLogger.error(text);
        myErrors.add(text);
    }

    public void error(String text, Throwable e) {
        StringBuffer sb = extractStackTrace(e);
        myLogger.error(text, e);
        myErrors.add(text + "\n" + sb.toString());
    }

    protected class MyEnvironment extends MpsEnvironment {
        public MyEnvironment(EnvironmentConfig config) {
            super(config);
        }

        @Nullable
        @Override
        protected ClassLoader rootClassLoader() {
            return myClassLoader;
        }
    }

    private class MyMessageHandler implements IMessageHandler {
        /*package*/ MyMessageHandler() {
        }

        @Override
        public void handle(IMessage msg) {
            switch (msg.getKind()) {
                case ERROR:
                    if (msg.getException() != null) {
                        error(extractStackTrace(msg.getException()).toString());
                    } else {
                        error(msg.getText());
                    }
                    break;
                case WARNING:
                    warning(msg.getText());
                    break;
                case INFORMATION:
                    info(msg.getText());
                    break;
                default:
            }
        }
    }

    protected class ObjectsToProcess {
        private final Set<Project> myProjects = new LinkedHashSet<Project>();
        private final Set<SModule> myModules = new LinkedHashSet<SModule>();
        private final Set<SModel> myModels = new LinkedHashSet<SModel>();

        public ObjectsToProcess(Set<? extends Project> mpsProjects, Set<SModule> modules, Set<SModel> models) {
            myProjects.addAll(mpsProjects);
            myModules.addAll(modules);
            myModels.addAll(models);
        }

        public Set<Project> getProjects() {
            return myProjects;
        }

        public Set<SModule> getModules() {
            return myModules;
        }

        public Set<SModel> getModels() {
            return myModels;
        }

        public boolean hasAnythingToGenerate() {
            return !(myModels.isEmpty()) || !(myProjects.isEmpty()) || !(myModules.isEmpty());
        }
    }
}
