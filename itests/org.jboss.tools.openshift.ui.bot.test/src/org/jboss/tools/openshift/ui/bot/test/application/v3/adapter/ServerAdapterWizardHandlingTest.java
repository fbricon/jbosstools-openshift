/*******************************************************************************
 * Copyright (c) 2007-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.lookup.ShellLookup;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizard;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizardPage;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsKilled;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OCBinary(cleanup=false, setOCInPrefs=true)
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = OpenShiftResources.EAP_SERVICE, template = OpenShiftResources.EAP_TEMPLATE_RESOURCES_PATH)
public class ServerAdapterWizardHandlingTest extends AbstractTest  {
	
	private static final String PROJECT_NAME = "kitchensink";

	private static final String GIT_REPO_URL = "https://github.com/jboss-developer/jboss-eap-quickstarts";
	
	private static final String GIT_REPO_DIRECTORY = "target/git_repo";
	
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;
	
	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;
	
	@BeforeClass
	public static void waitTillApplicationIsRunning() {
		cloneGitRepoAndImportProject();
	}
	
	private static void cloneGitRepoAndImportProject() {
		OpenShiftUtils.cloneGitRepository(GIT_REPO_DIRECTORY, GIT_REPO_URL, "7.3.x", true);
		OpenShiftUtils.importProjectUsingSmartImport(GIT_REPO_DIRECTORY, PROJECT_NAME);	}

	@Test
	public void testPreselectedConnectionForNewOpenShift3ServerAdapter() {
		openNewServerAdapterWizard();

		assertTrue("There should be preselected an existing OpenShift connection in new server adapter wizard.",
				new LabeledCombo(OpenShiftLabel.TextLabels.CONNECTION).getSelection().contains(connectionReq.getConnection().getUsername()));
	}

	@Test
	public void testProjectSelectedInProjectExplorerIsPreselected() {
		new ProjectExplorer().selectProjects(PROJECT_NAME);

		openNewServerAdapterWizard();
		next();

		String eclipseProject = Display.syncExec(new ResultRunnable<String>() {

			@Override
			public String run() {
				return new LabeledText("Eclipse Project: ").getSWTWidget().getText();
			}

		});

		assertTrue("Selected project from workspace should be preselected", eclipseProject.equals(PROJECT_NAME));
	}

	@Test
	public void testPodPathWidgetAccessibility() {
		openNewServerAdapterWizard();
		next();

		new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();

		new CheckBox("Use inferred Pod Deployment Path").toggle(false);

		LabeledText podPath = new LabeledText("Pod Deployment Path: ");
		String podDeploymentPath = "/opt/eap/standalone/deployments/";
		podPath.setText("");

		assertFalse("Next button should be disable if pod path is empty is selected.", nextButtonIsEnabled());

		podPath.setText(podDeploymentPath);

		assertTrue("Next button should be reeenabled if pod path is correctly filled in.", nextButtonIsEnabled());
	}

	@Test
	public void testApplicationSelectionWidgetAccessibility() {
		openNewServerAdapterWizard();
		next();

		new DefaultTreeItem(projectReq.getProjectName()).select();

		assertFalse("Next button should be disable if no application is selected.", nextButtonIsEnabled());

		new DefaultTreeItem(projectReq.getProjectName()).getItems().get(0).select();

		assertTrue("Next button should be enabled if application for a new server adapter is created.",
				nextButtonIsEnabled());
	}

	@Test
	public void testFinishButtonAccessibility() {
		openNewServerAdapterWizard();

		assertFalse("Finish button should be disabled on new server "
				+ "adapter wizard page where selection of a connection is done, "
				+ "because there are still missing details to successfully create a new"
				+ "OpenShift server adapter.", buttonIsEnabled(new FinishButton()));
	}

	@Test
	public void testSourcePathWidgetAccessibility() {
		openNewServerAdapterWizard();

		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);

		next();

		new PushButton(OpenShiftLabel.Button.ADVANCED_OPEN).click();

		LabeledText srcPath = new LabeledText("Source Path: ");
		String srcPathText = srcPath.getText();
		srcPath.setText("");

		assertFalse("Next button should be disable if source path is empty is selected.", nextButtonIsEnabled());

		srcPath.setText(srcPathText);

		assertTrue("Next button should be reeenabled if source path is correctly filled in.", nextButtonIsEnabled());

		srcPath.setText("invalid path");

		assertFalse("Next button should be disabled if source path is invalid or not existing.", nextButtonIsEnabled());

		srcPath.setText(srcPathText);

		assertTrue("Next button should be reeenabled if source path is correctly filled in.", nextButtonIsEnabled());
	}

	private boolean nextButtonIsEnabled() {
		return buttonIsEnabled(new NextButton());
	}

	private boolean buttonIsEnabled(Button button) {
		try {
			new WaitUntil(new ControlIsEnabled(button), TimePeriod.getCustom(5));
			return true;
		} catch (WaitTimeoutExpiredException ex) {
			return false;
		}
	}

	private void next() {
		new NextButton().click();
		TestUtils.acceptSSLCertificate();

		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new ControlIsEnabled(new BackButton()));
	}

	private void openNewServerAdapterWizard() {
		NewServerWizard dialog = new NewServerWizard();
		dialog.open();
		NewServerWizardPage page = new NewServerWizardPage(dialog);

		dialog.open();
		new WaitUntil(new JobIsKilled("Refreshing server adapter list"), TimePeriod.LONG, false);
		page.selectType(OpenShiftLabel.Others.OS3_SERVER_ADAPTER);
		dialog.next();
	}

	@After
	public void closeShell() {
		Shell shell = ShellLookup.getInstance().getShell(OpenShiftLabel.Shell.ADAPTER);
		if (shell != null) {
			new DefaultShell(OpenShiftLabel.Shell.ADAPTER);
			new CancelButton().click();
			new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.ADAPTER));
		}

		new WaitWhile(new JobIsRunning());
	}
}
