package org.goobi.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/*
 * Very simple rest api: returns JSON string with pseudo data
 */

@Path("/testingRest")
public class RestTest {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aeon")
    public String returnAeonJSON() {
        return "{\"transactionNumber\":286670,\"creationDate\":\"2021-07-23T19:18:39\",\"photoduplicationStatus\":1162,\"photoduplicationDate\":\"2021-07-23T19:23:09.947\",\"transactionStatus\":28,\"transactionDate\":\"2022-02-15T14:41:37.17\",\"customFieldValues\":{\"UseRestrictionNote\":null,\"ExtentPhysicalDescription\":null,\"RestrictionCode\":null,\"DigitalObjectID\":null,\"TopContainerURI\":null,\"LocationURI\":null,\"CollectionTitle\":null,\"Copy\":null,\"RootRecordURL\":null,\"Folder\":null,\"ERENotes\":null},\"requestFor\":{\"type\":\"Activity\",\"reference\":\"3938\"},\"username\":\"YALE\\\\jam384\",\"appointmentID\":null,\"bundleID\":null,\"callNumber\":\"MSS 73\",\"documentType\":\"Manuscript\",\"eadNumber\":\"https://puitestarchivesspace.library.yale.edu/repositories/6/archival_objects/2544864\",\"format\":\"PDF (reference use)\",\"forPublication\":false,\"itemAuthor\":\"Weigl, Karl, 1881-1949\",\"itemCitation\":\"\",\"itemDate\":\"\",\"itemEdition\":\"Folders 833-834\",\"itemInfo1\":\"Y\",\"itemInfo2\":\"MUS\",\"itemInfo3\":null,\"itemInfo4\":null,\"itemInfo5\":\"This is restricted\",\"itemIssue\":\"Series 4\",\"itemISxN\":null,\"itemNumber\":null,\"itemPages\":\"1-10\",\"itemPlace\":null,\"itemPublisher\":null,\"itemSubTitle\":\"Publishers' works lists\",\"itemTitle\":\"The Karl Weigl Papers\",\"itemVolume\":\"Box 26\",\"location\":\"lsfmusr\",\"maxCost\":null,\"pageCount\":null,\"referenceNumber\":\"39002058721078\",\"scheduledDate\":\"2021-07-28T04:00:00\",\"serviceLevel\":\"Standard\",\"shippingOption\":\"(DRMS) Book & Paper\",\"site\":\"MUS\",\"specialRequest\":null,\"subLocation\":null,\"systemID\":\"ArchivesSpace\",\"webRequestForm\":\"GenericRequestManuscript\"}";
    }

