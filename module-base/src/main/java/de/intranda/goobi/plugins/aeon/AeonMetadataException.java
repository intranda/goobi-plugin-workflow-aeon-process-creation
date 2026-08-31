package de.intranda.goobi.plugins.aeon;

import lombok.Getter;

/**
 * A configured metadata type could not be written: either the ruleset does not define it, or the docstruct it was
 * meant to go on does not permit it.
 *
 * The type name is carried separately from the message so that the caller can name it in a translated message rather
 * than putting an exception text on screen.
 */
public class AeonMetadataException extends Exception {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String metadataType;

    public AeonMetadataException(String metadataType, String reason) {
        super(metadataType + ": " + reason);
        this.metadataType = metadataType;
    }
}
