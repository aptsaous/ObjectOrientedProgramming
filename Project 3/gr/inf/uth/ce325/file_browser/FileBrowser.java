package gr.inf.uth.ce325.file_browser;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FileBrowser
{
    static Properties p;
    static Properties e;
    
    public static void main( String[] args )
    {
        try
        {
            FileInputStream propFile = new FileInputStream( args[0] ); // args[0]
            FileInputStream execFile = new FileInputStream( args[1] ); // args[1]

            p = new Properties( System.getProperties() );
            p.load( propFile );
            
            e = new Properties( System.getProperties() );
            e.load( execFile );
        }
        catch ( IOException ex )
        {
            System.err.println( ex.toString() );
        }

        SwingUtilities.invokeLater( new Runnable() {

            @Override
            public void run()
            {
                FileBrowserGUI gui = new FileBrowserGUI();

                gui.setVisible( true );
            }
        });

    }
}
