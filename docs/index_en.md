---
title: AEON data transfer
identifier: intranda_workflow_aeon_process_creation
description: This workflow plugin enables Goobi processes to be created on the basis of an initial AEON request. Among other things, it enables manual selection of elements from the AEON response.
published: true
keywords:
    - Goobi workflow
    - Plugin
    - Workflow Plugin
---
## Introduction
This plugin can be used to query individual data sets from AEON. After a record has been queried, all metadata from the so-called Metadata Cloud are retrieved and listed. The user can then select which data sets he or she would like to create as a Goobi operation.


## Installation
The plug-in consists of the following files to be installed:

```bash
plugin-intranda-workflow-aeon-process-creation-base.jar
plugin-intranda-workflow-aeon-process-creation-gui.jar
plugin_intranda_workflow_aeon_process_creation.xml
```

These files must be installed in the correct directories so that they are available under the following paths after installation:

```bash
/opt/digiverso/goobi/plugins/workflow/plugin-intranda-workflow-aeon-process-creation-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-intranda-workflow-aeon-process-creation-gui.jar
/opt/digiverso/goobi/config/plugin_intranda_workflow_aeon_process_creation.xml
```

This plugin has its own permission level for use. For this reason, users must have the required permissions. Therefore, please assign the following right to the user group of the corresponding users:

```
Plugin_workflow_aeon_process_creation
```

![Correctly assigned right for the users](screen2_en.png)


## Overview and functionality
If the user has the authorisation to use the plugin, the plugin `AEON Query Plugin` can be opened in the menu item `Workflow`.

![Opened plugin without executed search](screen3_en.png)

A transaction identifier can now be entered here to start the search for an AEON record.

![Displaying the data after performing a search](screen4_en.png)

If a successful search was performed, the `query details` area was filled with the fields that were defined within the configuration in the `<transaction>` area. In the area `Properties for all transactions` the freely definable properties from the configuration area `<properties>` are displayed.

These fields can be edited if this has been provided for in the configuration file. Violations of the validation are displayed directly next to the individual fields.

In the right-hand area of the user interface, the individual data records found within the Metadata Cloud for the barcode of the AEON record are listed. Each box represents an independent data record. More details of a data record can be displayed by expanding further fields of the boxes. The corresponding icons per box can be used to select the data sets for which processes are to be generated in Goobi.

![Expanded boxes with additional fields displayed](screen5_en.png)

Provided at least one record has been selected and there are no unfulfilled strict validations, clicking on the `Create processes` button allows the user to create the individual Goobi processes for the selected records based on the configured process template. This then takes the user to another view.

![Second page with the option of downloading a docket](screen6_en.png)

The left-hand pane lists the records for which processes have been created in Goobi. The right-hand pane offers the option of generating and downloading a docket for all created processes, where the associated processes are all listed together within a common batch.

The left-hand pane also names the process titles. Before the processes are created it shows, in batch creation mode, the single title of the batch process as configured in `<processTitle>`, and no title per record, because the records of a batch are structure elements of that one process rather than processes of their own. Where a process is created for each record, each title ends in `_…` until the process exists, standing for the four-digit position of its record among the selected ones (`_0001`, `_0002`, …). Once the processes exist, the actual titles are shown.


## Batch creation

In batch creation mode this plugin creates a single process for the request. Its METS file holds the transaction fields on the root structure element and one child structure element per selected record, carrying the catalogue data of that record and those of its fields that are stored as metadata.

Every record of the batch is identified by a record key: the transaction identifier followed by the four-digit position of the record among the selected ones, so the second record of transaction `286670` has the key `286670_0002`. The transaction makes the key unique across all processes. The key is written in two places:

* onto the child structure element of the record, in the metadata field named by `<childKeyMetadata>`,
* in front of the name of every process property that belongs to the record, as `<recordKey>_<propertyName>`, for example `286670_0002_Bib ID`.

Further steps of the workflow can use the key to tell which properties of the batch process belong to which of its records.

> **Upgrade note:** the record keys used to come from a counter shared by all processes in Goobi, so that the records of one batch were numbered `0473`, `0474`, and so on, continuing from the previous request. They are now numbered per request as described above, and processes no longer receive an `OrderNumber` property. Batches created earlier keep their old keys in their metadata and property names.

> **Upgrade note:** `<batchTitleSuffix>` and `<homeSiteField>` are replaced by `<processTitle>`. The former default title corresponds to `<processTitle>{Transaction Identifier}_{Home site}_group</processTitle>` together with a `<transaction>` field titled `Home site` reading `customFieldValues/HomeSite` - except that an empty home site now stops the request rather than becoming `unknown`. A configuration still carrying `<batchTitleSuffix>`, or lacking `<processTitle>`, is reported as an error and no process is created until it is corrected. `<homeSiteField>` is simply ignored.

