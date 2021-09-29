package de.intranda.goobi.plugins;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.intranda.goobi.plugins.aeon.AeonItem;
import de.intranda.goobi.plugins.aeon.AeonProperty;
import de.intranda.goobi.plugins.aeon.AeonTransmission;
import de.intranda.goobi.plugins.api.aeon.LoginResponse;
import de.intranda.goobi.plugins.api.aeon.QueueItem;
import de.intranda.goobi.plugins.api.aeon.User;
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
    //this will contain all fields inside <transmission> defined in config
    private List<HierarchicalConfiguration> transmissionFields;

    @Getter
    @Setter
    //this will contain all fields inside <processes> defined in config
    private List<HierarchicalConfiguration> processFields;

    @Getter
    @Setter
    private List<AeonProperty> propertyFields = new ArrayList<>();

    @Getter
    @Setter
    //set true when the request was a success (important for xhtml EL expressions rendered checks)
    private boolean requestSuccess = false;

    @Getter
    @Setter
    //the response from api (currently RestTest.java)
    private AeonTransmission transmission = new AeonTransmission();

    @Getter
    @Setter
    //input string entered into the TextField
    private String input;

    private Client client = ClientBuilder.newClient();

    private String api;
    private User user;

    @Override
    public PluginType getType() {
        return PluginType.Workflow;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_aeon_process_creation.xhtml";
    }

    /*
     * Sends a Request to the goobi api (RestTest.java) and
     * recieves JSON String as response which is parsed into the AeonTransmission object
     */
    public void sendRequest() {
        AeonTransmission response = null;
        setInput(input); //set this.input to textfield input

        if (this.input.equals("1234567890")) { //(JUST FOR TESTING: checks if input is 1234567890)
            try {
                response = this.client.target("http://localhost:8080/goobi/api/")
                        .path("testingRest")
                        .request(MediaType.APPLICATION_JSON)
                        .get(AeonTransmission.class);
            } catch (Exception e) {
                log.error(e + " " + e.getMessage());
            }
            setTransmission(response);
            if (this.transmission == null) {
                return;
            } else {
                setDefaultTransmissionValues(); //Checks Config for Default values!
                setRequestSuccess(true);
            }
        } else {
            LoginResponse res = client.target(api)
                    .path("Token")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(user, MediaType.APPLICATION_JSON), LoginResponse.class);

            List<QueueItem> queueItems = client.target(api)
                    .path("Queues")
                    .path(input)
                    .path("requests")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "BEARER " + res.getAccessToken())
                    .get(new GenericType<List<QueueItem>>(){});
            for (QueueItem qi: queueItems) {
                System.out.println(qi.toString());
            }


            //            String r =   client.target(api)
            //                    .path("Requests")
            //                    .path("249426")
            //                    .request(MediaType.APPLICATION_JSON)
            //                    .header("Authorization", "BEARER " + res.getAccessToken())
            //                    .get(String.class);
            //            System.out.println( r);

            setRequestSuccess(false);
        }
    }

    /*
     * resets the transmission object
     * (this resets the page to its default state)
     */
    public void resetRequest() {
        setTransmission(null);
        setRequestSuccess(false);
        input = "";
    }

    /*
     * Returns value of any attribute inside mainObject
     * (nested example: transmission.user.firstname called with:
     * getTransmissionFieldValue(transmission, "user.firstName") )
     */
    public Object getFieldValue(Object mainObject, String fieldName) {

        List<String> names = new LinkedList<>(Arrays.asList(fieldName.split("\\."))); //splits list into single attribute names
        Object obj = new Object();

        try {
            Class<?> c = mainObject.getClass(); //gets class of main object
            Field field = c.getDeclaredField(names.get(0)); //returns first field

            field.setAccessible(true);
            obj = field.get(mainObject); //defines object as specific attribute of mainObject
            field.setAccessible(false);

            names.remove(0); //removes first name

            //if its nested this loop will begin
            for (int i = 0; i < names.size(); i++) {
                field = obj.getClass().getDeclaredField(names.get(i)); //gets next field

                field.setAccessible(true);
                obj = field.get(obj); //sets object equal to its own value of the field
                field.setAccessible(false);
            }
        } catch (Exception e) {
            log.error(e + ": " + e.getMessage());
            e.printStackTrace();
        }
        return obj; //returns the object
    }

    /*
     * Returns the value of a field specified in the same way as function above
     * setFieldValue(transmission, "user.firstName", "Maxim")
     */
    public void setFieldValue(Object mainObject, String fieldName, Object newValue) {
        try {
            BeanUtils.setProperty(mainObject, fieldName, newValue); //uses BeanUtils to access property
        } catch (Exception e) {
            log.error(e + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * checks if value of certain property is null or empty string
     * if so it sets the value of the property to the default value
     * defined in the config
     */
    public void checkFieldValue(HierarchicalConfiguration field, Object object) {
        Object fieldvalue = getFieldValue(object, field.getString("@aeon"));
        if (fieldvalue == null || StringUtils.isEmpty(String.valueOf(fieldvalue))) {
            if (object instanceof Boolean) {
                setFieldValue(object, field.getString("@aeon"), field.getBoolean("value"));
            } else {
                setFieldValue(object, field.getString("@aeon"), field.getString("value"));
            }
        }
    }

    /*
     * Uses above defined functions to iterate through the config
     * and change values to default if necessary
     */
    public void setDefaultTransmissionValues() {

        //iterate through <transmission> (config)
        for (HierarchicalConfiguration field : this.transmissionFields) {
            checkFieldValue(field, this.transmission);
        }

        //iterate through <processes> (config)
        for (HierarchicalConfiguration field : this.processFields) {
            //iterate through items (to apply config settings)
            for (int i = 0; i < this.transmission.getItems().size(); i++) {
                checkFieldValue(field, this.transmission.getItems().get(i));
            }
        }
    }

    public void createProcesses() {
        // create new processes for selected items

        // TODO validate properties

        // TODO validate

        for (AeonItem item : transmission.getItems()) {
            if (item.isAccepted()) {
                // TODO create process
            }
        }
    }

    /**
     * Constructor TODO use empty constructor, remove configuration from here
     */
    public AeonProcessCreationWorkflowPlugin() {
        log.info("AeonProcessCreation workflow plugin started");

        //read config
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());

        api = config.getString("/aeon/url");
        user = new User(config.getString("/aeon/username"), config.getString("/aeon/password"));

        //read <transmission> and <processes>
        this.transmissionFields = config.configurationsAt("/transmission/field");
        this.processFields = config.configurationsAt("/processes/field");

        propertyFields.clear();
        List<HierarchicalConfiguration> properties = config.configurationsAt("/properties/field");
        for (HierarchicalConfiguration hc : properties) {
            AeonProperty property = new AeonProperty(hc);
            propertyFields.add(property);
        }
    }
}
