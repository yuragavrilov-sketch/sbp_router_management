package ru.copperside.sbprouter.management.traffic.application;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;

/**
 * Parses a GCSvc response XML body to detect a {@code <fault>} element and extract
 * the text of the first {@code <faultstring>} child. Namespace-agnostic (local-name matching).
 * Unparseable or blank input is treated as no fault.
 */
public class FaultParser {

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    public record Result(boolean hasFault, String faultString) {}

    private static final Result NO_FAULT = new Result(false, null);

    /**
     * Parse the XML bytes and return a fault result.
     * @param responseXml raw bytes of the GCSvc response; null/blank returns {@code {false, null}}.
     */
    public Result parse(byte[] responseXml) {
        if (responseXml == null || responseXml.length == 0) {
            return NO_FAULT;
        }
        boolean hasFault = false;
        String faultString = null;
        String currentTag = null;
        XMLStreamReader reader = null;
        try {
            reader = XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(responseXml));
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    currentTag = reader.getLocalName();
                    if ("fault".equalsIgnoreCase(currentTag)) {
                        hasFault = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if (hasFault && faultString == null && "faultstring".equalsIgnoreCase(currentTag)) {
                        String text = reader.getText();
                        if (text != null && !text.isBlank()) {
                            faultString = text.trim();
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    currentTag = null;
                }
                if (hasFault && faultString != null) {
                    break;
                }
            }
        } catch (Exception e) {
            return NO_FAULT;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) { /* best-effort */ }
            }
        }
        return new Result(hasFault, faultString);
    }
}
