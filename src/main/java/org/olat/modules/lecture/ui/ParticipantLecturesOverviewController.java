/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.lecture.ui;

import java.util.List;

import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.BreadcrumbPanelAware;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.modules.lecture.LectureModule;
import org.olat.modules.lecture.LectureService;
import org.olat.modules.lecture.model.LectureBlockStatistics;
import org.olat.modules.lecture.ui.ParticipantLecturesDataModel.LecturesCols;
import org.olat.modules.lecture.ui.component.LectureStatisticsCellRenderer;
import org.olat.modules.lecture.ui.component.PercentCellRenderer;
import org.olat.modules.lecture.ui.component.RateWarningCellRenderer;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * 
 * Initial date: 17 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ParticipantLecturesOverviewController extends FormBasicController implements BreadcrumbPanelAware {
	
	private FlexiTableElement tableEl;
	private BreadcrumbPanel stackPanel;
	private ParticipantLecturesDataModel tableModel;
	
	private final boolean withPrint;
	private final boolean withSelect;
	private final Identity assessedIdentity;
	private ParticipantLectureBlocksController lectureBlocksCtrl;
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private LectureModule lectureModule;
	@Autowired
	private LectureService lectureService;
	@Autowired
	private RepositoryService repositoryService;
	
	public ParticipantLecturesOverviewController(UserRequest ureq, WindowControl wControl) {
		this(ureq, wControl, ureq.getIdentity(), true, true);
	}
	
	public ParticipantLecturesOverviewController(UserRequest ureq, WindowControl wControl,
			Identity assessedIdentity, boolean withPrint, boolean withSelect) {
		super(ureq, wControl, "participant_overview");
		this.withPrint = withPrint;
		this.withSelect = withSelect;
		this.assessedIdentity = assessedIdentity;
		initForm(ureq);
		loadModel();
	}
	
	@Override
	public void setBreadcrumbPanel(BreadcrumbPanel stackPanel) {
		this.stackPanel = stackPanel;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(withPrint && formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("winid", "w" + layoutCont.getFormItemComponent().getDispatchID());
			layoutCont.getFormItemComponent().addListener(this);
			layoutCont.getFormItemComponent().contextPut("withPrint", Boolean.TRUE);
			setFormTitle("menu.my.lectures.alt");
		} else {
			setFormTitle("lectures.print.title", new String[]{
					StringHelper.escapeHtml(userManager.getUserDisplayName(assessedIdentity))
			});
		}
	
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.entry));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.plannedLectures));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.attendedLectures));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.absentLectures));
		if(lectureModule.isAuthorizedAbsenceEnabled()) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.authorizedAbsentLectures));
		}
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.progress, new LectureStatisticsCellRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.rateWarning, new RateWarningCellRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LecturesCols.rate, new PercentCellRenderer()));
		if(withSelect) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel("select", translate("select"), "select"));
		}
		
		tableModel = new ParticipantLecturesDataModel(columnsModel, getLocale()); 
		int paging = withPrint ? 20 : -1;
		tableEl = uifactory.addTableElement(getWindowControl(), "table", tableModel, paging, false, getTranslator(), formLayout);
		//TODO absence tableEl.setAndLoadPersistedPreferences(ureq, "participant-lectures");
		tableEl.setCustomizeColumns(false);
		tableEl.setEmtpyTableMessageKey("empty.lectures.list");
	}
	
	private void loadModel() {
		List<LectureBlockStatistics> statistics = lectureService.getParticipantLecturesStatistics(assessedIdentity);
		tableModel.setObjects(statistics);
		tableEl.reset(true, true, true);
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(flc.getFormItemComponent() == source && "print".equals(event.getCommand())) {
			doPrint(ureq);
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == tableEl) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				LectureBlockStatistics row = tableModel.getObject(se.getIndex());
				if("select".equals(cmd)) {
					doSelect(ureq, row);
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	private void doSelect(UserRequest ureq, LectureBlockStatistics statistics) {
		removeAsListenerAndDispose(lectureBlocksCtrl);
		
		RepositoryEntry entry = repositoryService.loadByKey(statistics.getRepoKey());
		lectureBlocksCtrl = new ParticipantLectureBlocksController(ureq, getWindowControl(), entry);
		listenTo(lectureBlocksCtrl);
		stackPanel.pushController(entry.getDisplayname(), lectureBlocksCtrl);
	}
	
	private void doPrint(UserRequest ureq) {
		ControllerCreator printControllerCreator = new ControllerCreator() {
			@Override
			public Controller createController(UserRequest lureq, WindowControl lwControl) {
				lwControl.getWindowBackOffice().getChiefController().addBodyCssClass("o_lectures_print");
				Controller printCtrl = new ParticipantLecturesOverviewController(lureq, lwControl, assessedIdentity, false, false);
				listenTo(printCtrl);
				return printCtrl;
			}					
		};
		ControllerCreator layoutCtrlr = BaseFullWebappPopupLayoutFactory.createPrintPopupLayout(printControllerCreator);
		openInNewBrowserWindow(ureq, layoutCtrlr);
	}
}
