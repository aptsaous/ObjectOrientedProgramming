package gr.uth.inf.ce325.web_server;

import java.util.HashMap;

public class IconsMap
{
    private HashMap hm;
    private String iconsFolder;

    public IconsMap( String iconsFolder )
    {
        hm = new HashMap();
        this.iconsFolder = iconsFolder;

        populate();

    }

    private void populate()
    {
        hm.put( ".txt", iconsFolder + "unknown.gif" );

        hm.put( ".jpg", iconsFolder + "image2.gif" );
        hm.put( ".jpeg", iconsFolder + "image2.gif" );
        hm.put( ".bmp", iconsFolder + "image2.gif" );
        hm.put( ".tiff", iconsFolder + "image2.gif" );
        hm.put( ".png", iconsFolder + "image2.gif" );

        hm.put( ".avi", iconsFolder + "movie.gif" );
        hm.put( ".mp4", iconsFolder + "movie.gif" );
        hm.put( ".ogv", iconsFolder + "movie.gif" );

        hm.put( ".mp3", iconsFolder + "sound2.gif" );
        hm.put( ".ogg", iconsFolder + "sound2.gif" );

        hm.put( ".dir", iconsFolder + "folder.gif" );
        hm.put( ".parent", iconsFolder + "back.gif" );
    }

    public synchronized String getIcon( String fileName )
    {
        int index;
        index= fileName.lastIndexOf('.');
        String extension = fileName.substring( index );

        if ( hm.containsKey( extension ) )
            return ( String ) hm.get( extension );
        else
            return iconsFolder + "unknown.gif";
    }
}
