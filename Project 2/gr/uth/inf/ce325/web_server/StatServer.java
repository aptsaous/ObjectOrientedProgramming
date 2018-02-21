package gr.uth.inf.ce325.web_server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class StatServer implements Runnable
{
    private String host; /* The host of the statistics server */
    private int port; /* The port where the statistics server listens for connections */

    public StatServer( String host, int port )
    {
        this.host = host;
        this.port = port;
    }

    public void run()
    {
        try ( ServerSocket stat = new ServerSocket() )
        {
            stat.bind( new InetSocketAddress( host, port ) );

            while ( true )
            {
                Socket conn = stat.accept();

                new Thread( new StatServerRequest( conn ) ).start();
            }
        }
        catch ( IOException ex )
        {
            System.err.println( "Error when running the statistics server!" );
            System.err.println( "Error: " + ex );
        }
    }
}
