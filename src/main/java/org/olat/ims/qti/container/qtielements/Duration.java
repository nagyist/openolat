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
*/

package org.olat.ims.qti.container.qtielements;

import org.dom4j.Element;
import org.olat.ims.qti.process.QTIHelper;

/**
 * Initial Date:  15.12.2004
 *
 * @author Mike Stock
 */
public class Duration extends GenericQTIElement {

	private static final long serialVersionUID = -5415237835782260846L;

	public static final String xmlClass = "duration";

	private long duration;
	
	/**
	 * @param el_element
	 */
	public Duration(Element el_element) {
		super(el_element);
		String sDuration = el_element.getTextTrim();
		duration = QTIHelper.parseISODuration(sDuration);
	}
	
	public long getDuration() {
		return duration;
	}
}
