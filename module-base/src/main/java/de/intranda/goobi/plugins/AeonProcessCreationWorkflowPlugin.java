package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.StringUtils;
import org.goobi.aeon.LoginResponse;
import org.goobi.aeon.User;
import org.goobi.beans.Batch;
import org.goobi.beans.Process;
import org.goobi.beans.Project;
import org.goobi.beans.Step;
import org.goobi.interfaces.IJsonPlugin;
import org.goobi.interfaces.ISearchField;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.IWorkflowPlugin;

import de.intranda.goobi.plugins.aeon.AeonConfigValidator;
import de.intranda.goobi.plugins.aeon.AeonExistingProcess;
import de.intranda.goobi.plugins.aeon.AeonFieldResolver;
import de.intranda.goobi.plugins.aeon.AeonMetadataException;
import de.intranda.goobi.plugins.aeon.AeonMetadataWriter;
import de.intranda.goobi.plugins.aeon.AeonProcessValues;
import de.intranda.goobi.plugins.aeon.AeonProperty;
import de.intranda.goobi.plugins.aeon.AeonRecord;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.HelperForm;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.MySQLHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import io.goobi.workflow.xslt.XsltToPdf;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class AeonProcessCreationWorkflowPlugin implements IWorkflowPlugin {

    //    ## Sample identifiers
    //    - 286670 - (DRMS) Book & Paper
    //    - 286528 - Audio Recording (DRMS) (also includes a restriction note in Aeon)
    //    - 287365 - Video (DRMS)
    //    - 287366 - Film (DRMS)
    //    - 287367 - Transmissives

    private static final long serialVersionUID = -3408502776490021170L;

    @Getter
    private String title = "intranda_workflow_aeon_process_creation";

    @Getter
    @Setter
    private boolean displayPopup = false;

    @Getter
    @Setter
    //this will contain all fields inside <processes> defined in config
    private transient List<AeonProperty> recordFields = new ArrayList<>();

    @Getter
    @Setter
    private transient List<AeonProperty> transactionFields = new ArrayList<>();

    @Getter
    @Setter
    private transient List<AeonProperty> propertyFields = new ArrayList<>();

    @Getter
    @Setter
    private transient List<AeonProperty> existingProcessFields = new ArrayList<>();

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

    private transient Client client = ClientBuilder.newClient();

    private String apiUrl;
    private transient User user;
    private String apiKey;

    private String defaultWorkflowName;
    private Map<String, String> specialWorkflowNames = new HashMap<>();
    private String opacName;
    private String batchDocstruct;
    private String batchTitleSuffix;

    // stands in for the part of a process title that is only decided when the process is created
    private static final String UNKNOWN_TITLE_PART = "\u2026";

    private String childKeyMetadataType;
    private String resourceIDMetadataType;

    // names of the AEON response fields this plugin needs to know by itself, as opposed to the ones the configured
    // <field> elements point at. All are configurable because AEON renames and relocates them: the material type and
    // the home site were once the flat fields shippingOption and itemInfo2 and now live inside the nested
    // customFieldValues map. A slash separated path addresses a field inside a nested map.
    private String materialTypeField;
    private String homeSiteField;
    private String usernameField;
    private String referenceNumberField;
    private String transactionNumberField;

    @Getter
    private String selectedWorkflow;
    @Getter
    private List<String> possibleWorkflows = new ArrayList<>();

    @Getter
    @Setter
    private transient List<AeonRecord> recordList = new ArrayList<>();

    private List<Process> generatedProcesses = new ArrayList<>();

    private BeanHelper bhelp = new BeanHelper();

    private IJsonPlugin opacPlugin;
    private transient ConfigOpacCatalogue coc = null;

    // material type of the current transaction, as delivered by aeon. Aeon used to ship this value in a field called
    // shippingOption, which is where the old name of this field came from.
    @Getter
    private String materialType;

    /**
     * A property field applies to the current transaction when it is not restricted to a material type, or when its
     * restriction matches the material type of this transaction.
     *
     * Display, validation, cloning and export all have to ask this same question. When they disagree, a field can end
     * up hidden and mandatory at the same time, which makes process creation impossible with nothing to correct.
     *
     * The decision itself belongs to the property; this method exists because the xhtml has no other way to pass the
     * material type of the transaction.
     */
    public boolean appliesToMaterialType(AeonProperty property) {
        return property.appliesTo(materialType);
    }

    /**
     * Whether a material type AEON did not deliver is worth reporting.
     *
     * An installation says through materialTypeField whether it works with material types at all. AEON no longer
     * delivers one, so an unconfigured field is a supported setup rather than a fault: no property restricted to a
     * material type applies, which is the intended result and nothing to report. A field the configuration does name
     * and AEON leaves blank is the opposite - a value was expected and did not arrive.
     */
    static boolean shouldReportMissingMaterialType(String materialTypeField, String materialType) {
        return StringUtils.isNotBlank(materialTypeField) && StringUtils.isBlank(materialType);
    }

    @Override
    public PluginType getType() {
        return PluginType.Workflow;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_workflow_aeon_process_creation.xhtml";
    }

    @Getter
    @Setter
    // defines the current operation type, possible values are creation to create new processes and cancel to disable processes
    private String operationType = "creation";

    @Getter
    private String creationMode = "single";

    // creating one process per record is a legacy mode, hidden and refused unless the configuration switches it on
    @Getter
    private boolean allowMultiProcessCreation;

    private String transactionFieldName;

    // configuration defects found at load time. They block process creation rather than being reported once and
    // forgotten, because each of them makes the plugin do the wrong thing without producing an error of its own.
    private List<String> configurationErrors = new ArrayList<>();

    private String cancellationProjectName;

    private String cancellationStepName;
    @Getter
    @Setter
    private String cancellationSepcialRights;

    @Getter
    private String overviewMode = "";

    /*
     * Sends a Request to the goobi api (RestTest.java) and
     * recieves JSON String as response which is parsed into the AeonTransaction object
     */
    @SuppressWarnings("unchecked")
    public void sendRequest() {
        recordList.clear();

        if (StringUtils.isBlank(this.input)) {
            Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_no_identifier_given"));
            return;
        }

        if (hasConfigurationErrors()) {
            return;
        }

        if ("creation".equals(operationType)) {

            if (!checkTransactionNumber()) {
                Helper.setFehlerMeldung("plugin_workflow_aeon_transactionIdentifierInUse");
                return;
            }

            Map<String, Object> map = null;

            if ("1234567890".equals(this.input)) { //(JUST FOR TESTING: checks if input is 1234567890)
                try {
                    map = client.target("http://localhost:8080" + new HelperForm().getApplicationWebsiteUrl() + "api/")
                            .path("testingRest")
                            .path("aeon")
                            .request(MediaType.APPLICATION_JSON)
                            .get(Map.class);

                } catch (Exception e) {
                    log.error(e + " " + e.getMessage());
                }

            } else if (StringUtils.isNotBlank(apiKey)) {
                try (Response response = client.target(apiUrl)
                        .path("Requests")
                        .path(input)
                        .request(MediaType.APPLICATION_JSON)
                        .header("X-AEON-API-KEY", apiKey)
                        .get()) {
                    MediaType contentType = response.getMediaType();
                    if (response.getStatus() != Response.Status.OK.getStatusCode() || contentType == null
                            || !contentType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                        String body = response.readEntity(String.class);
                        log.error("AEON API request to {}/Requests/{} failed: HTTP {}, Content-Type {}, body: {}", apiUrl, input,
                                response.getStatus(), contentType, StringUtils.abbreviate(body, 2000));
                        Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_identifier_not_found") + ": HTTP "
                                + response.getStatus());
                        return;
                    }
                    map = response.readEntity(new GenericType<Map<String, Object>>() {
                    });
                } catch (Exception e) {
                    Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_identifier_not_found") + ": " + e.getMessage());
                    return;
                }
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
            if (map != null) {
                try {
                    // get data from user table
                    String username = AeonFieldResolver.resolveString(map, usernameField);
                    if (StringUtils.isNotBlank(username) && !username.contains("\\")) {
                        Map<String, Object> userDataMap = client.target(apiUrl)
                                .path("Users")
                                .path(username)
                                .request(MediaType.APPLICATION_JSON)
                                .header("X-AEON-API-KEY", apiKey)
                                .get(Map.class);
                        // these two names serve double duty: they read from the Users response and they are the keys
                        // that a configured aeon="lastName" / aeon="eMailAddress" field resolves against, so they
                        // cannot be made configurable without also renaming what the configuration refers to
                        map.put("lastName", userDataMap.get("lastName"));
                        map.put("eMailAddress", userDataMap.get("eMailAddress"));
                    }
                } catch (Exception e) {
                    log.error(e);
                }

                // report the required fields AEON does not deliver; a field that is missing entirely is reported apart
                // from one that is merely empty, as the first usually means it got renamed in AEON. Both are notices
                // and neither stops the request: AEON drops and renames fields, and the plugin has to stay usable when
                // it does
                for (String fieldname : requiredFields) {
                    if (!AeonFieldResolver.containsPath(map, fieldname)) {
                        Helper.setMeldung(Helper.getTranslation("plugin_workflow_aeon_fieldMissing", fieldname));
                    } else if (StringUtils.isBlank(AeonFieldResolver.resolveString(map, fieldname))) {
                        Helper.setMeldung(Helper.getTranslation("plugin_workflow_aeon_fieldNull", fieldname));
                    }
                }

                // the material type decides which property fields apply. Without one, no field restricted to a material
                // type applies, and the request goes on carrying the unrestricted properties alone - a reduced result
                // rather than a broken one, hence a notice rather than a rejection, and no notice at all where an
                // unconfigured materialTypeField says AEON delivers none. What makes that safe is that display,
                // validation, cloning and export all read the restriction through appliesToMaterialType, so a field
                // left out is left out everywhere instead of turning invisible while staying mandatory
                materialType = AeonFieldResolver.resolveString(map, materialTypeField);
                if (shouldReportMissingMaterialType(materialTypeField, materialType)) {
                    Helper.setMeldung(Helper.getTranslation("plugin_workflow_aeon_materialTypeMissing", materialTypeField));
                }

                // the transaction number opens every generated process title, so an empty one would produce unusable
                // titles and break the duplicate detection that matches on them. It is the one field the request
                // cannot go on without
                String transactionNumber = AeonFieldResolver.resolveString(map, transactionNumberField);
                if (StringUtils.isBlank(transactionNumber)) {
                    Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_transactionNumberMissing", transactionNumberField));
                    return;
                }

                for (AeonProperty property : transactionFields) {
                    if (StringUtils.isNotBlank(property.getAeonField())) {
                        property.setValue(AeonFieldResolver.resolveString(map, property.getAeonField()));
                    }
                }

                for (AeonProperty property : propertyFields) {
                    if (StringUtils.isNotBlank(property.getAeonField())) {
                        property.setValue(AeonFieldResolver.resolveString(map, property.getAeonField()));
                    }
                }

                // use configured default template name
                selectedWorkflow = null;
                // first, check if a special workflow name was configured for the current type
                if (specialWorkflowNames.containsKey(materialType)) {
                    String workflowName = specialWorkflowNames.get(materialType);
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

                String catalogueIdentifier = AeonFieldResolver.resolveString(map, referenceNumberField);

                // three files decide which catalogue names a workflow is offered, so a configured name matching none
                // of them is only a spelling difference away. The lookup is therefore checked before use and reports
                // the names that do exist, rather than leaving the plugin null for the next line to dereference
                List<ConfigOpacCatalogue> catalogues = ConfigOpac.getInstance().getAllCatalogues(defaultWorkflowName);
                ConfigOpacCatalogue catalogue = AeonConfigValidator.catalogueByName(catalogues, opacName);
                if (catalogue == null) {
                    Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_opacNotFound", opacName,
                            AeonConfigValidator.catalogueTitles(catalogues)));
                    return;
                }
                // records arrive as json and are read through the json opac interface, so a catalogue served by any
                // other opac plugin cannot be used. Checked here because the cast below would name nothing
                if (!(catalogue.getOpacPlugin() instanceof IJsonPlugin)) {
                    Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_opacNotJson", opacName));
                    return;
                }

                coc = catalogue;
                opacPlugin = (IJsonPlugin) catalogue.getOpacPlugin();
                if ("1234567890".equals(this.input)) { //(JUST FOR TESTING: checks if input is 1234567890)
                    opacPlugin.setTestMode(true);
                } else {
                    for (ISearchField sf : opacPlugin.getSearchFieldList()) {
                        if ("Barcode".equals(sf.getId())) {
                            sf.setText(catalogueIdentifier);
                        }
                    }
                }
                try {
                    opacPlugin.search("", "", coc, null);
                } catch (Exception e) {
                    log.error(e);
                }

                // Bib ID and Volume number (bibId, volume)
                // ASpace resource ID (uri)
                // preparation for duplicate checks
                AeonProperty aspaceResourceIDField = null;
                AeonProperty ilsBibIdField = null;
                AeonProperty ilsVolumeNumberField = null;
                for (AeonProperty ap : recordFields) {
                    if ("bibId".equals(ap.getAeonField())) {
                        ilsBibIdField = ap;
                    } else if ("volume".equals(ap.getAeonField())) {
                        ilsVolumeNumberField = ap;
                    } else if ("uri".equals(ap.getAeonField())) {
                        aspaceResourceIDField = ap;
                    }
                }

                if (opacPlugin.getOverviewList() != null) {
                    for (Map<String, String> overview : opacPlugin.getOverviewList()) {
                        AeonRecord aeonRecord = new AeonRecord();
                        recordList.add(aeonRecord);
                        aeonRecord.setRecordData(overview);
                        for (AeonProperty p : recordFields) {
                            AeonProperty prop = p.cloneProperty();
                            prop.setValue(overview.get(prop.getAeonField()));
                            if (StringUtils.isNotBlank(prop.getValue())) {
                                aeonRecord.getProperties().add(prop);
                            }
                            prop.setOverwriteMainField(false);
                        }

                        // Process title: Job#-repository-AeonTN e.g 999-music-12345. The job# is an auto generated one-up number.
                        // Repository pulled from Aeon field - Site
                        // AeonTN pulled from Aeon field - transactionNumber

                        // get next free id
                        String repository = Optional.ofNullable(AeonFieldResolver.resolveString(map, homeSiteField)).orElse("unknown");

                        String generatedTitle = transactionNumber + "_" + repository;
                        aeonRecord.setProcessTitle(generatedTitle);

                        // copy properties
                        for (AeonProperty p : propertyFields) {
                            if (appliesToMaterialType(p)) {
                                AeonProperty prop = p.cloneProperty();
                                prop.setOverwriteMainField(true);
                                prop.setValue(p.getValue());
                                prop.setDefaultValue(p.getValue());
                                aeonRecord.getProcessProperties().add(prop);
                            }
                        }

                        //  check for duplicates in active projects, load the processes with the same transaction number
                        List<Process> processes =
                                ProcessManager.getProcesses("prozesse.erstellungsdatum desc", "prozesse.titel like \"%" + generatedTitle + "%\"",
                                        null);

                        for (Process other : processes) {
                            // check if properties matches
                            boolean isDuplicate = false;
                            boolean isBlocked = false;

                            // both stores are read once for this process, then every field is answered from whichever
                            // one it is configured for
                            AeonProcessValues values =
                                    new AeonProcessValues(MetadataManager.getMetadata(other.getId()), other.getProperties());

                            if ("ils".matches(overview.get("recordType"))) {
                                isDuplicate = ilsBibIdField != null && ilsVolumeNumberField != null
                                        && values.first(ilsBibIdField).equals(overview.get("bibId"))
                                        && values.first(ilsVolumeNumberField).equals(overview.get("volume"));
                            } else {
                                isDuplicate = aspaceResourceIDField != null
                                        && values.all(aspaceResourceIDField).contains(overview.get("uri"));
                            }

                            if (isDuplicate) {
                                if (other.getProjekt().getProjectIsArchived().booleanValue()) {
                                    isBlocked = true;
                                }

                                aeonRecord.setDuplicateTitle(other.getTitel());
                                aeonRecord.setDuplicate(true);
                                aeonRecord.setLocked(isBlocked);

                                AeonExistingProcess aep = new AeonExistingProcess();
                                aep.setTitle(other.getTitel());
                                aep.setDate(other.getErstellungsdatumAsString());
                                for (AeonProperty property : existingProcessFields) {
                                    AeonProperty aeonProperty = property.cloneProperty();
                                    aeonProperty.setValue("");
                                    aep.getDuplicateProperties().add(aeonProperty);
                                    extractProcessValues(values, aeonProperty);
                                }
                                aeonRecord.getExistingProcesses().add(aep);
                            }
                        }
                    }

                    Collections.sort(recordList);

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

    /**
     * True when no process carries this transaction number yet.
     *
     * The lookup goes through the metadata index, which Goobi fills from the top level docstruct whenever a process
     * writes its mets file. validateConfiguration makes sure the transaction number is configured as metadata, so
     * there is no second store to consult here.
     *
     * MetadataManager.getProcessesWithMetadata matches the value as a substring, so its result is only a candidate
     * list: transaction number 1234 also matches 12345. Each candidate is therefore compared exactly before it counts.
     */
    private boolean checkTransactionNumber() {
        for (Integer processId : MetadataManager.getProcessesWithMetadata(transactionFieldName, input)) {
            if (carriesTransactionNumber(processId)) {
                return false;
            }
        }
        return true;
    }

    /** true when the process carries exactly this transaction number, not merely one containing it */
    private boolean carriesTransactionNumber(Integer processId) {
        for (StringPair metadata : MetadataManager.getMetadata(processId)) {
            if (transactionFieldName.equals(metadata.getOne()) && input.equals(metadata.getTwo())) {
                return true;
            }
        }
        return false;
    }

    private int getNextId() {
        int value = 0;

        String sql = "SELECT max(property_value) FROM properties where property_name = 'OrderNumber' and object_type = 'process'";
        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            String s = new QueryRunner().query(connection, sql, MySQLHelper.resultSetToStringHandler);
            if (StringUtils.isNotBlank(s)) {
                value = Integer.parseInt(s);
            }
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (connection != null) {
                try {
                    MySQLHelper.closeConnection(connection);
                } catch (SQLException e) {
                    // do nothing
                }
            }
        }

        return value + 1;
    }

    /**
     * Copies the value the existing process carries into the field used to display it.
     */
    private void extractProcessValues(AeonProcessValues values, AeonProperty aeonProperty) {
        aeonProperty.setValue(values.first(aeonProperty));
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
        overviewMode = "";
    }

    public void nextPage() {
        // if validation fails, stay on first page
        if (validateSelection()) {
            screenName = "summary";
            overviewMode = "";
        }
    }

    private boolean validateSelection() {
        // check if at least one record was selected

        int numberOfRecordsToCreate = 0;
        for (AeonRecord rec : recordList) {
            if (rec.isAccepted()) {
                numberOfRecordsToCreate++;
            }
        }
        if (numberOfRecordsToCreate == 0) {
            Helper.setFehlerMeldung("plugin_workflow_aeon_nothing_selected");
            return false;
        }

        //  validate properties, details, process values

        for (AeonProperty prop : propertyFields) {
            if (appliesToMaterialType(prop) && !prop.isValid() && prop.isStrictValidation()) {
                Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_process_properties");
                return false;
            }
        }
        for (AeonRecord rec : recordList) {
            if (rec.isAccepted()) {
                for (AeonProperty prop : rec.getProcessProperties()) {
                    if (!prop.isValid() && prop.isStrictValidation()) {
                        Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_process_properties");
                        return false;
                    }
                }
            }
        }

        for (AeonProperty prop : transactionFields) {
            if (!prop.isValid()) {
                Helper.setFehlerMeldung("plugin_workflow_aeon_invalid_transaction_fields");
                return false;
            }
        }

        return true;
    }

    public void previousPage() {
        screenName = "request";
    }

    /**
     * Reports any configuration defect found at load time and tells the caller to stop.
     *
     * Both entry points ask: creating processes and searching for ones to cancel. A broken configuration otherwise
     * produces an empty screen on the cancellation path rather than saying what is wrong, which is the silent failure
     * this validation exists to remove.
     */
    private boolean hasConfigurationErrors() {
        for (String error : configurationErrors) {
            Helper.setFehlerMeldung(error);
        }
        return !configurationErrors.isEmpty();
    }

    public void createProcesses() {

        if (hasConfigurationErrors()) {
            return;
        }

        if (!validateSelection()) {
            return;
        }

        Process processTemplate = ProcessManager.getProcessByExactTitle(selectedWorkflow);
        Batch batch = null;

        // the ruleset is the same for every process of this request, so it is resolved once here rather than inside
        // each branch, and the metadata types are checked against it before anything is created: a missing ruleset
        // entry is a configuration defect, and stopping here means no process is written with part of its data
        // silently dropped
        Prefs prefs = processTemplate.getRegelsatz().getPreferences();

        List<AeonProperty> configuredFields = new ArrayList<>();
        configuredFields.addAll(transactionFields);
        configuredFields.addAll(recordFields);
        configuredFields.addAll(propertyFields);
        List<String> unknownTypes = AeonMetadataWriter.unknownTypes(prefs, configuredFields);
        if (!unknownTypes.isEmpty()) {
            Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_unknownMetadataType",
                    String.join(", ", unknownTypes)));
            return;
        }

        // create a single process with all selected records in it
        if ("single".equals(creationMode)) {
            Process process = new Process();
            process.setProjekt(processTemplate.getProjekt());
            process.setRegelsatz(processTemplate.getRegelsatz());
            process.setDocket(processTemplate.getDocket());

            bhelp.SchritteKopieren(processTemplate, process);
            bhelp.EigenschaftenKopieren(processTemplate, process);

            bhelp.EigenschaftHinzufuegen(process, "Template", processTemplate.getTitel());
            bhelp.EigenschaftHinzufuegen(process, "TemplateID", "" + processTemplate.getId());

            process.setTitel(getBatchProcessTitle());

            Fileformat masterFileformat = null;
            try {
                masterFileformat = new MetsMods(prefs);
                DigitalDocument digDoc = new DigitalDocument();
                masterFileformat.setDigitalDocument(digDoc);
                digDoc.setLogicalDocStruct(digDoc.createDocStruct(prefs.getDocStrctTypeByName(batchDocstruct)));
                digDoc.setPhysicalDocStruct(digDoc.createDocStruct(prefs.getDocStrctTypeByName("BoundBook")));
                // add CatalogIDDigital
                Metadata identifier = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
                identifier.setValue(input);
                digDoc.getLogicalDocStruct().addMetadata(identifier);
            } catch (UGHException e) {
                log.error(e);
            }

            DocStruct batchRoot = null;
            try {
                if (masterFileformat != null && masterFileformat.getDigitalDocument() != null) {
                    batchRoot = masterFileformat.getDigitalDocument().getLogicalDocStruct();
                }
            } catch (PreferencesException e) {
                log.error(e);
                return;
            }

            if (batchRoot == null) {
                // the catch above already logged why. The fileformat is assigned before the docstruct is built, so a
                // failure in that try - a batchDocstruct the ruleset does not define, for instance - leaves the
                // reference set and the root missing. Either way there is nothing to attach metadata to.
                Helper.setFehlerMeldung("Error while creating the Goobi processes");
                return;
            }

            // the transaction fields describe the request rather than any one record, so they go once onto the root of
            // the batch. That is also the docstruct Goobi indexes, which is what the duplicate check reads back.
            try {
                assignTransactionFields(process, batchRoot, prefs);
            } catch (AeonMetadataException e) {
                Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_metadataNotAllowed", e.getMetadataType()));
                return;
            }

            int nextFreeId = getNextId();
            // create batch mets file
            // root data: id/title = input
            for (AeonRecord r : recordList) {
                if (r.isAccepted()) {
                    // The order number is the correlation key: it links a property of the batch to the child process it belongs to. It is
                    // deliberately not a predicted child title - the split processes plugin composes the titles itself, from metadata that is not
                    // available here.
                    String orderNumber = getOrderAsString(nextFreeId);
                    nextFreeId++;
                    // for each selected record: create child docstruct, assign data
                    String recordIdentifier = r.getRecordData().get("uri"); // get uri from properties
                    try {
                        opacPlugin.setSelectedUrl(recordIdentifier);

                        // get metadata for selected record
                        Fileformat fileformat = opacPlugin.search("", "", coc, prefs);

                        // the record fields belong to this record, so they go onto its own docstruct. The fields that
                        // stay process properties keep the per-record prefix - the correlation key - which is the only
                        // way a property can say which record of the batch it came from.
                        assignRecordFields(r, process, fileformat.getDigitalDocument().getLogicalDocStruct(), prefs, orderNumber + "_");

                        //  save the correlation key as metadata, so the split processes plugin can match this child's properties
                        Metadata childKeyMetadata = new Metadata(prefs.getMetadataTypeByName(childKeyMetadataType));
                        childKeyMetadata.setValue(orderNumber);
                        fileformat.getDigitalDocument().getLogicalDocStruct().addMetadata(childKeyMetadata);

                        if (resourceIDMetadataType != null) {
                            Metadata resourceIDMetadata = new Metadata(prefs.getMetadataTypeByName(resourceIDMetadataType));
                            resourceIDMetadata.setValue(recordIdentifier);
                            fileformat.getDigitalDocument().getLogicalDocStruct().addMetadata(resourceIDMetadata);
                        }

                        masterFileformat.getDigitalDocument().getLogicalDocStruct().addChild(fileformat.getDigitalDocument().getLogicalDocStruct());

                    } catch (AeonMetadataException e) {
                        // nothing has been saved yet in this branch, so returning here creates nothing at all
                        Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_metadataNotAllowed", e.getMetadataType()));
                        return;
                    } catch (Exception e) {
                        log.error("Failed to create structure element for aeon record", e);
                        Helper.setFehlerMeldung("Failed to create structure element for aeon record - check that the metadata type '"
                                + childKeyMetadataType
                                + "' configured as <childKeyMetadata> exists in the ruleset and is allowed on the child docstruct types", e);
                    }
                }
            }

            bhelp.EigenschaftHinzufuegen(process, "OrderNumber", getOrderAsString(--nextFreeId));
            try {
                // save process and fileformat
                finalizeProcess(process, masterFileformat);
            } catch (WriteException | PreferencesException | IOException | SwapException | DAOException e) {
                log.error(e);
            }

        } else {
            // create a process for each record

            for (AeonRecord rec : recordList) {
                if (rec.isAccepted()) {
                    // create process

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
                    bhelp.EigenschaftenKopieren(processTemplate, process);

                    bhelp.EigenschaftHinzufuegen(process, "Template", processTemplate.getTitel());
                    bhelp.EigenschaftHinzufuegen(process, "TemplateID", "" + processTemplate.getId());

                    try {
                        // create mets file for selected record
                        String recordIdentifier = rec.getRecordData().get("uri"); // get uri from properties
                        opacPlugin.setSelectedUrl(recordIdentifier);
                        Fileformat fileformat = opacPlugin.search("", "", coc, prefs); // get metadata for selected record

                        // this process holds a single record, so everything goes onto its own docstruct, which is the
                        // top level one and therefore the one Goobi indexes
                        DocStruct recordStruct = fileformat.getDigitalDocument().getLogicalDocStruct();
                        assignTransactionFields(process, recordStruct, prefs);
                        assignRecordFields(rec, process, recordStruct, prefs, "");

                        int nextFreeId = getNextId();
                        String orderNumber = getOrderAsString(nextFreeId);

                        process.setTitel(rec.getProcessTitle() + "_" + orderNumber);
                        rec.setProcessTitle(process.getTitel());
                        bhelp.EigenschaftHinzufuegen(process, "OrderNumber", orderNumber);

                        // save process and fileformat
                        finalizeProcess(process, fileformat);
                    } catch (AeonMetadataException e) {
                        // processes created earlier in this loop are already saved and stay; the message says so
                        Helper.setFehlerMeldung(Helper.getTranslation("plugin_workflow_aeon_metadataNotAllowed", e.getMetadataType()));
                        return;
                    } catch (Exception e) {
                        log.error("Error while creating the Goobi processes", e);
                        Helper.setFehlerMeldung("Error while creating the Goobi processes", e);
                    }

                }
            }
        }

    }

    public void finalizeProcess(Process process, Fileformat fileformat)
            throws IOException, SwapException, WriteException, PreferencesException, DAOException {
        ProcessManager.saveProcess(process);
        process.writeMetadataFile(fileformat);
        generatedProcesses.add(process);
        screenName = "summary";
        overviewMode = "saved";
        Helper.setMeldung(Helper.getTranslation("plugin_workflow_aeon_processCreated") + ": " + process.getTitel());

        // start any open automatic tasks
        for (Step s : process.getSchritteList()) {
            if (StepStatus.OPEN.equals(s.getBearbeitungsstatusEnum()) && s.isTypAutomatisch()) {
                ScriptThreadWithoutHibernate myThread = new ScriptThreadWithoutHibernate(s);
                myThread.startOrPutToQueue();
            }
        }
    }

    /**
     * Appends the configured suffix to the title of a batch process. The suffix marks a process that is meant to be split into child processes; the
     * split plugin strips it again when it derives the child titles.
     *
     * @param recordTitle the composed title of the first record of the batch
     * @param suffix the configured suffix, may be null or blank
     */
    static String composeBatchTitle(String recordTitle, String suffix) {
        if (StringUtils.isBlank(suffix)) {
            return recordTitle;
        }
        return recordTitle + suffix.trim();
    }

    /**
     * Appends the configured suffix to the title of the batch process the given records will be combined into. All records of a transaction carry
     * the same title, so the first one decides.
     *
     * @param records the records of the current transaction, may be empty
     * @param suffix the configured suffix, may be null or blank
     */
    static String composeBatchTitle(List<AeonRecord> records, String suffix) {
        if (records.isEmpty()) {
            return "";
        }
        return composeBatchTitle(records.get(0).getProcessTitle(), suffix);
    }

    /**
     * The title of a process created for a single record. Its order number is drawn from the database when the process is created, so beforehand
     * the title can only be shown with that part marked as still open. Passing the incomplete title off as the final one would be a claim this
     * plugin cannot keep.
     *
     * @param recordTitle the title of the record, which after creation is the title of its process
     * @param processCreated whether the process exists already
     */
    static String composeRecordTitle(String recordTitle, boolean processCreated) {
        if (processCreated) {
            return recordTitle;
        }
        return recordTitle + "_" + UNKNOWN_TITLE_PART;
    }

    /**
     * The title of the batch process the accepted records are combined into, suffix included. Creation reads it from here as well, so what the
     * summary screen announces and what is created cannot drift apart.
     */
    public String getBatchProcessTitle() {
        return composeBatchTitle(recordList, batchTitleSuffix);
    }

    /**
     * The process title to show for a single record. Only used in per record creation mode: a batch has one title for all records, and the titles
     * of the child processes it is split into are not decided here.
     *
     * The decision needs the creation state of the request, which the xhtml has no other way to pass.
     */
    public String processTitleFor(AeonRecord record) {
        return composeRecordTitle(record.getProcessTitle(), "saved".equals(overviewMode));
    }

    public void setCreationMode(String creationMode) {
        this.creationMode = effectiveCreationMode(creationMode, allowMultiProcessCreation);
    }

    /**
     * The creation mode the plugin actually uses. Anything but an allowed 'multi' falls back to the batch mode, so neither a hidden radio nor an
     * unknown value can reach the legacy per record creation.
     *
     * @param requested the mode the request asked for
     * @param allowMulti whether the configuration allows the legacy per record creation
     * @return 'multi' if requested and allowed, 'single' otherwise
     */
    static String effectiveCreationMode(String requested, boolean allowMulti) {
        if (allowMulti && "multi".equals(requested)) {
            return "multi";
        }
        return "single";
    }

    public String getOrderAsString(int nextFreeId) {
        String orderNumber;
        if (nextFreeId > 999) {
            orderNumber = String.valueOf(nextFreeId);
        } else if (nextFreeId > 99) {
            orderNumber = "0" + nextFreeId;
        } else if (nextFreeId > 9) {
            orderNumber = "00" + nextFreeId;
        } else {
            orderNumber = "000" + nextFreeId;
        }
        return orderNumber;
    }

    /**
     * Writes the fields of one record. The value resolution is unchanged: a record field takes its value from the
     * matching per-record property when the operator edited one. Only where the value ends up is new - a field goes to
     * the METS file or to a process property according to the place it declares.
     */
    public void assignRecordFields(AeonRecord rec, Process process, DocStruct docStruct, Prefs prefs, String prefix)
            throws AeonMetadataException {

        // check if patron type yale was selected
        for (AeonProperty prop : rec.getProcessProperties()) {
            if ("Patron type".equals(prop.getTitle()) && "Yale".equals(prop.getValue())) {
                // if this is the case, all steps get higher priority
                for (Step step : process.getSchritte()) {
                    step.setPrioritaet(1);
                }
            }
        }

        for (AeonProperty prop : rec.getProperties()) {
            if (appliesToMaterialType(prop)) {
                if ("multiselect".equals(prop.getType())) {
                    // get local, global value
                    List<String> values = prop.getMultiselectSelectedValues();
                    for (AeonProperty localProperty : rec.getProcessProperties()) {
                        if (prop.getTitle().equals(localProperty.getTitle()) && !localProperty.getMultiselectSelectedValues().isEmpty()) {
                            values = localProperty.getMultiselectSelectedValues();
                        }
                    }
                    for (String value : values) {
                        write(prop, value, process, docStruct, prefs, prefix);
                    }
                } else {
                    String value = prop.getExportValue();
                    for (AeonProperty localProperty : rec.getProcessProperties()) {
                        if (prop.getTitle().equals(localProperty.getTitle()) && StringUtils.isNoneBlank(localProperty.getExportValue())) {
                            value = localProperty.getExportValue();
                        }
                    }

                    if (StringUtils.isNoneBlank(value)) {
                        write(prop, value, process, docStruct, prefs, prefix);
                    }
                }
            }
        }

        for (AeonProperty prop : rec.getProcessProperties()) {
            if ("multiselect".equals(prop.getType())) {
                for (String val : prop.getMultiselectSelectedValues()) {
                    write(prop, val, process, docStruct, prefs, prefix);
                }
            } else {
                write(prop, prop.getExportValue(), process, docStruct, prefs, prefix);
            }
        }
    }

    /**
     * Writes the transaction fields. They are the same for every record of a request, so they are written once per
     * process rather than once per record - in batch mode the old code wrote each of them once per record under a
     * different prefix, which stored the same value as many times as the batch had records.
     */
    private void assignTransactionFields(Process process, DocStruct docStruct, Prefs prefs) throws AeonMetadataException {
        for (AeonProperty prop : transactionFields) {
            if (StringUtils.isNoneBlank(prop.getExportValue())) {
                write(prop, prop.getExportValue(), process, docStruct, prefs, "");
            }
        }
    }

    /**
     * Routes one value to the store its field declares.
     *
     * A blank value is skipped for metadata but still written as a property: an empty metadatum has no meaning in a
     * METS file, while an empty property is what the plugin has always written and something downstream may rely on
     * the row existing.
     */
    private void write(AeonProperty field, String value, Process process, DocStruct docStruct, Prefs prefs, String prefix)
            throws AeonMetadataException {
        if (field.isMetadata()) {
            if (StringUtils.isNotBlank(value)) {
                AeonMetadataWriter.add(docStruct, prefs, field.getVariableName(), value);
            }
        } else {
            bhelp.EigenschaftHinzufuegen(process, prefix + field.getVariableName(), value);
        }
    }

    /**
     * Constructor try to use empty constructor, remove configuration from here
     */
    public AeonProcessCreationWorkflowPlugin() {
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
        batchDocstruct = config.getString("/processCreation/batchDocstruct");
        batchTitleSuffix = config.getString("/processCreation/batchTitleSuffix", "");
        allowMultiProcessCreation = config.getBoolean("/processCreation/allowMultiProcessCreation", false);
        // a mode chosen before the switch was turned off must not outlive it
        creationMode = effectiveCreationMode(creationMode, allowMultiProcessCreation);

        childKeyMetadataType = config.getString("/processCreation/childKeyMetadata");
        resourceIDMetadataType = config.getString("/processCreation/resourceIDMetadata", null);
        // no default: an absent element says AEON delivers no material type, which switches the restrictions off
        // rather than silently reinstating a field name AEON has since dropped
        materialTypeField = config.getString("/processCreation/materialTypeField", "");
        homeSiteField = config.getString("/processCreation/homeSiteField", "site");
        usernameField = config.getString("/processCreation/usernameField", "username");
        referenceNumberField = config.getString("/processCreation/referenceNumberField", "referenceNumber");
        transactionNumberField = config.getString("/processCreation/transactionNumberField", "transactionNumber");
        // process cancellation
        transactionFieldName = config.getString("/processCancellation/transactionFieldName");
        cancellationProjectName = config.getString("/processCancellation/projectName");
        cancellationStepName = config.getString("/processCancellation/stepName");
        cancellationSepcialRights = config.getString("/processCancellation/specialRights");

        //read <transaction> and <processes>
        transactionFields.clear();
        List<HierarchicalConfiguration> transactions = config.configurationsAt("/transaction/field");
        for (HierarchicalConfiguration hc : transactions) {
            AeonProperty property = new AeonProperty(hc, this);
            transactionFields.add(property);
        }
        recordFields.clear();
        List<HierarchicalConfiguration> processes = config.configurationsAt("/processes/field");
        for (HierarchicalConfiguration hc : processes) {
            AeonProperty property = new AeonProperty(hc, this);
            recordFields.add(property);
        }

        requiredFields = Arrays.asList(config.getStringArray("/requiredFields/field"));

        propertyFields.clear();
        List<HierarchicalConfiguration> properties = config.configurationsAt("/properties/field");
        for (HierarchicalConfiguration hc : properties) {
            AeonProperty property = new AeonProperty(hc, this);
            propertyFields.add(property);
        }

        existingProcessFields.clear();
        List<HierarchicalConfiguration> existingProcessProperties = config.configurationsAt("/existingProcessProperties/field");
        for (HierarchicalConfiguration hc : existingProcessProperties) {
            AeonProperty property = new AeonProperty(hc, this);
            existingProcessFields.add(property);
        }

        String sql = FilterHelper.criteriaBuilder("", true, null, null, null, true, false);
        // load all process templates
        List<Process> templates = ProcessManager.getProcesses("prozesse.titel", sql, null);
        for (Process p : templates) {
            possibleWorkflows.add(p.getTitel());
        }

        validateConfiguration();
    }

    /**
     * Checks the parts of the configuration whose mistakes produce no error of their own: a place attribute naming a
     * store that does not exist, and a transaction field name that does not match the field carrying the transaction
     * number. Both are collected rather than thrown, so that the request screen can name all of them at once.
     */
    private void validateConfiguration() {
        configurationErrors.clear();

        List<AeonProperty> allFields = new ArrayList<>();
        allFields.addAll(transactionFields);
        allFields.addAll(recordFields);
        allFields.addAll(propertyFields);
        allFields.addAll(existingProcessFields);

        for (String fieldTitle : AeonConfigValidator.invalidPlaces(allFields)) {
            configurationErrors.add(Helper.getTranslation("plugin_workflow_aeon_invalidPlace", fieldTitle));
        }

        AeonProperty transactionNumber = AeonConfigValidator.transactionNumberField(transactionFields, transactionNumberField);

        // the config need not carry a <processCancellation> section at all - the documented sample does
        // not. Deriving the name from the transaction field keeps the duplicate check and the
        // cancellation search working instead of leaving them searching for null.
        if (transactionNumber != null && StringUtils.isBlank(transactionFieldName)) {
            transactionFieldName = transactionNumber.getVariableName();
        }

        if (transactionNumber != null) {
            if (!transactionNumber.getVariableName().equals(transactionFieldName)) {
                configurationErrors.add(Helper.getTranslation("plugin_workflow_aeon_transactionFieldNameMismatch",
                        transactionFieldName, transactionNumber.getVariableName()));
            }
            // the duplicate check and the cancellation search look the transaction number up through the metadata
            // index, which only holds fields written as metadata
            if (!transactionNumber.isMetadata()) {
                configurationErrors.add(Helper.getTranslation("plugin_workflow_aeon_transactionFieldNotMetadata",
                        transactionNumber.getTitle()));
            }
        }

        // without a name to search by, the duplicate check and the cancellation search would return nothing
        // and read as "no existing process" for every request
        if (StringUtils.isBlank(transactionFieldName)) {
            configurationErrors.add(Helper.getTranslation("plugin_workflow_aeon_transactionFieldMissing"));
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

    public void updateProperties(String propertyName, String newValue, String additionalValue) { //NOSONAR
        for (AeonRecord aeonRecord : recordList) {
            for (AeonProperty prop : aeonRecord.getProcessProperties()) {
                if (prop.getTitle().equals(propertyName)) {
                    // configure new default value
                    prop.setDefaultValue(newValue);
                    // update property value
                    prop.setValue(newValue);
                    prop.setAdditionalValue(additionalValue);
                    break;
                }
            }
        }
    }

    public void updateMultiselectProperties(String propertyName, List<String> values) { //NOSONAR
        for (AeonRecord aeonRecord : recordList) {
            for (AeonProperty prop : aeonRecord.getProcessProperties()) {
                if (prop.getTitle().equals(propertyName)) {
                    // configure new default value
                    prop.setDefaultSelectedValues(new ArrayList<>(values));
                    prop.setMultiselectSelectedValues(new ArrayList<>(values));
                    break;
                }
            }
        }
    }

    public void searchForDeactivateProcess() {

        recordList.clear();

        // the old query compared property_value against the field name and against the searched number at the same
        // time, which no row can satisfy, so it never matched anything. The lookup now goes through the metadata
        // index, the same one the duplicate check reads.
        List<Integer> candidateIds = MetadataManager.getProcessesWithMetadata(transactionFieldName, input);
        if (candidateIds.isEmpty()) {
            return;
        }

        // that lookup matches the value as a substring, so searching for 123 also returns 1234 and 91234. These
        // records are what cancelProcesses acts on, so an inexact match here cancels a process nobody asked about.
        List<Integer> processIds = candidateIds.stream().filter(this::carriesTransactionNumber).toList();
        if (processIds.isEmpty()) {
            return;
        }

        String sql = "prozesse.ProzesseID in ("
                + processIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                + ") AND projekte.projectIsArchived = false ";

        List<Process> processList = ProcessManager.getProcesses("prozesse.titel", sql, null);

        // the field the cancellation list is sorted by, taken from the configuration rather than hardcoded
        AeonProperty orderField = null;
        for (AeonProperty ap : recordFields) {
            if ("containerGrouping".equals(ap.getAeonField())) {
                orderField = ap;
            }
        }

        for (Process process : processList) {
            AeonRecord aeonRecord = new AeonRecord();
            aeonRecord.setProcessTitle(process.getTitel());
            aeonRecord.setExistingProcess(process);

            AeonProcessValues values =
                    new AeonProcessValues(MetadataManager.getMetadata(process.getId()), process.getProperties());
            aeonRecord.setOrderValue(orderField == null ? "" : values.first(orderField));

            for (AeonProperty p : recordFields) {
                AeonProperty prop = p.cloneProperty();
                prop.setOverwriteMainField(true);
                extractProcessValues(values, prop);
                aeonRecord.getProperties().add(prop);

            }
            for (AeonProperty property : propertyFields) {
                AeonProperty aeonProperty = property.cloneProperty();
                aeonProperty.setOverwriteMainField(true);
                extractProcessValues(values, aeonProperty);

                aeonRecord.getProcessProperties().add(aeonProperty);
            }

            //            Orders that are before condition assessment step - cancel and don’t retain in Goobi.
            //            Anything after Condition Assessment cancel and retain in Goobi.
            //            Public Services could cancel through Aeon TN plugin before condition assessment.
            //            Only Preservation can cancel after condition assessment

            aeonRecord.setDisabled(true);

            try {
                org.goobi.beans.User u = Helper.getCurrentUser();
                if (u != null && u.getAllUserRoles().contains(cancellationSepcialRights)) {
                    aeonRecord.setDisabled(false);
                }

            } catch (Exception e) {
                log.error(e);
            }
            aeonRecord.setDuplicate(true);

            aeonRecord.setDuplicateTitle(process.getTitel());

            AeonExistingProcess aep = new AeonExistingProcess();
            aep.setTitle(process.getTitel());
            aep.setDate(process.getErstellungsdatumAsString());
            for (AeonProperty property : existingProcessFields) {
                AeonProperty aeonProperty = property.cloneProperty();
                aeonProperty.setValue("");
                aep.getDuplicateProperties().add(aeonProperty);
                extractProcessValues(values, aeonProperty);
            }

            aeonRecord.getExistingProcesses().add(aep);

            // new implementation checks, if a certain step is finished
            // if yes, only a special user group is allowed to cancel items

            for (Step step : process.getSchritte()) {
                if (step.getTitel().equals(cancellationStepName) && step.getBearbeitungsstatusEnum() != StepStatus.DONE) {
                    aeonRecord.setDisabled(false);
                    aeonRecord.setDeletable(true);
                    break;
                }
            }

            recordList.add(aeonRecord);
        }

        if (!recordList.isEmpty()) {
            Collections.sort(recordList);
            setRequestSuccess(true);
        }
    }

    public void cancelProcesses() {
        for (AeonRecord aeonRecord : recordList) {

            Project project = null;
            try {
                project = ProjectManager.getProjectByName(cancellationProjectName);
            } catch (DAOException e) {
                log.error(e);
            }
            if (project == null) {
                Helper.setFehlerMeldung("keinProjektAngegeben");
                return;
            }
            // move selected processes to configured project
            if (!aeonRecord.isDisabled() && aeonRecord.isAccepted()) {
                Process process = aeonRecord.getExistingProcess();
                if (aeonRecord.isDeletable()) {
                    ProcessManager.deleteProcess(process);
                } else {
                    process.setProjekt(project);
                    process.setProjectId(project.getId());
                    ProcessManager.saveProcessInformation(process);
                }
            }
        }
        resetRequest();
    }

    private transient AeonRecord popupItem;

    public AeonRecord getPopupItem() {
        return popupItem;
    }

    public void setPopupItem(AeonRecord popupItem) {
        this.popupItem = popupItem;
    }
}
