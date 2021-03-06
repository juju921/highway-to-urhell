package com.highway2urhell.service.impl;

import com.highway2urhell.domain.EntryPathData;
import com.highway2urhell.domain.TypePath;
import com.highway2urhell.exception.H2HException;
import com.highway2urhell.service.AbstractLeechService;
import com.highway2urhell.service.FilterStaticFileH2H;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileSystemService extends AbstractLeechService {

    private final static String WEBXML = "web.xml";
    public static final String FRAMEWORK_NAME = "SYSTEM";

    private String pathWebXml;

    public FileSystemService() {
        super(FRAMEWORK_NAME);
        setTriggerAtStartup(true);
    }

    @Override
    protected void gatherData(List<EntryPathData> incoming) {
        String rootPath = System.getProperty("H2H_PATH");
        try {
            if (rootPath == null) {
                throw new H2HException("Unknow Variable Path H2h. Please Set pathH2h to location application deployment.");
            }
            if ("".equals(rootPath)) {
                throw new H2HException(
                        "Variable Path H2h is empty. Please Set pathH2h to location application deployment.");
            }
            // Step 1 search Web.XML
            searchAndParseWebXML(rootPath);
            // Step 2 search static files by categories
            searchStaticFiles(rootPath);

        } catch (H2HException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void searchStaticFiles(String rootPath) {
        File fileRoot = new File(rootPath);
        searchStatic(fileRoot);
    }

    private void searchStatic(File file) {
        if (file.isDirectory()) {
            if (file.canRead()) {
                for (File tempFile : file.listFiles(new FilterStaticFileH2H())) {
                    if (tempFile.isDirectory()) {
                        searchStatic(tempFile);
                    } else {
                        EntryPathData entry = new EntryPathData();
                        entry.setTypePath(TypePath.STATIC);
                        entry.setUri(tempFile.getPath());
                        addEntryPath(entry);
                    }
                }
            }
        } else {
            EntryPathData entry = new EntryPathData();
            entry.setTypePath(TypePath.STATIC);
            entry.setUri(file.getPath());
            addEntryPath(entry);
        }
    }

    private void searchAndParseWebXML(String rootPath) {
        File fileRoot = new File(rootPath);
        searchWebXML(fileRoot);
        LOGGER.info("pathWebXml : " + pathWebXml);
        if (pathWebXml != null && !"".equals(pathWebXml)) {
            parseWebXml(pathWebXml);
        }
    }

    private void parseWebXml(String pathWebXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new File(pathWebXml));
            extractServlet(document);
            extractFilter(document);
            extractListener(document);
        } catch (ParserConfigurationException e) {
            LOGGER.error("error while parsing web.xml " + pathWebXml, e);
        } catch (SAXException e) {
            LOGGER.error("error while parsing web.xml " + pathWebXml, e);
        } catch (IOException e) {
            LOGGER.error("error while parsing web.xml " + pathWebXml, e);
        }
    }

    private void searchWebXML(File file) {
        if (file.isDirectory()) {
            if (file.canRead()) {
                for (File tempFile : file.listFiles()) {
                    if (tempFile.isDirectory()) {
                        searchWebXML(tempFile);
                    } else {
                        if (WEBXML.equals(tempFile.getName().toLowerCase())) {
                            pathWebXml = tempFile.getAbsolutePath();
                        }
                    }
                }
            }
        } else {
            if (WEBXML.equals(file.getName().toLowerCase())) {
                pathWebXml = file.getAbsolutePath();
            }
        }
    }

    private void extractListener(Document document) {
        NodeList nodeListServlet = document.getDocumentElement().getElementsByTagName("listener");
        for (int i = 0; i < nodeListServlet.getLength(); i++) {
            Node node = nodeListServlet.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                EntryPathData web = new EntryPathData();
                Element elem = (Element) node;
                String name = "listener-class";
                web.setClassName(getNodeValue(elem, name));
                web.setTypePath(TypePath.LISTENER);
                addEntryPath(web);
            }
        }
    }

    private void extractFilter(Document document) {
        NodeList nodeListMapping = document.getDocumentElement().getElementsByTagName("filter-mapping");
        NodeList nodeListServlet = document.getDocumentElement().getElementsByTagName("filter");
        for (int i = 0; i < nodeListServlet.getLength(); i++) {
            Node node = nodeListServlet.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                EntryPathData web = new EntryPathData();
                Element elem = (Element) node;
                web.setMethodName(getNodeValue(elem, "filter-name"));
                web.setClassName(getNodeValue(elem, "filter-class"));
                web.setTypePath(TypePath.FILTER);
                for (int j = 0; j < nodeListMapping.getLength(); j++) {
                    Element elemMapping = (Element) nodeListMapping.item(j);
                    if (getNodeValue(elemMapping, "filter-name").equals(web.getMethodName())) {
                        NodeList urlPattern = elemMapping.getElementsByTagName("url-pattern");
                        if (urlPattern != null && urlPattern.getLength() > 0) {
                            web.setUri(urlPattern.item(0).getChildNodes().item(0).getNodeValue());
                        } else {
                            // Check servlet-Name
                            NodeList nameServlet = elemMapping.getElementsByTagName("servlet-name");
                            if (nameServlet != null && nameServlet.getLength() > 0) {
                                web.setUri(nameServlet.item(0).getChildNodes().item(0).getNodeValue());
                            }
                        }
                    }
                }
                addEntryPath(web);
            }
        }
    }

    private void extractServlet(Document document) {
        NodeList nodeListMapping = document.getDocumentElement().getElementsByTagName("servlet-mapping");
        NodeList nodeListServlet = document.getDocumentElement().getElementsByTagName("servlet");
        for (int i = 0; i < nodeListServlet.getLength(); i++) {
            Node node = nodeListServlet.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                EntryPathData web = new EntryPathData();
                Element elem = (Element) node;
                web.setMethodName(getNodeValue(elem, "servlet-name"));
                web.setClassName(getNodeValue(elem, "servlet-class"));
                web.setTypePath(TypePath.SERVLET);
                for (int j = 0; j < nodeListMapping.getLength(); j++) {
                    Element elemMapping = (Element) nodeListMapping.item(j);
                    if (getNodeValue(elemMapping, "servlet-name").equals(web.getMethodName())) {
                        web.setUri(getNodeValue(elemMapping, "url-pattern"));
                    }
                }
                addEntryPath(web);
            }
        }
    }

    private String getNodeValue(Element elem, String name) {
        return elem.getElementsByTagName(name).item(0).getChildNodes().item(0).getNodeValue();
    }
}
