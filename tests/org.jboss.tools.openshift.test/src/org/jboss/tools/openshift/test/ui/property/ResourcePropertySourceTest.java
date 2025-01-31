/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property;

import static org.jboss.tools.openshift.test.ui.property.util.Assert.assertPropertyDescriptorsEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.ExtTextPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.PrefixPropertySourceKey;
import org.jboss.tools.openshift.internal.ui.property.ResourcePropertySource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.openshift.restclient.model.IResource;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ResourcePropertySourceTest {

	@Mock
	private IResource resource;
	private ResourcePropertySource<IResource> source;

	@Before
	public void setup() {
		Map<String, String> labels = new HashMap<>();
		labels.put("foo", "bar");
		labels.put("bar", "bbar");
		Map<String, String> annotations = new HashMap<>();
		annotations.put("xyz", "abc");
		annotations.put("efg", "def");

		when(resource.getName()).thenReturn("aname");
		when(resource.getNamespaceName()).thenReturn("anamespace");
		when(resource.getKind()).thenReturn("akind");
		when(resource.getLabels()).thenReturn(labels);
		when(resource.getAnnotations()).thenReturn(annotations);
		when(resource.getAnnotation("xyz")).thenReturn("abc");
		when(resource.getAnnotation("efg")).thenReturn("def");
		when(resource.getCreationTimeStamp()).thenReturn("2014");
		when(resource.getResourceVersion()).thenReturn("9999");

		source = new ResourcePropertySource<>(resource);
	}

	@Test
	public void getPropertyValue() {
		assertEquals("aname", source.getPropertyValue(ResourcePropertySource.Ids.NAME));
		assertEquals("anamespace", source.getPropertyValue(ResourcePropertySource.Ids.NAMESPACE));
		assertEquals("akind", source.getPropertyValue(ResourcePropertySource.Ids.KIND));
		assertEquals("2014", source.getPropertyValue(ResourcePropertySource.Ids.CREATED));
		assertEquals("9999", source.getPropertyValue(ResourcePropertySource.Ids.RESOURCE_VERSION));
		assertEquals("abc", source.getPropertyValue(new PrefixPropertySourceKey("Annotations", "xyz")));
		assertEquals("def", source.getPropertyValue(new PrefixPropertySourceKey("Annotations", "efg")));
		assertEquals("bar", source.getPropertyValue(new PrefixPropertySourceKey("Labels", "foo")));
	}

	@Test
	public void getPropertyDescriptor() {
		IPropertyDescriptor[] exp = new IPropertyDescriptor[] {
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.KIND, "Kind", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.NAME, "Name", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.NAMESPACE, "Namespace", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.CREATED, "Creation Timestamp", "Basic"),
				new ExtTextPropertyDescriptor(ResourcePropertySource.Ids.RESOURCE_VERSION, "Resource Version", "Basic"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Annotations", "xyz"), "xyz", "Annotations"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Annotations", "efg"), "efg", "Annotations"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Labels", "foo"), "foo", "Labels"),
				new ExtTextPropertyDescriptor(new PrefixPropertySourceKey("Labels", "bar"), "bar", "Labels") };
		assertPropertyDescriptorsEquals(exp, source.getPropertyDescriptors());
	}

}
