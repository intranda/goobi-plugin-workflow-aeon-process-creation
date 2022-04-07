package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.StringUtils;
import org.goobi.aeon.LoginResponse;
import org.goobi.aeon.User;
import org.goobi.beans.Batch;
import org.goobi.beans.Masterpiece;
import org.goobi.beans.Masterpieceproperty;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Project;
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
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.MySQLHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
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

    // test identifier
    // 284994 (DRMS) Book & Paper
    // 286670 (DRMS) Book & Paper
    // 286528 - Audio Recording (DRMS)
    // 287365 - Video (DRMS)
    // 287366 - Film (DRMS)
    // 287367 - Transmissives

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

    // contains a list of required fields that are mandatory in the metadata cloud result
    private List<String> requiredFields;

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

    private String defaultWorkflowName;
    private Map<String, String> specialWorkflowNames = new HashMap<>();
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

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_aeon_process_creation.xhtml";
    }

    @Getter
    @Setter
    // defines the current operation type, possible values are creation to create new processes and cancel to disable processes
    private String operationType = "creation";

    private String transactionFieldName;
    private String cancellationPropertyName;
    private String cancellationPropertyValue;
    private String cancellationProjectName;

    /*
     * Sends a Request to the goobi api (RestTest.java) and
     * recieves JSON String as response which is parsed into the AeonTransaction object
     */
    @SuppressWarnings("unchecked")
    public void sendRequest() {
        recordList.clear();
        if ("creation".equals(operationType)) {

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
                try {
                    // get data from user table
                    String username = (String) map.get("username");
                    if (StringUtils.isNotBlank(username) && !username.contains("\\")) {
                        Map<String, Object> userDataMap = client.target(apiUrl)
                                .path("Users")
                                .path(username)
                                .request(MediaType.APPLICATION_JSON)
                                .header("X-AEON-API-KEY", apiKey)
                                .get(Map.class);
                        map.put("lastName", userDataMap.get("lastName"));
                        map.put("eMailAddress", userDataMap.get("eMailAddress"));
                    }
                } catch (Exception e) {
                    log.error(e);
                }

                // validate if required fields are available
                for (String fieldname : requiredFields) {
                    if (map.containsKey(fieldname) && map.get(fieldname) == null) {
                        Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_fieldNull", fieldname));
                    }
                }

                shippingOption = (String) map.get("shippingOption");
                for (AeonProperty property : transactionFields) {
                    if (StringUtils.isNotBlank(property.getAeonField())) {
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

                for (AeonProperty property : propertyFields) {
                    if (StringUtils.isNotBlank(property.getAeonField())) {
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

                // use configured default template name
                selectedWorkflow = null;
                // first, check if a special workflow name was configured for the current type
                if (specialWorkflowNames.containsKey(shippingOption)) {
                    String workflowName = specialWorkflowNames.get(shippingOption);
                    if (possibleWorkflows.contains(workflowName)) {
                        selectedWorkflow = workflowName;
                    }
                }
                // otherwise try to use the default workflow
                if (selectedWorkflow == null && possibleWorkflows.contains(defaultWorkflowName)) {
                    selectedWorkflow = defaultWorkflowName;
                }

                // if it doesn't exist, use first existing template
                if (selectedWorkflow == null) {
                    selectedWorkflow = possibleWorkflows.get(0);
                }

                String catalogueIdentifier = (String) map.get("referenceNumber");

                IOpacPlugin myImportOpac = null;

                for (ConfigOpacCatalogue configOpacCatalogue : ConfigOpac.getInstance().getAllCatalogues(defaultWorkflowName)) {
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
                int nextFreeId = getNextProcessId() + 1;

                // Bib ID and Volume number (bibId, volume)
                // ASpace resource ID (uri)
                // preparation for duplicate checks
                String aspaceResourceIDProperty = "";
                String ilsBibId = "";
                String ilsVolumeNumber = "";
                for (AeonProperty ap : recordFields) {
                    if ("bibId".equals(ap.getAeonField())) {
                        ilsBibId = ap.getPropertyName();
                    } else if ("volume".equals(ap.getAeonField())) {
                        ilsVolumeNumber = ap.getPropertyName();
                    } else if ("uri".equals(ap.getAeonField())) {
                        aspaceResourceIDProperty = ap.getPropertyName();
                    }
                }

                if (opacPlugin.getOverviewList() != null) {
                    for (Map<String, String> overview : opacPlugin.getOverviewList()) {
                        AeonRecord record = new AeonRecord();
                        recordList.add(record);
                        record.setRecordData(overview);
                        for (AeonProperty p : recordFields) {
                            AeonProperty prop = p.cloneProperty();
                            prop.setValue(overview.get(prop.getAeonField()));
                            if (StringUtils.isNotBlank(prop.getValue())) {
                                record.getProperties().add(prop);
                            }
                        }

                        //                        Process title: Job#-repository-AeonTN e.g 999-music-12345. The job# is an auto generated one-up number.
                        //                        Repository pulled from Aeon field - Site
                        //                        AeonTN pulled from Aeon field - transactionNumber

                        // get next free id
                        String repository = (String) map.get("site");
                        int transactionNumber = (int) map.get("transactionNumber");

                        String generatedTitle = (nextFreeId + 1) + "_" + repository + "_" + transactionNumber;
                        record.setProcessTitle(generatedTitle);

                        // copy properties
                        for (AeonProperty p : propertyFields) {
                            if (StringUtils.isBlank(shippingOption) || p.getShippingOption() == null
                                    || p.getShippingOption().equals(shippingOption)) {
                                AeonProperty prop = p.cloneProperty();
                                prop.setOverwriteMainField(true);
                                prop.setValue(p.getValue());
                                prop.setDefaultValue(p.getValue());
                                record.getProcessProperties().add(prop);
                            }
                        }

                        //  check for duplicates in active projects, load the processes with the same transaction number
                        List<Process> processes = ProcessManager.getProcesses("prozesse.erstellungsdatum desc",
                                "prozesse.titel like \"%" + repository + "_" + transactionNumber + "%\"");

                        for (Process other : processes) {
                            if (!other.getProjekt().getProjectIsArchived()) {
                                // check if properties matches
                                boolean isDuplicate = false;
                                if ("ils".matches(overview.get("recordType"))) {
                                    String bib = "";
                                    String vol = "";

                                    for (Processproperty pp : other.getEigenschaften()) {
                                        if (ilsBibId.equals(pp.getTitel())) {
                                            bib = pp.getWert();
                                        } else if (ilsVolumeNumber.equals(pp.getTitel())) {
                                            vol = pp.getWert();
                                        }
                                    }
                                    if (bib.equals(overview.get("bibId")) && vol.equals(overview.get("volume"))) {
                                        isDuplicate = true;
                                    }
                                } else {
                                    for (Processproperty pp : other.getEigenschaften()) {
                                        if (aspaceResourceIDProperty.equals(pp.getTitel())) {
                                            if (pp.getWert().equals(overview.get("uri"))) {
                                                isDuplicate = true;
                                            }
                                        }
                                    }
                                }

                                if (isDuplicate) {
                                    record.setDuplicate(true);
                                    for (AeonProperty property : record.getProperties()) {
                                        AeonProperty aeonProperty = property.cloneProperty();
                                        aeonProperty.setValue("");
                                        record.getDuplicateProperties().add(aeonProperty);
                                        extractProcessValues(other, aeonProperty);
                                    }
                                    for (AeonProperty property : record.getProcessProperties()) {
                                        AeonProperty aeonProperty = property.cloneProperty();
                                        aeonProperty.setValue("");
                                        record.getDuplicateProperties().add(aeonProperty);
                                        extractProcessValues(other, aeonProperty);
                                    }
                                }
                            }
                        }
                    }
                    setRequestSuccess(true);
                } else {
                    //  aeon request valid, but no record in metadata cloud
                }
            } else {
                //  no valid aeon request
            }
        } else {
            searchForDeactivateProcess();
        }
    }

    private int getNextProcessId() {
        String sql = "SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            int value = new QueryRunner().query(connection, sql, MySQLHelper.resultSetToIntegerHandler, connection.getCatalog(), "prozesse");
            return value;
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (connection != null) {
                try {
                    MySQLHelper.closeConnection(connection);
                } catch (SQLException e) {
                }
            }
        }
        return 0;
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
                if (other.getVorlagenSize() > 0) {
                    for (Templateproperty templateProperty : other.getVorlagen().get(0).getEigenschaften()) {
                        if (templateProperty.getTitel().equals(aeonProperty.getPropertyName())) {
                            aeonProperty.setValue(templateProperty.getWert());
                            break;
                        }
                    }
                }
                break;
            default:
                if (other.getWerkstueckeSize() > 0) {
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
        initializeConfiguration();
        generatedProcesses.clear();
        setRequestSuccess(false);
        input = "";
        screenName = "request";
        operationType = "creation";
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

                if (rec.isDuplicate()) {
                    // generate different title, if its a duplicate
                    rec.setProcessTitle(rec.getProcessTitle() + "_" + dateFormat.format(new Date()));
                }

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
                for (AeonProperty prop : rec.getProperties()) {
                    if (StringUtils.isBlank(shippingOption) || prop.getShippingOption() == null || prop.getShippingOption().equals(shippingOption)) {
                        String value = prop.getExportValue();
                        for (AeonProperty localProperty : rec.getProcessProperties()) {
                            if (prop.getTitle().equals(localProperty.getTitle()) && StringUtils.isNoneBlank(localProperty.getExportValue())) {
                                value = localProperty.getExportValue();
                            }
                        }

                        if (StringUtils.isNoneBlank(value)) {
                            switch (prop.getPlace()) {
                                case "process":
                                    bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), value);
                                    break;
                                case "work":
                                    bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), value);
                                    break;
                                case "template":
                                    bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), value);
                                    break;
                            }
                        }
                    }
                }
                for (AeonProperty prop : transactionFields) {
                    if (StringUtils.isNoneBlank(prop.getExportValue())) {
                        switch (prop.getPlace()) {
                            case "process":
                                bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), prop.getExportValue());
                                break;
                            case "work":
                                bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), prop.getExportValue());
                                break;
                            case "template":
                                bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), prop.getExportValue());
                                break;
                        }
                    }
                }

                for (AeonProperty prop : rec.getProcessProperties()) {
                    //                    if (StringUtils.isNoneBlank(prop.getValue())) {
                    switch (prop.getPlace()) {
                        case "process":
                            bhelp.EigenschaftHinzufuegen(process, prop.getPropertyName(), prop.getExportValue());
                            break;
                        case "work":
                            bhelp.EigenschaftHinzufuegen(process.getWerkstuecke().get(0), prop.getPropertyName(), prop.getExportValue());
                            break;
                        case "template":
                            bhelp.EigenschaftHinzufuegen(process.getVorlagen().get(0), prop.getPropertyName(), prop.getExportValue());
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
        initializeConfiguration();
    }

    private void initializeConfiguration() {
        //read config
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());

        apiUrl = config.getString("/aeon/url");
        apiKey = config.getString("/aeon/apiKey");
        user = new User(config.getString("/aeon/username", ""), config.getString("/aeon/password", ""));

        defaultWorkflowName = config.getString("/processCreation/defaultWorkflowName");

        for (HierarchicalConfiguration hc : config.configurationsAt("/processCreation/workflowName")) {
            String doctype = hc.getString("@type");
            String workflowName = hc.getString(".");
            specialWorkflowNames.put(doctype, workflowName);
        }

        opacName = config.getString("/processCreation/opacName");

        // process cancellation
        transactionFieldName = config.getString("/processCancellation/transactionFieldName");
        cancellationPropertyName = config.getString("/processCancellation/propertyName");
        cancellationPropertyValue = config.getString("/processCancellation/propertyValue");
        cancellationProjectName = config.getString("/processCancellation/projectName");

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

        requiredFields = Arrays.asList(config.getStringArray("/requiredFields/field"));
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
        for (Process p : templates) {
            possibleWorkflows.add(p.getTitel());
        }
    }

    public void acceptAllItems() {
        for (AeonRecord rec : recordList) {
            if (!rec.isDisabled()) {
                rec.setAccepted(true);
            }
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
        this.selectedWorkflow = selectedWorkflow;
    }

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

    public void searchForDeactivateProcess() {

        StringBuilder sql = new StringBuilder();
        //        sql.append("SELECT ");
        //        sql.append("p1.prozesseID ");
        //        sql.append(", p2.wert ");
        //        sql.append("FROM ");
        //        sql.append("prozesseeigenschaften p1 ");
        //        sql.append("LEFT JOIN ");
        //        sql.append("prozesseeigenschaften p2 ON p1.prozesseID = p2.prozesseID ");
        //        sql.append("AND p1.titel = '");
        //        sql.append(transactionFieldName);
        //        sql.append("' AND p2.titel = '");
        //        sql.append(cancelationPropertyName);
        //        sql.append("' WHERE ");
        //        sql.append("p1.wert = '");
        //        sql.append(transactionNumber);
        //        sql.append("' ");
        // first value: process id
        // second value: cancelation allowed value:

        sql.append("(prozesse.ProzesseID in (select prozesseID from prozesseeigenschaften where prozesseeigenschaften.Titel = '");
        sql.append(transactionFieldName);
        sql.append("' AND prozesseeigenschaften.Wert = '");
        sql.append(input);
        sql.append("')) AND projekte.projectIsArchived = false ");

        recordList.clear();

        List<Process> processList = ProcessManager.getProcesses("prozesse.titel", sql.toString());

        for (Process process : processList) {
            //                 Process other = ProcessManager.getProcessByExactTitle(generatedTitle);
            AeonRecord record = new AeonRecord();
            record.setProcessTitle(process.getTitel());
            record.setExistingProcess(process);

            for (AeonProperty p : recordFields) {
                AeonProperty prop = p.cloneProperty();
                prop.setOverwriteMainField(true);
                extractProcessValues(process, prop);
                record.getProperties().add(prop);

            }
            for (AeonProperty property : propertyFields) {
                AeonProperty aeonProperty = property.cloneProperty();
                aeonProperty.setOverwriteMainField(true);
                extractProcessValues(process, aeonProperty);

                record.getProcessProperties().add(aeonProperty);
            }

            // TODO check if property is set, mark it as cancellable
            record.setDisabled(true);
            if (process.getEigenschaften() != null) {
                for (Processproperty pp : process.getEigenschaften()) {
                    if (pp.getTitel().equals(cancellationPropertyName) && cancellationPropertyValue.equals(pp.getWert())) {
                        record.setDisabled(false);
                        break;
                    }
                }
            }

            recordList.add(record);

            //        ### Allow cancelation of items
            //        - should happen in the same AEON Goobi interface (e.g. on the right side)
            //        - User shall:
            //          - type in TN number
            //          - display all the items, that were part of the request
            //          - User selects the ones to be cancelled
            //          - allow the cancelation only if specific property with name `Can be cancelled` is set to `true`
            //        - _ToDo Yale:_ Decide if deletion or deactivation
            //        - _ToDo intranda:_ develop this

        }

        if (!recordList.isEmpty()) {
            setRequestSuccess(true);
        }
    }

    public void cancelProcesses() {
        for (AeonRecord record : recordList) {
            Project project = null;
            try {
                project = ProjectManager.getProjectByName(cancellationProjectName);
            } catch (DAOException e) {
                log.error(e);
            }
            if (project == null) {
                Helper.setFehlerMeldung("keinProjektAngegeben");
            }
            // move selected processes to configured project
            if (!record.isDisabled() && record.isAccepted()) {
                Process process = record.getExistingProcess();
                process.setProjekt(project);
                process.setProjectId(project.getId());
                ProcessManager.saveProcessInformation(process);
            }
        }
        resetRequest();
    }
}
