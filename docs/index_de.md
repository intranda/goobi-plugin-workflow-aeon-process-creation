---
title: AEON Datenübernahme
identifier: intranda_workflow_aeon_process_creation
description: Dieses Workflow Plugin ermöglicht die Erstellung von Goobi Vorgängen auf der Grundlage einer ersten AEON-Anfrage. Es ermöglicht unter anderem eine manuelle Auswahl von Elementen aus der AEON-Antwort.
published: true
keywords:
    - Goobi workflow
    - Plugin
    - Workflow Plugin
---
## Einführung
Mit diesem Plugin können einzelne Datensätze aus AEON abgefragt werden. Nachdem ein Datensatz abgefragt wurde, werden alle Metadaten aus der sog. Metadata Cloud abgefragt und aufgelistet. Der Nutzer kann anschließend auswählen, welche Datensätze er als Goobi-Vorgang anlegen möchte.


## Installation
Das Plugin besteht insgesamt aus den folgenden zu installierenden Dateien:

```bash
plugin-intranda-workflow-aeon-process-creation-base.jar
plugin-intranda-workflow-aeon-process-creation-gui.jar
plugin_intranda_workflow_aeon_process_creation.xml
```

Diese Dateien müssen in den richtigen Verzeichnissen installiert werden, so dass diese nach der Installation unter den folgenden Pfaden vorliegen:

```bash
/opt/digiverso/goobi/plugins/workflow/plugin-intranda-workflow-aeon-process-creation-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-intranda-workflow-aeon-process-creation-gui.jar
/opt/digiverso/goobi/config/plugin_intranda_workflow_aeon_process_creation.xml
```

Dieses Plugin verfügt über eine eigene Berechtigungsstufe für die Verwendung. Aus diesem Grund müssen Nutzer über die erforderlichen Rechte verfügen. Bitte weisen Sie daher der Benutzergruppe der entsprechenden Nutzer das folgende Recht zu:

```
Plugin_workflow_aeon_process_creation
```

![Korrekt zugewiesenes Recht für die Nutzer](screen2_de.png)


## Überblick und Funktionsweise
Wenn der Nutzer über die Berechtigung zur Nutzung des Plugins verfügt, kann im Menüpunkt `Worklow` das Plugin `AEON Abfrage Plugin` geöffnet werden.

![Geöffnetes Plugin ohne ausgeführte Suche](screen3_de.png)

Hier kann nun ein Transaction identifier eingetragen werden, um die Suche nach einem AEON-Record zu starten.

![Anzeige der Daten nach Ausführung einer Suche](screen4_de.png)

Wenn eine erfolgreiche Suche durchgeführt wurde, wurde der Bereich `Abfragedetails` mit den Feldern gefüllt, die innerhalb der Konfiguration im Bereich `<transaction>` festgelegt wurden. Im Bereich `Eigenschaften für alle Vorgänge` werden die frei definierbaren Eigenschaften aus dem Konfigurationsbereich `<properties>` angezeigt.

Diese Felder lassen sich bearbeiten, sofern dies in der Konfigurationsdatei vorgesehen wurde. Verstöße gegen die Validierung werden unmittelbar bei den einzelnen Feldern angezeigt.

Im rechten Bereich der Nutzeroberfläche werden die einzelnen ermittelten Datensätze aufgelistet, die innerhalb der Metadata Cloud zu dem Barcode des AEON-Records gefunden wurden. Jede Box stellt hierbei einen eigenständigen Datensatz dar. Mehr Details eines Datensatzes lassen sich anzeigen, indem weitere Felder der Boxen ausgeklappt werden. Über die entsprechenden Icons pro Box lassen sich diejenigen Datensätze auswählen, für die Vorgänge in Goobi erzeugt werden sollen.

![Ausgeklappte Boxen mit weiteren angezeigten Feldern](screen5_de.png)

Sofern mindestens ein Datensatz ausgewählt wurde und keine unerfüllten strikten Validierungen vorliegen, lassen sich mittels Klick auf den Button `Vorgänge anlegen` die einzelnen Goobi-Vorgänge für die ausgewählten Datensätze auf der Basis der konfigurierten Produktionsvorlage anlegen. Anschließend gelangt der Nutzer somit auf eine weitere Ansicht.

