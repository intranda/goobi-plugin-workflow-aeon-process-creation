package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.aeon.LoginResponse;
import org.goobi.aeon.User;
import org.goobi.beans.Batch;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.Template;
import org.goobi.beans.Templateproperty;
import org.goobi.interfaces.IJsonPlugin;
import org.goobi.interfaces.ISearchField;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.IOpacPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.intranda.goobi.plugins.aeon.AeonProperty;
import de.intranda.goobi.plugins.aeon.AeonRecord;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import io.goobi.workflow.xslt.XsltToPdf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;

@PluginImplementation
@Log4j2
public class AeonProcessCreationWorkflowPlugin implements IWorkflowPlugin, IPlugin {

    @Getter
    private String title = "intranda_workflow_aeon_process_creation";

    //    @Getter
    //    @Setter
    //    //this will contain all fields inside <transaction> defined in config
    //    private List<HierarchicalConfiguration> transactionFields;

    @Getter
    @Setter
    //this will contain all fields inside <processes> defined in config
    private List<AeonProperty> recordFields = new ArrayList<>();

    @Getter
    @Setter
    private List<AeonProperty> transactionFields = new ArrayList<>();

    @Getter
    @Setter
    private List<AeonProperty> propertyFields = new ArrayList<>();

    @Getter
    @Setter
    //set true when the request was a success (important for xhtml EL expressions rendered checks)
    private boolean requestSuccess = false;

    @Getter
    @Setter
    // set to 'request' to show the first screen and to 'summary' to show created processes
    private String screenName = "request";

    @Getter
    @Setter
    //input string entered into the TextField
    private String input;

    private Client client = ClientBuilder.newClient();

    private String apiUrl;
    private User user;
    private String apiKey;

    private String workflowName;
    private String opacName;

    @Getter
    private String selectedWorkflow;
    @Getter
    private List<String> possibleWorkflows = new ArrayList<>();

    @Getter
    @Setter
    private List<AeonRecord> recordList = new ArrayList<>();

    private List<Process> generatedProcesses = new ArrayList<>();

    private BeanHelper bhelp = new BeanHelper();

    private IJsonPlugin opacPlugin;
    private ConfigOpacCatalogue coc = null;

