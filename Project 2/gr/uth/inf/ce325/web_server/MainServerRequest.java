package gr.uth.inf.ce325.web_server;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainServerRequest implements Runnable
{
    private Socket sock; /* Socket to communicate with the client */

    public static volatile int servicedRequests = 0;
    public static volatile int badRequests = 0;
    public static volatile int fileNotFoundRequests = 0;
    public static volatile int methodNotAllowedRequests = 0;
    public static volatile int internalServerErrors = 0;
    public static volatile double averageServiceTime = 0;

    public MainServerRequest( Socket sock )
    {
        this.sock = sock;
    }

    public void run()
    {
        try ( BufferedReader in = new BufferedReader( new InputStreamReader( sock.getInputStream() ) ) )
        {
            String request; /* Request sent by the client */
            String statusCode; /* HTTP Status code */
            String header; /* Header sent by the client */
            String host = "None";
            String userAgent = "None";

            Date connectionDate = new Date(); /* Approximate date of connection to the server */

            request = in.readLine(); /* Gets the request */

            if ( request == null )
                return;

            do
            {
                header = in.readLine();

                if ( header.startsWith( "Host:" ) )
                    host = header;

                if ( header.startsWith( "User-Agent:" ) )
                    userAgent = header;

            }
            while ( !header.equals( "" ) );

            String[] req = request.split( "\\s+" ); /* Splits the request to parts */

            try ( PrintWriter out = new PrintWriter( sock.getOutputStream(), true ) )
            {
                if ( req.length != 3 )
                {
                    statusCode = "400 Bad Request";
                    badRequests++;
                    errorResponse( statusCode, out );
                    WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, statusCode, userAgent );

                    return;
                }

                if ( !req[0].equals( "GET" ) )
                {
                    statusCode = "405 Method Not Allowed";
                    methodNotAllowedRequests++;
                    errorResponse( statusCode, out );
                    WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, statusCode, userAgent );

                    return;
                }

                req[1] = URLDecoder.decode( req[1], "UTF-8" );

                File file = new File( WebServer.root, req[1].substring( 1, req[1].length() ) );

                if ( !file.exists() )
                {
                    statusCode = "404 Not Found";
                    fileNotFoundRequests++;
                    errorResponse( statusCode, out );
                    WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, statusCode, userAgent );

                    return;
                }

                try ( OutputStream data = new BufferedOutputStream( sock.getOutputStream() ) )
                {
                    String version;

                    if ( req[2].equals( "HTTP/1.1" ) )
                    {
                        version = req[2];

                        if ( host.equals( "" ) )
                        {
                            statusCode = "400 Bad Request";
                            badRequests++;
                            errorResponse( statusCode, out );
                            WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, statusCode, userAgent );
                            return;

                        }

                    }
                    else if ( req[2].equals( "HTTP/1.0" ) )
                    {
                        version = req[2];
                    }
                    else
                    {
                        statusCode = "400 Bad Request";
                        badRequests++;
                        errorResponse( statusCode, out );
                        WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, statusCode, userAgent );
                        return;
                    }

                    statusCode = "200 OK";
                    if ( file.isFile() )
                    {
                        int index = req[1].lastIndexOf('.');
                        String ext = req[1].substring( index );
                        sendFile(statusCode, version, out, file, ext, data);
                    }
                    else
                    {
                        File html;

                        html = searchForHTML( file );

                        if ( html != null )
                        {
                            int index = html.getName().lastIndexOf('.');
                            String ext = html.getName().substring( index );

                            try
                            {
                                sendFile( statusCode, version, out, html, ext, data );
                            }
                            catch ( IOException ex )
                            {
                                internalServerErrors++;
                                WebServer.logger.errorLog( sock.getLocalAddress().toString(), connectionDate, request, ex.getStackTrace().toString() );
                            }

                        }
                        else
                        {
                            sendDirectory( out, file );
                        }
                    }


                }
                catch ( IOException ex )
                {
                    internalServerErrors++;
                    WebServer.logger.errorLog( sock.getLocalAddress().toString(), connectionDate, request, ex.getStackTrace().toString() );
                }

            }

            WebServer.logger.log( sock.getLocalAddress().toString(), connectionDate, request, "200 OK", userAgent );

            Date finishedRequest = new Date();
            long servicedRequestTime = finishedRequest.getTime() - connectionDate.getTime();
            averageServiceTime = ( averageServiceTime * servicedRequests + servicedRequestTime ) / ( servicedRequests + 1 );
            servicedRequests++;

        }
        catch ( IOException ex )
        {
            internalServerErrors++;
            try
            {
                WebServer.logger.errorLog(sock.getLocalAddress().toString(), new Date(), "Not available", ex.getStackTrace().toString());
            }
            catch ( IOException e )
            {
            }
        }

    }

    private File searchForHTML( File file )
    {
        for ( File f : file.listFiles() )
        {
            if ( f.isFile() && ( f.getName().equals( "index.html" ) || ( f.getName().equals( "index.htm" ) ) ) )
                return f;
        }

        return null;
    }

    public void sendDirectory( PrintWriter out, File file )
    {
        StringBuilder html = new StringBuilder();

        html.append( "<!DOCTYPE html>\r\n" );
        html.append( "<html>\r\n" );
        html.append( "<head>\r\n" );
        html.append( "<style> .size, .date {padding: 0 30px} h1.header {color: red; vertical-align: middle;}</style>\r\n" );
        html.append( "<title>CE325 HTTP Server</title>\r\n" );
        html.append( "<h1 class=\"header\"><img src=\"/icons/java.png\" /> CE325 HTTP Server</h1>\r\n" );
        html.append( "<h1>Current Folder: " + ( file.equals( WebServer.root ) ? "/" : file.getName() ) + "</h1>\r\n" );
        html.append( "</head>\r\n" );
        html.append( "<body>\r\n" );
        html.append( "<table>\r\n" );
        html.append( "<tr><th></th><th>Filename</th><th>Size</th><th>Last Modified</th>\r\n" );

        if ( !file.equals( WebServer.root ) )
        {
            int index = WebServer.root.getPath().length();
            String ext = "";

            if ( file.getParent().substring( index ).equals( "" ) )
                ext = "/";

            ext += file.getParent().substring(index);

            html.append( "<tr><td><img src=\"" + WebServer.icon.getIcon( ".parent" ) + "\"/></td>" );
            html.append( "<td class=\"link\"><a href=\"" + ext + "\">" + "Parent directory" + "</a></td>" );

        }


        for ( File arxeio : file.listFiles() )
        {
            int index = WebServer.root.getPath().length();
            String ext;
            ext = arxeio.getPath().substring( index );

            html.append( "<tr><td><img src=\"" + ( arxeio.isFile() ? WebServer.icon.getIcon( arxeio.getName() ) : WebServer.icon.getIcon( ".dir" ) ) + "\"/></td>" );
            html.append("<td class=\"link\"><a href=\"" + ext + "\">" + arxeio.getName() + "</a></td>");
            html.append( "<td class=\"size\">" + getFileSize( arxeio ) + "</td>" );
            html.append( "<td class=\"date\">" + ( new Date ( arxeio.lastModified() ) ) + "</td>\n" );

        }

        html.append( "</table>\r\n" );
        html.append( "</body>\r\n" );
        html.append( "</html>\r\n" );

        out.print("HTTP/1.1 200 OK" + "\r\n");
        Date date = new Date();
        out.print( "Date: " + date + "\r\n" );
        out.print( WebServer.SERVERNAME + "\r\n" );
        out.print( "Content-length: " + html.toString().length() + "\r\n" );
        out.print( "Connection: close\r\n" );
        out.print( "Content-type: text/html; charset=utf-8\r\n\r\n" );
        out.print( html.toString() );
        out.flush();

    }

    public void sendFile( String statusCode, String version, PrintWriter out, File file, String ext, OutputStream data ) throws IOException
    {
        out.print( version + " " + statusCode + "\r\n" );
        Date date = new Date();
        out.print( "Date: " + date + "\r\n" );
        out.print( WebServer.SERVERNAME + "\r\n" );
        out.print( "Last-Modified: " + getLastModifiedDate( file.lastModified() ) + "\r\n" );
        out.print( "Content-length: " + file.length() + "\r\n" );
        out.print( "Connection: close\r\n" );
        out.print( "Content-type: " + WebServer.mime.getMIMEType( ext ) + "\r\n\r\n" );
        out.flush();

        int count;
        byte[] buffer = new byte[ 8192 ];
        FileInputStream in = new FileInputStream( file.getPath() );

        while ( ( ( count = in.read( buffer ) ) != -1 ) )
        {
            data.write( buffer );
            data.flush();
        }

        in.close();

    }

    public void errorResponse( String statusCode, PrintWriter out )
    {
        String title, body;

        switch ( statusCode )
        {
            case "400 Bad Request":
                title = "Bad Request";
                body = "HTTP Error 400: Bad Request";
                break;
            case "404 Not Found":
                title = "File Not Found";
                body = "HTTP Error 404: File Not Found";
                break;
            case "405 Method Not Allowed":
                title = "Method Not Allowed";
                body = "HTTP Error 405: Method Not Allowed";
                break;
            case "500 Internal Server Error":
                title = "Internal Server Error";
                body = "HTTP Error 500: Internal Server Error";
                break;
            default:
                title = "Unknown Error";
                body = "HTTP Error: Unknown Error";
        }

        StringBuilder html = new StringBuilder();

        html.append( "<!DOCTYPE html>\r\n" );
        html.append( "<html>\r\n" );
        html.append( "<head>\r\n" );
        html.append( "<style> .size, .date {padding: 0 30px} h1.header {color: red; vertical-align: middle;}</style>\r\n" );
        html.append( "<title>" + title + "</title>\r\n" );
        html.append( "<h1 class=\"header\">" + body + "</h1>\r\n" );
        html.append( "</head>\r\n" );
        html.append( "<body>\r\n" );
        html.append( "</body>\r\n" );
        html.append( "</html>\r\n" );

        out.print( "HTTP/1.1 " + statusCode + "\r\n" );
        Date date = new Date();
        out.print( "Date: " + date + "\r\n" );
        out.print( WebServer.SERVERNAME + "\r\n" );
        out.print( "Content-length: " + html.toString().length() + "\r\n" );
        out.print( "Connection: close\r\n" );
        out.print( "Content-type: text/html\r\n\r\n" );
        out.print( html.toString() );
        out.flush();
    }

    private String getLastModifiedDate( long miliseconds )
    {
        String dateFormat = "EEE, d MMM YYYY HH:mm:ss z";
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat( dateFormat );
        dateFormatGmt.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        return dateFormatGmt.format( miliseconds );
    }

    private String getFileSize( File file )
    {
        double bytes = file.length();
        double kilobytes = ( bytes / 1024);
        double megabytes = (kilobytes / 1024);
        double gigabytes = (megabytes / 1024);

        if ( gigabytes >= 1 )
            return String.format( "%.1f GB", gigabytes );
        else if ( megabytes >= 1 )
            return String.format( "%.1f MB", megabytes );
        else if ( kilobytes >= 1 )
            return String.format( "%.1f KB", kilobytes );
        else
            return String.format( "%.1f B", bytes );
    }
}
