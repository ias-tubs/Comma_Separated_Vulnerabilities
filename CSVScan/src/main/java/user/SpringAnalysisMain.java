package user;

import analysis.CreateEdge;
import analysis.IFDSTaintAnalysisProblem;
import analysis.data.DFF;
import boomerang.scene.jimple.BoomerangPretransformer;
import heros.solver.FlowFunctionDotExport;
import org.json.JSONArray;
import org.json.JSONObject;
import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.toolkits.ide.JimpleIFDSSolver;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.tagkit.SignatureTag;
import soot.util.Chain;
import util.ItemPrinterShort;
import util.SolverUtil;
import util.SootUtil;

import java.io.*;
import java.util.*;

import static util.SolverUtil.getResultAtUnit;
import static util.SootUtil.isPrimitiveDataTypeOrStringOrWrapper;
import static util.SootUtil.isPrimitiveDataTypeOrWrapper;

public class SpringAnalysisMain {

    protected static JimpleIFDSSolver<?, ?> solver = null;
    private CreateEdge createEdge;
    public static JimpleBasedInterproceduralCFG icfg = null;
    List<SootMethodRef> sources = new ArrayList<>();
    List<SootMethodRef> sinks = new ArrayList<>();
    private final List<SootMethodRef> argActivatedSources = new ArrayList<>();
    private final List<SootMethodRef> baseActivatedSources = new ArrayList<>();
    private final List<SootMethodRef> invokerTaintedByArg = new ArrayList<>();

