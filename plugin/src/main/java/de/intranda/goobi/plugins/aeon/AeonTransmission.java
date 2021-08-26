package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;

import lombok.Data;

@Data
public class AeonTransmission {
	private String id;
	private String creationDate;	//maybe type Date instead
	private String title;
	private AeonUser user;
	ArrayList<AeonItem> items;
}