![Zweite Seite mit der Möglichkeit einen Laufzettel herunterzuladen](screen6_de.png)

Hier werden im linken Bereich noch einmal diejenigen Datensätze aufgeführt, für die Vorgänge in Goobi angelegt wurden. Im rechten Bereich steht die Möglichkeit zur Verfügung einen Laufzettel für alle erzeugten Vorgänge zu generieren und herunterzuladen, wo die zugehörigen Vorgänge alle innerhalb eines gemeinsamen Batches zusammengefasst aufgeführt werden.

Im linken Bereich werden außerdem die Vorgangstitel benannt. Vor dem Anlegen der Vorgänge wird im Modus für Batch-Vorgänge der einzelne Titel des Batch-Vorgangs einschließlich `<batchTitleSuffix>` angezeigt und kein Titel pro Datensatz: die Kindvorgänge, in die der Batch später aufgeteilt wird, werden hier nicht benannt, da ihre Titel erst beim Aufteilen zusammengesetzt werden. Wird für jeden Datensatz ein eigener Vorgang angelegt, endet jeder Titel auf `_…` als Platzhalter für die Ordnungsnummer, die erst beim Anlegen des Vorgangs vergeben wird. Sobald die Vorgänge existieren, werden die tatsächlichen Titel angezeigt.


## Übergabe an das Plugin zum Aufteilen von Vorgängen

Im Batch-Modus erzeugt dieses Plugin einen Vorgang, der pro ausgewähltem Datensatz ein Kind-Strukturelement enthält. Das Step-Plugin `intranda_step_split_processes` teilt diesen später in einen Vorgang pro Kindelement auf.

Die Titel der Teilvorgänge werden **nicht** hier festgelegt. Sie werden beim Aufteilen aus konfigurierten Titelbestandteilen zusammengesetzt, da die zugrunde liegenden Metadaten erst in die METS-Datei des Batches geladen werden und bei dessen Erstellung noch nicht vorliegen.

Dieses Plugin liefert die Verknüpfung: Jedes Kind-Strukturelement erhält seine Ordnungsnummer in dem unter `<childKeyMetadata>` genannten Metadatenfeld, und die Eigenschaften des Batch-Vorgangs werden als `<Ordnungsnummer>_<Eigenschaftsname>` benannt. Das aufteilende Plugin liest diesen Schlüssel aus jedem Kindelement und entscheidet damit, welche Eigenschaften zu welchem Teilvorgang gehören.

Beide Plugins müssen daher übereinstimmend konfiguriert werden:

| AEON | Vorgänge aufteilen |
|------|--------------------|
| `<childKeyMetadata>_OrderNumber</childKeyMetadata>` | `<childKeyMetadata>_OrderNumber</childKeyMetadata>` |
| `<batchTitleSuffix>_group</batchTitleSuffix>` | `<title name="{parent}" strip="_group"/>` |

Keines der beiden Plugins kann die Konfiguration des anderen prüfen.

> **Hinweis zur Aktualisierung:** Diese Version ersetzt das frühere `<processTitleMetadata>`, das einen vorab berechneten Titel des Teilvorgangs geschrieben hat. Die beiden Plugins müssen gemeinsam ausgeliefert werden, zu einem Zeitpunkt, an dem kein Batch auf das Aufteilen wartet. Ein von der früheren Version erzeugter Batch trägt Eigenschaftsnamen mit dem vorab berechneten Titel als Präfix, nach dem das neue Plugin nicht sucht; seine Eigenschaften würden stillschweigend nicht kopiert.


## Konfiguration
Die Konfiguration des Plugins erfolgt über die Konfigurationsdatei `plugin_intranda_workflow_aeon_process_creation.xml` und kann im laufenden Betrieb angepasst werden. Im folgenden ist eine beispielhafte Konfigurationsdatei aufgeführt:

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
        <opacName>metadata cloud</opacName>
        <homeSiteField>customFieldValues/HomeSite</homeSiteField>
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

