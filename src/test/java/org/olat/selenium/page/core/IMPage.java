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
package org.olat.selenium.page.core;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.graphene.Graphene;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Predicate;

/**
 * Drive the chat / instant messaging from OpenOLAT
 * 
 * Initial date: 07.07.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class IMPage {
	
	private WebDriver browser;
	
	public IMPage(WebDriver browser) {
		this.browser = browser;
	}
	
	/**
	 * Open and join the group chat
	 * @return
	 */
	public IMPage openGroupChat() {
		By openBy = By.className("o_sel_im_open_tool_chat");
		OOGraphene.waitElement(openBy);
		WebElement openButton = browser.findElement(openBy);
		openButton.click();
		OOGraphene.waitBusy();
		
		By imModalBy = By.className("o_im_chat");
		OOGraphene.waitElement(imModalBy);
		return this;
	}
	
	/**
	 * Send a message (Warning: character as - are replace with emoticons)
	 * @param message
	 * @return
	 */
	public IMPage sendMessage(String message) {
		By messageBy = By.cssSelector(".o_im_chat_form input[type='text']");
		WebElement messageEl = browser.findElement(messageBy);
		messageEl.sendKeys(message);
		
		By sendMessageBy = By.cssSelector(".o_im_chat_form a.btn.btn-default");
		WebElement sendMessageButton = browser.findElement(sendMessageBy);
		sendMessageButton.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Check that a message appears in the chat history. The timeout
	 * is set to 10 seconds.
	 * @param message
	 * @return
	 */
	public IMPage assertOnMessage(String message) {
		Graphene.waitModel().withTimeout(10, TimeUnit.SECONDS).until(new MessagePredicate(message));
		return this;
	}
	
	private static class MessagePredicate implements Predicate<WebDriver> {
		
		private static final By historyBy = By.cssSelector(".o_im_chat_history .o_im_body");
		
		private final String message;
		
		public MessagePredicate(String message) {
			this.message = message;
		}

		@Override
		public boolean apply(WebDriver browser) {
			boolean found = false;
			List<WebElement> history = browser.findElements(historyBy);
			for(WebElement m:history) {
				if(m.getText().contains(message)) {
					found = true;
				}
			}
			return found;
		}
	}
}
