package gr.uth.inf.ce325.web_server;

import gr.uth.inf.ce325.xml_parser.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class WebServer
{
    private static int mainServerPort; /* Port of the main server */
    private static int statServerPort; /* Port of the statistics server */
    private static String logDirectory;
    private static String errorlogDirectory;

    static Date serverStartedTime; /* Server's startup time */
    static File root; /* Root directory of the server */
    static MIMEMap mime; /* Matches file extensions with MIME types */
    static IconsMap icon; /* Matches file extensions with icons */
    static Logger logger;

    public static final String SERVERNAME = "CE325 (Java based server)";

    public static void main( String[] args ) throws InterruptedException
    {
        if ( args.length != 1 )
        {
            System.err.println( "Usage: java WebServer <configuration file>" );
            System.exit(1);
        }

        String config = args[0]; // na allax8ei

        setUpWebServer( config );

        MainServer mainServer = new MainServer( "localhost", mainServerPort );
        StatServer statServer = new StatServer( "localhost", statServerPort );

        mime = new MIMEMap();
        icon = new IconsMap( "/icons/" ); /* takes as an argument the folder, that has the icons */
        logger = new Logger( logDirectory, errorlogDirectory );

        Thread mainThread = new Thread( mainServer );
        Thread statThread = new Thread( statServer );

        serverStartedTime = new Date(); /* Gets the time when the server has been started */

        /* Starts both servers */
        mainThread.start();
        statThread.start();

        /* Waits for both servers to finish */
        mainThread.join();
        statThread.join();

    }

    private static void setUpWebServer( String config )
    {

        Node node;

        DocumentBuilder docBuilder = new DocumentBuilder();
        Document doc = docBuilder.getDocument( config );

        Node rootNode = doc.getRootNode();

        node = findNode( rootNode, "listen" );

        if ( node == null )
        {
            System.out.println( "[ERROR]: Node with name listen was not found!" );
            System.exit( 1 );
        }

        mainServerPort = Integer.parseInt( node.getFirstAttribute().getValue() );

        node = findNode( rootNode, "statistics" );

        if ( node == null )
        {
            System.out.println( "[ERROR]: Node with name statistics was not found!" );
            System.exit( 1 );
        }

        statServerPort = Integer.parseInt( node.getFirstAttribute().getValue() );

        node = findNode( rootNode, "document-root" );

        if ( node == null )
        {
            System.out.println( "[ERROR]: Node with name document-root was not found!" );
            System.exit( 1 );
        }

        root = new File( node.getFirstAttribute().getValue() );

        if ( !root.exists() )
        {
            System.out.println( "[ERROR]: The given root directory does not exist!" );
            System.exit(1);
        }

        if ( root.isFile() )
        {
            System.out.println( "[ERROR]: Document root should be a folder, not a file!" );
            System.exit( 1 );
        }

        node = findNode( rootNode, "log" );

        if ( node == null )
        {
            System.out.println( "[ERROR]: Node with name log was not found!" );
            System.exit( 1 );
        }

        logDirectory = node.getFirstChild().getFirstAttribute().getValue();
        errorlogDirectory = node.getNextChild().getFirstAttribute().getValue();

    }

    private static Node findNode( Node root, String nodeName )
    {

        Node node = null;

        if ( root.getName().equals( nodeName ) )
            return root;

        for ( int i = 0; i < root.getChildren().size(); i++ )
        {

            node = root.getChildren().get(i);

            if ( node.getName().equals( nodeName ) )
                return node;

            node = traverseTree( node, nodeName );

            if ( node != null )
                if ( node.getName().equals( nodeName ) )
                    return node;

        }

        return node;
    }

    private static Node traverseTree( Node node, String nodeName )
    {

        Node curr;

        for ( int i = 0; i < node.getChildren().size(); i++ )
        {

            if ( node.getChildren().get(i).getName().equals( nodeName ) )
                return node.getChildren().get(i);

            curr = traverseTree( node.getChildren().get(i), nodeName );

            if ( curr != null )
                if ( curr.getName().equals( nodeName ) )
                    return curr;

        }

        return null;
    }
}
