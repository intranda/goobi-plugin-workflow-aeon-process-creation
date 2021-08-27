package org.goobi.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Path;

/*
 * Very simple rest api: returns JSON string with pseudo data 
 */

@Path("/testingRest")
public class RestTest {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String returnJSON() {
        return "{"
        		+ "\"id\":1234567890,"
        		+ "\"creationDate\":\"18.02.2021 03:28 PM\","
        		+ "\"title\":\"Title of Transmission\","
        		+ "\"user\":{"
        		+ "\"firstName\":\"Peter\","
        		+ "\"lastName\":\"Miller\","
        		+ "\"mailAddress\":\"peter@mail.io\""
        		+ "},"
        		+ "\"items\":["
        		+ "{ \"id\":\"abc_123_def_0001\", \"title\":\"item A title\", \"publicationType\":\"\", \"shelfmark\":\"II 346 2a\" },"
        		+ "{ \"id\":\"abc_123_def_0002\", \"title\":\"item B title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2b\" },"
        		+ "{ \"id\":\"abc_123_def_0003\", \"title\":\"item C title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2c\" },"
        		+ "{ \"id\":\"abc_123_def_0004\", \"title\":\"item D title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2d\" },"
        		+ "{ \"id\":\"abc_123_def_0005\", \"title\":\"item E title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2e\" },"
        		+ "{ \"id\":\"abc_123_def_0006\", \"title\":\"item F title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2f\" },"
        		+ "{ \"id\":\"abc_123_def_0007\", \"title\":\"item G title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2g\" },"
        		+ "{ \"id\":\"abc_123_def_0008\", \"title\":\"item H title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2h\" },"
        		+ "{ \"id\":\"abc_123_def_0009\", \"title\":\"item I title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2i\" },"
        		+ "{ \"id\":\"abc_123_def_0010\", \"title\":\"item J title\", \"publicationType\":\"Monograph\", \"shelfmark\":\"II 346 2j\" }"
        		+ "]"
        		+ "}";
    }
}
