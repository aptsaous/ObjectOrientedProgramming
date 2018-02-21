package gr.uth.inf.ce325.web_server;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Logger
{
    private String logFile;
    private String errorLogFile;
    private FileWriter logStream;
    private FileWriter errorLogStream;

    public Logger( String logFile, String errorLogFile )
    {
        this.logFile = logFile;
        this.errorLogFile = errorLogFile;
    }

    public synchronized void log( String IP, Date connectionDate, String request, String statusCode, String userAgent ) throws IOException
    {
        try
        {
            logStream = new FileWriter( logFile, true );

            String info;

            info = String.format( "%s - [%s] - %s -> %s - \"%s\"\n", IP, connectionDate, request, statusCode, userAgent );
            logStream.write( info );
            logStream.flush();
        }
        finally
        {
            if ( logStream != null )
                logStream.close();

        }

    }

    public synchronized void errorLog( String IP, Date requestDate, String request, String exceptionStackTrace ) throws IOException
    {
        try
        {
            errorLogStream = new FileWriter( errorLogFile, true );

            String info;

            info = String.format( "1. %s\n2. %s\n3. %s\n4. %s\n\n", IP, requestDate, request, exceptionStackTrace );

            errorLogStream.write( info );
            errorLogStream.flush();
        }
        finally
        {
            if ( errorLogStream != null )
                errorLogStream.close();

        }
    }

}
