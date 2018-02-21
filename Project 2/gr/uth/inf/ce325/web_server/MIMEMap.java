package gr.uth.inf.ce325.web_server;

import java.util.HashMap;

public class MIMEMap
{
    private HashMap hm;

    public MIMEMap()
    {
        hm = new HashMap();

        populate();

    }

    private void populate()
    {
        /* Text */
        hm.put( ".txt", "text/plain" );

        /* Image */
        hm.put( ".jpg", "image/jpeg" );
        hm.put( ".jpeg", "image/jpeg" );
        hm.put( ".bmp", "image/bmp" );
        hm.put( ".tiff", "image/tiff" );
        hm.put( ".png", "image/png" );
        hm.put( ".ppm", "image/x-portable-pixmap" );
        hm.put( ".gif", "image/gif" );

        /* Video */
        hm.put( ".avi", "video/x-msvideo" );
        hm.put( ".mp4", "video/mp4" );
        hm.put( ".flv", "video/x-flv" );
        hm.put( ".ogv", "video/ogg" );
        hm.put( ".webm", "video/webm" );

        /* Audio */
        hm.put( ".mp3", "audio/mpeg" );
        hm.put( ".ogg", "audio/ogg" );

        /* Office */
        hm.put( ".doc", "application/msword" );
        hm.put( ".xls", "application/vnd.ms-excel" );
        hm.put( ".ppt", "application/vnd.ms-powerpoint" );
        hm.put( ".pps", "application/vnd.ms-powerpoint" );

        /* HTML */
        hm.put( ".html", "text/html" );
        hm.put( ".htm", "text/html" );
    }

    public synchronized String getMIMEType( String extension )
    {
        if ( hm.containsKey( extension ) )
            return ( String ) hm.get( extension );
        else
            return "text/plain";
    }
}
