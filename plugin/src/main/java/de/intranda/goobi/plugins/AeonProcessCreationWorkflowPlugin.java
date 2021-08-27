package de.intranda.goobi.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.intranda.goobi.plugins.aeon.AeonItem;
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
    private List<HierarchicalConfiguration> processFields;
    
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
    			setDefaultTransmissionValues();
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
    
    /*
     * Returns value of any attribute 
     * (nested or not: example -> transmission.user.firstname called with:
     * getTransmissionFieldValue("user.firstName") )
     */
    public Object getFieldValue(Object mainObject ,String fieldName) {
    	List<String> names = new LinkedList<String>(Arrays.asList(fieldName.split("\\.")));
    	Object obj = new Object();
    	
    	try {
    		//System.out.println("main object: "+mainObject);
    		Class<?> c = mainObject.getClass();
    		Field field = c.getDeclaredField(names.get(0));
	    	
	    	field.setAccessible(true);
	    	obj = field.get(mainObject);
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
    	//System.out.println("got value for: "+mainObject);
    	return obj;
    }
    
    public void setFieldValue(Object mainObject, String fieldName, Object newValue) {
    	try {	
    		BeanUtils.setProperty(mainObject, fieldName, newValue);
    	} catch (Exception e) {
    		log.error(e + ": " + e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    public void setDefaultTransmissionValues() {
    	
    	for(HierarchicalConfiguration field : this.transmissionFields) {
    		checkFieldValue(field, this.transmission);
    	}
    	
    	for(HierarchicalConfiguration field : this.processFields) {
    		for(int i = 0; i < this.transmission.getItems().size(); i++) {
    			//System.out.println("item: "+ this.transmission.getItems().get(i));
    			checkFieldValue(field, this.transmission.getItems().get(i));
    		}
    	}
    	//System.out.println(this.transmission);
    }
    
    public void checkFieldValue(HierarchicalConfiguration field, Object object) {
    	Object fieldvalue = getFieldValue(object, field.getString("[@aeon]"));
		if(fieldvalue == null || StringUtils.isEmpty(String.valueOf(fieldvalue))) {
			if(object instanceof Boolean) {
				setFieldValue(object, field.getString("[@aeon]") , field.getBoolean("value"));
			}else {
				setFieldValue(object, field.getString("[@aeon]") , field.getString("value"));
			}
		}
    }

    /**
     * Constructor
     */
    public AeonProcessCreationWorkflowPlugin() {
        log.info("AeonProcessCreation workflow plugin started");
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);        
        
        this.transmissionFields = config.configurationsAt("transmission.field");
        this.processFields = config.configurationsAt("processes.field");
        
//        System.out.println(getFieldValue(transmission, "id"));
//        setFieldValue(transmission, "id", "4321");
//        System.out.println(getFieldValue(transmission, "id"));
//        System.out.println(getTransmissionFieldValue("user.firstName"));
        
//        System.out.println(this.transmissionFields);
//        for(HierarchicalConfiguration sub : transmissionFields) {
//        	System.out.println(sub.getString("[@aeon]"));
//        }
//        System.out.println("printed");
    }
}
