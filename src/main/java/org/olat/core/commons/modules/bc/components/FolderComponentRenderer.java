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
*/ 

package org.olat.core.commons.modules.bc.components;

import java.util.Iterator;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.commands.FolderCommandFactory;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.winmgr.AJAXFlags;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;

/**
 * @author Felix Jost
 */
public class FolderComponentRenderer extends DefaultComponentRenderer {

	private final ListRenderer listRenderer;
	private final CrumbRenderer crumbRenderer;

	/**
	 * Constructor for TableRenderer. Singleton and must be reentrant
	 * There must be an empty contructor for the Class.forName() call
	 */
	public FolderComponentRenderer() {
		super();
		listRenderer = new ListRenderer();
		crumbRenderer = new CrumbRenderer();
	}

	/**
	 * @see org.olat.core.gui.render.ui.ComponentRenderer#render(org.olat.core.gui.render.Renderer, org.olat.core.gui.render.StringOutput, org.olat.core.gui.components.Component, org.olat.core.gui.render.URLBuilder, org.olat.core.gui.translator.Translator, org.olat.core.gui.render.RenderResult, java.lang.String[])
	 */
	@Override
	public void render(Renderer renderer, StringOutput target, Component source, URLBuilder ubu, Translator translator, RenderResult renderResult, String[] args) {
		FolderComponent fc = (FolderComponent) source;
		// is called for the current inline html
		int renderType = 0;
		if (args != null && args.length > 0) {
			if (args[0].equals("list")) renderType = 0;
			if (args[0].equals("crumb")) renderType = 1;
		}
		// get ajax flag for link rendering
		boolean iframePostEnabled = renderer.getGlobalSettings().getAjaxFlags().isIframePostEnabled();
		
		if (renderType == 1) {
			crumbRenderer.render(fc, target, ubu, iframePostEnabled);
		} else {
			renderList(target, fc, ubu, translator, iframePostEnabled);
		}
	}

	private void renderList(StringOutput target, FolderComponent fc, URLBuilder ubu, Translator translator, boolean iframePostEnabled) {
		
		VFSContainer currentContainer = fc.getCurrentContainer();
		boolean canWrite = currentContainer.canWrite() == VFSConstants.YES;
		boolean canCreateFolder = true;
		if(currentContainer.getLocalSecurityCallback() != null && !currentContainer.getLocalSecurityCallback().canCreateFolder()) {
			canCreateFolder = false;
		}
		
		boolean canDelete = false;
		boolean canVersion = FolderConfig.versionsEnabled(fc.getCurrentContainer());
		boolean canMail = fc.isCanMail();
		for (Iterator<VFSItem> iter = fc.getCurrentContainerChildren().iterator(); iter.hasNext();) {
			VFSItem child = iter.next();
			if (child.canDelete() == VFSConstants.YES) {
				canDelete = true;
				break;
			}
		}
		
		String formName = "folder" + CodeHelper.getRAMUniqueID();
		target.append("<form  method=\"post\" id=\"").append(formName).append("\" action=\"");
		ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] {FolderRunController.FORM_ACTION }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		target.append("\" onsubmit=\"if ( b_briefcase_isChecked('").append(formName)
			.append("', '").append(Formatter.escapeSingleAndDoubleQuotes(StringHelper.escapeHtml(translator.translate("alert")).toString())) 
			.append("')) { if(o_info.linkbusy) return false; else o_beforeserver(); return true; } else {return false; }\"");
		if (iframePostEnabled) { // add ajax iframe target
			StringOutput so = new StringOutput();
			ubu.appendTarget(so);
			target.append(so.toString());
		}
		target.append(">");

		target.append("<div class=\"o_bc_createactions clearfix\"><ul class='nav navbar-nav navbar-right'>");
		if (canWrite) {
			// add folder actions: upload file, create new folder, create new file

			if(canVersion) {
			// deleted files
				target.append("<li><a class=\"o_bc_deletedfiles\" href=\"");
				ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] { "dfiles"  }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
				target.append("\"");
				if (iframePostEnabled) { // add ajax iframe target
					StringOutput so = new StringOutput();
					ubu.appendTarget(so);
					target.append(so.toString());
				}
				target.append("><i class='o_icon o_icon_recycle o_icon-fw'></i> ");
				target.append(translator.translate("dfiles"));
				target.append("</a></li>");
			}
			
