import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

/**
 *  Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads for
 *  pathfinding, under some constraints.
 *  See OSM documentation on
 *  <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 *  <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 *  and the java
 *  <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 *  @author Alan Yao
 */
public class MapDBHandler extends DefaultHandler {
    /**
     * Only allow for non-service roads; this prevents going on pedestrian streets as much as
     * possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
     * roads, but in practice we walk all over them with such impunity that we forget cars can
     * actually drive on them.
     */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private final GraphDB g;
    private LinkedList<GraphNode> connectedList = new LinkedList<GraphNode>();
    private long id;
    private long lat;
    private long lon ;
    private long value;

    private Long wayid;

    public MapDBHandler(GraphDB g) {
        this.g = g;
    }

    /**
     * Called at the beginning of an element. Typically, you will want to handle each element in
     * here, and you may want to track the parent element.
     * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
     *            if Namespace processing is not being performed.
     * @param localName The local name (without prefix), or the empty string if Namespace
     *                  processing is not being performed.
     * @param qName The qualified name (with prefix), or the empty string if qualified names are
     *              not available. This tells us which element we're looking at.
     * @param attributes The attributes attached to the element. If there are no attributes, it
     *                   shall be an empty Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another exception.
     * @see Attributes
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        /* Some example code on how you might begin to parse XML files. */


        if (qName.equals("node")) {
            activeState = "node";

            if (attributes != null) {

                id = Long.parseLong(attributes.getValue("id"), 10);
                lat = Long.parseLong(attributes.getValue("lat"), 10);
                lon = Long.parseLong(attributes.getValue("lon"), 10);
                GraphNode node = new GraphNode(id, lat, lon);
                g.setNodeinfo(id, node);
            }

            } else if (qName.equals("way")) {
                activeState = "way";
                wayid = Long.parseLong(attributes.getValue("id"), 10);
//            System.out.println("Beginning a way...");
            } else if (activeState.equals("way") && qName.equals("nd")) {
                String k = attributes.getValue("ref");
                GraphNode node = g.getNodeinfo().get(k);
                connectedList.add(node);


            } else if (activeState.equals("node") && qName.equals("tag") && attributes.getValue("k")
                    .equals("name")) {
                // System.out.println("Node with name: " + attributes.getValue("v"));
                String name = attributes.getValue("v");
                GraphNode node = g.getNodeinfo().get(id);
                node.setName(name);
            }
        }

        /**
         * Receive notification of the end of an element. You may want to take specific terminating
         * actions here, like finalizing vertices or edges found.
         * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or
         *            if Namespace processing is not being performed.
         * @param localName The local name (without prefix), or the empty string if Namespace
         *                  processing is not being performed.
         * @param qName The qualified name (with prefix), or the empty string if qualified names are
         *              not available.
         * @throws SAXException  Any SAX exception, possibly wrapping another exception.
         */
        @Override
        public void endElement (String uri, String localName, String qName)throws SAXException {
            if (qName.equals("way")) {
                //  System.out.println("Finishing a way...");
                for( int i = 0; i < connectedList.size(); i++){
                    if(i ==0) {
                        g.addConnection(connectedList.get(0).getid(), connectedList.get(1).getid());
                    }
                    if(i ==connectedList.size()-1){
                        g.addConnection(connectedList.get(i).getid(), connectedList.get(i-1).getid());
                    }
                    else{
                        g.addConnection(connectedList.get(i-1).getid(), connectedList.get(i).getid());
                        g.addConnection(connectedList.get(i).getid(), connectedList.get(i-1).getid());
                    }

                }

                connectedList.clear();
            } else if (qName.equals("node")) {

            }
        }
    }