    @Path("metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String returnMetadataJSON() {
        return "[\n"
                + "    {\"title\": \"Marcheￌﾁ de Saint-Pierre : meￌﾁlodrame en cinq actes / par Benjamin Antier et Alexis de Comberousse. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1848655\",\"bibId\": \"1848655\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Antier, Benjamin, 1787-1870. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Simplette la chevrieￌﾀre; vaudeville en un acte ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2083565\",\"bibId\": \"2083565\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Cogniard, Theￌﾁodore, 1806-1872. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Balochard, ou, Samedi, dimanche et lundi : vaudeville en trois actes / par MM. Depeuty et E. Vanderbuch. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2157244\",\"bibId\": \"2157244\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Dupeuty, M. (Charles), 1798-1865. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Bathilde; drame en trois actes ... Repreￌﾁsenteￌﾁ, pour la premieￌﾀre fois, aￌﾀ Paris, sur le Theￌﾁaￌﾂtre de la renaissance (Salle Ventadour) le 14 janvier 1839. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2509110\",\"bibId\": \"2509110\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Maquet, Auguste, 1813-1888. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1843\"\n"
                + "    },\n"
                + "    {\"title\": \"Maiￌﾂtresse et la fianceￌﾁe; drame en deux actes, meￌﾂleￌﾁ de chants, ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2785687\",\"bibId\": \"2785687\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Souvestre, Eￌﾁmile. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Jeunesse de Goethe, comeￌﾁdie en un acte, en vers, par Madame Louise Colet-Revoil. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2075761\",\"bibId\": \"2075761\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Colet, Louise, 1810-1876. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Faut que jeunesse se passe; comedie en trois actes, en prose, par m. de Rougemont, repreￌﾁsenteￌﾁ pour la premieￌﾀre fois sur le Theￌﾁatre-Francais, le ler juillet 1839. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2715653\",\"bibId\": \"2715653\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Rougemont, M. de (Michel-Nicholas Balisson), 1781-1840. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Trois bals : vaudeville en trois actes / par M. Bayard. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1963934\",\"bibId\": \"1963934\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Bayard, Jean-Francￌﾧois-Alfred, 1796-1853. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Rigobert; ou, Fais-mois rire; comeￌﾁdie-drame, en trois actes ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2129285\",\"bibId\": \"2129285\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Deligny, Eugeￌﾀne, 1816-1881. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Maurice, comeￌﾁdie-vaudeville en deux actes, par MM. Meￌﾁlesville [pseud.] et C. Duveyrier. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2158669\",\"bibId\": \"2158669\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Meￌﾁlesville, M., 1787-1865. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Manoir de Montlouvier : drame en cinq actes, en prose / par M. Rosier. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2690141\",\"bibId\": \"2690141\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Rosier, Joseph-Bernard, 1804-1880. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Diane de Chivri : drame en cinq actes / par M. Freￌﾁdeￌﾁric Soulieￌﾁ. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2772587\",\"bibId\": \"2772587\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Soulieￌﾁ, Freￌﾁdeￌﾁric, 1800-1847. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Fils de la folle : drame en cinq actes / par M. Freￌﾁdeￌﾁric Soulieￌﾁ. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2772588\",\"bibId\": \"2772588\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Soulieￌﾁ, Freￌﾁdeￌﾁric, 1800-1847. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Pascal et Chambord : comeￌﾁdie en deux actes, meￌﾂleￌﾁe de chant / par MM. Anicet Bourgeois et Edouard Brisebarre. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1862527\",\"bibId\": \"1862527\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Anicet-Bourgeois, M. (Auguste), 1806-1871. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Dieu vous beￌﾁnisse! : comeￌﾁdie-vaudeville en un acte / par MM. Ancelot et Paul Duport. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1862546\",\"bibId\": \"1862546\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Ancelot, Jacques-Arseￌﾀne-Francￌﾧois-Polycarpe, 1794-1854. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Lekain aￌﾀ Draguignan, comeￌﾁdie en deux actes, meￌﾂleￌﾁe de chant, par MM. De Forges et Paul Vermond, repreￌﾁseneￌﾁe pour la premieￌﾀre fois, aￌﾀ Paris, sur le Theￌﾁaￌﾂtre du Palais-Royal, le 23 janvier 1839. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2126526\",\"bibId\": \"2126526\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Forges, A. de (Auguste), 1805-1881. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1843\"\n"
                + "    },\n"
                + "    {\"title\": \"Marguerite d'Yorck; meￌﾁlodrame historique en trois actes, avec un prologue, ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2198884\",\"bibId\": \"2198884\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Fournier, N. (Narcisse), 1803-1880. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Amandine : vaudeville en deux actes / par MM. de Rougemont et A. Monnier. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2711689\",\"bibId\": \"2711689\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Balisson de Rougemont, Michel-Nicolas, 1781-1840. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Alchimiste : drame en cinq actes, en vers / par Alexandre Dumas. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2153993\",\"bibId\": \"2153993\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Dumas, Alexandre, 1802-1870. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1843\"\n"
                + "    },\n"
                + "    {\"title\": \"Plastron : comeￌﾁdie en deux actes, meￌﾂleￌﾁe du chant : repreￌﾁsenteￌﾁe, pour la premieￌﾀre fois, aￌﾀ Paris, sur le theￌﾁaￌﾂtre du Vaudeville, le 27 avril 1839 / par MM. Xavier, Duvert et Lauzanne. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2682778\",\"bibId\": \"2682778\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Xavier, M., 1798-1865. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Eￌﾁmile; ou, Six teￌﾂtes dans un chapeau; comeￌﾁdie-vaudeville en un acte, ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1965133\",\"bibId\": \"1965133\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Bayard, Jean-Francￌﾧois-Alfred, 1796-1853. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Mademoiselle de Belle-Isle; drame en cinq actes, en prose ... Repreￌﾁsenteￌﾁ pour la premieￌﾀre fois, aￌﾀ Paris, sur le Theￌﾁaￌﾂtre Francￌﾧais, le 2 avril 1839. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2162369\",\"bibId\": \"2162369\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Dumas, Alexandre, 1802-1870. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1843\"\n"
                + "    },\n"
                + "    {\"title\": \"Maria, drame en deux actes, meￌﾂleￌﾁ de chant, par MM. Paul Foucher et Laurencin. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2189911\",\"bibId\": \"2189911\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Foucher, Paul. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Deux jeunes femmes : drame en cinq actes et en prose / par V. de Saint-Hilaire ; repreￌﾁsenteￌﾁ, pour la premieￌﾀre fois, aￌﾀ Paris, sur le theￌﾁaￌﾂtre de la Renaissance, le 7 juin 1839. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2668718\",\"bibId\": \"2668718\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Saint-Hilaire, Amable Vilain de, b. 1795. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Gabrielle, ou, Les aides-de-camp : comeￌﾁdie-vaudeville en deux actes / par MM. Ancelot et Paul Duport. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=1862545\",\"bibId\": \"1862545\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Ancelot, Jacques-Arseￌﾀne-Francￌﾧois-Polycarpe, 1794-1854. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Naufrage de la Meￌﾁduse; drame en cinq actes ... \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2127636\",\"bibId\": \"2127636\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Desnoyer, Charles, 1806-1858. \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    },\n"
                + "    {\"title\": \"Vaudevilliste : comeￌﾁdie en un acte, en prose / Par MM. T. Sauvage et Maurice Saint-Aguet. \",\"recordType\": \"ils\",\"barcode\": \"39002001054627\",\"uri\": \"/ils/item/4168081?bib=2716227\",\"bibId\": \"2716227\",\"itemId\": \"4168081\",\"holdingId\": \"3055899\",\"author\": \"Sauvage, T. (Thomas) \",\"callNumber\": \"Hf75 3 19-20\",\"volume\": \"19-20\",\"pubDate\": \"1839\"\n"
                + "    }\n"
                + "]";
    }
}
