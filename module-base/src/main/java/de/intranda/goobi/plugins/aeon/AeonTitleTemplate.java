package de.intranda.goobi.plugins.aeon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

/**
 * The configured process title: fixed text with {Field title} placeholders, e.g. {Transaction Identifier}_{Home site}_group.
 *
 * A placeholder names a field by its title. The values are supplied when the title is rendered rather than when it is parsed, because the operator
 * can still edit the fields between loading a request and creating its processes. There is no escape syntax: braces are not legal in a process
 * title anyway.
 *
 * Only the field values are cleaned of characters Goobi does not permit in a title. The fixed text is configured on purpose and stays as written.
 */
public final class AeonTitleTemplate {

    /**
     * The outcome of rendering. The title is only usable when no referenced field was empty.
     *
     * @param title the rendered title, lacking the values of the empty fields
     * @param emptyFields the titles of the referenced fields that had no value, each once, in order of appearance
     */
    public record Result(String title, List<String> emptyFields) {
        public boolean isComplete() {
            return emptyFields.isEmpty();
        }
    }

    private static final String REPLACEMENT = "_";

    /** a fixed text part, or a field title when isField is set */
    private record Part(String text, boolean isField) {
    }

    private final List<Part> parts;

    private AeonTitleTemplate(List<Part> parts) {
        this.parts = parts;
    }

    /**
     * @param template the configured title, fixed text with {Field title} placeholders
     * @return the parsed template
     * @throws IllegalArgumentException if the template is blank, a brace is unmatched or nested, or a placeholder names no field
     */
    public static AeonTitleTemplate parse(String template) {
        if (StringUtils.isBlank(template)) {
            throw new IllegalArgumentException("the process title template is empty");
        }
        List<Part> parts = new ArrayList<>();
        int position = 0;
        while (position < template.length()) {
            int open = template.indexOf('{', position);
            int close = template.indexOf('}', position);
            if (open < 0) {
                if (close >= 0) {
                    throw new IllegalArgumentException("'}' without an opening '{' in the process title template " + template);
                }
                parts.add(new Part(template.substring(position), false));
                break;
            }
            if (close >= 0 && close < open) {
                throw new IllegalArgumentException("'}' without an opening '{' in the process title template " + template);
            }
            if (close < 0) {
                throw new IllegalArgumentException("'{' without a closing '}' in the process title template " + template);
            }
            String field = template.substring(open + 1, close);
            if (field.contains("{")) {
                throw new IllegalArgumentException("nested '{' in the process title template " + template);
            }
            if (StringUtils.isBlank(field)) {
                throw new IllegalArgumentException("a placeholder names no field in the process title template " + template);
            }
            if (open > position) {
                parts.add(new Part(template.substring(position, open), false));
            }
            parts.add(new Part(field.trim(), true));
            position = close + 1;
        }
        return new AeonTitleTemplate(parts);
    }

    /**
     * The titles of the fields the template refers to, each once, in order of appearance.
     *
     * @return the referenced field titles
     */
    public List<String> references() {
        Set<String> fields = new LinkedHashSet<>();
        for (Part part : parts) {
            if (part.isField()) {
                fields.add(part.text());
            }
        }
        return List.copyOf(fields);
    }

    /**
     * @param valueOf the current value of a field, by its title. May return null for a field without a value.
     * @param invalidCharacterRegex the characters Goobi does not permit in a process title, each replaced by an underscore
     * @return the title, together with the referenced fields that were empty
     */
    public Result render(Function<String, String> valueOf, String invalidCharacterRegex) {
        StringBuilder title = new StringBuilder();
        Set<String> emptyFields = new LinkedHashSet<>();
        for (Part part : parts) {
            if (!part.isField()) {
                title.append(part.text());
                continue;
            }
            String value = valueOf.apply(part.text());
            if (StringUtils.isBlank(value)) {
                emptyFields.add(part.text());
            } else {
                title.append(value.replaceAll(invalidCharacterRegex, REPLACEMENT));
            }
        }
        return new Result(title.toString(), List.copyOf(emptyFields));
    }
}
