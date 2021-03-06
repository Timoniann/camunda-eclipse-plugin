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
 * @author Innar Made
 ******************************************************************************/
package org.camunda.bpm.modeler.ui.features.event;

import org.camunda.bpm.modeler.core.features.MultiUpdateFeature;
import org.camunda.bpm.modeler.core.features.UpdateDecorationFeature;
import org.camunda.bpm.modeler.core.features.api.IDecorateFeature;
import org.camunda.bpm.modeler.core.features.event.EventDecorateFeature;
import org.camunda.bpm.modeler.core.utils.GraphicsUtil;
import org.camunda.bpm.modeler.core.utils.StyleUtil;
import org.camunda.bpm.modeler.ui.features.AbstractDefaultDeleteFeature;
import org.eclipse.bpmn2.BoundaryEvent;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IDirectEditingFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.ILayoutFeature;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.features.impl.DefaultResizeShapeFeature;
import org.eclipse.graphiti.mm.algorithms.Ellipse;

public class BoundaryEventFeatureContainer extends AbstractEventFeatureContainer {

	public static String BOUNDARY_EVENT_CANCEL = "cancel.activity";
	public static String BOUNDARY_EVENT_DISTANCE = "boundary.distance";

	@Override
	public boolean canApplyTo(Object o) {
		return super.canApplyTo(o) && o instanceof BoundaryEvent;
	}

	@Override
	public ICreateFeature getCreateFeature(IFeatureProvider fp) {
		return new CreateBoundaryEventFeature(fp);
	}
	
	@Override
	public IAddFeature getAddFeature(IFeatureProvider fp) {
		return new AddBoundaryEventFeature(fp) {
//			
//			@Override
//			protected void decorate(Ellipse decorateContainer) {
//				Ellipse circle = GraphicsUtil.createIntermediateEventCircle(decorateContainer);
//				circle.setStyle(StyleUtil.getStyleForClass(getDiagram()));
//			}
		};
	}

	@Override
	public IDecorateFeature getDecorateFeature(IFeatureProvider fp) {
		return new EventDecorateFeature(fp) {
			
			@Override
			protected void decorate(Ellipse decorateContainer) {
				Ellipse circle = GraphicsUtil.createIntermediateEventCircle(decorateContainer);
				circle.setStyle(StyleUtil.getStyleForClass(getDiagram()));
			}
		};
	}
	
	@Override
	public IUpdateFeature getUpdateFeature(IFeatureProvider fp) {
		return new MultiUpdateFeature(fp)
			.addUpdateFeature(new UpdateBoundaryEventFeature(fp))
			.addUpdateFeature(super.getUpdateFeature(fp));
	}

	@Override
	public IDirectEditingFeature getDirectEditingFeature(IFeatureProvider fp) {
		return null;
	}

	@Override
	public ILayoutFeature getLayoutFeature(IFeatureProvider fp) {
		return new LayoutBoundaryEventFeature(fp);
	}

	@Override
	public IMoveShapeFeature getMoveFeature(IFeatureProvider fp) {
		return new MoveBoundaryEventFeature(fp);
	}

	@Override
	public IResizeShapeFeature getResizeFeature(IFeatureProvider fp) {
		return new DefaultResizeShapeFeature(fp) {
			@Override
			public boolean canResizeShape(IResizeShapeContext context) {
				return false;
			}
		};
	}

	@Override
	public IDeleteFeature getDeleteFeature(IFeatureProvider fp) {
		return new AbstractDefaultDeleteFeature(fp);
	}
	
	@Override
	public ICustomFeature[] getCustomFeatures(IFeatureProvider fp) {
		ICustomFeature[] superFeatures = super.getCustomFeatures(fp);
		ICustomFeature[] thisFeatures = new ICustomFeature[1 + superFeatures.length];

		int i;
		for (i = 0; i < superFeatures.length; ++i) {
			thisFeatures[i] = superFeatures[i];
		}
		
		thisFeatures[i++] = new MorphBoundaryEventFeature(fp);
		return thisFeatures;
	}
	
}