Innerhalb des Bereiches `<aeon>` werden die Zugangsdaten verwaltet. Hier müssen die URL zur RESTful API von AEON sowie Nutzername und Passwort hinterlegt werden. Soll alternativ ein Key für die Verwendung der API genutzt werden muss dieser angegeben werden.

Im Block `<processCreation>` kann definiert werden, welche OPAC-Konfiguration zur Anfrage von Katalogdaten genutzt werden soll und auf Basis welcher Produktionsvorlage die einzelnen Vorgänge erzeugt werden sollen. Zwei seiner Parameter benennen AEON-Felder und keine Goobi-Einstellungen:

Parameter           |  Erläuterung
------------------- | -----------------------------------------------------
`materialTypeField` | Name des AEON-Feldes, das den Materialtyp enthält. Sein Wert wird mit dem Attribut `type` der Felder in `<properties>` verglichen und entscheidet damit, welche Eigenschaften angezeigt und validiert werden. Standardmäßig leer, da AEON keinen Materialtyp mehr liefert: es gilt dann kein auf einen Materialtyp beschränktes Feld, sodass die erzeugten Vorgänge allein die unbeschränkten Eigenschaften tragen, und es wird nichts gemeldet - das ist das beabsichtigte Verhalten und kein Fehler. Der Parameter ist nur dort zu setzen, wo AEON einen Materialtyp liefert; ein hier benanntes, von AEON aber leer gelassenes Feld wird als Hinweis gemeldet und die Anfrage fortgesetzt.
`homeSiteField`     | Name des AEON-Feldes, das den Home Site enthält. Sein Wert bildet den Repository-Teil des erzeugten Vorgangstitels. Standardwert: `customFieldValues/HomeSite`.
`transactionNumberField` | Name des AEON-Feldes, das die Transaktionsnummer enthält. Sein Wert bildet den Anfang des erzeugten Vorgangstitels. Enthält der AEON-Record keine Transaktionsnummer, wird die Anfrage mit einer Fehlermeldung abgewiesen, da kein brauchbarer Vorgangstitel gebildet werden kann. Standardwert: `transactionNumber`.
`usernameField`     | Name des AEON-Feldes, das den Benutzernamen des Patrons enthält. Damit werden Nachname und Mailadresse am AEON-Endpunkt `Users` nachgeschlagen. Standardwert: `username`.
`referenceNumberField` | Name des AEON-Feldes, das den Barcode enthält, mit dem der konfigurierte Katalog abgefragt wird. Standardwert: `referenceNumber`.
`batchTitleSuffix`  | Optionales Suffix, das an den Titel eines Batch-Vorgangs angehängt wird, zum Beispiel `_group`, um ihn als Vorgang zu kennzeichnen, der in Teilvorgänge aufgeteilt werden soll. Das Plugin zum Aufteilen von Vorgängen entfernt es beim Ableiten der Titel der Teilvorgänge wieder, daher müssen dieser Wert und das `strip`-Attribut jenes Plugins übereinstimmen. Fehlt der Wert oder ist er leer, bleibt der Batch-Titel unverändert. Gilt nur für die Batch-Erstellung; Vorgänge, die einzeln pro Datensatz erzeugt werden, werden nie aufgeteilt und sind davon nicht betroffen.
`childKeyMetadata`  | Name des Metadatenfeldes, das auf jedes Kind-Strukturelement eines Batches geschrieben wird und dessen Ordnungsnummer erhält. Es ist der Korrelationsschlüssel, anhand dessen das Plugin zum Aufteilen von Vorgängen die Eigenschaften den Kindelementen zuordnet, und es muss dasselbe Feld benennen wie das `childKeyMetadata` jenes Plugins. Der Metadatentyp muss im Regelsatz vorhanden und für die Kind-Docstruct-Typen zugelassen sein.