> **Upgrade note:** `<processTitleMetadata>` is no longer read. The properties of a batch used to be prefixed with a title predicted for each record; they are now prefixed with the record key. Batches created earlier keep their old property names.


## Configuration
The plugin is configured via the configuration file `plugin_intranda_workflow_aeon_process_creation.xml` and can be adapted during operation. The following is an example configuration file:

```xml
<config_plugin>
    <aeon>
        <url>https://example.com/api</url>
        <apiKey>1234567890</apiKey>
        <username>username</username>
        <password>password</password>
    </aeon>

    <processCreation>
        <workflowName>digitisation on demand</workflowName>
        <opacName>Metadata cloud</opacName>
        <processTitle>{Transaction Identifier}_group</processTitle>
    </processCreation>

    <transaction>
        <field aeon="transactionNumber">
            <title>Transaction Identifier</title>
            <type readonly="true">input</type>
            <variable place="process">Transaction Identifier</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
            <validation strict="true">\d+</validation>
            <message>Your content does not follow our validation guidelines</message>
        </field>

        <field aeon="creationDate">
            <title>Creation Date</title>
            <type readonly="false">input</type>
            <variable place="process">Creation date</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
            <message>Your content does not follow our validation guidelines</message>
        </field>

        <field aeon="itemTitle">
            <title>Title</title>
            <type readonly="false">input</type>
            <variable place="process">Main title</variable>
            <value>Transmission</value>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>

        <field aeon="documentType">
            <title>Document type</title>
            <type readonly="false">input</type>
            <variable place="process">Doc type</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>

        <field aeon="username">
            <title>User Name</title>
            <type readonly="false">input</type>
            <variable place="process">User</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
    </transaction>

    <properties>
        <field>
            <title>Strict validation example</title>
            <type readonly="false">input</type>
            <variable place="process">Strict field</variable>
            <value>no numeric value</value>            
            <help>This is my description for the field so that everyone gets some help.</help>
            <validation strict="true">\d+</validation>
            <message>Value is no number</message>
        </field>

        <field>
            <title>Loose validation example</title>
            <type readonly="false">input</type>
            <variable place="process">Loose field</variable>
            <value>no numeric value</value>
            <help>This is my description for the field so that everyone gets some help.</help>
            <validation strict="false">\d+</validation>
            <message>Value is no number</message>
        </field>

        <field>
            <title>no validation example</title>
            <type readonly="false">input</type>
            <variable place="process">Text field</variable>
            <value>default value</value>
            <help>This is my description for the field so that everyone gets some help.</help>
            <message>Your content does not follow our validation guidelines</message>
        </field>
        <field>
            <title>Dropdown example</title>
            <type readonly="false">select</type>
            <variable place="work">CoWorker</variable>
            <value>Steffen</value>
            <select>Steffen</select>
            <select>Oliver</select>
            <select>Robert</select>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>

        <field>
            <title>Vocabulary example</title>
            <type readonly="false">vocabulary</type>
            <variable place="work">Vocabulary</variable>
            <vocabularyName>Subject terms</vocabularyName>
            <vocabularyField>type=Person</vocabularyField>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
        <field>
            <title>Boolean example</title>
            <type readonly="false">checkbox</type>
            <variable place="process">Status</variable>
            <value>true</value>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
    </properties>

    <processes>
        <field aeon="bibId" displayAlways="true" displayInTitle="false">
            <title>Bib Identifier</title>
            <type readonly="true">input</type>
            <variable place="process">Text field</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
        <field aeon="title" displayAlways="true" displayInTitle="true">
            <title>Item Title</title>
            <type readonly="false">input</type>
            <variable place="process">Text field</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
        <field aeon="itemId" displayAlways="false" displayInTitle="false">
            <title>Item Identifier</title>
            <type readonly="true">input</type>
            <variable place="process">Text field</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
            <field aeon="holdingId" displayAlways="false" displayInTitle="false">
            <title>Holding Identifier</title>
            <type readonly="true">input</type>
            <variable place="process">Text field</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
        <field aeon="recordType" displayAlways="false" displayInTitle="false">
            <title>Record type</title>
            <type readonly="false">select</type>
            <variable place="work">publication</variable>
            <select>ils</select>
            <select>aspace</select>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
        <field aeon="collection" displayAlways="false" displayInTitle="false">
            <title>Collection</title>
            <type readonly="false">input</type>
            <variable place="work">Text field</variable>
            <help>This is my description for the field so that everyone gets some help.</help>
        </field>
    </processes>
</config_plugin>
```

