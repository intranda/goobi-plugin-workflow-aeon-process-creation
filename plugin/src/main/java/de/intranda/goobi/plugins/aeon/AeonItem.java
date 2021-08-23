package de.intranda.goobi.plugins.aeon;

import lombok.Data;

@Data
public class AeonItem {
	private String id;
	private String title;
	private String publicationType;
	private String shelfmark;
	
	private boolean accepted = false;
	
	private boolean showDetails = false;
	
	public void decline() {
		System.out.println("declined");
		setAccepted(false);
	}
	
	public void accept() {
		System.out.println("accepted");
		setAccepted(true);
	}
}