    private final List<SootMethodRef> invokerAndOptionalAssignTaintedByArg = new ArrayList<>();
    Set<SootClass> taintedJPARepos = new HashSet<>();
    Set<SootClass> classesWithJPARepo = new HashSet<>();
    Map<SootClass, SootClass> JPArepoToHandledType = new HashMap<>();
    Map<SootMethodRef, SootClass> JPAMethodRefToJPARepo = new HashMap<>();
    private static String appName = "";

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Please specify the name of the app first, sourceDir second and the dependencyDir third.");
            System.out.println("The conifgPath to a xml file with beans is optional");
            System.exit(-1);
        }
        appName = args[0];
        String sourceDir = args[1];
        String dependencyDir = args[2];
        String configPath = "";
        if (args.length == 4) {
            configPath = args[3];
        }

        System.out.println("Source directory: " + sourceDir);
        System.out.println("Dependency directory: " + dependencyDir);
        System.out.println("---------------------------------------------");

        SpringAnalysisMain sam = new SpringAnalysisMain();
        sam.executeStaticAnalysis(sourceDir, dependencyDir, configPath);
        System.out.println("Determining flows into JPA Storage");
        sam.determineTaintedJPASources();
        System.out.println("---------------------------------------------");
        sam.writeClassesOfTaintedJPARepoToDisk();
        System.out.println("Wrote flows into JPA Storage to Disk");
        System.out.println("---------------------------------------------");
        System.out.println("Searching for Flows to Sinks");
        sam.iterateSinks(sam.sinks, false);
        Chain<SootClass> loadedClasses = Scene.v().getClasses();
        Set<SootMethod> whitelistedMethods = new HashSet<>();
        for (SootClass loadedClass : loadedClasses) {
            if (loadedClass.getName().contains("EmployeeService") || loadedClass.getName().contains("TransactionController")) {
                List<SootMethod> wlMethods = loadedClass.getMethods();
                for (SootMethod sm : wlMethods) {
                    if (!sm.isStaticInitializer() && (sm.getName().contains("exportEmployees") || sm.getName().contains("exportColumnsValue") || sm.getName().contains("transfer")|| sm.getName().contains("notPassing"))) {
                        whitelistedMethods.add(sm);
                    }
                }
            }
        }
        FlowFunctionDotExport flowFunctionDotExport = new FlowFunctionDotExport<>(solver, new ItemPrinterShort<>(), whitelistedMethods);
        flowFunctionDotExport.dumpDotFile("out.txt");
    }

    /**
     * Getting Soot ready by loading the necessary classes and preparing the classes by executing Jasmine
     *
     * @param sourceDirectory     The directory containing the source files to be analyzed
     * @param dependencyDirectory The directory containing the source files of the used libraries
     */
    private void setupSoot(String sourceDirectory, String dependencyDirectory, String configPath) {
        // explicitly resetting all Soot settings, so we don't have stuff in the cache
        G.reset();
        Scene.v().reset();
        Scene.v().releaseActiveHierarchy();
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseFastHierarchy();
        Scene.v().releaseClientAccessibilityOracle();
        Scene.v().releaseReachableMethods();
        Scene.v().releaseSideEffectAnalysis();

        Options.v().set_num_threads(1);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(false);
        Options.v().set_soot_classpath(getSootClassPath(dependencyDirectory));
        Options.v().setPhaseOption("cg", "safe-newinstance:true");

        // we want spark and not cha
        Options.v().setPhaseOption("cg.cha", "enabled:false");

        // cg will be huge if that is not on
        Options.v().setPhaseOption("cg", "all-reachable:true");

        // Enable SPARK call-graph construction
        Options.v().setPhaseOption("cg.spark", "enabled:true");
        Options.v().setPhaseOption("cg.spark", "verbose:false");
        Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");

        // annotation processor
        Options.v().setPhaseOption("jap", "enabled:true");
        Options.v().set_allow_cg_errors(true);

        // more annotation settings
        Options.v().set_write_local_annotations(true);

        // good for large graphs
        Options.v().set_weak_map_structures(true);

        // needs to be on for full body and also makes graph construction more stable
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_derive_java_version(true);
        Options.v().set_full_resolver(true);

        // good for better understanding of flow propagation
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.ls", "enabled:true");

        // THIS NEEDS TO BE HERE IT WILL NOT SHOW NAMES OTHERWISE
        Options.v().setPhaseOption("jb.sils", "enabled:false");

        // apparently i need to explicitly add classes so that the resolving works properly for std lib
        Scene.v().addBasicClass("java.lang.StringBuilder");
        Scene.v().addBasicClass("org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody");
        Scene.v().addBasicClass("java.util.Optional");
        Scene.v().addBasicClass("java.util.ArrayList");
	    Scene.v().addBasicClass("com.codahale.metrics.Gauge",SootClass.HIERARCHY);
        Options.v().set_process_dir(Collections.singletonList(sourceDirectory));
        Options.v().set_exclude(excludedPackages());
        Options.v().set_no_bodies_for_excluded(true);
        Scene.v().loadNecessaryClasses();

        System.out.println("Loaded Soot Classes");
        System.out.println("---------------------------------------------");


        // start Jasmine
        CreateEdge createEdge = new CreateEdge();
        createEdge.initCallGraph(configPath);
        System.out.println("Prepared Classes with Jasmine");
        System.out.println("---------------------------------------------");
        this.createEdge = createEdge;
    }

    /**
     * Add class files of the supporting libraries to the classpath, as well as the Java library files
     *
     * @param dependencyDirectory The directory where the supporting libraries are located
     * @return The Soot classpath
     */
    private static String getSootClassPath(String dependencyDirectory) {
        // must be on classpath to load Java library files
        StringBuilder sootCp = new StringBuilder("VIRTUAL_FS_FOR_JDK");
        File file = new File(dependencyDirectory);
        File[] fs = file.listFiles();
        if (fs != null) {
            for (File f : Objects.requireNonNull(fs)) {
                if (!f.isDirectory())
                    sootCp.append(File.pathSeparator).append(dependencyDirectory).append(File.separator).append(f.getName());
            }
        }
        return sootCp.toString();
    }

    /**
     * Writes the class of a tainted JPA repository to disk, as this allows us to perform matching based on the datatype
     * if we assume that different repositories that work on the same datatype will access the same database.
     */
    private void writeClassesOfTaintedJPARepoToDisk() {
        JSONArray jsonArray = new JSONArray();
        if (this.classesWithJPARepo.isEmpty()) {
            return;
        }

        JSONObject[] jsonObjects = new JSONObject[this.classesWithJPARepo.size()];
        for (int i = 0; i < jsonObjects.length; i++) {
            jsonObjects[i] = new JSONObject();
        }
        List<SootClass> classesWithTaintedJPA = this.classesWithJPARepo.stream().toList();

        // convert Soot method ref to JSON
        for (int i = 0; i < this.classesWithJPARepo.size(); i++) {
            JSONObject jo = jsonObjects[i];
            jo.put("declaring_class", classesWithTaintedJPA.get(i).toString());
            jsonArray.put(jo);
        }
        try (FileWriter jsonOut = new FileWriter("/intermediate_results/classes_with_tainted_repo.json")) {
            jsonOut.write(jsonArray.toString(4));
            jsonOut.flush();
        } catch (IOException e) {
            System.out.println("There has been an exception while trying to write the source JSON file.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a file that contains SootMethodRefs in JSON format and converts them back into SootMethodRefs
     *
     * @param filename The name of the JSON file
     * @return A List of SootMethodRefs, built from the elements of the file
     */
    private static List<SootMethodRef> loadSootMethodRefsFromJSON(String filename) {
        // get JSON string
        String jsonString = readFileAsJSONString(filename);

        // convert the JSON String to an array
        JSONArray jsonArray = new JSONArray(jsonString);
        List<SootMethodRef> methodRefsFromDisk = new ArrayList<>();

        // iterate over all elements in the JSON array
        for (Object jsonObject : jsonArray) {
            JSONObject jo = (JSONObject) jsonObject;

            // the class that is declaring the method
            String declaringClass = jo.get("declaring_class").toString();
            SootClass declaringClassSoot = Scene.v().getSootClass(declaringClass);

            // the name of the method
            String methodName = jo.getString("method_name");

            // true if the method is static
            boolean isStatic = jo.getBoolean("is_static");

            // the return type of the method
            String returnTypeString = jo.getString("return_type");
            Type returnTypeSoot;
            // in theory a source should never return void
            if (returnTypeString.equals("java.lang.Void")) {
                returnTypeSoot = VoidType.v();
            } else {
                returnTypeSoot = RefType.v(returnTypeString);
            }

            // parse the types of the arguments of the method
            String parameterTypesString = jo.getString("parameter_types");

            // highly specific to the way we generate the JSON file in the first place
            parameterTypesString = parameterTypesString.replace("[", "");
            parameterTypesString = parameterTypesString.replace("]", "");

            // convert the text representation of the arguments to Soot Types
            List<Type> parameterTypes = new ArrayList<>();
            for (String parameterType : parameterTypesString.split(",")) {
                if (!parameterType.isEmpty()) {
                    //specify arrays with ARRAY:dimension:type
                    if (parameterType.startsWith("ARRAY")) {
                        String[] arrayType = parameterType.split(":");
                        parameterTypes.add(ArrayType.v(RefType.v(arrayType[2]), Integer.parseInt(arrayType[1])));
                    } else {
                        parameterTypes.add(RefType.v(parameterType));
                    }
                }
            }

            // assemble the SootMethodRef of the method
            methodRefsFromDisk.add(new SootMethodRefImpl(declaringClassSoot, methodName, parameterTypes, returnTypeSoot, isStatic));
        }
        return methodRefsFromDisk;
    }

    /**
     * Reads the file as a JSON String
     *
     * @param filename The file containing the JSON data
     * @return A JSON string that represents the content of the file
     */
    private static String readFileAsJSONString(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String text = sb.toString();
            br.close();
            return text;
        } catch (IOException e) {
            // IMPORTANT return valid JSON even if empty, because on the first the JSON will always be empty
            System.out.println("Didnt fina a json file, assuming empty ...");
            return "[]";
        }
    }

    /**
     * Find tainted flows into JPA repositories in order to determine potentially tainted flows that originate from the
     * JPA repositories
     */
    private void determineTaintedJPASources() {
        // all other repositories need to implement/extend this class
        SootClass sc = Scene.v().getSootClass("org.springframework.data.repository.Repository");
        if (sc.isPhantom()) {
            return;
        }
        List<SootClass> jpaRepositories = new ArrayList<>();
        if (sc.isInterface()) {
            // determine all repositories in Scene that extend the main repository class
            jpaRepositories = Scene.v().getActiveHierarchy().getSubinterfacesOf(sc);
        } else {
            jpaRepositories = Scene.v().getActiveHierarchy().getSubclassesOf(sc);
        }

        // iterate over all repositories and evaluate if there is a flow reaching a saving method
        for (SootClass jpaRepo : jpaRepositories) {
            List<SootMethod> jpaRepoMethods = jpaRepo.getMethods();
            for (SootMethod repoMethod : jpaRepoMethods) {

                // check all methods that save something
                if (repoMethod.getName().contains("save")) {

                    // get all Units where the saving method is called
                    Collection<Unit> saveCallers = icfg.getCallersOf(repoMethod);

                    // check for any taint that reaches a saving method
                    for (Unit saveCaller : saveCallers) {

                        // get taints that are alive at the Unit that calls the saving method
                        Set<DFF> taintsAtJPA = getResultAtUnit(solver, saveCaller);
                        List<Value> callArgs = ((Stmt) saveCaller).getInvokeExpr().getArgs();

                        // check if one of the taints is used as a parameter for the saving method
                        for (Value callArg : callArgs) {
                            if (taintsAtJPA.stream().anyMatch(taint -> taint.getValue().equals(callArg))) {
                                // LIMITATION less messy output, turn on again for full reporting
                                this.iterateSinks(List.of(repoMethod.makeRef()), true);
                                this.taintedJPARepos.add(jpaRepo);
                                SootClass argClass = Scene.v().getSootClass(callArg.getType().toString());
                                this.classesWithJPARepo.add(argClass);
                                // the taints are handled redundantly if we dont break
                                break;
                            }else{
                                boolean dangerousTaint = false;
                                for(DFF taint: taintsAtJPA.stream().toList()){
                                    if(taint.getBase().equivTo(callArg)){
                                        if(taint.getValue() instanceof JInstanceFieldRef){
                                            if(!isPrimitiveDataTypeOrWrapper(((JInstanceFieldRef) taint.getValue()).getField().getType())){
                                                dangerousTaint = true;
                                            }
                                        }else {
                                            dangerousTaint = true;
                                        }

                                    }
                                }
                                if(dangerousTaint){
                                    this.iterateSinks(List.of(repoMethod.makeRef()), true);
                                    SootClass argClass = Scene.v().getSootClass(callArg.getType().toString());
                                    this.classesWithJPARepo.add(argClass);
                                    // the taints are handled redundantly if we dont break
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // Jasmine seems to create synthetic methods for JPA methods
            if (!jpaRepo.getShortName().endsWith("Impl")) {
                try {
                    // same as above
                    SootClass syntheticJPAClass = Scene.v().getSootClass("synthetic.method." + jpaRepo.getShortName() + "Impl");
                    if (syntheticJPAClass.isPhantom()) {
                        continue;
                    }
                    for (SootMethod syntheticJPAMethod : syntheticJPAClass.getMethods()) {
                        if (syntheticJPAMethod.getName().contains("save")) {
                            Collection<Unit> saveCallers = icfg.getCallersOf(syntheticJPAMethod);
                            for (Unit saveCaller : saveCallers) {
                                Set<DFF> taintsAtJPA = getResultAtUnit(solver, saveCaller);
                                List<Value> callArgs = ((Stmt) saveCaller).getInvokeExpr().getArgs();
                                // check if one of the taints is used as a parameter for the saving method
                                for (Value callArg : callArgs) {
                                    if (taintsAtJPA.stream().anyMatch(taint -> taint.getValue().equivTo(callArg))) {
                                        this.iterateSinks(List.of(syntheticJPAMethod.makeRef()), true);
                                        SootClass argClass = Scene.v().getSootClass(callArg.getType().toString());
                                        this.classesWithJPARepo.add(argClass);
                                        // the taints are handled redundantly if we dont break
                                        break;
                                    }else{
                                        boolean dangerousTaint = false;
                                        for(DFF taint: taintsAtJPA.stream().toList()){
                                            if(taint.getBase().equivTo(callArg)){
                                                if(taint.getValue() instanceof JInstanceFieldRef){
                                                    if(!isPrimitiveDataTypeOrWrapper(((JInstanceFieldRef) taint.getValue()).getField().getType())){
                                                        dangerousTaint = true;
                                                    }
                                                }else {
                                                    dangerousTaint = true;
                                                }

                                            }
                                        }
                                        if(dangerousTaint){
                                            this.iterateSinks(List.of(syntheticJPAMethod.makeRef()), true);
                                            SootClass argClass = Scene.v().getSootClass(callArg.getType().toString());
                                            this.classesWithJPARepo.add(argClass);
                                            // the taints are handled redundantly if we dont break
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (RuntimeException classNotFound) {
                    continue;
                }
            }
        }
        if (!this.taintedJPARepos.isEmpty()) {
            System.out.println("Found flows into Repository");
        }

    }

    /**
     * Packages that should not be considered on a body resolution level by Soot
     *
     * @return A List of package names that should have no bodies when Soot builds the callgraph
     */
    private static List<String> excludedPackages() {
        List<String> excludedPackages = new ArrayList<>();
        excludedPackages.add("javax.swing.*");
        excludedPackages.add("org.apache.fop.fonts.CodePointMapping");
        excludedPackages.add("java.awt.*");
        excludedPackages.add("sun.awt.*");
        excludedPackages.add("com.sun.java.swing.*");
        excludedPackages.add("reactor.*");
        excludedPackages.add("net.sf.cglib.*");
        excludedPackages.add("org.springframework.*");
        excludedPackages.add("org.apache.poi.*");
        excludedPackages.add("com.mysql.*");
        excludedPackages.add("org.ehcache.impl.internal.*");
        excludedPackages.add("ch.qos.*");
        excludedPackages.add("org.apache.*");
        excludedPackages.add("org.eclipse.*");
        excludedPackages.add("java.util.*");
        excludedPackages.add("java.lang.String");
        return excludedPackages;
    }

    /**
     * Wrapper that handles the setup of Soot and starts the execution of all needed Soot transformers
     *
     * @param sourceDirectory     The directory containing the source files to be analyzed
     * @param dependencyDirectory The directory containing the source files of the used libraries
     */
    protected void executeStaticAnalysis(String sourceDirectory, String dependencyDirectory, String configPath) {
        setupSoot(sourceDirectory, dependencyDirectory, configPath);
        executeSootTransformers();
        if (solver == null) {
            throw new NullPointerException("Something went wrong solving the IFDS problem!");
        }
    }

    private void executeSootTransformers() {
        //Apply all necessary packs of soot. This will execute the respective Transformer
        PackManager.v().getPack("cg").apply();
        System.out.println("Applied the Callgraph Pack");
        System.out.println("---------------------------------------------");

        // Must have for Boomerang
        BoomerangPretransformer.v().reset();
        BoomerangPretransformer.v().apply();
        System.out.println("Applied Boomerang Pretransfromer");
        System.out.println("---------------------------------------------");

        System.out.println("Starting Analysis");
        Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
        PackManager.v().getPack("wjtp").add(transform);
        PackManager.v().getPack("wjtp").apply();
        System.out.println("Finished Analysis");
        System.out.println("---------------------------------------------");
    }

    private List<SootMethodRef> generateSourcesFromJasmine() {
        List<SootMethodRef> returnList = new ArrayList<>();
        for (SootMethod sootMethod : this.createEdge.getJimpleUtils().getterMethods) {
            SootClass sootClass = sootMethod.getDeclaringClass();
            String methodName = sootMethod.getName();
            Type returnType = sootMethod.getReturnType();
            List<Type> parameterTypes = sootMethod.getParameterTypes();
            boolean isStatic = sootMethod.isStatic();
            returnList.add(new SootMethodRefImpl(sootClass, methodName, parameterTypes, returnType, isStatic));
        }
        return returnList;
    }

    private Set<SootMethodRef> getSourcesBasedOnType() {
        // get the json file with identified classes that represent entities with tainted JPA repositories
        String jsonString = readFileAsJSONString("/intermediate_results/classes_with_tainted_repo.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        Set<SootClass> jpaTaintedClasses = new HashSet<>();
        for (Object jsonObject : jsonArray) {
            JSONObject jo = (JSONObject) jsonObject;

            // the class that is declaring the method
            String declaringClass = jo.get("declaring_class").toString();
            SootClass sootClass = Scene.v().getSootClass(declaringClass);
            if (!sootClass.isPhantom()) {
                jpaTaintedClasses.add(sootClass);
            }
        }

        SootClass sc = Scene.v().getSootClass("org.springframework.data.repository.Repository");
        if (sc.isPhantom()) {
            return new HashSet<>();
        }
        List<SootClass> jpaRepositories = new ArrayList<>();
        if (sc.isInterface()) {
            // determine all repositories in Scene that extend the main repository class
            jpaRepositories = Scene.v().getActiveHierarchy().getSubinterfacesOf(sc);
        } else {
            jpaRepositories = Scene.v().getActiveHierarchy().getSubclassesOf(sc);
        }

        Set<SootMethodRef> taintedMethodRefs = new HashSet<>();
        // iterate over all repositories and evaluate if there is a repository that has the same type
        for (SootClass jpaRepo : jpaRepositories) {
            // match class type against signature
            SignatureTag signatureTag = (SignatureTag) jpaRepo.getTag("SignatureTag");
            SootClass JPAhandledClass = typeMatchesJPASignature(signatureTag.getSignature(), jpaTaintedClasses.stream().toList());
            if (JPAhandledClass != null) {
                for (SootMethod repoMethod : jpaRepo.getMethods()) {
                    // assume that every method that returns something is dangerous
                    if (repoMethod.getReturnType() != VoidType.v() && !repoMethod.getName().contains("save")) {
                        taintedMethodRefs.add(new SootMethodRefImpl(repoMethod.getDeclaringClass(), repoMethod.getName(), repoMethod.getParameterTypes(), repoMethod.getReturnType(), repoMethod.isStatic()));
                        this.JPArepoToHandledType.putIfAbsent(jpaRepo, JPAhandledClass);
                        this.JPAMethodRefToJPARepo.putIfAbsent(repoMethod.makeRef(), jpaRepo);
                    }
                }
                if (!jpaRepo.getShortName().endsWith("Impl")) {
                    SootClass syntheticJPAClass = Scene.v().getSootClass("synthetic.method." + jpaRepo.getShortName() + "Impl");
                    if (!syntheticJPAClass.isPhantom()) {
                        for (SootMethod repoMethod : syntheticJPAClass.getMethods()) {
                            if (repoMethod.getReturnType() != VoidType.v() && !repoMethod.getName().contains("save")) {
                                taintedMethodRefs.add(new SootMethodRefImpl(syntheticJPAClass, repoMethod.getName(), repoMethod.getParameterTypes(), repoMethod.getReturnType(), repoMethod.isStatic()));
                                this.JPArepoToHandledType.putIfAbsent(jpaRepo, JPAhandledClass);
                                this.JPAMethodRefToJPARepo.putIfAbsent(repoMethod.makeRef(), jpaRepo);
                            }
                        }
                    }
                }
            }
        }
        return taintedMethodRefs;
    }

    private SootClass typeMatchesJPASignature(String signature, List<SootClass> jpaTaintedClasses) {
        for (SootClass jpaTaintedClass : jpaTaintedClasses) {
            String jpaTaintedClassEscaped = jpaTaintedClass.toString().replaceAll("\\.", "/");
            if (signature.contains("<L" + jpaTaintedClassEscaped)) {
                return jpaTaintedClass;
            }
        }
        return null;
    }

    protected Transformer createAnalysisTransformer() {
        List<SootMethodRef> sources = new ArrayList<>();
        List<SootMethodRef> sinks = new ArrayList<>();

        this.sources = sources;
        this.sources.addAll(this.generateSourcesFromJasmine());
        this.sources.addAll(getSourcesBasedOnType());

        this.argActivatedSources.addAll(sparkMissingEdgesMethodsArgActivated());
        this.argActivatedSources.addAll(observedArgActivatedSources());

        this.baseActivatedSources.addAll(sparkMissingEdgesMethodsBaseActivated());
        this.baseActivatedSources.addAll(observedBaseActivatedSources());

        this.invokerAndOptionalAssignTaintedByArg.addAll(invokerAndAssignedToTaintedByArg());
        //remove duplicates
        this.sources = new ArrayList<>(new HashSet<>(this.sources));
        this.invokerTaintedByArg.addAll(invokerTaintedByArg());
        this.sinks = sinks;
        this.sinks.addAll(getSpringSinks());
        System.out.println("Loaded Sources and Sinks\n---------------------------------------------");
        return new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
                IFDSTaintAnalysisProblem problem = new IFDSTaintAnalysisProblem(icfg, sources, argActivatedSources, baseActivatedSources, invokerTaintedByArg, invokerAndOptionalAssignTaintedByArg);
                System.out.println("Created ICFG\n---------------------------------------------");
                @SuppressWarnings({"rawtypes", "unchecked"})
                JimpleIFDSSolver<?, ?> solver = new JimpleIFDSSolver<>(problem) {

                };
                System.out.println("Starting Solver");
                solver.solve();
                System.out.println("Solver is Finished\n---------------------------------------------");
                SpringAnalysisMain.solver = solver;
                SpringAnalysisMain.icfg = icfg;
            }
        };
    }

    public static List<SootMethodRef> getSpringSinks() {
        //IMPORTANT if you use new SootClass here, the class will automatically be published to the scene and override the correct information
        List<SootMethodRef> sinks = new ArrayList<>(loadSootMethodRefsFromJSON("sinks.json"));
        return sinks;
    }

    public void iterateSinks(List<SootMethodRef> sinks, boolean isJPASink) {
        for (SootMethodRef sink : sinks) {
            SootMethod sinkMethodImpl;
            try {
                // retrieve the actual method from the scene
                sinkMethodImpl = SootUtil.retrieveSootMethod(sink.getDeclaringClass().getName(), sink.getName(), sink.getParameterTypes(), sink.getReturnType());
            } catch (Exception e) {
                continue;
            }
            if (sinkMethodImpl == null) {
                continue;
            }
            // retrieve the units that make calls to the method
            //LIMITATION it seems to not work for interfaceinvoke, needs to be the actual class
            List<Unit> callersOfSink = (List<Unit>) icfg.getCallersOf(sinkMethodImpl);

            for (Unit callerOfSink : callersOfSink) {

                // check if there are taints present at the call to the sink
                Set<DFF> taintsAtSink = SolverUtil.getResultAtUnit(solver, callerOfSink);
                if (!taintsAtSink.isEmpty()) {
                    Stmt s = (Stmt) callerOfSink;
                    InvokeExpr ie = s.getInvokeExpr();
                    final List<Value> callArgs = ie.getArgs();

                    // save all taints that actually are used in the sink
                    List<DFF> qualifiedTaints = new ArrayList<>();

                    // check if a taint is going into the sink
                    for (DFF taint : taintsAtSink) {
                        for (Value callArg : callArgs) {
                            // dont consider Strings or primitive datatypes in repositories
                            if ((callArg.equivTo(taint.getValue())&&(!isPrimitiveDataTypeOrStringOrWrapper(taint.getValue().getType()) || !isJPASink))
                                || (taint.getBase() != null && callArg.equivTo(taint.getBase())&&((taint.getValue() instanceof JInstanceFieldRef &&!isPrimitiveDataTypeOrWrapper(((JInstanceFieldRef) taint.getValue()).getField().getType()))||!isJPASink))) {
                                if(!qualifiedTaints.contains(taint)){
                                    qualifiedTaints.add(taint);
                                }

                            }
                        }
                    }
                    // generate the flow backwards
                    if (!qualifiedTaints.isEmpty()) {
                        for (DFF taint : qualifiedTaints) {
                            //List<SootMethod> toBePrintedMethods = new ArrayList<>();
                            StringBuilder reportBuilder = new StringBuilder();
                            findSourceFromDFF(taint, reportBuilder);
                            if (!reportBuilder.isEmpty()) {
                                if(!(taint.getGeneratedAt() != null && icfg.getMethodOf(taint.getGeneratedAt()).equals(icfg.getMethodOf(callerOfSink)))){
                                    SootMethodRef taintCurrrentMethod = icfg.getMethodOf(callerOfSink).makeRef();
                                    reportBuilder.append("\n---------------------------------------------------------\n");
                                    reportBuilder.append("METHOD: ").append(taintCurrrentMethod.toString());
                                    reportBuilder.append("\n---------------------------------------------------------\n");
                                }
                                reportBuilder.append("->");
                                reportBuilder.append(callerOfSink);
                                if (isJPASink) {
                                    if(taint.getBase() != null){
                                        System.out.println("\n\n\nPersist of Object Type: " + taint.getBase().getType().toString());

                                    }else{
                                        System.out.println("\n\n\nPersist of Object Type: " + taint.getValue().getType().toString());

                                    }
                                    System.out.println(reportBuilder);
                                } else {
                                    System.out.println("\n\n\nFound flow into Sink with Name: " + sinkMethodImpl.getName());
                                    System.out.println(reportBuilder);
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public static List<SootMethodRef> observedArgActivatedSources() {
        List<SootMethodRef> sources = new ArrayList<>();
        return sources;
    }

    public static List<SootMethodRef> observedBaseActivatedSources() {
        List<SootMethodRef> sources = new ArrayList<>();
        SootClass springbootPage = Scene.v().getSootClass("org.springframework.data.domain.Page");
        SootMethodRef springbootIterator = new SootMethodRefImpl(springbootPage, "iterator", Collections.emptyList(), RefType.v("java.util.Iterator"), false);
        sources.add(springbootIterator);
        return sources;
    }

    public static List<SootMethodRef> sparkMissingEdgesMethodsBaseActivated() {
        List<SootMethodRef> sources = new ArrayList<>();
        // ---- classes ------
        SootClass javaOptional = Scene.v().getSootClass("java.util.Optional");
        SootClass javaCollection = Scene.v().getSootClass("java.util.Collection"); // interface
        SootClass javaIterator = Scene.v().getSootClass("java.util.Iterator"); // interface
        SootClass javaList = Scene.v().getSootClass("java.util.List"); // interface
        SootClass javaIterable = Scene.v().getSootClass("java.lang.Iterable"); // interface
        SootClass javaMap = Scene.v().getSootClass("java.util.Map"); // interface
        SootClass stringBuilder = Scene.v().getSootClass("java.lang.StringBuilder");

        SootMethodRef stringBuilderToString = new SootMethodRefImpl(stringBuilder, "toString", Collections.emptyList(), RefType.v("java.lang.String"), false);
        SootMethodRef mapGet = new SootMethodRefImpl(javaMap, "get", List.of(RefType.v("java.lang.Object")), RefType.v("java.lang.Object"), false);

        SootMethodRef orElseThrow = new SootMethodRefImpl(javaOptional, "orElseThrow", List.of(RefType.v("java.util.function.Supplier")), RefType.v("java.lang.Object"), false);
        SootMethodRef optionalGet = new SootMethodRefImpl(javaOptional, "get", Collections.emptyList(), RefType.v("java.lang.Object"), false);

        SootMethodRef iterableIterator = new SootMethodRefImpl(javaIterable, "iterator", Collections.emptyList(), RefType.v("java.util.Iterator"), false);

        SootMethodRef collectionIterator = new SootMethodRefImpl(javaCollection, "iterator", Collections.emptyList(), RefType.v("java.util.Iterator"), false);
        SootMethodRef iteratorNext = new SootMethodRefImpl(javaIterator, "next", Collections.emptyList(), RefType.v("java.lang.Object"), false);

        SootMethodRef listIterator = new SootMethodRefImpl(javaList, "iterator", Collections.emptyList(), RefType.v("java.util.Iterator"), false);

        sources.add(stringBuilderToString); // modeling does not work, use this for now
        sources.add(listIterator);
        sources.add(iterableIterator);
        sources.add(iteratorNext);
        sources.add(collectionIterator);
        sources.add(orElseThrow);
        sources.add(optionalGet);
        sources.add(mapGet);
        return sources;
    }

    public static List<SootMethodRef> sparkMissingEdgesMethodsArgActivated() {
        List<SootMethodRef> sources = new ArrayList<>();
        SootClass javaArrayList = Scene.v().getSootClass("java.util.ArrayList"); // problems with references, Boomerang is called correctly but didnt find alias
        SootClass javaList = Scene.v().getSootClass("java.util.List"); //interface
        SootClass javaHashMap = Scene.v().getSootClass("java.util.HashMap");

        SootMethodRef addAllList = new SootMethodRefImpl(javaList, "addAll", List.of(RefType.v("java.util.Collection")), BooleanType.v(), false);
        sources.add(addAllList);

        SootMethodRef addAllArrayList = new SootMethodRefImpl(javaArrayList, "addAll", List.of(RefType.v("java.util.Collection")), BooleanType.v(), false);
        sources.add(addAllArrayList);

        SootMethodRef addArrayList = new SootMethodRefImpl(javaArrayList, "add", List.of(RefType.v("java.lang.Object")), BooleanType.v(), false);
        sources.add(addArrayList);

        SootMethodRef addList = new SootMethodRefImpl(javaList, "add", List.of(RefType.v("java.lang.Object")), BooleanType.v(), false);
        sources.add(addList);

        SootMethodRef hashMapPut = new SootMethodRefImpl(javaHashMap, "put", List.of(RefType.v("java.lang.Object"), RefType.v("java.lang.Object")), RefType.v("java.lang.Object"), false);
        sources.add(hashMapPut);

        return sources;
    }

    public static List<SootMethodRef> invokerTaintedByArg() {
        List<SootMethodRef> sources = new ArrayList<>();
        SootClass jsonObject = Scene.v().getSootClass("org.json.JSONObject");
        SootClass javaArrayList = Scene.v().getSootClass("java.util.ArrayList");
        SootClass javaList = Scene.v().getSootClass("java.util.List"); // interface
        SootClass javaHashMap = Scene.v().getSootClass("java.util.HashMap");

        SootClass jsonArray = Scene.v().getSootClass("org.json.JSONArray");

        SootMethodRef jsonArrayPut = new SootMethodRefImpl(jsonArray, "put", List.of(RefType.v("java.lang.Object")), RefType.v("org.json.JSONArray"), false);
        sources.add(jsonArrayPut);
        SootMethodRef addAllList = new SootMethodRefImpl(javaList, "addAll", List.of(RefType.v("java.util.Collection")), BooleanType.v(), false);
        sources.add(addAllList);
        SootMethodRef jsonObjectInit = new SootMethodRefImpl(jsonObject, "<init>", List.of(RefType.v("java.lang.Object")), VoidType.v(), false);
        sources.add(jsonObjectInit);
        SootMethodRef addAllArrayList = new SootMethodRefImpl(javaArrayList, "addAll", List.of(RefType.v("java.util.Collection")), BooleanType.v(), false);
        sources.add(addAllArrayList);

        SootMethodRef addArrayList = new SootMethodRefImpl(javaArrayList, "add", List.of(RefType.v("java.lang.Object")), BooleanType.v(), false);
        sources.add(addArrayList);

        SootMethodRef addList = new SootMethodRefImpl(javaList, "add", List.of(RefType.v("java.lang.Object")), BooleanType.v(), false);
        sources.add(addList);

        SootMethodRef hashMapPut = new SootMethodRefImpl(javaHashMap, "put", List.of(RefType.v("java.lang.Object"), RefType.v("java.lang.Object")), RefType.v("java.lang.Object"), false);
        sources.add(hashMapPut);

        return sources;
    }

    public static List<SootMethodRef> invokerAndAssignedToTaintedByArg() {
        List<SootMethodRef> sources = new ArrayList<>();
        SootClass stringBuilder = Scene.v().getSootClass("java.lang.StringBuilder");
        SootMethodRef stringBuilderAppend = new SootMethodRefImpl(stringBuilder, "append", List.of(RefType.v("java.lang.String")), RefType.v("java.lang.StringBuilder"), false);
        sources.add(stringBuilderAppend);
        return sources;
    }

    public void findSourceFromDFF(DFF taintCurrent, StringBuilder reportBuilder) {
        if (taintCurrent == null) {
            return;
        }
        if(taintCurrent.getGeneratedAt() == null){
            System.out.println("INCOMPLETE FLOW CHAIN FOR TAINT: "+taintCurrent);
            reportBuilder.insert(0, "STMT: INCOMPLETE -> TAINT: " + taintCurrent);
        }else{
            reportBuilder.insert(0, "STMT: " + taintCurrent.getGeneratedAt() + " -> TAINT: " + taintCurrent);
        }
        if (taintCurrent.getPreviousDFF() != null) {
            reportBuilder.insert(0, " -> ");
            findSourceFromDFF(taintCurrent.getPreviousDFF(), taintCurrent, reportBuilder);
        }else {
            if(taintCurrent.getGeneratedAt() == null){
                reportBuilder.insert(0, " METHOD: COULD NOT DETERMINE METHOD FLOW INCOMPLETE ");
            } else {
                SootMethodRef taintCurrrentMethod = icfg.getMethodOf(taintCurrent.getGeneratedAt()).makeRef();
                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                reportBuilder.insert(0, "METHOD: " + taintCurrrentMethod.toString());
                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                if (taintCurrent.getGeneratedAt() instanceof DefinitionStmt definitionStmt) {
                    if (this.JPAMethodRefToJPARepo.containsKey(definitionStmt.getInvokeExpr().getMethodRef())) {
                        SootMethodRef jpaMethodRef = definitionStmt.getInvokeExpr().getMethodRef();
                        SootClass jpaRepoClass = this.JPAMethodRefToJPARepo.get(jpaMethodRef);
                        reportBuilder.insert(0, "The Repository handles Objects of Type: " + this.JPArepoToHandledType.get(jpaRepoClass).toString() + "\n");
                        reportBuilder.insert(0, "Taint loaded from Repository: " + jpaRepoClass.toString() + "\n");
                    }
                }
            }
        }
    }

    public void findSourceFromDFF(DFF taintCurrent, DFF taintPrev, StringBuilder reportBuilder) {
        if (taintCurrent == null) {
            return;
        }
        if(taintCurrent.getGeneratedAt() == null || taintPrev.getGeneratedAt() == null){
            System.out.println("INCOMPLETE FLOW CHAIN BECAUSE OF TAINTS " + taintCurrent +"and/or "+ taintPrev);
        }else{
            SootMethodRef taintCurrrentMethod = icfg.getMethodOf(taintCurrent.getGeneratedAt()).makeRef();
            SootMethodRef taintPrevMethod = icfg.getMethodOf(taintPrev.getGeneratedAt()).makeRef();
            if (!taintCurrrentMethod.equals(taintPrevMethod)) {

                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                reportBuilder.insert(0, "METHOD: " + taintPrevMethod.toString());
                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                reportBuilder.insert(0, " -> ");
            }
            reportBuilder.insert(0, "STMT: " + taintCurrent.getGeneratedAt() + " -> TAINT: " + taintCurrent);
        }

        if (taintCurrent.getPreviousDFF() != null) {
            reportBuilder.insert(0, " -> ");
            findSourceFromDFF(taintCurrent.getPreviousDFF(), taintCurrent, reportBuilder);
        } else {
            if(taintCurrent.getGeneratedAt() == null){
                reportBuilder.insert(0, " METHOD: COULD NOT DETERMINE METHOD FLOW INCOMPLETE ");
            } else {
                SootMethodRef taintCurrrentMethod = icfg.getMethodOf(taintCurrent.getGeneratedAt()).makeRef();
                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                reportBuilder.insert(0, "METHOD: " + taintCurrrentMethod.toString());
                reportBuilder.insert(0, "\n---------------------------------------------------------\n");
                if (taintCurrent.getGeneratedAt() instanceof DefinitionStmt definitionStmt) {
                    if (this.JPAMethodRefToJPARepo.containsKey(definitionStmt.getInvokeExpr().getMethodRef())) {
                        SootMethodRef jpaMethodRef = definitionStmt.getInvokeExpr().getMethodRef();
                        SootClass jpaRepoClass = this.JPAMethodRefToJPARepo.get(jpaMethodRef);
                        reportBuilder.insert(0, "The Repository handles Objects of Type: " + this.JPArepoToHandledType.get(jpaRepoClass).toString() + "\n");
                        reportBuilder.insert(0, "Taint loaded from Repository: " + jpaRepoClass.toString() + "\n");
                    }
                }
            }
        }
    }

}
