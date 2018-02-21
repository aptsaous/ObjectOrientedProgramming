package gr.inf.uth.ce325.file_browser;

import java.io.File;

class FileNode
{
    private String fileName;
    private File file;
    private boolean expanded = false;

    public FileNode( File file )
    {
        this.file = file;
        fileName = file.getName();
    }

    public File getFile()
    {
        return file;
    }

    public boolean isExpanded()
    {
        return expanded;
    }

    public void setExpanded( boolean status )
    {
        expanded = status;
    }

    @Override
    public String toString()
    {
        if ( fileName.length() == 0 )
            return FileBrowserGUI.getRootDir();
        else
            return fileName;
    }

}