(function() {
	tinymce.create('org.olat.ims.qti21.ui.editor', {

		/**
		 * Returns information about the plugin as a name/value array.
		 * The current keys are longname, author, authorurl, infourl and version.
		 *
		 * @returns Name/value array containing information about the plugin.
		 * @type Array 
		 */
		getInfo : function() {
			return {
				longname : 'OpenOLATQTI',
				author : 'frentix GmbH',
				authorurl : 'http://www.frentix.com',
				infourl : 'http://www.frentix.com',
				version : '1.2.3'
			};
		},

		/**
		 * Not used, adButton used instead
		 */
		createControl : function(n, cm) {
			return null;
		},
	
		/**
		 * Initializes the plugin, this will be executed after the plugin has been created.
		 * This call is done before the editor instance has finished it's initialization so use the onInit event
		 * of the editor instance to intercept that event.
		 *
		 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
		 * @param {string} url Absolute URL to where the plugin is located.
		 */
		init : function(ed, url) {
			
			var $ = ed.$, selection = ed.selection;
			var cachedTrans, cachedCoreTrans;
			var cachedHelp;
			var lastSelectedGap, lastSelectedHottext;
			
			// Load the OLAT translator.
			function translator() {	
				if(cachedTrans) return cachedTrans;
				var mainWin = o_getMainWin();
				if (mainWin) {
					cachedTrans = jQuery(document).ooTranslator().getTranslator(mainWin.o_info.locale, 'org.olat.ims.qti21.ui.editor');
				} else {
					cachedTrans = {	translate : function(key) { return key; } }
				}
				return cachedTrans;
			}
			function coreTranslator() {	
				if(cachedCoreTrans) return cachedCoreTrans;
				var mainWin = o_getMainWin();
				if (mainWin) {
					cachedCoreTrans = jQuery(document).ooTranslator().getTranslator(mainWin.o_info.locale, 'org.olat.core');
				} else {
					cachedCoreTrans = {	translate : function(key) { return key; } }
				}
				return cachedCoreTrans;
			}
	
			function showTextDialog(e) {
				showDialog(e, "string")
			}
			
			function showNumericalDialog(e) {
				showDialog(e, "float")
			}

			function showDialog(e, gapType) {
				var ffxhrevent = ed.getParam("ffxhrevent");
				if(typeof lastSelectedGap != 'undefined') {
					var textEntryEl = jQuery(lastSelectedGap).closest("span[data-qti='textentryinteraction']");
					var responseIdentifier = textEntryEl.attr('data-qti-response-identifier');
					var solution = jQuery(textEntryEl).children().html();
					var emptySolution = (solution == "" || solution == "&nbsp;" ? "true" : "false");
					o_ffXHREvent(ffxhrevent.formNam, ffxhrevent.dispIdField, ffxhrevent.dispId, ffxhrevent.eventIdField, 2, false, false, false,
							'cmd', 'gapentry', 'responseIdentifier', responseIdentifier, 'selectedText', solution, 'emptySolution', emptySolution, 'newEntry', false);
				} else {
					var counter = 1;
					var newSelectedText = ed.selection.getContent({format: 'text'})
					
					tinymce.each(ed.dom.select("span[data-qti]"), function(node) {
						var identifier = jQuery(node).attr('data-qti-response-identifier');
						if(identifier.lastIndexOf("RESPONSE_", 0) == 0) {
							var id = parseInt(identifier.substring(9, identifier.length));
							if(id > counter) {
								counter = id;
							}	
						}
				    });
					
					var responseIdentifier = "RESPONSE_" + (counter + 1);
					if(typeof newSelectedText === "undefined" || newSelectedText.length == 0) {
						newSelectedText = "gap";
					}
					
					var placeholder = createTextEntryPlaceholder(responseIdentifier, newSelectedText, 'textentryinteraction', gapType);
					var holderHtml = new tinymce.html.Serializer().serialize(placeholder);
					ed.insertContent(holderHtml);

					o_ffXHREvent(ffxhrevent.formNam, ffxhrevent.dispIdField, ffxhrevent.dispId, ffxhrevent.eventIdField, 2, false, false, false,
							'cmd', 'gapentry', 'responseIdentifier', responseIdentifier, 'newEntry', true, 'selectedText', newSelectedText, 'gapType', gapType);
				}
				ed.setDirty(true);
			}
			
			function guid() {
				function s4() {
				    return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
				}
				return s4() + s4() + s4() + s4() + s4() + s4() + s4();
			}
			
			function createHottext(e) {
				var responseIdentifier;
				if(typeof lastSelectedHottext != 'undefined') {
					responseIdentifier = jQuery(lastSelectedHottext).data('data-identifier')
				} else {
					var counter = 1;
					var selectedText = ed.selection.getContent({format: 'text'});
					var preSelect = false;
					if(selectedText == null || selectedText.length == 0) {
						selectedText = "text";
						preSelect = true;
					}

					var identifier = "ht" + guid();
					var placeholder = createHottextPlaceholder(identifier, selectedText, false, 'hottext');
					var holderHtml = new tinymce.html.Serializer().serialize(placeholder);
					ed.insertContent(holderHtml);
					
					if(preSelect) {
						var selectedNode = ed.dom.select("span[data-qti-identifier=" + identifier + "] span[contenteditable=true]");
						ed.selection.select(selectedNode[0], true);
					}
					
					jQuery("span.hottext[data-qti-identifier='" + identifier + "'] a", ed.getBody()).each(function(index, el) {
						correctHottextEvent(el);
					});
				}
			}

			ed.addButton('olatqtifibtext', {
				title : translator().translate('new.fib'),
				icon : 'gaptext',
				stateSelector: ['span[data-qti-gap-type=string]'],
				onclick: showTextDialog
			});
			
			ed.addButton('olatqtifibnumerical', {
				title : translator().translate('new.fib') + ' Numerical',
				icon : 'gapnumerical',
				stateSelector: ['span[data-qti-gap-type=float]'],
				onclick: showNumericalDialog
			});

			ed.addButton('olatqtihottext', {
				title : translator().translate('new.hottext'),
				icon : 'hottext',
				stateSelector: ['span[data-qti=hottext]'],
				onclick: createHottext
			});
			
			ed.addButton('editgap', {
				title: 'edit',
				icon: 'edit',
				onclick: showDialog
			});
			
			ed.addMenuItem('olatqtifibtext', {
				text : translator().translate('new.fib'),
				icon : 'gapnumerical',
				stateSelector: ['span[data-qti-gap-type=string]'],
				onclick: showNumericalDialog
			});
			
			ed.addMenuItem('olatqtifibnumerical', {
				text : translator().translate('new.fib.numerical') + ' Numerical',
				icon : 'gaptext',
				stateSelector: ['span[data-qti-gap-type=float]'],
				onclick: showTextDialog
			});
			
			ed.addMenuItem('olatqtihottext', {
				text : translator().translate('new.hottext'),
				icon : 'hottext',
				stateSelector: ['span[data-qti=hottext]'],
				onclick: createHottext
			});
			
			ed.on('NodeChange', function(e) {
				if (lastSelectedGap && lastSelectedGap.id != e.element.src) {
					lastSelectedGap = undefined;
				}
				if (lastSelectedHottext && lastSelectedHottext.id != e.element.src) {
					lastSelectedHottext = undefined;
				}
				
				if (ed.dom.is(e.element, 'span[data-qti=textentryinteraction]')) {
					lastSelectedGap = e.element;
				} else if (jQuery(e.element).parent("span[data-qti='textentryinteraction']").size() > 0) {
					lastSelectedGap = e.element;
				}
				
				if (jQuery(e.element).parent('span.hottext').size() > 0) {
					lastSelectedHottext = e.element;
				}
				
				jQuery(e.element).parent("span[data-qti-gap-type=float]").each(function(index, el) {
					if(jQuery(e.element).prop("tagName").toLowerCase() == "span") {
						var solution = jQuery(e.element).text();
						if(!jQuery.isNumeric(solution)) {
							jQuery(el).addClass('error');
						} else {
							jQuery(el).removeClass('error');
						}
					}
				});

				jQuery("span.hottext[data-copy='needlistener'] a", e.element).each(function(index, el) {
					var hottext = jQuery(el).parent("span.hottext").attr('data-copy','blues');
					var ev = jQuery._data(el, 'events');
					if(ev && ev.click) {
						//double check 
					} else {
						correctHottextEvent(el);
					}
				});
				
				jQuery("span.textentryinteraction[data-copy='needlistener'] a", e.element).each(function(index, el) {
					var textEntry = jQuery(el).parent("span.textentryinteraction");
					var hottext = textEntry.attr('data-copy','blues');
					var ev = jQuery._data(el, 'events');
					if(ev && ev.click) {
						//double check 
					} else {
						textEntryEvent(textEntry);
					}
				});
			});
			
			function createTextEntryPlaceholder(responseIdentifier, content, interaction, gapType) {
				var placeholder = new tinymce.html.Node('span', 1);
				placeholder.attr({
					"data-qti": interaction,
					"data-qti-response-identifier": responseIdentifier,
					"data-qti-solution" : content,
					"data-qti-gap-type": gapType,
					"data-mce-placeholder": "",
					"data-textentryinteraction": "empty",
					"class": interaction,
					"contenteditable": "false"
				});
				
				var readonly = ed.getParam("readonly");
	            var editable = readonly == "1" ? "false" : "true";
				var contentholder = new tinymce.html.Node('span', 1);
	            contentholder.attr({ "contenteditable": editable });
	            var textNode = new tinymce.html.Node('#text', 3);
	            textNode.raw = true;
	            textNode.value = content;
	            contentholder.append(textNode);
	            placeholder.append(contentholder);

	            var aHolder = new tinymce.html.Node('a', 1);
	            aHolder.attr({ "contenteditable": "false" });
	            var aTextHolder = new tinymce.html.Node('i', 1);
	            aTextHolder.attr({ "contenteditable": "false" });
	            var aTextNode = new tinymce.html.Node('#text', 3);
	            aTextNode.raw = true;
	            aTextNode.value = '&nbsp;';
	            aTextHolder.append(aTextNode);
	            aHolder.append(aTextHolder);
	            placeholder.append(aHolder);
				return placeholder;
			}
			
			function createHottextPlaceholder(identifier, content, correct, interaction) {
				var placeholder = new tinymce.html.Node('span', 1);
				placeholder.attr({
					"data-qti": interaction,
					"data-qti-identifier": identifier,
					"class": interaction,
					"contenteditable": "false"
				});

				var readonly = ed.getParam("readonly");
	            var editable = readonly == "1" ? "false" : "true";
	            
	            var checkHolder = new tinymce.html.Node('a', 1);
	            checkHolder.attr({
					"contenteditable": "false",
					"class": (correct ? "checked": "")
	            });
	            var aTextHolder = new tinymce.html.Node('i', 1);
	            aTextHolder.attr({ "contenteditable": "false" });
	            var aTextNode = new tinymce.html.Node('#text', 3);
	            aTextNode.raw = true;
	            aTextNode.value = '&nbsp;';
	            aTextHolder.append(aTextNode);
	            checkHolder.append(aTextHolder);
	            placeholder.append(checkHolder);

	            var contentholder = new tinymce.html.Node('span', 1);
	            contentholder.attr({ "contenteditable": editable });
	            var textNode = new tinymce.html.Node('#text', 3);
	            textNode.raw = true;
	            textNode.value = content;
	            contentholder.append(textNode);
	            placeholder.append(contentholder);
				return placeholder;
			}
			
			function correctHottextEvent(linkEl) {
				jQuery(linkEl).click(function() {
					var ffxhrevent = ed.getParam("ffxhrevent");
					var jLinkEl = jQuery(linkEl);
					var identifier = jLinkEl.parent("span.hottext").data('qti-identifier');
					o_ffXHRNFEvent(ffxhrevent.formNam, ffxhrevent.dispIdField, ffxhrevent.dispId, ffxhrevent.eventIdField, 2,
							'cmd', 'hottext', 'identifier', identifier, 'correct', jLinkEl.hasClass('checked') ? "false" : "true");
					if(jLinkEl.hasClass('checked')) {
						jLinkEl.removeClass('checked');
					} else {
						jLinkEl.addClass('checked');
					}
					ed.setDirty(true);
				});
			}
			
			function textEntryEvent(textEntryEl) {
				jQuery("a", textEntryEl).click(function() {
					var ffxhrevent = ed.getParam("ffxhrevent");
					var responseIdentifier = jQuery(textEntryEl).attr('data-qti-response-identifier');
					var solution = jQuery(textEntryEl).children().html();
					var emptySolution = (solution == "" || solution == "&nbsp;" ? "true" : "false");
					o_ffXHREvent(ffxhrevent.formNam, ffxhrevent.dispIdField, ffxhrevent.dispId, ffxhrevent.eventIdField, 2, false, false, false,
							'cmd', 'gapentry', 'responseIdentifier', responseIdentifier, 'selectedText', solution, 'emptySolution', emptySolution);
					ed.setDirty(true);
				});
			}
			
			function getTextContent(node) {
				var content = '';
				var walker = new tinymce.dom.TreeWalker(node);
				var textNode;
				while ((textNode = walker.next())) {
					if (textNode.type == 3) {
						if(content.length > 0) content += ' ';
						content += textNode.value;
					} else if(textNode.nodeType == 3) {
						if(content.length > 0) content += ' ';
						content += textNode.nodeValue;
					}
				}
				return content;
			}
			
			ed.addCommand('qtiUpdateTextEntry', function (ui, value) {
				var val = eval(value);
				var responseIdentifier = val['responseIdentifier'];
				var solution = val['data-qti-solution'];
				jQuery("span[data-qti-response-identifier='" + responseIdentifier+ "']>span", ed.getBody()).each(function(index, el) {
					jQuery(el).text(solution);
				});
				
				jQuery("span[data-qti-response-identifier='" + responseIdentifier+ "']", ed.getBody()).each(function(index, el) {
					textEntryEvent(jQuery(el));
				});
			});

			// Load Content CSS upon initialization
			ed.on('init', function() {
				if (ed.settings.content_css !== false) {
					ed.dom.loadCSS(url + "/css/content.css");
				}
				jQuery(".textentryinteraction", ed.getBody()).each(function(index, el) {
					textEntryEvent(el);
				});
				
				jQuery("span.hottext a", ed.getBody()).each(function(index, el) {
					correctHottextEvent(el);
				});
			});
			
			ed.on('preInit', function() {
				ed.parser.addNodeFilter('textentryinteraction,hottext', function(nodes) {
					var i = nodes.length, node, placeHolder, videoScript;
					while (i--) {
						node = nodes[i];
						if (node.name == 'textentryinteraction') {
							var responseIdentifier = node.attr('responseidentifier');
							var gapType = node.attr('openolattype');
							var solution = node.attr('data-qti-solution');
							if(typeof solution === "undefined") {
								solution = "&nbsp;";
							}
							if(typeof gapType === "undefined") {
								gapType = "string";
							}
							var placeHolder = createTextEntryPlaceholder(responseIdentifier, solution, 'textentryinteraction', gapType);
							node.replace(placeHolder);
						} else if (node.name == 'hottext') {
			
							var identifier = node.attr('identifier');
							var correctHottexts = ed.getParam("correctHottexts");
							var correct = jQuery.inArray(identifier, correctHottexts) >= 0;
							var content = getTextContent(node);
							var placeHolder = createHottextPlaceholder(identifier, content, correct, 'hottext', 'hottext');
							node.replace(placeHolder);
						}
					}
				});
			});
			
			/** 
             * This onSetContent handler is used to convert the comments to placeholder images (e.g. when loading).
             */
			ed.on('PreProcess', function(e) {
				tinymce.each(ed.dom.select("span[data-qti=textentryinteraction]"), function(node) {
					var identifier = jQuery(node).attr('data-qti-response-identifier');
					var solution = jQuery(node).children().html();
					var textNode = ed.dom.create("textEntryInteraction", {
						responseIdentifier: identifier,
						"data-qti-solution": solution,
						"data-qti-solution-empty": (solution == "" || solution == "&nbsp;" ? "true" : "false")
					});
					
					var alone = node.previousSibling == null && (node.nextSibling == null || jQuery(node.nextSibling).attr("type") == "_moz");
					ed.dom.replace(textNode, node, false);
					if(alone) {
						jQuery(textNode).after(String.fromCharCode(160));
					}
			    });
				
				tinymce.each(ed.dom.select("span[data-qti=hottext]"), function(node) {
					var identifier = jQuery(node).data('qti-identifier');
					var textNode = ed.dom.create("hottext", { identifier: identifier });
					var hottext = jQuery('span[contenteditable="true"]', node).html();
					textNode.textContent = hottext;
					ed.dom.replace(textNode, node, false);
			    });
			});
			
			ed.on('PastePreProcess', function (e) {
				var replace = false;
				var wrappedContent = '<div id="' + guid() + '">' + e.content + '</div>';
				var htmlContent = jQuery(wrappedContent);

				jQuery(htmlContent).find("span[data-qti='hottext']").each(function(index, el) {
					var hotId = 'ht' + guid();
					jQuery(el).attr('data-qti-identifier', hotId);
					jQuery(el).attr('data-copy', 'needlistener');
					replace = true;
				});
				
				jQuery(htmlContent).find("span[data-qti='textentryinteraction']").each(function(index, el) {
					var entryId = 'te' + guid();
					jQuery(el).attr('data-qti-response-identifier', entryId);
					jQuery(el).attr('data-copy', 'needlistener');
					var gapType = jQuery(el).attr("data-qti-gap-type");
					var solution = jQuery(el).attr("data-qti-solution");
					var ffxhrevent = ed.getParam("ffxhrevent");
					o_ffXHRNFEvent(ffxhrevent.formNam, ffxhrevent.dispIdField, ffxhrevent.dispId, ffxhrevent.eventIdField, 2,
							'cmd', 'copy-gapentry', 'responseIdentifier', entryId, 'newEntry', true, 'selectedText', solution, 'gapType', gapType);
					//add it because tiny delete it
					jQuery("a", el).append(jQuery("<i class='visible'>&nbsp;</i>"));
					replace = true;
				});
				
				if(replace) {
					e.content = jQuery(htmlContent).html();
				}
			});
		}
	});

	// Register plugin
	tinymce.PluginManager.add('olatqti', org.olat.ims.qti21.ui.editor);
})();