The access data is managed within the `<aeon>` area. The URL to the RESTful API of AEON as well as the user name and password must be stored here. If, alternatively, a key is to be used for the use of the API, this must be specified.

In the block `<processCreation>` it can be defined which OPAC configuration is to be used to request catalogue data and on the basis of which production template the individual processes are to be created.

`opacName` has to name a catalogue exactly as Goobi offers it to the workflow named in `defaultWorkflowName`, and the comparison is case sensitive. Which names those are is not decided by a single file: `goobi_opac.xml` names the catalogues, but one served by the JSON OPAC plugin appears under the `name` attribute of each `<config>` in `plugin_intranda_opac_json.xml` instead, and that plugin may offer different ones per workflow. A name matching none of them is reported as an error naming the catalogues that do exist, and the request stops. The catalogue also has to be served by the JSON OPAC plugin, because the records are read as JSON; one served by any other OPAC plugin is reported and stops the request as well.

Two of its parameters name AEON fields rather than Goobi settings:

Parameter           |  Explanation
------------------- | -----------------------------------------------------
`materialTypeField` | Name of the AEON field holding the material type. Its value is matched against the `type` attribute of the fields in `<properties>` and therefore decides which properties are displayed and validated. Empty by default, because AEON no longer delivers a material type: no field restricted to one applies then, so the created processes carry the unrestricted properties alone, and nothing is reported - that is the intended setup rather than a fault. Configure it only where AEON does deliver a material type; a field named here but left blank by AEON is reported as a note and the request continues.
`transactionNumberField` | Name of the AEON field holding the transaction number. If the AEON record carries no transaction number, the request is rejected with an error, because a request without one could not be recognised when it is sent a second time. Default: `transactionNumber`.
`usernameField` | Optional. Name of the AEON field holding the patron user name, for example `username`. If set, it is used to look up the last name and the mail address at the AEON `Users` endpoint, which fields with `aeon="lastName"` or `aeon="eMailAddress"` can then read. Not set by default, so no lookup takes place.
`referenceNumberField` | Name of the AEON field holding the barcode that the configured catalogue is queried with. Default: `referenceNumber`.
`defaultWorkflowName` | Title of the process template preselected on the request page, both before a request is loaded and after it. If no template has this title, the first template by title is preselected instead. The operator can choose another template before creating the processes.
`workflowName` | Optional and repeatable. A process template for one material type, named in the `type` attribute, for example `<workflowName type="Audio Recording (DRMS)">Audio files</workflowName>`. Once a request with that material type is loaded, this template replaces the default in the preselection. An entry naming a template that does not exist is ignored. Without a material type, no entry applies, including one without a `type` attribute.
`processTitle` | Required. Title of the created process: fixed text with placeholders in braces, each naming a field of `<transaction>` or `<properties>` by its `<title>`, for example `{Transaction Identifier}_{Home site}_group`. A placeholder stands for the value the field holds when the processes are created, so it includes what the operator entered. In values, characters Goobi does not allow in a process title are replaced by an underscore; the fixed text is kept as written. If a referenced field is empty, nothing is created and the empty fields are named; a property field that does not apply to the material type of the request counts as empty. Each empty field the title needs is marked on the request screen with *Needed for the process title* until it is filled in. A batch title already used by another process is refused as well. A placeholder naming no field, or unmatched braces, is reported as a configuration error. Processes created one per record append the four-digit position of their record among the selected ones, `_0001`, `_0002`, and so on. As these numbers repeat in every request, such titles are only unique if the template contains something unique to the request, such as the transaction identifier; each of them is checked against the existing processes before anything is created.
`allowMultiProcessCreation` | Legacy option. `true` offers a choice on the request page between the batch creation and the creation of one process per record. Default `false`: the choice is hidden and the plugin always creates a batch process.
`childKeyMetadata` | Name of the metadata field written onto every child structure element of a batch, receiving the record key of that child, for example `286670_0001`, as described under *Batch creation*. The metadata type must exist in the ruleset and be allowed on the child docstruct types.

All of them accept a slash separated path to address a field inside a nested map. Older AEON versions kept the material type in the flat field `shippingOption`, which would be configured as `<materialTypeField>shippingOption</materialTypeField>`.

This is followed by the configuration of the fields that are displayed and can be imported. This is divided into 3 areas. The area `<transaction>` contains those fields that are read from the AEON record. The area `<properties>` contains freely definable properties that belong to the transactions to be created. And the area `<processes>` contains the fields that were obtained from the Metadata Cloud when querying the data.

The configuration within the three areas is almost identical. Each field is defined by a `<field>` element. Within this, the following options are subsequently provided:

