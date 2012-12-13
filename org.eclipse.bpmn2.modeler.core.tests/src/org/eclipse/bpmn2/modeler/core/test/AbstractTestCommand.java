package org.eclipse.bpmn2.modeler.core.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.impl.DocumentRootImpl;
import org.eclipse.bpmn2.modeler.core.ModelHandlerLocator;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.services.GraphitiUi;

public abstract class AbstractTestCommand extends RecordingCommand {
	
	protected AbstractBpmnEditorTest testCase;
	protected String diagramName;
	
	protected Diagram diagram;	
	protected IDiagramTypeProvider diagramTypeProvider;
	protected Resource diagramResource;

	protected Throwable recordedException;
	
	private static final String TEST_DIR = "target/test";
	
	public AbstractTestCommand(AbstractBpmnEditorTest importBpmnModelTest, String diagramName) {
		super(importBpmnModelTest.getEditingDomain());
		
		this.testCase = importBpmnModelTest;
		this.diagramName = diagramName;
	}

	@Override
	protected void doExecute() {

		try {
			diagram = Graphiti.getPeCreateService().createDiagram("BPMN2", diagramName, true);

			String fileName = diagramName.replaceAll("/", ".").replaceAll(".bpmn", ".diagram");
			URI uri = URI.createFileURI(testCase.getTempFolder().newFile(fileName).getAbsolutePath());
			
			ModelHandlerLocator.put(uri, testCase.getModelHandler());
	
			diagramResource = testCase.getEditingDomain().getResourceSet().createResource(uri);
			diagramResource.getContents().add(diagram);
			
			diagramTypeProvider = GraphitiUi.getExtensionManager().createDiagramTypeProvider(
					diagram,
					"org.eclipse.bpmn2.modeler.ui.diagram.MainBPMNDiagramType");

			diagramTypeProvider.getDiagramEditor().getResourceSet();
			
			Bpmn2ResourceImpl resource = testCase.getResource();
			
			List<EObject> contents = resource.getContents();
			if (!contents.isEmpty()) {
				DocumentRootImpl rootImpl = (DocumentRootImpl) resource.getContents().get(0);
				Definitions definitions = rootImpl.getDefinitions();
				if (definitions != null) {
					List<BPMNDiagram> diagrams = definitions.getDiagrams();
					
					if (!diagrams.isEmpty()) {
						BPMNDiagram bpmnDiagram = diagrams.get(0);
						diagramTypeProvider.getFeatureProvider().link(diagram, bpmnDiagram);
					}
				}
			}

			new File(TEST_DIR).mkdir();
			File diagramFile = new File (diagramName);
			
			try {
				FileOutputStream outBefore = new FileOutputStream(new File(TEST_DIR+File.separatorChar+"before."+diagramFile.getName()));
				resource.save(outBefore, Collections.emptyMap());
				outBefore.close();
			} catch (Exception e) {
				// We cant always write models, we have tests with broken models, wich can not be saved
				// not checking for exception type, because they are somewhat arbitary (e.g. BasicIndexOutofBoundsException)
			}
			
			test(diagramTypeProvider, diagram);
			
			try {
				FileOutputStream outAfter = new FileOutputStream(new File(TEST_DIR+File.separatorChar+"after."+diagramFile.getName()));
				resource.save(outAfter, Collections.emptyMap());
				outAfter.close();
			}catch (Exception e) {
				// We cant always write models, we have tests with broken models, wich can not be saved, see above
			}
			
		} catch (RuntimeException e) {
			this.recordedException = e;
			throw e;
			
		} catch (Throwable e) {
			this.recordedException = e;
			throw new RuntimeException(e);
		}
	}

	public abstract void test(IDiagramTypeProvider diagramTypeProvider, Diagram diagram) throws Throwable;

	public Throwable getRecordedException() {
		return recordedException;
	}
}
