package org.eclipse.bpmn2.modeler.core.test.feature;

import org.eclipse.bpmn2.modeler.core.importer.ModelImport;
import org.eclipse.bpmn2.modeler.core.test.AbstractEditorTest;
import org.eclipse.bpmn2.modeler.core.test.util.NonTransactional;
import org.eclipse.emf.transaction.RecordingCommand;
import org.junit.Before;

/**
 * Feature test which allows the model to be non-transactionally imported
 * 
 * @author nico.rehwaldt
 */
@NonTransactional
public class AbstractNonTransactionalFeatureTest extends AbstractEditorTest {

	@Before
	public void before() {
		transactionalExecute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				importModel();
			}
		});
	}

	protected ModelImport modelImport;

	protected void importModel() {
		if (bpmnResource != null) {
			modelImport = new ModelImport(diagramTypeProvider, bpmnResource);
			modelImport.execute();
		}
	}
	
	protected void transactionalExecute(RecordingCommand command) {
		editingDomain.getCommandStack().execute(command);
	}
}
