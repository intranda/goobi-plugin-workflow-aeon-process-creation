package de.intranda.goobi.plugins.api.aeon;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement
public class QueueItem {

    private Integer transactionNumber;
    private String creationDate;
    private Integer photoduplicationStatus;
    private String photoduplicationDate;
    private Integer transactionStatus;
    private String transactionDate;
    private RequestAssociation requestFor;
    private String username;
    private Integer bundleID;
    private String callNumber;
    private String documentType;
    private String eadNumber;
    private String format;
    private Boolean forPublication;
    private String itemAuthor;
    private String itemCitation;
    private String itemDate;
    private String itemEdition;
    private String itemInfo1;
    private String itemInfo2;
    private String itemInfo3;
    private String itemInfo4;
    private String itemInfo5;
    private String itemIssue;
    private String itemISxN;
    private String itemNumber;
    private String itemPages;
    private String itemPlace;
    private String itemPublisher;
    private String itemSubTitle;
    private String itemTitle;
    private String itemVolume;
    private String location;
    private String maxCost;
    private Integer pageCount;
    private String referenceNumber;
    private String scheduledDate;
    private String serviceLevel;
    private String shippingOption;
    private String site;
    private String specialRequest;
    private String subLocation;
    private String systemID;
    private String webRequestForm;
    @Override
    public String toString() {
        return "QueueItem [transactionNumber=" + transactionNumber + ", creationDate=" + creationDate + ", photoduplicationStatus="
                + photoduplicationStatus + ", photoduplicationDate=" + photoduplicationDate + ", transactionStatus=" + transactionStatus
                + ", transactionDate=" + transactionDate + ", requestFor=" + requestFor + ", username=" + username + ", bundleID=" + bundleID
                + ", callNumber=" + callNumber + ", documentType=" + documentType + ", eadNumber=" + eadNumber + ", format=" + format
                + ", forPublication=" + forPublication + ", itemAuthor=" + itemAuthor + ", itemCitation=" + itemCitation + ", itemDate=" + itemDate
                + ", itemEdition=" + itemEdition + ", itemInfo1=" + itemInfo1 + ", itemInfo2=" + itemInfo2 + ", itemInfo3=" + itemInfo3
                + ", itemInfo4=" + itemInfo4 + ", itemInfo5=" + itemInfo5 + ", itemIssue=" + itemIssue + ", itemISxN=" + itemISxN + ", itemNumber="
                + itemNumber + ", itemPages=" + itemPages + ", itemPlace=" + itemPlace + ", itemPublisher=" + itemPublisher + ", itemSubTitle="
                + itemSubTitle + ", itemTitle=" + itemTitle + ", itemVolume=" + itemVolume + ", location=" + location + ", maxCost=" + maxCost
                + ", pageCount=" + pageCount + ", referenceNumber=" + referenceNumber + ", scheduledDate=" + scheduledDate + ", serviceLevel="
                + serviceLevel + ", shippingOption=" + shippingOption + ", site=" + site + ", specialRequest=" + specialRequest + ", subLocation="
                + subLocation + ", systemID=" + systemID + ", webRequestForm=" + webRequestForm + "]";
    }

}
