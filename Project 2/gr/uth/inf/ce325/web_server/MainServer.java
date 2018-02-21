package gr.uth.inf.ce325.web_server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer implements Runnable
{
    private String host; /* The host of the statistics server */
    private int port; /* The port where the statistics server listens for connections */

    public MainServer( String host, int port )
    {
        this.host = host;
        this.port = port;
    }

    public void run()
    {
        try ( ServerSocket server = new ServerSocket() )
        {
            server.bind( new InetSocketAddress( host, port ) );

            while ( true )
            {
                Socket conn = server.accept();

                new Thread( new MainServerRequest( conn ) ).start();
            }
        }
        catch ( IOException ex )
        {
            System.err.println( "Error when running the main server!" );
            System.err.println( "Error: " + ex );
        }
    }
}
