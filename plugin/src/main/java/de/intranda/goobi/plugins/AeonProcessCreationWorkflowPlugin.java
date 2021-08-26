package de.intranda.goobi.plugins;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.intranda.goobi.plugins.aeon.AeonTransmission;
import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class AeonProcessCreationWorkflowPlugin implements IWorkflowPlugin, IPlugin {
    
    @Getter
    private String title = "intranda_workflow_aeon_process_creation";
    
    @Getter
    @Setter
    private List<HierarchicalConfiguration> transmissionFields;
    
    @Getter
    @Setter
    private boolean requestSuccess = false;
    
    @Getter
    @Setter
    private AeonTransmission transmission = new AeonTransmission();
    
    @Getter
    @Setter
    private String input;
        
    @Getter
    private String value;
    
    @Getter
    Client client = ClientBuilder.newClient();

    @Override
    public PluginType getType() {
        return PluginType.Workflow;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_aeon_process_creation.xhtml";
    }
    
    public void sendRequest() {
    	AeonTransmission response = null;
    	setInput(input);							//set this.input to textfield input
    	
    	if(this.input.equals("1234567890")) {				//(just for testing)
    		System.out.println("request sent");
    		try {
    			response = this.client.target("http://localhost:8080/goobi/api/")
    					.path("testingRest")
    					.request(MediaType.APPLICATION_JSON)
    					.get(AeonTransmission.class);
    		}catch(Exception e) {
    			log.error(e+" "+e.getMessage());
    		}
    		setTransmission(response);
    		if(this.transmission == null) {
    			return;
    		}else {
    			setRequestSuccess(true);
    			System.out.println(getTransmission());
    		}
    	}else {
    		System.out.println(this.input);
    		setRequestSuccess(false);
    	}
    }
    
    public void resetRequest() {
    	setTransmission(null);
    	setRequestSuccess(false);
    	input = "";
    }
    
    public void setDefaultTransmissionvalues() {
    	XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
    	List<HierarchicalConfiguration> transFields = config.configurationsAt("transmission.field");
    	List<HierarchicalConfiguration> processFields = config.configurationsAt("processes.field");
    }
    
    //********** Returns value of any object (nested or not: example -> user.firstname) ************
    public Object getTransmissionFieldValue(String fieldName) {
    	List<String> names = new LinkedList<String>(Arrays.asList(fieldName.split("\\.")));
    	Object obj = new Object();
    	//String str = "";
    	try {
	    	Field field = transmission.getClass().getDeclaredField(names.get(0));
	    	
	    	field.setAccessible(true);
	    	obj = field.get(transmission);
	    	field.setAccessible(false);
	    	
	    	names.remove(0);
	    	
	    	for(int i = 0; i < names.size(); i++) {
	    		field = obj.getClass().getDeclaredField(names.get(i));
	    		
	    		field.setAccessible(true);
	    		obj = field.get(obj);
	    		field.setAccessible(false);
	    	}
    	} catch (Exception e){
    		log.error(e + ": " + e.getMessage());
    		e.printStackTrace();
    	}
    	return obj;
    }

    /**
     * Constructor
     * @throws SecurityException 
     * @throws NoSuchFieldException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public AeonProcessCreationWorkflowPlugin() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        log.info("AeonProcessCreation workflow plugin started");
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);        
        value = config.getString("value", "default value");
        
        this.transmissionFields = config.configurationsAt("transmission.field");
        
//        System.out.println(getTransmissionFieldValue("id"));
//        System.out.println(getTransmissionFieldValue("user.firstName"));
        
//        System.out.println(this.transmissionFields);
//        for(HierarchicalConfiguration sub : transmissionFields) {
//        	System.out.println(sub.getString("[@aeon]"));
//        }
//        System.out.println("printed");
    }
}
