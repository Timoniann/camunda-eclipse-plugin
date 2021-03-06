/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/
package org.camunda.bpm.modeler.core.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.modeler.core.AbstractPropertyChangeListenerProvider;
import org.camunda.bpm.modeler.core.Activator;
import org.camunda.bpm.modeler.core.IBpmn2RuntimeExtension;
import org.camunda.bpm.modeler.core.preferences.ShapeStyle;
import org.camunda.bpm.modeler.core.property.TabDescriptor;
import org.camunda.bpm.modeler.core.runtime.ModelExtensionDescriptor.Property;
import org.camunda.bpm.modeler.core.runtime.ModelExtensionDescriptor.Value;
import org.camunda.bpm.modeler.core.utils.ModelUtil.Bpmn2DiagramType;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

/**
 * @deprecated We are not using the target runtime concept anymore, 
 * should be removed everywehere
 * 
 * @author drobisch
 *
 */
public class TargetRuntime extends AbstractPropertyChangeListenerProvider {

	// extension point ID for Target Runtimes
	public static final String RUNTIME_ID = "org.camunda.bpm.modeler.runtime";
	public static final String DEFAULT_RUNTIME_ID = "org.camunda.bpm.modeler.runtime.none";
	
	// our cached registry of target runtimes contributed by other plugins
	protected static TargetRuntime targetRuntimes[];
	protected static TargetRuntime currentRuntime;
	
	protected String name;
	protected String[] versions;
	protected String id;
	protected String description;
	private IBpmn2RuntimeExtension runtimeExtension;
	protected ModelDescriptor modelDescriptor;
	protected ArrayList<TabDescriptor> tabDescriptors;
	protected ArrayList<ModelExtensionDescriptor> modelExtensions;
	protected ArrayList<ModelEnablementDescriptor> modelEnablements;
	protected ModelEnablementDescriptor defaultModelEnablements;
	protected ArrayList<FeatureContainerDescriptor> featureContainers;
	protected HashMap<Class, ShapeStyle> shapeStyles;

	public TargetRuntime(String id, String name, String versions, String description) {
		this.id = id;
		this.name = name;
		if (versions!=null)
			this.versions = versions.split("[, ]");
		this.description = description;
	}
	
	public static TargetRuntime getRuntime(String id) {
		if (getAllRuntimes() == null) {
			return null;
		}
		
		for (TargetRuntime rt : getAllRuntimes()) {
			if (rt.id.equals(id))
				return rt;
		}
		return null;
	}
	
	public static TargetRuntime getCurrentRuntime() {
		return currentRuntime;
	}
	
	public static void setCurrentRuntime(TargetRuntime rt) {
		currentRuntime = rt;
	}
	
	public static TargetRuntime getDefaultRuntime() {
		return getRuntime(DEFAULT_RUNTIME_ID);
	}
	
	/**
	 * 
	 * @return the first runtime which is non default, if there are more than on runtime or if there is only the default runtime
	 * return the DEFAULT_RUNTIME_ID
	 */
	public static String getFirstNonDefaultId(){
		String runtimeId = null;
		int nonDefaultRuntimeCount = 0;
		
		if (TargetRuntime.getAllRuntimes() == null) {
			return TargetRuntime.DEFAULT_RUNTIME_ID;
		}
		
		for (TargetRuntime rt :TargetRuntime.getAllRuntimes()) {
			if (!rt.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)){
				nonDefaultRuntimeCount++;
				runtimeId = rt.getId();
			}
		}
		
