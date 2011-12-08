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
* frentix GmbH, Switzerland, http://www.frentix.com
* <p>
*/
package org.olat.ims.cp.ui;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.vfs.AbstractVirtualContainer;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSStatus;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.core.util.vfs.filters.VFSItemFilter;
import org.olat.ims.cp.ContentPackage;

/**
 * 
 * Description:<br>
 * this the root of the VFS for the CP with three
 * containers as children: CP Pages, media (files - html...) and raw files
 * 
 * <P>
 * Initial Date:  4 mai 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
//fxdiff FXOLAT-125: virtual file system for CP
public class VFSRootCPContainer extends AbstractVirtualContainer implements VFSContainer {
	
	private VFSContainer rootContainer;

	private VFSSecurityCallback secCallback;

	private final List<VFSItem> roots = new ArrayList<VFSItem>();
	
	public VFSRootCPContainer(String name, ContentPackage cp, VFSContainer rootContainer, Translator translator) {
		super(name);
		this.rootContainer = rootContainer;
		
		String contentTitle = translator.translate("cpfileuploadcontroller.pages");
		VFSCPContainer cpContainer = new VFSCPContainer(contentTitle, cp);
		roots.add(cpContainer);
		
		String mediaTitle = translator.translate("cpfileuploadcontroller.media");
		VFSContainer mediaContainer = new VFSMediaFilesContainer(mediaTitle, cloneContainer(rootContainer));
		mediaContainer.setDefaultItemFilter(new VFSMediaFilter(true));
		roots.add(mediaContainer);

		String rawTitle = translator.translate("cpfileuploadcontroller.raw");
		VFSContainer rawContainer = new VFSMediaFilesContainer(rawTitle, cloneContainer(rootContainer));
		rawContainer.setDefaultItemFilter(new VFSMediaFilter(false));
		roots.add(rawContainer);
	}
	
	private VFSContainer cloneContainer(VFSContainer container) {
		if(container instanceof OlatRootFolderImpl) {
			OlatRootFolderImpl folder = (OlatRootFolderImpl)container;
			return new OlatRootFolderImpl(folder.getRelPath(), folder.getParentContainer());
		} else if(container instanceof LocalFolderImpl) {
			LocalFolderImpl folder = (LocalFolderImpl)container;
			LocalFolderImpl clone = new LocalFolderImpl(folder.getBasefile());
			clone.setParentContainer(folder.getParentContainer());
			return clone;
		}
		return null;
	}
	
	@Override
	public boolean isSame(VFSItem vfsItem) {
		if(this == vfsItem) {
			return true;
		}
		for(VFSItem root:roots) {
			if(root.isSame(vfsItem)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public VFSItem resolve(String path) {
		for(VFSItem root:roots) {
			if(root instanceof VFSContainer) {
				VFSContainer container = (VFSContainer)root;
				VFSItem item = container.resolve(path);
				if(item != null) {
					return item;
				}
			}
		}
		return null;
	}

	@Override
	public List<VFSItem> getItems() {
		return roots;
	}

	@Override
	public List<VFSItem> getItems(VFSItemFilter filter) {
		return getItems();
	}

	@Override
	public VFSContainer createChildContainer(String name) {
		return rootContainer.createChildContainer(name);
	}

	@Override
	public VFSLeaf createChildLeaf(String name) {
		return rootContainer.createChildLeaf(name);
	}

	@Override
	public VFSContainer getParentContainer() {
		return null;
	}

	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		//
	}

	@Override
	public VFSStatus canWrite() {
		return VFSConstants.YES;
	}

	@Override
	public VFSSecurityCallback getLocalSecurityCallback() {
		return secCallback;
	}

	@Override
	public void setLocalSecurityCallback(VFSSecurityCallback secCallback) {
		this.secCallback = secCallback;
	}
}
