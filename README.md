# Goobi workflow Plugin: goobi-plugin-workflow-aeon-process-creation

<img src="https://goobi.io/wp-content/uploads/logo_goobi_plugin.png" align="right" style="margin:0 0 20px 20px;" alt="Plugin for Goobi workflow" width="175" height="109">

This is the AeonProcessCreation Plugin for Goobi workflow. It allows to create multiple Goobi processes based on an AEON request.

This is a plugin for Goobi workflow, the open source workflow tracking software for digitisation projects. More information about Goobi workflow is available under https://goobi.io. If you want to get in touch with the user community simply go to https://community.goobi.io.

## Getting Started

To get the Plugin up and running in its current state, please follow instructions below:

1. Copy `RestTest.java` into `org/goobi/api/rest` for rest api functionality
2. Add piece of code to `/opt/digiverso/goobi/config/goobi_rest.xml` to allow access to rest api

```html
<endpoint path="/testingRest.*">
    <method name="get">
        <allow />
    </method>
    <method name="post">
        <allow />
    </method>
</endpoint>
```

3. Start goobi and the plugin should be functional

**Important**

aeon attributes in the config xml of the plugin **have** to be paths of JSON sub objects to needed property!
Example if JSON transmission response is built like this:

```JSON
{
    "id":"1234",
    "user":{
        "name":"Maxim"
    },
    "items":[
        { "id":"1", "title":"Hello"}
    ]
}
```

So to map the information in the config correctly the desired fields would **have** to be inside `<transmission>`!
For example to access:

- id: `<field aeon="id">`
- name of user: `<field aeon="user.name">`

to map the items, the desired fields **have** to be defined in `<processes>` and assume that `item` is the base object!
So for example to access:

- items id: `<field aeon="id">`
- items title: `<field aeon="title">`

This is because the Java backend objects will be mapped according to the JSON response! 
The main transmission information and item information will be handled seperately.

## Problems

* no messages in the form of `#{msgs. ...}` added yet, every text is hardcoded
* java objects are designed for pseudo JSON code sent by rest api
* no implementation of properties box

## Plugin details

More information about the functionality of this plugin and the complete documentation can be found in the central documentation area at https://docs.goobi.io

Detail | Description
--- | ---
**Plugin identifier**       | intranda_workflow_aeon_process_creation
**Plugin type**             | Workflow plugin
**Licence**                 | GPL 2.0 or newer  
**Documentation (German)**  | - no documentation available -
**Documentation (English)** | - no documentation available -

## Goobi details

Goobi workflow is an open source web application to manage small and large digitisation projects mostly in cultural heritage institutions all around the world. More information about Goobi can be found here:

Detail | Description
--- | ---
**Goobi web site**  | https://www.goobi.io
**Twitter**         | https://twitter.com/goobi
**Goobi community** | https://community.goobi.io

## Development

This plugin was developed by intranda. If you have any issues, feedback, question or if you are looking for more information about Goobi workflow, Goobi viewer and all our other developments that are used in digitisation projects please get in touch with us.  

Contact | Details
--- | ---
**Company name**  | intranda GmbH
**Address**       | Bertha-von-Suttner-Str. 9, 37085 Göttingen, Germany
**Web site**      | https://www.intranda.com
**Twitter**       | https://twitter.com/intranda
