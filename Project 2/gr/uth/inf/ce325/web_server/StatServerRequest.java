package gr.uth.inf.ce325.web_server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

public class StatServerRequest implements Runnable
{
    private Socket sock; /* Socket to communicate with the client */

    public StatServerRequest( Socket sock )
    {
        this.sock = sock;
    }

    public void run()
    {
        try ( PrintWriter out = new PrintWriter( sock.getOutputStream(), true ) )
        {

            Date date = new Date();

            StringBuilder html = new StringBuilder();

            html.append( "<!DOCTYPE html>\r\n" );
            html.append( "<html>\r\n" );
            html.append( "<head>\r\n" );
            html.append( "<style> .size, .date {padding: 0 30px} h1.header {color: red; vertical-align: middle;}</style>\r\n" );
            html.append( "<title>CE325 HTTP Server Statistics</title>\r\n" );
            html.append( "<h1 class=\"header\">CE325 HTTP Server Statistics</h1>\r\n" );
            html.append( "</head>\r\n" );
            html.append( "<body>\r\n" );
            html.append( "<table>\r\n" );
            html.append( "<tr><th>Category</th><th>Statistics</th></tr>\r\n" );
            html.append( "<tr><td>Started At:</td><td>" + WebServer.serverStartedTime + "</td></tr>\r\n" );
            html.append( "<tr><td>Running for:</td><td>" + getRunningTime( date ) + "</td></tr>\r\n" );
            html.append( "<tr><td>All Serviced Requests:</td><td>" + MainServerRequest.servicedRequests + "</td></tr>\r\n" );
            html.append( "<tr><td>HTTP 400 Requests:</td><td>" + MainServerRequest.badRequests + "</tr>\r\n" );
            html.append( "<tr><td>HTTP 404 Requests:</td><td>" + MainServerRequest.fileNotFoundRequests + "</tr>\r\n" );
            html.append( "<tr><td>HTTP 405 Requests:</td><td>" + MainServerRequest.methodNotAllowedRequests + "</tr>\r\n" );
            html.append( "<tr><td>HTTP 500 Requests:</td><td>" + MainServerRequest.internalServerErrors + "</tr>\r\n" );
            html.append( "<tr><td>Average Service Time (msec):</td><td>" + MainServerRequest.averageServiceTime + "</td></tr>\r\n" );
            html.append( "</table>\r\n" );
            html.append( "</body>\r\n" );
            html.append( "</html>\r\n" );

            sendResponse( out, html.toString(), date );

        }
        catch ( IOException ex )
        {
            System.out.println( "Remote IP Address: " + sock.getLocalAddress() );
            System.out.println( "Date: " + ( new Date() ) );
            System.out.println( "Exception's Stack Trace: " + ex.getStackTrace() );
        }
    }

    private void sendResponse( PrintWriter out, String html, Date date )
    {

        out.print( "HTTP/1.1 200 OK\r\n" );
        out.print( "Date: " + date + "\r\n" );
        out.print( WebServer.SERVERNAME + "\r\n" );
        out.print( "Content-length: " + html.length() + "\r\n" );
        out.print( "Connection: close\r\n" );
        out.print( "Content-type: text/html\r\n\r\n" );
        out.print( html );

        out.flush();
    }

    /* Gets the current date and returns the time, that the server is running */
    private String getRunningTime( Date date )
    {
        long startedTimeMilisecs = WebServer.serverStartedTime.getTime();
        long currTimeMilisecs = date.getTime();

        long runningTimeMilisecs = currTimeMilisecs - startedTimeMilisecs;

        int seconds = ( int ) ( ( runningTimeMilisecs / 1000 ) % 60 );
        int minutes = ( int ) ( ( runningTimeMilisecs / ( 1000 * 60 ) ) % 60 );
        int hours = ( int ) ( ( runningTimeMilisecs / ( 1000 * 60 * 60 ) ) % 24 );
        int days = ( int ) ( ( runningTimeMilisecs / ( 1000 * 60 * 60 ) ) / 24 );

        return String.format( "%d days, %d hours, %d min, %d sec", days, hours, minutes, seconds );

    }


}
