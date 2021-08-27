package de.intranda.goobi.plugins.aeon;

import lombok.Data;

/*
 * The items from JSON which will be turned into processes
 * (have to be specifically designed for aeon JSON response)
 */
@Data
public class AeonItem {
	private String id;
	private String title;
	private String publicationType;
	private String shelfmark;
	
	private boolean accepted = false;
	
	private boolean showDetails = false;
}