Alle akzeptieren einen mit Schrägstrichen getrennten Pfad, um ein Feld innerhalb einer verschachtelten Map anzusprechen. Ältere AEON-Versionen hielten den Materialtyp und den Home Site in den flachen Feldern `shippingOption` und `itemInfo2`, was als `<materialTypeField>shippingOption</materialTypeField>` und `<homeSiteField>itemInfo2</homeSiteField>` zu konfigurieren wäre.

Anschließend erfolgt die Konfiguration der angezeigten und importierbaren Felder. Dies ist in 3 Bereiche aufgeteilt. Der Bereich `<transaction>` enthält diejenigen Felder, die aus dem AEON-Record gelesen werden. Der Bereich `<properties>` enthält frei definierbare Eigenschaften, die zu den zu erzeugenden Vorgängen gehören. Und der Bereich `<processes>` enthält die Felder, die bei der Abfrage der Daten aus der Metadata Cloud bezogen wurden.

Die Konfiguration innerhalb der drei Bereiche ist annähernd identisch. Jedes Feld wird jeweils durch ein  Element `<field>` definiert. Innerhalb davon sind anschließend folgende Möglichkeiten vorgesehen:

Parameter           |  Erläuterung
------------------- | -----------------------------------------------------
`title`             | Anzeigename des Feldes
`type`              | Typ des Feldes, mögliche Werte sind `input` (Textfeld), `select` (Auswahlfeld), `vocabulary` (Auswahlfeld aus einem Vokabular), `checkbox` (Checkbox), `input-dont-overwrite` (Textfeld, das im linken Bereich editierbar ist, aber nicht im rechten Bereich), `radio-dont-overwrite` (Radio-Buttons, die im linken Bereich editierbar sind, aber nicht im rechten Bereich)
`readonly`          | Die Werte `true` oder `false` definieren, ob der Inhalt des Feldes bearbeitet werden darf.
`aeon`              | Name des json Elements, aus dem das Feld befüllt wird. Ein mit Schrägstrichen getrennter Pfad spricht ein Element innerhalb einer verschachtelten Map an, zum Beispiel `customFieldValues/MaterialType`. Dieser Parameter wird für die frei definierbare Eigenschaften nicht ausgewertet.
`displayAlways`     | Die Werte `true` oder `false` definieren, ob das Feld angezeigt wird, wenn nur ein minimierter Datensatz angezeigt wird. Dieser Parameter wird ausschließlich für die Felder aus der Metadata Cloud ausgewertet.
`displayInTitle`    | Die Werte `true` oder `false` definieren, ob das Feld innerhalb der Kopfzeile der Box aufgeführt wird. Dieser Parameter wird ausschließlich für die Felder aus der Metadata Cloud ausgewertet.
`variable`          | Dieser Parameter definiert den Namen der zu erstellenden Eigenschaft. Dieser Parameter wird für Daten aus der Metadata Cloud nicht ausgewertet.
`place`             | Hier wird der Typ der Eigenschaft festgelegt. Mögliche Werte sind `work` (Werkstückeigenschaft), `process` (Vorgangseigenschaft), `template` (Vorlageneigenschaft).
`value`             | Mit diesem Parameter kann ein Default Wert festgelegt werden. Falls ein AEON Element konfiguriert und durch die Abfrage erhalten wurde, wird der konfigurierte default Wert mit dem Inhalt aus AEON überschrieben.
`help`              | Mit diesem Parameter lässt sich ein Hilfetext definieren der optional angezeigt werden kann.
`validation`        | Dieser Parameter definiert einen regulären Ausdruck, gegen den der Feldinhalt validiert werden kann.
`strict`            | Die Werte `true` oder `false` definieren, ob ein Verstoß gegen die Validierung zwingend behoben werden muss, bevor Vorgänge angelegt werden können.
`message`           | Dieser Parameter legt eine Meldung fest, die im Falle von Validierungsfehlern angezeigt werden soll.
`vocabularyName`    | Dieser Parameter definiert ein zu nutzendes Vokabular.
`vocabularyField`   | Mit dem Parameter `fieldname=value` kann das Trefferset auf Werte eingeschränkt werden, die diese Definition erfüllen. Das Feld ist wiederholbar, um mehrere Einschränkungen gleichzeitig zu definieren.
