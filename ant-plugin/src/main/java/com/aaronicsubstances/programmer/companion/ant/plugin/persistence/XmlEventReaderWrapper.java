package com.aaronicsubstances.programmer.companion.ant.plugin.persistence;

import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Wraps around XMLEventReader to provide lookahead functionality of XMLEvents with internal buffer.
 */
public class XmlEventReaderWrapper implements AutoCloseable {
    private final ArrayList<XMLEvent> readEvents = new ArrayList<>();
    private final XMLEventReader xmlReader;

    public XmlEventReaderWrapper(XMLEventReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    @Override
	public void close() throws Exception {
        xmlReader.close();
	}

    public XMLEvent lookahead(int distance) throws Exception {
        while (distance >= readEvents.size()) {
            XMLEvent xmlEvent = xmlReader.nextEvent();
            readEvents.add(xmlEvent);
        }
        return readEvents.get(distance);
    }

    public XMLEvent consume(int count) throws Exception {
        XMLEvent lastXmlEvent = null;
        for (int i = 0; i < count; i++) {
            lastXmlEvent = consume();
        }
        return lastXmlEvent;
    }

    public XMLEvent consume() throws Exception {
        XMLEvent xmlEvent = lookahead(0);
        if (!readEvents.isEmpty()) {
            readEvents.remove(0);
        }
        return xmlEvent;
    }

    public static boolean isXmlTag(XMLEvent xmlEvent) {
        return xmlEvent.isStartElement() || xmlEvent.isEndElement();
    }

    public XMLEvent consumeNextTag() throws Exception {
        while (true) {
            XMLEvent xmlEvent = lookahead(0);
            if (xmlEvent.isStartElement() || xmlEvent.isEndElement()) {
                consume();
                return xmlEvent;
            }
            // abort, unless event is whitespace.
            if (xmlEvent.asCharacters().isWhiteSpace()) {
                consume();
            }
            else {
                throw createAbortException("Expected start/end element but found " +
                    xmlEvent, xmlEvent);
            }
        }
    }

    public StartElement requireDocOpener(String name) throws Exception {
        XMLEvent nextTag = consume();
        if (!nextTag.isStartElement()) {
            nextTag = consumeNextTag();
        }
        if (nextTag.isStartElement()) {
            StartElement startElement = nextTag.asStartElement();
            if (name == null || name.equals(startElement.getName().getLocalPart())) {                
                return startElement;
            }
        }
        String expectation = describeExpectedTag(name, true);
        throw createAbortException("Expected root element " + expectation + " but found " +
            nextTag, nextTag);
    }

    public StartElement requireStartElement(String name) throws Exception {
        return requireStartElement(new String[]{ name });
    }

    public StartElement requireStartElement(String[] names) throws Exception {
        StartElement startElem = locateStartElement(names);
        if (startElem != null) {
            return startElem;
        }
        StringBuilder expectation = new StringBuilder();
        if (names != null) {
            for (String name : names) {
                if (name == null) {
                    continue;
                }
                if (expectation.length() > 0) {
                    expectation.append(", ");
                }
                expectation.append(describeExpectedTag(name, true));
            } 
        }
        if (expectation.length() == 0) {
            expectation.append("<>");
        }
        XMLEvent nextTag = lookahead(0);
        throw createAbortException("Expected " + expectation + " but found " +
                    nextTag, nextTag);
	}

    public StartElement locateStartElement(String name) 
            throws Exception {
        return locateStartElement(new String[]{ name });
    }

    public StartElement locateStartElement(String[] names) throws Exception {
        XMLEvent nextTag = consumeNextTag();
        while (nextTag.isStartElement()) {
            StartElement startElem = nextTag.asStartElement();
            if (names == null) {
                return startElem;
            }
            for (String name : names) {
                if (name == null || name.equals(startElem.getName().getLocalPart())) {
                    return startElem;
                }
            }
            // skip unnecessary tags.
            nextTag = skipPastTag();
        }
        // push back.
        readEvents.add(0, nextTag);
        return null;
    }

    private XMLEvent skipPastTag() throws Exception {
        // increase depth any time we encounter a start element.
        // decrease depth any time we encounter an end element,
        // except when depth is at bottommost (0), in which case
        // we read past, and return
        int depth = 1;
        XMLEvent xmlEvent;
        do {
            xmlEvent = consume();
            if (xmlEvent.isStartElement()) {
                depth++;
            }
            else if (xmlEvent.isEndElement()) {
                depth--;
            }
        } while (depth <= 0 && xmlEvent.isEndElement());
        return consumeNextTag();
    }

    public EndElement requireEndElement(String name) throws Exception {
        EndElement endElem = locateEndElement(name);
        if (endElem != null) {
            return endElem;
        }
        String expectation = describeExpectedTag(name, false);
        XMLEvent nextTag = lookahead(0);
        throw createAbortException("Expected " + expectation + " but found " +
            nextTag, nextTag);
    }

    public EndElement locateEndElement(String name)  throws Exception {
        XMLEvent nextTag = consumeNextTag();
        // skip unnecessary tags.
        while (nextTag.isStartElement()) {
            nextTag = skipPastTag();
        }
        if (nextTag.isEndElement()) {
            EndElement endElem = nextTag.asEndElement();
            if (name == null || name.equals(endElem.getName().getLocalPart())) {
                return endElem;
            }
        }
        // push back.
        readEvents.add(0, nextTag);
        return null;
    }

    public String readElementValue() throws Exception {
        StringBuilder content = null;
        XMLEvent nextEvent;
        while (!(nextEvent = lookahead(0)).isEndElement()) {
            if (!nextEvent.isCharacters()) {
                throw createAbortException("Expected element text here but found " +
                    nextEvent, nextEvent);
            }
            if (content == null) {
                content = new StringBuilder();
            }
            content.append(nextEvent.asCharacters().getData());
            consume();
        }
        return content != null ? content.toString() : null;
	}

    static String describeExpectedTag(String name, boolean start) {
        StringBuilder desc = new StringBuilder();
        if (name != null) {
            desc.append(name);
        }
        if (start) {
            desc.insert(0, "<");
        }
        else {
            desc.insert(0, "</");
        }
        return desc.toString().trim() + ">";
    }

    public static Exception createAbortException(String errMsg, XMLEvent event) {
        Location loc = event.getLocation();
        String locMsg = String.format("Ln: %s; Col: %s ", loc.getLineNumber(),
            loc.getColumnNumber());
        throw new RuntimeException(locMsg + ' ' + errMsg);
    }

    public static Attribute requireAttribute(StartElement startElement, String name) 
            throws Exception {
        QName qName = new QName(name);
        Attribute att = startElement.getAttributeByName(qName);
        if (att == null) {
            throw createAbortException("Attribute " + qName + " not found", startElement);
        }
        return att;
	}

    public static String requireAttributeValue(StartElement startElement, String name) 
            throws Exception {
        Attribute att = requireAttribute(startElement, name);
        return att.getValue();
	}

    public static int requireAttributeValueAsInt(StartElement startElement, String name) 
            throws Exception {
        Attribute att = requireAttribute(startElement, name);
        try {
            return Integer.parseInt(att.getValue());
        }
        catch (NumberFormatException ex) {
            throw createAbortException("Value of " + "@" + att.getName() + 
                " is invalid 32-bit signed integer", att);
        }
	}

    public static boolean requireAttributeValueAsBoolean(StartElement startElement, String name) 
            throws Exception {
        Attribute att = requireAttribute(startElement, name);
        return Boolean.parseBoolean(att.getValue());
	}
}