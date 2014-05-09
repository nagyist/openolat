/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
* Initial code contributed and copyrighted by<br>
* JGS goodsolutions GmbH, http://www.goodsolutions.ch
* <p>
*/
package org.olat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.core.commons.chiefcontrollers.BaseChiefController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.core.id.context.TabContext;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.LogDelegator;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * input: e.g. [repoentry:123] or [repoentry:123][CourseNode:456] or ...
 * 
 * 
 * <P>
 * Initial Date: 16.06.2006 <br>
 * 
 * @author Felix Jost
 */
public class NewControllerFactory extends LogDelegator {
	private static NewControllerFactory INSTANCE = new NewControllerFactory();
	// map of controller creators, setted by Spring configuration
	private Map<String, ContextEntryControllerCreator> contextEntryControllerCreators = new HashMap<String, ContextEntryControllerCreator>();

	/**
	 * Get an instance of the new controller factory
	 * 
	 * @return
	 */
	public static NewControllerFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Singleton constructor
	 */
	private NewControllerFactory() {
	//
	}
	
	public static String translateResourceableTypeName(String resourceableTypeName, Locale locale) {
		Translator trans = Util.createPackageTranslator(BaseChiefController.class, locale);
		return trans.translate(resourceableTypeName);
	}

	/**
	 * Add a context entry controller creator for a specific key. This is used to
	 * add new creators at runtime, e.g. from a self contained module. It is
	 * allowed to overwrite existing ContextEntryControllerCreator. Use the
	 * canLaunch() method to check if for a certain key something is already
	 * defined.
	 * 
	 * @param key
	 * @param controllerCreator
	 */
	public synchronized void addContextEntryControllerCreator(String key, ContextEntryControllerCreator controllerCreator) {
		ContextEntryControllerCreator oldCreator = contextEntryControllerCreators.get(key);
		contextEntryControllerCreators.put(key, controllerCreator);
		// Add config logging to console
		logInfo("Adding context entry controller creator for key::" + key + " and value::" + controllerCreator.getClass().getCanonicalName() 
				+ (oldCreator == null ? "" : " replaceing existing controller creator ::" + oldCreator.getClass().getCanonicalName()), null);
	}

	/**
	 * Check if a context entry controller creator is available for the given key
	 * 
	 * @param key
	 * @return true: key is known; false: key can not be used
	 */
	public boolean canLaunch(String key) {
		return contextEntryControllerCreators.containsKey(key);
	}

	/**
	 * Check first context entry can be launched
	 * a further check is mostly not possible, as it gets validated through the BC-stack while building the controller-chain
	 * 
	 * return true: if this will be launchable at least for the first step.
	 */
	public boolean validateCEWithContextControllerCreator(final UserRequest ureq, final WindowControl wControl, ContextEntry ce){
		String firstType = ce.getOLATResourceable().getResourceableTypeName();
		if (canLaunch(firstType)){
			return contextEntryControllerCreators.get(firstType).validateContextEntryAndShowError(ce, ureq, wControl);
		}
		return false;
	}
	
	/**
	 * Launch a controller in a tab or a site with the business path
	 * @param businessPath
	 * @param ureq
	 * @param origControl
	 */
	public boolean launch(String businessPath, UserRequest ureq, WindowControl origControl) {
		BusinessControl bc = BusinessControlFactory.getInstance().createFromString(businessPath);
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, origControl);
		return launch(ureq, bwControl);
	}
	
	private ContextEntryControllerCreator getContextEntryControllerCreator(String type) {
		ContextEntryControllerCreator typeHandler = contextEntryControllerCreators.get(type);
		if(typeHandler != null) {
			return typeHandler.clone();
		}
		return null;
	}

	/**
	 * Launch a controller in a tab or site in the given window from a user
	 * request url
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public boolean launch(UserRequest ureq, WindowControl wControl) {
		BusinessControl bc = wControl.getBusinessControl();
		ContextEntry mainCe = bc.popLauncherContextEntry();
		OLATResourceable ores = mainCe.getOLATResourceable();

		// Check for RepositoryEntry resource
		boolean ceConsumed = false;
		RepositoryEntry re = null;
		if (ores.getResourceableTypeName().equals(OresHelper.calculateTypeName(RepositoryEntry.class))) {
			if(ores instanceof RepositoryEntry) {
				re = (RepositoryEntry)ores;
				ores = re.getOlatResource();
				ceConsumed = true;
			} else {
				// It is a repository-entry => get OLATResourceable from RepositoryEntry
				RepositoryManager repom = RepositoryManager.getInstance();
				re = repom.lookupRepositoryEntry(ores.getResourceableId());
				if (re != null){
					ores = re.getOlatResource();
					ceConsumed = true;
					mainCe.upgradeOLATResourceable(re);
				}
			}
		}

		// was brasato:: DTabs dts = wControl.getDTabs();
		UserSession usess = ureq.getUserSession();
		Window window = Windows.getWindows(usess).getWindow(ureq);

		if (window == null) {
			logDebug("Found no window for jumpin => take WindowBackOffice", null);
			window = wControl.getWindowBackOffice().getWindow();
		}
		DTabs dts = window.getDTabs();
		DTab dt = dts.getDTab(ores);
		if (dt != null) {
			// tab already open => close it
			dts.removeDTab(ureq, dt);// disposes also dt and controllers
		}

		String firstType = mainCe.getOLATResourceable().getResourceableTypeName();
		// String firstTypeId = ClassToId.getInstance().lookup() BusinessGroup
		ContextEntryControllerCreator typeHandler = getContextEntryControllerCreator(firstType);
		if (typeHandler == null) {
			logWarn("Cannot found an handler for context entry: " + mainCe, null);
			return false;//simply return and don't throw a red screen
		}
		if (!typeHandler.validateContextEntryAndShowError(mainCe, ureq, wControl)){
			//simply return and don't throw a red screen
			return false;
		}
		
		List<ContextEntry> entries = new ArrayList<ContextEntry>(5);
		while(bc.hasContextEntry()) {
			entries.add(bc.popLauncherContextEntry());
		}
		List<ContextEntry> ces = new ArrayList<ContextEntry>(entries.size() + 1);
		ces.add(mainCe);
		if(entries.size() > 0) {
			ces.addAll(entries);
		}

		TabContext context = typeHandler.getTabContext(ureq, ores, mainCe, entries);
		String siteClassName = typeHandler.getSiteClassName(ces, ureq);	
		// open in existing site
		if (siteClassName != null) {
			dts.activateStatic(ureq, siteClassName, context.getContext());
			return true;
		} else {
			// or create new tab
			//String tabName = typeHandler.getTabName(mainCe, ureq);
			// create and add Tab
			dt = dts.createDTab(context.getTabResource(), re, context.getName());
			if (dt == null) {
				// user error message is generated in BaseFullWebappController, nothing to do here
				return false;
			} else {
				WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, dt.getWindowControl());
				usess.addToHistory(ureq, bc);

				Controller launchC = typeHandler.createController(ces, ureq, bwControl);
				if (launchC == null) {
					throw new AssertException("ControllerFactory could not create a controller to be launched. Please validate businesspath " 
							+ bc.getAsString() + " for type " + typeHandler.getClass().getName() + " in advance with validateContextEntryAndShowError().");
				}

				dt.setController(launchC);
				if(dts.addDTab(ureq, dt)) {
					dts.activate(ureq, dt, context.getContext()); // null: do not activate to a certain view
					return true;
				} else {
					return false;
				}
			}
		}
	}
}