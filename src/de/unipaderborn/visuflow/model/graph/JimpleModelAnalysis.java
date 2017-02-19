package de.unipaderborn.visuflow.model.graph;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.unipaderborn.visuflow.model.VFClass;
import de.unipaderborn.visuflow.model.VFMethod;
import de.unipaderborn.visuflow.model.VFMethodEdge;
import de.unipaderborn.visuflow.model.VFUnit;
import soot.Body;
import soot.G;
import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.util.Chain;

public class JimpleModelAnalysis {

	private int methodcount = 0;
	private int edgeCount = 0;

	private Map<SootMethod, VFMethod> methodMap = new HashMap<>();

	private String[] sootString;

	public void setSootString(String[] s) {
		this.sootString = s;
	}

	public void createICFG(final ICFGStructure methodGraph, List<VFClass> vfClasses) {
		G.reset();
		Transform transform = new Transform("wjap.myTransform", new SceneTransformer() {
			@Override
			protected void internalTransform(String phase, Map<String, String> arg1) {
				createJimpleHierarchyWithCfgs(vfClasses);
				createICFG();
			}

			private void createICFG() {
				CallGraph cg = Scene.v().getCallGraph();
				SootMethod entryMethod = null;
				java.util.List<SootMethod> listMethod = Scene.v().getEntryPoints();
				Iterator<SootMethod> iterEntryMethod = listMethod.iterator();
				while (iterEntryMethod.hasNext()) {
					entryMethod = iterEntryMethod.next();
					if (entryMethod.isMain()) {
						methodcount++;
						VFMethod vfmethod;
						if (methodMap.containsKey(entryMethod)) {
							vfmethod = methodMap.get(entryMethod);
						} else {
							vfmethod = new VFMethod(entryMethod);
							methodMap.put(entryMethod, vfmethod);
						}
						methodGraph.listMethods.add(vfmethod);
						break;
					}
				}

				traverseMethods(entryMethod, cg);
			}

			private void createJimpleHierarchyWithCfgs(List<VFClass> vfClasses) {
				Chain<SootClass> classes = Scene.v().getClasses();
				for (SootClass sootClass : classes) {
					if (sootClass.isJavaLibraryClass() || sootClass.isLibraryClass()) {
						continue;
					}

					VFClass currentClass = new VFClass(sootClass);
					vfClasses.add(currentClass);

					for (SootMethod sootMethod : sootClass.getMethods()) {
						VFMethod currentMethod;
						if (methodMap.containsKey(sootMethod)) {
							currentMethod = methodMap.get(sootMethod);
						} else {
							currentMethod = new VFMethod(sootMethod);
							methodMap.put(sootMethod, currentMethod);
						}
						currentMethod.setVfClass(currentClass);
						Body body = sootMethod.retrieveActiveBody();
						for (Unit unit : body.getUnits()) {
							addFullyQualifiedName(unit, sootClass, sootMethod);
							VFUnit currentUnit = new VFUnit(unit);
							currentUnit.setVfMethod(currentMethod);
							currentMethod.getUnits().add(currentUnit);
						}
						currentMethod.setBody(body);
						currentMethod.setControlFlowGraph(new ControlFlowGraphGenerator().generateControlFlowGraph(currentMethod));
						currentClass.getMethods().add(currentMethod);
					}
				}
			}

			private void addFullyQualifiedName(Unit unit, SootClass sootClass, SootMethod sootMethod) {
				unit.addTag(new Tag() {
					@Override
					public byte[] getValue() throws AttributeValueException {
						String fqn = sootClass.getName() + "." + sootMethod.getName() + "." + unit.toString();
						try {
							return fqn.getBytes("utf-8");
						} catch (UnsupportedEncodingException e) {
							AttributeValueException ave = new AttributeValueException();
							ave.initCause(e);
							throw ave;
						}
					}

					@Override
					public String getName() {
						return "Fully Qualified Name";
					}
				});
			}

			private void traverseMethods(SootMethod source, CallGraph cg) {
				Targets tc = new Targets(cg.edgesOutOf(source));
				while (tc.hasNext()) {
					SootMethod destination = (SootMethod) tc.next();
					// System.out.println(destination+" is java library "+destination.isJavaLibraryMethod());
					if (!destination.isJavaLibraryMethod()) {
						boolean edgeConnection = false;
						Iterator<VFMethodEdge> edgeIterator = methodGraph.listEdges.iterator();
						while(edgeIterator.hasNext())
						{
							VFMethodEdge vfMethodEdge = (VFMethodEdge) edgeIterator.next();
							if(vfMethodEdge.getSourceMethod().getSootMethod().equals(source) && vfMethodEdge.getDestMethod().getSootMethod().equals(destination))
							{
								edgeConnection = true;
								break;
							}
						}
						
						if(edgeConnection)
							continue;
						boolean methodPresent = false;
						Iterator<VFMethod> iteratorMethod = methodGraph.listMethods.iterator();
						while (iteratorMethod.hasNext()) {
							if (iteratorMethod.next().getSootMethod().equals(destination)) {
								methodPresent = true;
								break;
							}
						}

						if (!methodPresent) {
							methodcount++;
							VFMethod method;
							if (methodMap.containsKey(destination)) {
								method = methodMap.get(destination);
								method.setId(methodcount);
							} else {
								method = new VFMethod(methodcount, destination);
								methodMap.put(destination, method);
							}
							methodGraph.listMethods.add(method);
						}
						VFMethod sourceMethod = null;
						VFMethod destinationMethod = null;
						Iterator<VFMethod> iteratorMethods = methodGraph.listMethods.iterator();
						while (iteratorMethods.hasNext()) {
							VFMethod method = iteratorMethods.next();
							if (method.getSootMethod().equals(source)) {
								sourceMethod = method;
							}
							if (method.getSootMethod().equals(destination)) {
								destinationMethod = method;
							}
						}
						edgeCount++;
						VFMethodEdge edge = new VFMethodEdge(edgeCount, sourceMethod, destinationMethod);
						destinationMethod.addIncomingEdge(edge);
						System.out.println("Method " + destinationMethod + " Edge " + edge);
						if (destinationMethod.getIncomingEdges().isEmpty()) {
							System.out.println("adding didnt work");
						}
						methodGraph.listEdges.add(edge);
						traverseMethods(destination, cg);
					}
				}
			}
		});

		PackManager.v().getPack("wjap").add(transform);
		// Run Soot
		Main.main(sootString);
	}
}
