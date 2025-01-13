package org.goobi.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/*
 * Very simple rest api: returns JSON string with pseudo data
 */

@Path("/testingRest")
public class TestEndpoint {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aeon")
    public String returnAeonJSON() {
        return "{\"transactionNumber\":286670,\"creationDate\":\"2021-07-23T19:18:39\",\"photoduplicationStatus\":1162,\"photoduplicationDate\":\"2021-07-23T19:23:09.947\",\"transactionStatus\":28,\"transactionDate\":\"2022-02-15T14:41:37.17\",\"customFieldValues\":{\"UseRestrictionNote\":null,\"ExtentPhysicalDescription\":null,\"RestrictionCode\":null,\"DigitalObjectID\":null,\"TopContainerURI\":null,\"LocationURI\":null,\"CollectionTitle\":null,\"Copy\":null,\"RootRecordURL\":null,\"Folder\":null,\"ERENotes\":null},\"requestFor\":{\"type\":\"Activity\",\"reference\":\"3938\"},\"username\":\"YALE\\\\jam384\",\"appointmentID\":null,\"bundleID\":null,\"callNumber\":\"MSS 73\",\"documentType\":\"Manuscript\",\"eadNumber\":\"https://puitestarchivesspace.library.yale.edu/repositories/6/archival_objects/2544864\",\"format\":\"PDF (reference use)\",\"forPublication\":false,\"itemAuthor\":\"Weigl, Karl, 1881-1949\",\"itemCitation\":\"\",\"itemDate\":\"\",\"itemEdition\":\"Folders 833-834\",\"itemInfo1\":\"Y\",\"itemInfo2\":\"MUS\",\"itemInfo3\":null,\"itemInfo4\":null,\"itemInfo5\":\"This is restricted\",\"itemIssue\":\"Series 4\",\"itemISxN\":null,\"itemNumber\":null,\"itemPages\":\"1-10\",\"itemPlace\":null,\"itemPublisher\":null,\"itemSubTitle\":\"Publishers' works lists\",\"itemTitle\":\"The Karl Weigl Papers\",\"itemVolume\":\"Box 26\",\"location\":\"lsfmusr\",\"maxCost\":null,\"pageCount\":null,\"referenceNumber\":\"39002058721078\",\"scheduledDate\":\"2021-07-28T04:00:00\",\"serviceLevel\":\"Standard\",\"shippingOption\":\"(DRMS) Book & Paper\",\"site\":\"MUS\",\"specialRequest\":null,\"subLocation\":null,\"systemID\":\"ArchivesSpace\",\"webRequestForm\":\"GenericRequestManuscript\"}";
    }
}