Parameter           |  Explanation
------------------- | -----------------------------------------------------
`title` | display name of the field
`type` | Type of field, possible values are `input` (text field), `select` (selection field), `vocabulary` (selection field from a vocabulary), `checkbox` (checkbox), `input-dont-overwrite` (text field editable on the left but not on the right),`radio-dont-overwrite` (radio buttons editable in the left area but not in the right area).
`readonly` | The values `true` or `false` define whether the content of the field may be edited.
`aeon` | Name of the json element from which the field is filled. A slash separated path addresses an element inside a nested map, for example `customFieldValues/MaterialType`. This parameter is not evaluated for the freely definable properties.
`displayAlways` | The values `true` or `false` define whether the field is displayed when only a minimised record is displayed. This parameter is only evaluated for the fields from the Metadata Cloud.
`displayInTitle` | The values `true` or `false` define whether the field is listed within the header of the box. This parameter is only evaluated for the fields from the Metadata Cloud.
`variable` | This parameter defines the name of the property to be created. This parameter is not evaluated for data from the Metadata Cloud.
`place` | Defines where the value is stored. `metadata` writes a metadatum into the METS file; the `variable` is then the name of a metadata type that the workflow's ruleset must define and that must be allowed on the docstruct of the record. `process` writes a Goobi process property, and the `variable` is the property name. The default is `process`. `work` and `template` are accepted for backwards compatibility and behave like `process`; the workpiece and template properties they name were never implemented.
`value` | This parameter can be used to set a default value. If an AEON element is configured and obtained by the query, the configured default value is overwritten with the content from AEON.
`help` | This parameter can be used to define a help text that can optionally be displayed.
`validation` | This parameter defines a regular expression against which the field content can be validated.
`strict` | The values `true` or `false` define whether a validation violation must be resolved before operations can be created.
`message` | This parameter specifies a message to be displayed in case of validation errors.
`vocabularyName` |  This parameter defines a vocabulary to be used.
`vocabularyField` | The parameter `fieldname=value` can be used to restrict the hit set to values that meet this definition. The field is repeatable to define several restrictions at the same time.

### Storing values as metadata

A field configured with `place="metadata"` is written into the process's METS file instead of
becoming a process property. Its `variable` names a metadata type, which the ruleset of the selected
workflow has to define and which has to be allowed on the docstruct the record is imported as. If
either is missing, process creation stops with an error and creates nothing, rather than creating a
process with part of its data silently dropped.

Choose the store by what the value is. Catalogue and transaction data describes the object and
belongs in the METS file. Values that steer the workflow — output formats, OCR options, the patron
type — are job parameters and stay process properties, where they can be displayed and edited on the
task screens.

**Metadata is exported, properties are not.** METS files are handed to the viewer or DMS on export,
so do not configure patron names or other personal data as metadata.

**A `multiselect` field written as metadata emits one metadatum per selected value.** If the metadata
type is declared in the ruleset with `num="1o"` or another setting that limits it to a single
occurrence, the second selected value throws an exception that aborts the whole request; nothing
checks for this ahead of time. A `multiselect` field configured with `place="metadata"` therefore
needs a metadata type that allows repetition (`num="*"`).

The same collision can happen with two entirely different fields: two fields configured with the same
metadata type name are both written to the same docstruct in single-process mode, so either give them
distinct type names or declare the type `num="*"` in the ruleset.

The transaction number is a special case: it must be configured with `place="metadata"`.
`<processCancellation><transactionFieldName>` is optional; when it is present it must be set to
exactly the same `variable`, and when it is absent the plugin derives the same name from the
transaction number field's `variable` on its own. Both the duplicate check and the process search
look the transaction number up in Goobi's metadata index, and a mismatch would make them find nothing
at all. The plugin refuses to create processes when the configured name and the variable disagree.

Two limitations are worth knowing before switching an existing installation over:

* **Processes created before the change carry properties and no metadata.** The duplicate check and
  the process search will not find them, so a re-requested old transaction is not reported as a
  duplicate and an old process cannot be cancelled through the plugin.
* **Records are recognised as duplicates by their identifier, not by the process title.** A record is
  reported as already digitised when a process carries its `uri` (ArchivesSpace) or its `bibId` and
  `volume` (ILS), in whichever store those `<processes>` fields are configured for. This covers every
  transaction, not only the current one. A record inside a batch process is not found, for the
  reason given in the next point.
* **In batch mode, per-record metadata is not searchable.** Each record's values are written to the
  docstruct of that record, which is a child of the batch object. Goobi indexes only the top level
  docstruct, and `$(meta....)` resolves only against the top level and the first child. The record
  metadata is present in the METS file but cannot be searched for or used in scripts. The transaction
  fields sit on the batch root and are not affected.
