package org.camunda.bpm.modeler.ui.property.tabs.builder;

import static org.camunda.bpm.modeler.core.utils.ExtensionUtil.*;
import static org.camunda.bpm.modeler.core.utils.ExtensionUtil.updateExtension;

import java.util.List;

import org.camunda.bpm.modeler.core.utils.ExtensionUtil;
import org.camunda.bpm.modeler.runtime.engine.model.FailedJobRetryTimeCycleType;
import org.camunda.bpm.modeler.runtime.engine.model.ModelFactory;
import org.camunda.bpm.modeler.runtime.engine.model.ModelPackage;
import org.camunda.bpm.modeler.ui.change.filter.ExtensionChangeFilter;
import org.camunda.bpm.modeler.ui.change.filter.FeatureChangeFilter;
import org.camunda.bpm.modeler.ui.property.tabs.binding.ModelTextBinding;
import org.camunda.bpm.modeler.ui.property.tabs.binding.change.EAttributeChangeSupport;
import org.camunda.bpm.modeler.ui.property.tabs.util.HelpText;
import org.camunda.bpm.modeler.ui.property.tabs.util.PropertyUtil;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.ExtensionAttributeValue;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.graphiti.ui.platform.GFPropertySection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Factory for retry time cycle enabled elements
 * 
 * @author nico.rehwaldt
 */
public class RetryEnabledPropertiesBuilder extends AbstractPropertiesBuilder<BaseElement> {

	private static final EStructuralFeature RETRY_CYCLE_FEATURE = ModelPackage.eINSTANCE.getDocumentRoot_FailedJobRetryTimeCycle();
	
	public RetryEnabledPropertiesBuilder(Composite parent, GFPropertySection section, BaseElement bo) {
		super(parent, section, bo);
	}

	@Override
	public void create() {
		createRetryCycleText();
	}
	
	protected Text createRetryCycleText() {

		final Text retryText = PropertyUtil.createUnboundText(section, parent, "Retry Time Cycle");

		PropertyUtil.attachNote(retryText, HelpText.TIME_CYCLE);
		
		// observing the checkbox and updating the model
		
		new RetryCycleStringTextBinding(bo, retryText).establish();
		
		return retryText;
	}
	
	/**
	 * Binding for the retry time cycle element
	 * 
	 * @author nico.rehwaldt
	 */
	private class RetryCycleStringTextBinding extends ModelTextBinding<String> {

		public RetryCycleStringTextBinding(EObject model, Text control) {
			super(model, control);
		}

		@Override
		protected String toString(String value) {
			if (value == null) {
				return "";
			} else {
				return value;
			}
		}

		@Override
		protected String fromString(String value) {
			if (value == null || value.trim().isEmpty()) {
				return null;
			} else {
				return value;
			}
		}

		@Override
		public String getModelValue() {
			List<FailedJobRetryTimeCycleType> result =
					(List<FailedJobRetryTimeCycleType>) getExtensions(bo, FailedJobRetryTimeCycleType.class);
			if (!result.isEmpty()) {
				return result.get(0).getText();
			}
			return null;
		}

		@Override
		public void setModelValue(String value) {
			transactionalUpdateRetryTimeCycle(value);
		}
		
		@Override
		protected void ensureChangeSupportAdded() {
			EAttributeChangeSupport changeSupport = new EAttributeChangeSupport(model, RETRY_CYCLE_FEATURE, control);
			changeSupport.setFilter(new ExtensionChangeFilter(model, RETRY_CYCLE_FEATURE).or(new FeatureChangeFilter(model, RETRY_CYCLE_FEATURE)));
			
			EAttributeChangeSupport.ensureAdded(changeSupport, control);
		}
	}
	
	// invocation of transactional behavior ///////////////
	
	protected void transactionalUpdateRetryTimeCycle(String newValue) {
		TransactionalEditingDomain domain = getTransactionalEditingDomain();
		domain.getCommandStack().execute(new UpdateRetryTimeCycleCommand(domain, newValue));
	}

	protected TransactionalEditingDomain getTransactionalEditingDomain() {
		return TransactionUtil.getEditingDomain(bo);
	}
	
	// transactional behavior //////////////////////////////
	
	protected void updateRetryCycle(String retryTimeCycle) {
		FailedJobRetryTimeCycleType retryCycle = ModelFactory.eINSTANCE.createFailedJobRetryTimeCycleType();
		retryCycle.setText(retryTimeCycle.trim());
	
		updateExtension(bo, RETRY_CYCLE_FEATURE, retryCycle);
	}
	
	protected void removeRetryCycle() {
		removeExtensionByFeature(bo, RETRY_CYCLE_FEATURE);
		
		// remove empty extension element
		EStructuralFeature extensionValuesFeature = bo.eClass().getEStructuralFeature("extensionValues");
		ExtensionAttributeValue values = ExtensionUtil.getExtensionAttributeValue(bo);
		if (values != null && (values.getValue() == null || values.getValue().isEmpty())) {
			bo.eUnset(extensionValuesFeature);
		}
	}
	
	// commands ///////////////////////////////////////////
	
	/**
	 * Command which takes care of updating the retry time cycle
	 * 
	 * @author nico.rehwaldt
	 */
	private class UpdateRetryTimeCycleCommand extends RecordingCommand {

		private String newValue;
		
		public UpdateRetryTimeCycleCommand(TransactionalEditingDomain domain, String newValue) {
			super(domain);
			
			this.newValue = newValue;
		}

		@Override
		protected void doExecute() {

			
			if (newValue == null || newValue.trim().isEmpty()) {
				removeRetryCycle();
			} else {
				updateRetryCycle(newValue);
			}
		}
	}
}