		if (nonDefaultRuntimeCount == 1 && runtimeId != null){
			return runtimeId;
		}else{
			return TargetRuntime.DEFAULT_RUNTIME_ID;
		}
	}
	
	public static TargetRuntime[] getAllRuntimes() {
		if (targetRuntimes==null) {
			// load runtimes contributions from other plugins
			ArrayList<TargetRuntime> rtList = new ArrayList<TargetRuntime>();
			
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
					RUNTIME_ID);

			try {
				// first create all the Target Runtimes because other
				// extension elements refer to these by runtimeId
				for (IConfigurationElement e : config) {
					if (e.getName().equals("runtime")) {
						String id = e.getAttribute("id");
						String name = e.getAttribute("name");
						String versions = e.getAttribute("versions");
						String description = e.getAttribute("description");
						TargetRuntime rt = new TargetRuntime(id,name,versions,description);
						
						rt.setRuntimeExtension((IBpmn2RuntimeExtension) e.createExecutableExtension("class"));
					
						rtList.add(rt);
					}
				}
				
				targetRuntimes = rtList.toArray(new TargetRuntime[rtList.size()]);
				
				// process model first
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) {
						currentRuntime = getRuntime(e);
						
						if (e.getName().equals("model")) {
							ModelDescriptor md = new ModelDescriptor();
							if (e.getAttribute("uri")!=null) {
								String uri = e.getAttribute("uri");
								md.setEPackage(EPackage.Registry.INSTANCE.getEPackage(uri));
								md.setEFactory(md.getEPackage().getEFactoryInstance());
							}
							if (e.getAttribute("resourceFactory")!=null)
								md.setResourceFactory((ResourceFactoryImpl) e.createExecutableExtension("resourceFactory"));
							currentRuntime.setModelDescriptor(md);
						}
					}
				}
				
				// need to process the Default Runtime first (defined in o.e.b.m.ui) because
				// other plugins can refer to this.
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) {
						currentRuntime = getRuntime(e);
						if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
							if (e.getName().equals("modelEnablement")) {
								ModelEnablementDescriptor me;
								String type = e.getAttribute("type");
								String profile = e.getAttribute("profile");
								currentRuntime.addModelEnablements(me = new ModelEnablementDescriptor());
								me.setType(type);
								me.setProfile(profile);
								
								if (e.getAttribute("override") != null) {
									me.setOverride(new Boolean(e.getAttribute("override")));
								}
								
								for (IConfigurationElement c : e.getChildren()) {
									String object = c.getAttribute("object");
									String feature = c.getAttribute("feature");
									if (c.getName().equals("enable")) {
										me.setEnabled(object, feature, true);
									} else if (c.getName().equals("disable")) {
										me.setEnabled(object, feature, false);
									}
								}

							}
						}
					}
				}
				// process propertyTab, propertyExtension, customTask, modelExtension and modelEnablement next
				for (IConfigurationElement e : config) {
					if (!e.getName().equals("runtime")) {
						currentRuntime = getRuntime(e);
						
						if (e.getName().equals("propertyTab")) {
							if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
								// already done
								continue;
							}
						}
						else if (e.getName().equals("featureContainer")) {
							String id = e.getAttribute("id");
							FeatureContainerDescriptor fc = new FeatureContainerDescriptor(currentRuntime);
							fc.type = e.getAttribute("type");
							fc.containerClassName = e.getAttribute("class");
							currentRuntime.addFeatureContainer(fc);
						}
						else if (e.getName().equals("modelExtension")) {
							String id = e.getAttribute("id");
							String name = e.getAttribute("name");
							ModelExtensionDescriptor me = new ModelExtensionDescriptor(id,name);
							me.type = e.getAttribute("type");
							me.description = e.getAttribute("description");
							getModelExtensionProperties(me,e);
							currentRuntime.addModelExtension(me);
						}
						else if (e.getName().equals("modelEnablement")) {
							if (currentRuntime.getId().equals(TargetRuntime.DEFAULT_RUNTIME_ID)) {
								// already done
								continue;
							}
							ModelEnablementDescriptor me;
							String type = e.getAttribute("type");
							String profile = e.getAttribute("profile");
							currentRuntime.addModelEnablements(me = new ModelEnablementDescriptor());
							me.setType(type);
							me.setProfile(profile);
							
							if (e.getAttribute("override") != null) {
								me.setOverride(new Boolean(e.getAttribute("override")));
							}
							
							for (IConfigurationElement c : e.getChildren()) {
								String object = c.getAttribute("object");
								String feature = c.getAttribute("feature");
								if (c.getName().equals("enable")) {
									me.setEnabled(object, feature, true);
								} else if (c.getName().equals("disable")) {
									me.setEnabled(object, feature, false);
								}
							}

						}
						else if (e.getName().equals("style")) {
							String object = e.getAttribute("object");
							String foreground = e.getAttribute("foreground");
							String background = e.getAttribute("background");
							String textColor = e.getAttribute("textColor");
							String font = e.getAttribute("font");
							EClass eclass = (EClass)Bpmn2Package.eINSTANCE.getEClassifier(object);
							ShapeStyle ss = new ShapeStyle(foreground, background, textColor, font);
							currentRuntime.getShapeStyles().put(eclass.getInstanceClass(), ss);
						}
					}
				}


				// All done parsing configuration elements
				// now go back and fix up some things...
				for (TargetRuntime rt : targetRuntimes) {
					
					if (rt.modelDescriptor==null) {
						rt.modelDescriptor = getDefaultRuntime().getModelDescriptor(); 
					}
					// add customTask and modelExtension features to modelEnablements
					// these are enabled by default and can't be disabled.
					for (ModelEnablementDescriptor me : rt.getModelEnablements()) {
						for (ModelExtensionDescriptor med : rt.getModelExtensions()) {
							for (Property p : med.getProperties()) {
								me.setEnabled(med.getType(), p.name, true);
							}
						}
					}
				}
				
			} catch (Exception ex) {
				Activator.logError(ex);
			}
		}
		return targetRuntimes;
	}
	
	private static Object getModelExtensionProperties(ModelExtensionDescriptor ct, IConfigurationElement e) {
		
		String elem = e.getName();
		if ("value".equals(elem)) {
			String id = e.getAttribute("id");
			Value val = new Value(id);
			for (IConfigurationElement c : e.getChildren()) {
				Object propValue = getModelExtensionProperties(null, c);
				val.getValues().add(propValue);
			}
			return val;
		}
		else if ("property".equals(elem)) {
			String name = e.getAttribute("name");
			String value = e.getAttribute("value");
			String ref = e.getAttribute("ref");
			String type = e.getAttribute("type");
			String description = e.getAttribute("description");
			Property prop = new Property(name,description);
			prop.type = type;
			if (value!=null)
				prop.getValues().add(value);
			else if (ref!=null) {
				prop.ref = ref;
			}
			else if(e.getChildren().length > 0){
				Object o = getModelExtensionProperties(null, e.getChildren()[0]);
				if (o instanceof Value)
					prop.getValues().addAll(((Value)o).getValues());
			}
			return prop;
		}
		else {
			for (IConfigurationElement c : e.getChildren()) {
				Object o = getModelExtensionProperties(null, c);
				if (o instanceof Property && ct!=null)
					ct.getProperties().add((Property)o);
			}
		}
		return null;
	}

	private static TargetRuntime getRuntime(IConfigurationElement e) {
		String runtimeId = e.getAttribute("runtimeId");
		TargetRuntime rt = getRuntime(runtimeId);
		if (rt==null)
			return currentRuntime;
		return rt;
	}
	
	public ModelDescriptor getModelDescriptor() {
		return modelDescriptor;
	}
	
	public void setModelDescriptor(ModelDescriptor md) {
		md.setRuntime(this);
		this.modelDescriptor = md;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDescription() {
		return description;
	}
	
	public ArrayList<ModelExtensionDescriptor> getModelExtensions()
	{
		if (modelExtensions==null) {
			modelExtensions = new ArrayList<ModelExtensionDescriptor>();
		}
		return modelExtensions;
	}
	
	public void addModelExtension(ModelExtensionDescriptor me) {
		me.setRuntime(this);
		getModelExtensions().add(me);
	}
	
	public ArrayList<FeatureContainerDescriptor> getFeatureContainers()
	{
		if (featureContainers==null) {
			featureContainers = new ArrayList<FeatureContainerDescriptor>();
		}
		return featureContainers;
	}
	
	public void addFeatureContainer(FeatureContainerDescriptor me) {
		me.setRuntime(this);
		getFeatureContainers().add(me);
	}

	public FeatureContainerDescriptor getFeatureContainer(EClass clazz) {
		for (FeatureContainerDescriptor fcd : getFeatureContainers()) {
			String className = clazz.getInstanceClassName();
			if (className.equals(fcd.type))
				return fcd;
			// well, that didn't work...
			// The "type" name should be the BPMN2 element's interface definition;
			// if it's an implementation class name, try to convert it to its
			// interface name.
			className = className.replaceFirst("\\.impl\\.", ".");
			className = className.replaceFirst("Impl$", "");
			if (className.equals(fcd.type))
				return fcd;
		}
		return null;
	}

	public ArrayList<ModelEnablementDescriptor> getModelEnablements()
	{
		if (modelEnablements==null) {
			modelEnablements = new ArrayList<ModelEnablementDescriptor>();
		}
		return modelEnablements;
	}
	
	public ModelEnablementDescriptor getModelEnablements(EObject object)
	{
		return getModelEnablements(Bpmn2DiagramType.COLLABORATION);
		//		return getModelEnablements( ModelUtil.getDiagramType(object) );
	}
	
	public ModelEnablementDescriptor getModelEnablements(Bpmn2DiagramType diagramType)
	{
		return getModelEnablements(diagramType, null);
	}
	
	public ModelEnablementDescriptor getModelEnablements(Bpmn2DiagramType diagramType, String profile)
	{
		for (ModelEnablementDescriptor me : getModelEnablements()) {
			String s = diagramType.name();
			if (diagramType == Bpmn2DiagramType.NONE && me.getType()==null) {
				if (profile==null || profile.equalsIgnoreCase(me.getProfile()))
					return me;
			}
			if (s.equalsIgnoreCase(me.getType())) {
				if (profile==null || profile.equalsIgnoreCase(me.getProfile()))
					return me;
			}
		}
		return new ModelEnablementDescriptor();
	}
	
	public void addModelEnablements(ModelEnablementDescriptor me) {
		me.setRuntime(this);
		getModelEnablements().add(me);
	}
	
	protected Map<Class, ShapeStyle> getShapeStyles() {
		if (shapeStyles==null) {
			shapeStyles = new HashMap<Class, ShapeStyle>();
		}
		return shapeStyles;
	}
	
	public ShapeStyle getShapeStyle(Class c) {
		ShapeStyle ss = getShapeStyles().get(c);
		if (ss==null && !TargetRuntime.DEFAULT_RUNTIME_ID.equals(id)) {
			// not defined in this TargetRuntime - check default TargetRuntime
			ss = TargetRuntime.getDefaultRuntime().getShapeStyle(c);
			if (ss!=null)
				return ss;
		}
		return new ShapeStyle(ss);
	}
	
	private ArrayList<TabDescriptor> getTabs() {
		if (tabDescriptors==null)
			tabDescriptors = new ArrayList<TabDescriptor>();
		return tabDescriptors;
	}

	public static TabDescriptor findTabDescriptor(String id) {
		for (TargetRuntime rt : TargetRuntime.getAllRuntimes()) {
			TabDescriptor tab = rt.getTabDescriptor(id);
			if (tab!=null)
				return tab;
		}
		return null;
	}
	
	public TabDescriptor getTabDescriptor(String id) {
		for (TabDescriptor tab : getTabs()) {
			if (tab.getId().equals(id))
				return tab;
		}
		return null;
	}
	
	/**
	 * @return
	 */
	public ArrayList<TabDescriptor> getTabDescriptors() {
		ArrayList<TabDescriptor> list = new ArrayList<TabDescriptor>();
		for (TabDescriptor tab : getTabs()) {
			if (!list.contains(tab))
				list.add(tab);
		}
		
		return list;
	}
	
	public IBpmn2RuntimeExtension getRuntimeExtension() {
		return runtimeExtension;
	}

	public void setRuntimeExtension(IBpmn2RuntimeExtension runtimeExtension) {
		this.runtimeExtension = runtimeExtension;
	}
}