    @Getter
    private String shippingOption;

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
     * recieves JSON String as response which is parsed into the AeonTransaction object
     */
    @SuppressWarnings("unchecked")
    public void sendRequest() {
        recordList.clear();
        Map<String, Object> map = null;

        if (this.input.equals("1234567890")) { //(JUST FOR TESTING: checks if input is 1234567890)
            try {
                map = client.target("http://localhost:8080/goobi/api/")
                        .path("testingRest")
                        .path("aeon")
                        .request(MediaType.APPLICATION_JSON)
                        .get(Map.class);

            } catch (Exception e) {
                log.error(e + " " + e.getMessage());
            }

        } else {

            if (StringUtils.isNotBlank(apiKey)) {
                map = client.target(apiUrl)
                        .path("Requests")
                        .path(input)
                        .request(MediaType.APPLICATION_JSON)
                        .header("X-AEON-API-KEY", apiKey)
                        .get(Map.class);
            } else {
                LoginResponse res = client.target(apiUrl)
                        .path("Token")
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(user, MediaType.APPLICATION_JSON), LoginResponse.class);
                map = client.target(apiUrl)
                        .path("Requests")
                        .path(input)
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "BEARER " + res.getAccessToken())
                        .get(Map.class);
            }
        }
        if (map != null) {
            shippingOption = (String) map.get("shippingOption");
            for (AeonProperty property : transactionFields) {
                if (StringUtils.isNoneBlank(property.getAeonField())) {
                    Object value = map.get(property.getAeonField());
                    if (value instanceof String) {
                        property.setValue((String) value);
                    } else if (value instanceof Integer) {
                        property.setValue(((Integer) value).toString());
                    } else {
                        property.setValue((String) value);
                    }
                }
            }

            String catalogueIdentifier = (String) map.get("referenceNumber");

            IOpacPlugin myImportOpac = null;

            for (ConfigOpacCatalogue configOpacCatalogue : ConfigOpac.getInstance().getAllCatalogues(workflowName)) {
                if (configOpacCatalogue.getTitle().equals(opacName)) {
                    myImportOpac = configOpacCatalogue.getOpacPlugin();
                    coc = configOpacCatalogue;
                }
            }

            opacPlugin = (IJsonPlugin) myImportOpac;
            if (this.input.equals("1234567890")) { //(JUST FOR TESTING: checks if input is 1234567890)
                opacPlugin.setTestMode(true);
            } else {
                for (ISearchField sf : opacPlugin.getSearchFieldList()) {
                    if (sf.getId().equals("Barcode")) {
                        sf.setText(catalogueIdentifier);
                    }
                }
            }
            try {
                opacPlugin.search("", "", coc, null);
            } catch (Exception e) {
                log.error(e);
            }

            if (opacPlugin.getOverviewList() != null) {
                for (Map<String, String> overview : opacPlugin.getOverviewList()) {
                    AeonRecord record = new AeonRecord();
                    recordList.add(record);
                    record.setRecordData(overview);
                    for (AeonProperty p : recordFields) {
                        AeonProperty prop = p.cloneProperty();
                        prop.setValue(overview.get(prop.getAeonField()));
                        record.getProperties().add(prop);

                    }
                    String generatedTitle = overview.get("uri").replaceAll("[\\W]", "");
                    record.setProcessTitle(generatedTitle);

                    // TODO check if record needs to be disabled
                    // record.setDisabled(true);

                    // copy properties
                    for (AeonProperty p : propertyFields) {
                        if (StringUtils.isBlank(shippingOption) || p.getShippingOption() == null || p.getShippingOption().equals(shippingOption)) {
                            AeonProperty prop = p.cloneProperty();
                            prop.setOverwriteMainField(true);
                            prop.setValue(p.getValue());
                            prop.setDefaultValue(p.getValue());
                            record.getProcessProperties().add(prop);
                        }
                    }



                    //  check for duplicates in active projects

                    Process other = ProcessManager.getProcessByExactTitle(generatedTitle);
                    if (other != null && !other.getProjekt().getProjectIsArchived()) {
                        record.setDuplicate(true);
                        // TODO new process title, add suffix like date, or process id

                        for (AeonProperty property :   record.getProperties()) {
                            AeonProperty aeonProperty = property.cloneProperty();
                            aeonProperty.setValue("");
                            record.getDuplicateProperties().add(aeonProperty);
                            extractProcessValues(other, aeonProperty);
                        }
                        for (AeonProperty property :  record.getProcessProperties()) {
                            AeonProperty aeonProperty = property.cloneProperty();
                            aeonProperty.setValue("");
                            record.getDuplicateProperties().add(aeonProperty);
                            extractProcessValues(other, aeonProperty);
                        }
                    }
                }
                setRequestSuccess(true);
            } else {
                // TODO aeon request valid, but no record in metadata cloud
            }
        } else {
            // TODO no valid aeon request
        }
    }

    private void extractProcessValues(Process other, AeonProperty aeonProperty) {
        switch (aeonProperty.getPlace()) {
            case "process":
                for (Processproperty processProperty : other.getEigenschaften()) {
                    if (processProperty.getTitel().equals(aeonProperty.getPropertyName())) {
                        aeonProperty.setValue(processProperty.getWert());
                        break;
                    }
                }
                break;
            case "template":
                if (other.getVorlagenSize()>0) {
                    for (Templateproperty templateProperty : other.getVorlagen().get(0).getEigenschaften()) {
                        if (templateProperty.getTitel().equals(aeonProperty.getPropertyName())) {
                            aeonProperty.setValue(templateProperty.getWert());
                            break;
                        }
                    }
                }
                break;
            default:
                if (other.getWerkstueckeSize()>0) {
                    for (Masterpieceproperty templateProperty : other.getWerkstuecke().get(0).getEigenschaften()) {
                        if (templateProperty.getTitel().equals(aeonProperty.getPropertyName())) {
                            aeonProperty.setValue(templateProperty.getWert());
                            break;
                        }
                    }
                }
                break;
        }
    }

    /*
     * resets the transaction object
     * (this resets the page to its default state)
     */
    public void resetRequest() {
        generatedProcesses.clear();
        setRequestSuccess(false);
        input = "";
        screenName = "request";
    }

    public void createProcesses() {

        // check if at least one record was selected

        int numberOfRecordsToCreate = 0;
        for (AeonRecord rec : recordList) {
            if (rec.isAccepted()) {
                numberOfRecordsToCreate++;
            }
        }
        if (numberOfRecordsToCreate == 0) {
            Helper.setFehlerMeldung("plugin_workflow_aeon_nothing_selected");
            return;
        }

        //  validate properties, details, process values

        for (AeonProperty prop : propertyFields) {
            if (StringUtils.isBlank(shippingOption) || prop.getShippingOption() == null || prop.getShippingOption().equals(shippingOption)) {
                if (!prop.isValid() && prop.isStrictValidation()) {
                    Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_process_properties");
                    return;
                }
            }
        }
        for (AeonRecord rec : recordList) {
            if (rec.isAccepted()) {
                for (AeonProperty prop : rec.getProcessProperties()) {
                    if (!prop.isValid() && prop.isStrictValidation()) {
                        Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_process_properties");
                        return;
                    }
                }
            }
        }

        for (AeonProperty prop : transactionFields) {
            if (!prop.isValid()) {
                Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_transaction_fields");
                return;
            }
        }

        Process processTemplate = ProcessManager.getProcessByExactTitle(selectedWorkflow);
        Batch batch = null;
        for (AeonRecord rec : recordList) {
            if (rec.isAccepted()) {
                // create process
                // TODO generate different title, if its a duplicate

                Process process = new Process();
                if (batch == null) {
                    batch = new Batch();
                    batch.setBatchName(input);
                    batch.setBatchLabel(input);
                    ProcessManager.saveBatch(batch);
                }
                process.setBatch(batch);

                process.setProjekt(processTemplate.getProjekt());
                process.setRegelsatz(processTemplate.getRegelsatz());
                process.setDocket(processTemplate.getDocket());

                bhelp.SchritteKopieren(processTemplate, process);
                bhelp.ScanvorlagenKopieren(processTemplate, process);
                bhelp.WerkstueckeKopieren(processTemplate, process);
                bhelp.EigenschaftenKopieren(processTemplate, process);

                bhelp.EigenschaftHinzufuegen(process, "Template", processTemplate.getTitel());
                bhelp.EigenschaftHinzufuegen(process, "TemplateID", "" + processTemplate.getId());

                List<Masterpiece> mpl = process.getWerkstuecke();
                if (mpl.isEmpty()) {
                    Masterpiece mp = new Masterpiece();
                    mp.setProzess(process);
                    mpl.add(mp);
                }
                List<Template> tl = process.getVorlagen();
                if (tl.isEmpty()) {
                    Template t = new Template();
                    t.setProzess(process);
                    tl.add(t);
                }

                // add properties
                //                for (AeonProperty prop : propertyFields) {
                //                    if (StringUtils.isBlank(shippingOption) || prop.getShippingOption() == null || prop.getShippingOption().equals(shippingOption)) {
                //                        String value = prop.getValue();
                //                        for (AeonProperty localProperty : rec.getProcessProperties()) {
                //                            if (prop.getTitle().equals(localProperty.getTitle()) && StringUtils.isNoneBlank(localProperty.getValue())) {
                //                                value = localProperty.getValue();
                //                            }
                //                        }
                //
                //                        if (StringUtils.isNoneBlank(value)) {
                //                            switch (prop.getPlace()) {
                //                                case "process":
                //                                    bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), value);
                //                                    break;
                //                                case "work":
                //                                    bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), value);
                //                                    break;
                //                                case "template":
                //                                    bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), value);
                //                                    break;
                //                            }
                //                        }
                //                    }
                //                }
                for (AeonProperty prop : transactionFields) {
                    if (StringUtils.isNoneBlank(prop.getValue())) {
                        switch (prop.getPlace()) {
                            case "process":
                                bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), prop.getValue());
                                break;
                            case "work":
                                bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), prop.getValue());
                                break;
                            case "template":
                                bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), prop.getValue());
                                break;
                        }
                    }
                }

                for (AeonProperty prop : rec.getProcessProperties()) {
                    //                    if (StringUtils.isNoneBlank(prop.getValue())) {
                    switch (prop.getPlace()) {
                        case "process":
                            bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), prop.getValue());
                            break;
                        case "work":
                            bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), prop.getValue());
                            break;
                        case "template":
                            bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), prop.getValue());
                            break;
                    }
                    //                    }
                }

                try {
                    // create mets file for selected record
                    Prefs prefs = processTemplate.getRegelsatz().getPreferences();
                    String recordIdentifier = rec.getRecordData().get("uri"); // get uri from properties
                    opacPlugin.setSelectedUrl(recordIdentifier);
                    Fileformat fileformat = opacPlugin.search("", "", coc, prefs); // get metadata for selected record
                    // is additional metadata neeeded?

                    process.setTitel(rec.getProcessTitle());

                    //                    if (ProcessManager.countProcessTitle(generatedTitle, null) > 0) {
                    //                        Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_titleInUse", generatedTitle));
                    //                        rec.setAccepted(false);
                    //                        continue;
                    //                    }

                    // save process
                    ProcessManager.saveProcess(process);

                    // save fileformat
                    process.writeMetadataFile(fileformat);
                    generatedProcesses.add(process);
                } catch (Exception e) {
                    log.error(e);
                }

                // start any open automatic tasks
                for (Step s : process.getSchritteList()) {
                    if (s.getBearbeitungsstatusEnum().equals(StepStatus.OPEN) && s.isTypAutomatisch()) {
                        ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(s);
                        myThread.startOrPutToQueue();
                    }
                }
            }
        }
        screenName = "summary";
    }

    /**
     * Constructor try to use empty constructor, remove configuration from here
     */
    public AeonProcessCreationWorkflowPlugin() {
        log.info("AeonProcessCreation workflow plugin started");

        //read config
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());

        apiUrl = config.getString("/aeon/url");
        apiKey = config.getString("/aeon/apiKey");
        user = new User(config.getString("/aeon/username", ""), config.getString("/aeon/password", ""));

        workflowName = config.getString("/processCreation/workflowName");
        opacName = config.getString("/processCreation/opacName");

        //read <transaction> and <processes>
        List<HierarchicalConfiguration> transactions = config.configurationsAt("/transaction/field");
        for (HierarchicalConfiguration hc : transactions) {
            AeonProperty property = new AeonProperty(hc, this);
            transactionFields.add(property);
        }
        List<HierarchicalConfiguration> processes = config.configurationsAt("/processes/field");
        for (HierarchicalConfiguration hc : processes) {
            AeonProperty property = new AeonProperty(hc, this);
            recordFields.add(property);
        }

        //        this.transactionFields = config.configurationsAt("/transaction/field");
        //        this.processFields = config.configurationsAt("/processes/field");

        propertyFields.clear();
        List<HierarchicalConfiguration> properties = config.configurationsAt("/properties/field");
        for (HierarchicalConfiguration hc : properties) {
            AeonProperty property = new AeonProperty(hc, this);
            propertyFields.add(property);
        }

        String sql = FilterHelper.criteriaBuilder("", true, null, null, null, true, false);
        // load all process templates
        List<Process> templates = ProcessManager.getProcesses("prozesse.titel", sql);
        // use configured default template name
        for (Process p : templates) {
            possibleWorkflows.add(p.getTitel());
            if (p.getTitel().equals(workflowName)) {
                selectedWorkflow = p.getTitel();
            }
        }
        // if it doesn't exist, use first existing template
        if (selectedWorkflow == null) {
            selectedWorkflow = templates.get(0).getTitel();
        }

    }

    public void acceptAllItems() {
        for (AeonRecord rec : recordList) {
            rec.setAccepted(true);
        }
    }

    public void declineAllItems() {
        for (AeonRecord rec : recordList) {
            rec.setAccepted(false);
        }
    }

    public void generateDockets() {
        String rootpath = ConfigurationHelper.getInstance().getXsltFolder();
        Path xsltfile = Paths.get(rootpath, "docket_multipage.xsl");
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = "batch_docket" + ".pdf";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            // write docket to servlet output stream
            try {
                ServletOutputStream out = response.getOutputStream();
                XsltToPdf ern = new XsltToPdf();
                ern.startExport(generatedProcesses, out, xsltfile.toString());
                out.flush();
            } catch (IOException e) {
                log.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
    }

    public void setSelectedWorkflow(String selectedWorkflow) {
        // TODO check, if records needs to be disabled/enabled, when workflow is changed
        this.selectedWorkflow = selectedWorkflow;
    }

    //    public void propertyValueChangeListener(ValueChangeEvent e) {
    //        UIComponent nameInput = e.getComponent();
    //
    //        for (String atttr : nameInput.getAttributes().keySet()) {
    //            System.out.println(atttr + ": " + nameInput.getAttributes().get(atttr));
    //        }
    //
    //        System.out.println(nameInput.getId());
    //
    //        for (String atttr : nameInput.getPassThroughAttributes().keySet()) {
    //            System.out.println(atttr + ": " + nameInput.getPassThroughAttributes().get(atttr));
    //        }
    //
    //        System.out.println("valueChangeListener invoked:" + " OLD: " + e.getOldValue() + " NEW: " + e.getNewValue());
    //    }

    public void updateProperties(String propertyName, String oldValue, String newValue) {
        for (AeonRecord record : recordList) {
            for (AeonProperty prop : record.getProcessProperties()) {
                if (prop.getTitle().equals(propertyName)) {
                    // configure new default value
                    prop.setDefaultValue(newValue);
                    // update property value
                    //                    if (oldValue.equals(prop.getValue())) {
                    prop.setValue(newValue);
                    //                    }
                    break;
                }
            }
        }
    }
}