			if(canWrite) {
				if(fc.getExternContainerForCopy() != null && (fc.getExternContainerForCopy().getLocalSecurityCallback() == null ||
						fc.getExternContainerForCopy().getLocalSecurityCallback().canCopy())) {
					//option copy file
					target.append("<li><a class=\"o_bc_copy\" href=\"");
					ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] { "copyfile"  }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
					target.append("\"");
					if (iframePostEnabled) { // add ajax iframe target
						StringOutput so = new StringOutput();
						ubu.appendTarget(so);
						target.append(so.toString());
					}
					target.append("><i class='o_icon o_icon_copy o_icon-fw'></i> ");
					target.append(translator.translate("copyfile"));
					target.append("</a></li>");
				}
				
				// option upload	
				target.append("<li><a class='o_bc_upload' href=\"");
				ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] { "ul"  }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
				target.append("\"");
				if (iframePostEnabled) { // add ajax iframe target
					StringOutput so = new StringOutput();
					ubu.appendTarget(so);
					target.append(so.toString());
				}
				target.append("><i class='o_icon o_icon_upload o_icon-fw'></i> ");
				target.append(translator.translate("ul"));			
				target.append("</a></li>");
	
				if(canCreateFolder) {
					// option new folder
					target.append("<li><a class=\"b_bc_newfolder\" href=\"");
					ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] { "cf"  }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
					target.append("\"");
					if (iframePostEnabled) { // add ajax iframe target
						StringOutput so = new StringOutput();
						ubu.appendTarget(so);
						target.append(so.toString());
					}
					target.append("><i class='o_icon o_icon_new_folder o_icon-fw'></i> ");
					target.append(translator.translate("cf"));
					target.append("</a></li>");
				}
	
				// option new file
				target.append("<li><a class=\"b_bc_newfile\" href=\"");
				ubu.buildURI(target, new String[] { VelocityContainer.COMMAND_ID }, new String[] { "cfile"  }, iframePostEnabled ? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
				target.append("\"");
				if (iframePostEnabled) { // add ajax iframe target
					StringOutput so = new StringOutput();
					ubu.appendTarget(so);
					target.append(so.toString());
				}
				target.append("><i class='o_icon o_icon_new_document o_icon-fw'></i> ");
				target.append(translator.translate("cfile"));
				target.append("</a></li>");
			}
		}
		
		//placeholder for the search
		target.append("</ul></div>");
		
		// add current file bread crumb path
		crumbRenderer.render(fc, target, ubu, iframePostEnabled);			

		// add file listing for current folder
		target.append("<div class='o_table_wrapper'>");
		listRenderer.render(fc, target, ubu, translator, iframePostEnabled);

		if (fc.getCurrentContainerChildren().size() > 0) {
			target.append("<div class='o_table_footer'>")
			      .append("<div class=\"o_table_checkall input-sm\">")
			      .append("<label class='checkbox-inline'><a href=\"#\" onclick=\"javascript:b_briefcase_toggleCheck('").append(formName).append("', true)\">")
			      .append("<input type=\"checkbox\" checked=\"checked\" disabled=\"disabled\" />")
			      .append(translator.translate("checkall"))
			      .append("</a></label> <label class='checkbox-inline'><a href=\"#\" onclick=\"javascript:b_briefcase_toggleCheck('").append(formName).append("', false)\">")
			      .append("<input type=\"checkbox\" disabled=\"disabled\" />")
			      .append(translator.translate("uncheckall"))
			      .append("</a></label></div>")
			      .append("<div class='o_table_buttons'>");
			
			if(canMail) {
				target.append("<input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_MAIL);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("send")));
				target.append("\" />");
			}
			
			target.append("<input type=\"submit\" class='btn btn-default' name=\"")
			      .append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_DOWNLOAD_ZIP)
			      .append("\" value=\"")
			      .append(StringHelper.escapeHtml(translator.translate("download")))
			      .append("\" />");
			
			if (canDelete) {
				// delete
				target.append("<input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_DEL);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("del")));
				target.append("\" />");
			}

			if (canWrite) {
				// move
				target.append("<input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_MOVE);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("move")));
				// copy
				target.append("\" /><input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_COPY);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("copy")));
				target.append("\" />");
				// zip
				target.append("<input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_ZIP);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("zip")));
				//unzip
				target.append("\" /><input type=\"submit\" class='btn btn-default' name=\"");
				target.append(FolderRunController.ACTION_PRE).append(FolderCommandFactory.COMMAND_UNZIP);
				target.append("\" value=\"");
				target.append(StringHelper.escapeHtml(translator.translate("unzip")));
				target.append("\" />");				
			}
			target.append("</div></div>");
		}
		target.append("</div></form>");
	}
}
