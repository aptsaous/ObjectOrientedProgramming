package gr.inf.uth.ce325.file_browser;


import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class FileBrowserGUI extends JFrame implements TreeSelectionListener, ActionListener
{
    private JPanel panel;
    private JTree tree;
    private JScrollPane scrollTree;
    private JScrollPane scrollFilePanel;
    private DefaultMutableTreeNode root;
    private DefaultMutableTreeNode currentNode;
    private int filesOrFolders;
    private JButton prevButton;
    private JCheckBoxMenuItem asIcons;
    private  JCheckBoxMenuItem asList;
    private JSplitPane splitPane;
    private FileNode currentFolder;
    private File selectedFile;
    private JLabel searchProgress;
    private String searchFileName;
    private Pattern pattern;
    private DefaultListModel listModel;
    private JList list;
    private String selectedFileName;
    private String selectedFolderName;
    private String deletedFolder;
    private boolean listIsEnabled = false;
    private int foundFiles;

    final int WIDTH = 1000;
    final int HEIGHT = 700;

    public FileBrowserGUI()
    {
        super( "File Browser" );

        setSize( WIDTH, HEIGHT );
        setLayout( new BorderLayout() );

        setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

        createMainMenu();
        createSearchMenu();
        createTree();
        setTreeIcons();
        createFilePanel();
        createSplitPane();

        String path = System.getProperty( "user.home" );

        currentFolder = new FileNode( new File( path ) );
        currentNode = new DefaultMutableTreeNode( currentFolder );

        setPath( path ); // triggers tree event


        scrollFilePanel.addComponentListener( new ComponentListener() {

            public void componentResized( ComponentEvent e )
            {
                //int height = e.getComponent().getHeight();
                int width = e.getComponent().getWidth();
                //width -= 150;
                int columns = width / 160;
                int rows = filesOrFolders / columns;
                rows++;

                scrollFilePanel.setPreferredSize( new Dimension( columns*160, rows*160 ) );
                panel.setPreferredSize( new Dimension( columns*160, rows*160 ) );

                //System.out.println("Arxeia: " + filesOrFolders );
                //System.out.println( "Width: " + width + " Height: " + height );
                //System.out.println( "Columns: " + columns + " Rows: " + rows );
            }

            @Override
            public void componentMoved( ComponentEvent e ) {}

            @Override
            public void componentHidden( ComponentEvent e ) {}

            @Override
            public void componentShown( ComponentEvent e ) {}

        });

        addComponentListener( new ComponentListener() {

            public void componentResized( ComponentEvent e )
            {
                //int height = e.getComponent().getHeight();
                int width = e.getComponent().getWidth();
                width -= 150;
                int columns = width / 160;
                int rows = filesOrFolders / columns;
                rows++;

                scrollFilePanel.setPreferredSize( new Dimension( columns * 160, rows * 160 ) );
                //System.out.println("Arxeia: " + filesOrFolders);
                //System.out.println("Width: " + width + " Height: " + height);
                //System.out.println("Columns: " + columns + " Rows: " + rows);
            }

            @Override
            public void componentMoved( ComponentEvent e ) {}

            @Override
            public void componentHidden( ComponentEvent e ) {}

            @Override
            public void componentShown( ComponentEvent e ) {}

        });


    }

    /*******************************************/
    /*                UI Functions             */
    /*******************************************/

    private void createMainMenu()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu appMenu = new JMenu( "<html><b>File Browser</b></html>" );
        JMenu fileMenu = new JMenu( "File" );
        JMenu editMenu = new JMenu( "Edit" );
        JMenu viewMenu = new JMenu( "View" );

        JMenuItem about = new JMenuItem( "About" );
        JMenuItem exit = new JMenuItem( "Exit" );

        JMenuItem createNewFile = new JMenuItem( "New file" );
        JMenuItem createNewFolder = new JMenuItem( "New folder" );

        JMenuItem renameFile = new JMenuItem( "Rename file" );
        JMenuItem renameFolder = new JMenuItem( "Rename folder" );
        JMenuItem deleteFile = new JMenuItem( "Delete file" );
        JMenuItem deleteFolder = new JMenuItem( "Delete folder" );

        asIcons = new JCheckBoxMenuItem( "As Icons" );
        asList = new JCheckBoxMenuItem( "As List" );

        appMenu.add( about );
        appMenu.add( new JSeparator() );
        appMenu.add( exit );

        fileMenu.add( createNewFile );
        fileMenu.add( createNewFolder );

        editMenu.add( renameFile );
        editMenu.add( renameFolder );
        editMenu.add( new JSeparator() );
        editMenu.add( deleteFile );
        editMenu.add( deleteFolder );

        viewMenu.add( asIcons );
        viewMenu.add( asList );

        menuBar.add( appMenu );
        menuBar.add( fileMenu );
        menuBar.add( editMenu );
        menuBar.add( viewMenu );

        setJMenuBar( menuBar );

        asIcons.setState( true );
        asList.setState( false );

        about.addActionListener( this );
        exit.addActionListener( this );

        createNewFile.addActionListener( this );
        createNewFolder.addActionListener( this );

        renameFile.addActionListener( this );
        renameFolder.addActionListener( this );
        deleteFile.addActionListener( this );
        deleteFolder.addActionListener( this );

        asIcons.addActionListener( this );
        asList.addActionListener( this );

    }

    private void createSearchMenu()
    {
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout( new BorderLayout() );

        JLabel searchLabel = new JLabel();

        ImageIcon searchIcon = createImageIcon( "/icons/search.png" );
        Image searchImage = searchIcon.getImage().getScaledInstance( 15, 15, Image.SCALE_SMOOTH );
        searchIcon = new ImageIcon( searchImage );
        searchLabel.setIcon( searchIcon );

        JTextField searchField = new JTextField( "Search", 20 );
        searchField.setHorizontalAlignment( JTextField.CENTER );
        searchField.setLayout( new BorderLayout() );

        searchField.add( searchLabel, BorderLayout.WEST );

        searchPanel.add( searchField, BorderLayout.EAST );
        add( searchPanel, BorderLayout.NORTH );

        searchField.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e )
            {
                listIsEnabled = true;

                searchFileName = e.getActionCommand();

                pattern = null;
                foundFiles = 0;

                panel.removeAll();
                panel.setLayout( new BorderLayout() );
                searchProgress = new JLabel();
                panel.add( searchProgress, BorderLayout.NORTH );
                searchProgress.setText( "Searching... " );
                panel.revalidate();
                panel.repaint();
                panel.updateUI();

                if ( list == null )
                {
                    listModel = new DefaultListModel();
                    list = new JList( listModel );
                    list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
                    list.setCellRenderer(new IconListRenderer());

                    list.addMouseListener( new MouseAdapter() {

                        @Override
                        public void mouseClicked( MouseEvent e )
                        {
                            super.mouseClicked(e);

                            JList list = ( JList ) e.getSource();

                            if ( e.getClickCount() == 2 )
                            {
                                int index = list.locationToIndex( e.getPoint() );

                                File file = ( File ) listModel.getElementAt( index );

                                if ( file == null )
                                    return;

                                if ( file.isFile() )
                                    executeFile( file.getPath() );
                                else
                                    setPath( file.getPath() );

                                //System.out.println( file.getPath() );
                            }
                        }
                    });

                    /*list.addListSelectionListener( new ListSelectionListener() {

                        @Override
                        public void valueChanged( ListSelectionEvent e )
                        {
                            if ( !e.getValueIsAdjusting() )
                            {
                                File file = ( File ) list.getSelectedValue();

                                if ( file == null )
                                    return;

                                if ( file.isFile() )
                                    executeFile( file.getPath() );
                                else
                                    setPath( file.getPath() );

                            }
                        }
                    });*/
                }

                panel.setPreferredSize( new Dimension( 100, 10000 ) );
                panel.add( list );
                
                if ( listModel != null )
                    listModel.removeAllElements();


                Runnable runnable = new Runnable() {

                    @Override
                    public void run()
                    {
                        if ( isRegex( searchFileName ) )
                        {
                            pattern = Pattern.compile( searchFileName );
                            searchFile( currentFolder.getFile(), searchFileName, true, pattern );
                        }
                        else
                            searchFile( currentFolder.getFile(), searchFileName, false, pattern );


                        searchProgress.setText( "Search completed.. " + foundFiles + " found" );
                    }
                };

                Thread thread = new Thread( runnable );
                thread.start();

            }
        });
    }

    private class IconListRenderer extends DefaultListCellRenderer
    {
        public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
        {
            JLabel label = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
            File file = ( File ) value;
            ImageIcon icon;
            Image searchImage;

            if ( file.isDirectory() )
            {
                icon = createImageIcon( "/icons/folder.png" );
                searchImage = icon.getImage().getScaledInstance( 20, 20, Image.SCALE_SMOOTH );
                icon = new ImageIcon( searchImage );
            }
            else
            {
                icon = createImageIcon( getImagePath( file.getName() ) );
                searchImage = icon.getImage().getScaledInstance( 20, 20, Image.SCALE_SMOOTH );
                icon = new ImageIcon( searchImage );
            }

            label.setIcon( icon );
            label.setText( file.getPath() );
            label.setHorizontalTextPosition( RIGHT );

            return label;
        }

    }

    private void createTree()
    {
        File rootFile = new File( getRootDir() );
        FileNode rootNode = new FileNode( rootFile );

        root = new DefaultMutableTreeNode( rootNode );

        expandNode( false, 2, root );

        tree = new JTree( root );
        scrollTree = new JScrollPane( tree );

        scrollTree.setMinimumSize( new Dimension( 150, 1000 ) );
        scrollTree.setMaximumSize( new Dimension( 300, 1000 ) );

        tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

        tree.addTreeSelectionListener( this );

    }

    private void createFilePanel()
    {
        panel = new JPanel();
        panel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
        //panel.setPreferredSize(new Dimension(100, 5000));

        scrollFilePanel = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        //scrollFilePanel.setPreferredSize(new Dimension(700, 1000));

        panel.addMouseListener( new MouseAdapter() {

            @Override
            public void mousePressed( MouseEvent e )
            {
                super.mousePressed( e );

                JPopupMenu menu = new JPopupMenu();
                JMenuItem newFileRC = new JMenuItem( "New file" );
                JMenuItem newFolderRC = new JMenuItem( "New folder" );

                menu.add( newFolderRC );
                menu.add( newFileRC );

                if ( e.isPopupTrigger() )
                    menu.show( e.getComponent(), e.getX(), e.getY() );

                newFileRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        newFile();
                    }
                });
                
                newFolderRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        newFolder();
                    }
                });

            }

            @Override
            public void mouseReleased( MouseEvent e )
            {
                super.mouseReleased(e);

                JPopupMenu menu = new JPopupMenu();
                JMenuItem newFileRC = new JMenuItem( "New file" );
                JMenuItem newFolderRC = new JMenuItem( "New folder" );

                menu.add( newFolderRC );
                menu.add( newFileRC );

                if ( e.isPopupTrigger() )
                    menu.show( e.getComponent(), e.getX(), e.getY() );

                newFileRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        newFile();
                    }
                });

                newFolderRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        newFolder();
                    }
                });

            }
        });
    }

    private void createSplitPane()
    {
        splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, scrollTree, scrollFilePanel );

        splitPane.setOneTouchExpandable( false );
        splitPane.setDividerLocation( 150 );

        //splitPane.setEnabled(false);
        add( splitPane, BorderLayout.CENTER );
    }

    private void createButtons()
    {
        File[] files = currentFolder.getFile().listFiles();

        if ( files == null )
            return;

        filesOrFolders = files.length;

        int columns;

        if ( scrollFilePanel.getWidth() == 0 )
            columns = ( getWidth() - 150 ) / 160;
        else
            columns = scrollFilePanel.getWidth() / 160;

        int rows = filesOrFolders / columns;

        rows++;

        panel.setPreferredSize( new Dimension( columns * 160, rows * 160 ) );
        scrollFilePanel.setPreferredSize( new Dimension( columns * 160, rows * 160 ) );

        Arrays.sort( files );

        String iconPath = "/icons/folder.png";

        for ( File file : files )
        {
            if (  file.isHidden() || file.isFile() )
                continue;

            createButton( file, iconPath );
        }

        for ( File file : files )
        {
            if ( file.isHidden() || file.isDirectory() )
                continue;

            iconPath = getImagePath( file.getName() );

            createButton( file, iconPath );

        }

        panel.revalidate();
        panel.repaint();
        panel.updateUI();


    }

    private void createButton( File file, String iconPath )
    {
        ImageIcon searchIcon = createImageIcon( iconPath );
        Image searchImage = searchIcon.getImage().getScaledInstance( 90, 90, Image.SCALE_SMOOTH );
        searchIcon = new ImageIcon( searchImage );

        JButton button = new JButton( file.getName(), searchIcon );

        button.setPreferredSize( new Dimension( 150, 150 ) );
        button.setMinimumSize( new Dimension( 150, 150 ) );
        button.setMaximumSize( new Dimension( 150, 150 ) );

        button.setSelected( true );
        button.requestFocus();
        button.setOpaque( false );
        button.setContentAreaFilled( false );
        button.setBorderPainted( false );
        button.setMargin( new Insets( 0, 0, 0, 0 ) );

        String fileName = "";
        String name = file.getName();

        for ( int i = 0; i < name.length(); i++ )
        {
            if ( ( ( i % 15 ) == 0 ) && ( i >= 15 ) )
                fileName += "<br>";

            fileName += name.charAt(i);

        }

        button.setText("<html><center>" + fileName + "</center></html>");
        button.setIconTextGap(0);
        button.setVerticalTextPosition( SwingConstants.BOTTOM );
        button.setHorizontalTextPosition( SwingConstants.CENTER );

        panel.add( button );

        button.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked( MouseEvent e )
            {
                JButton jButton = ( JButton ) e.getComponent();
                String filename = jButton.getText();

                filename = filename.replaceAll( "(<html><center>)|(</center></html>)|(<br>)", "" );

                if ( e.getClickCount() == 1 )
                {
                    if ( prevButton != null )
                        prevButton.setBorderPainted( false );

                    jButton.setBorderPainted( true );

                    File file = new File( currentFolder.getFile().getPath() + "/" + filename );

                    if ( file.isFile() )
                        selectedFileName = filename;
                    else
                        selectedFolderName = filename;


                    prevButton = jButton;

                    panel.revalidate();
                    panel.repaint();
                    panel.updateUI();

                }
                else if ( e.getClickCount() == 2 )
                {
                    String path = currentFolder.getFile().getPath() + "/" + filename;

                    File file = new File( path );

                    if ( file.isFile() )
                    {
                        executeFile( path );
                        return;
                    }

                    setPath( path );
                    panel.removeAll();
                    createButtons();

                }

            }

            public void mousePressed( MouseEvent e )
            {
                JButton jButton = ( JButton ) e.getComponent();
                String filename = jButton.getText();
                
                
                filename = filename.replaceAll( "(<html><center>)|(</center></html>)|(<br>)", "" );

                selectedFile = new File( currentFolder.getFile().getPath() + "/" + filename );

                JPopupMenu menu = new JPopupMenu();
                JMenuItem renameRC = new JMenuItem( "Rename" );
                JMenuItem deleteRC = new JMenuItem( "Delete" );
                
                menu.add( renameRC );
                menu.add( new JSeparator() );
                menu.add( deleteRC );
                

                if ( selectedFile.isFile() )
                    selectedFileName = filename;
                else
                    selectedFolderName = filename;

                if ( e.isPopupTrigger() )
                    menu.show( e.getComponent(), e.getX(), e.getY() );

                renameRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        if ( selectedFile.isFile() )
                            renameFile();
                        else
                            renameFolder();
                    }
                });

                deleteRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        if ( selectedFile.isFile() )
                            deleteFile();
                        else
                            deleteDirectory();
                    }
                });
                
                panel.revalidate();
                panel.repaint();
                panel.updateUI();

            }

            @Override
            public void mouseReleased( MouseEvent e )
            {
                super.mouseReleased(e);

                JButton jButton = ( JButton ) e.getComponent();
                String filename = jButton.getText();


                filename = filename.replaceAll( "(<html><center>)|(</center></html>)|(<br>)", "" );

                selectedFile = new File( currentFolder.getFile().getPath() + "/" + filename );

                JPopupMenu menu = new JPopupMenu();
                JMenuItem renameRC = new JMenuItem( "Rename" );
                JMenuItem deleteRC = new JMenuItem( "Delete" );

                menu.add( renameRC );
                menu.add( new JSeparator() );
                menu.add( deleteRC );


                if ( selectedFile.isFile() )
                    selectedFileName = filename;
                else
                    selectedFolderName = filename;

                if ( e.isPopupTrigger() )
                    menu.show( e.getComponent(), e.getX(), e.getY() );

                renameRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        if ( selectedFile.isFile() )
                            renameFile();
                        else
                            renameFolder();
                    }
                });

                deleteRC.addActionListener( new ActionListener() {

                    @Override
                    public void actionPerformed( ActionEvent e )
                    {
                        if ( selectedFile.isFile() )
                            deleteFile();
                        else
                            deleteDirectory();
                    }
                });

                panel.revalidate();
                panel.repaint();
                panel.updateUI();


            }
        });
    }
       
    public void createTable( FileNode node )
    {

        String[] columnNames = { "Icon", "File Name", "Type", "Size", "Date Modified" };

        File[] fileList = node.getFile().listFiles();

        if ( fileList == null )
            return;

        int files = fileList.length;

        JTable table = new JTable()
        {
            public Class getColumnClass( int column )
            {

                if ( column == 0 )
                    return Icon.class;

                return String.class;
            }
        };

        JTableHeader header = table.getTableHeader();
        panel.add( header, BorderLayout.NORTH );
        table.setShowGrid( false );

        DefaultTableModel dtm = new DefaultTableModel() {
            @Override
            public boolean isCellEditable( int row, int column )
            {
                return false;
            }
        };

        dtm.setColumnIdentifiers( columnNames );
        table.setModel( dtm );
        table.setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );

        for ( File file : fileList )
        {
            if ( file.isHidden() )
                continue;

            Icon icon;

            if ( System.getProperty("os.name").contains("Mac") )
            {
                final JFileChooser fc = new JFileChooser();
                icon = fc.getUI().getFileView(fc).getIcon(file);
            }
            else
                icon = FileSystemView.getFileSystemView().getSystemIcon(file);

            Object[] dataColumns = { icon, file.getName(), getFileType( file ), getFileSize( file ), new Date( file.lastModified() ) };
            dtm.addRow(dataColumns);

        }

        table.getColumnModel().getColumn(0).setMaxWidth(35);
        Dimension d = table.getPreferredSize();
        panel.setPreferredSize( new Dimension( d.width, ( table.getRowHeight() * files + 1 ) ) );
        //table.setPreferredScrollableViewportSize(table.getPreferredSize());
        scrollFilePanel.getVerticalScrollBar().setValue(0);

        panel.add( table,BorderLayout.CENTER );

        table.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked( MouseEvent e )
            {
                super.mouseClicked(e);

                if ( e.getClickCount() == 2 )
                {
                    JTable tbl = ( JTable ) e.getSource();
                    int row = tbl.getSelectedRow();

                    String fileName = ( String ) tbl.getModel().getValueAt( row, 1 );

                    File file = new File( currentFolder.getFile().getPath() + "/" + fileName );

                    if ( file.isFile() )
                        executeFile( file.getPath() );
                    else
                        setPath( file.getPath() );

                    //System.out.println( file.getPath() );
                }

            }
        });

        panel.revalidate();
        panel.repaint();
        panel.updateUI();

    }

    private String getFileType( File file )
    {
        if ( file.isDirectory() )
            return "Folder";

        String ext;

        int index = file.getName().lastIndexOf(".");

        if ( index == -1 )
            return "Unknown"; // unknown type, has no extension

        ext = file.getName().substring(index);

        return ext.substring(1).toUpperCase();
    }

    /*******************************************/
    /*          Listener Functions             */
    /*******************************************/

    public void actionPerformed( ActionEvent e )
    {
        String command = e.getActionCommand();

        if ( command.equals( "About" ) )
        {
            JOptionPane.showMessageDialog( null, "CE325 File Browser\nCoded by: Konstantinos Theodosiou & Apostolos Tsaousis", "About", JOptionPane.INFORMATION_MESSAGE );
        }
        else if ( command.equals( "Exit" ) )
        {
            System.exit( 0 );
        }
        else if ( command.equals( "As List" ) )
        {
            if ( asList.getState() )
            {
                asIcons.setState( false );
                panel.removeAll();
                panel.setLayout( new BorderLayout() );
                //scrollFilePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

                createTable( ( FileNode ) currentNode.getUserObject() );
            }
            else
                asList.setState( true );

        }
        else if ( command.equals( "As Icons" ) )
        {
            if( asIcons.getState() )
            {
                asList.setState( false );
                panel.removeAll();
                panel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
                scrollFilePanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );

                createButtons();
            }
            else
                asIcons.setState( true );

        }
        else if ( command.equals( "New file" ) )
        {
            newFile();
        }
        else if ( command.equals( "New folder" ) )
        {
            newFolder();
        }
        else if ( command.equals( "Rename file" ) )
        {
            renameFile();
        }
        else if ( command.equals( "Rename folder" ) )
        {
            renameFolder();
        }
        else if ( command.equals( "Delete file" ) )
        {
            deleteFile();
        }
        else if ( command.equals( "Delete folder" ) )
        {
            deleteDirectory();
        }

    }

    public void valueChanged( TreeSelectionEvent e )
    {
        panel.removeAll();
           
        if ( listIsEnabled )
        {
            panel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
            listIsEnabled = false;
        }

        currentNode = ( DefaultMutableTreeNode ) e.getPath().getLastPathComponent();
        currentFolder = ( FileNode ) currentNode.getUserObject();

        FileNode fileNode = (FileNode) currentNode.getUserObject();

        //TreePath treePath = new TreePath( currentNode );

        //if ( tree.isExpanded( treePath ) )
          //  return;

        if ( asIcons.getState() )
            createButtons();
        else
            createTable( fileNode );

        expandNode( false, 2, currentNode );

        this.setTitle( currentFolder.getFile().getName() );
        this.revalidate();
        this.repaint();
    }

    /*******************************************/
    /*         Basic Utility Functions         */
    /*******************************************/

    private void setPath( String path )
    {
        String[] dirNames;
        String rootDir = getRootDir();

        if ( rootDir.equals( "/") )
        {
            path = path.substring( 1 );

            dirNames = path.split( "/" );
        }
        else
        {
            path = path.substring( 3 );

            dirNames = path.split( "\\\\|/" );
        }

        TreePath tPath = new TreePath( root );

        DefaultMutableTreeNode tNode = root;

        for ( String dirName : dirNames )
        {
            DefaultMutableTreeNode child = matchChildNode( tNode, dirName );
            FileNode childFileNode = ( FileNode ) child.getUserObject();

            String currentPath = childFileNode.getFile().getPath() + "/" + selectedFolderName;

            if ( currentPath.equals( deletedFolder ) )
            {
                DefaultMutableTreeNode childNode;
                String childNodeName;

                for ( int i = 0; i < child.getChildCount(); i++ )
                {
                    childNode = ( DefaultMutableTreeNode ) child.getChildAt( i );
                    FileNode cfn = ( FileNode ) childNode.getUserObject();

                    childNodeName = cfn.getFile().getName();

                    if ( childNodeName.equals( selectedFolderName ) )
                    {
                        child.remove( childNode );
                        break;
                    }
                }

            }

            if( child == null )
                return;

            tPath = tPath.pathByAddingChild( child );
            tNode = child;
        }

        tree.expandPath( tPath );
        tree.scrollPathToVisible( tPath );
        tree.setSelectionPath( tPath );
        tree.setExpandsSelectedPaths( true );


    }

    private DefaultMutableTreeNode matchChildNode( DefaultMutableTreeNode node, String childName )
    {
        FileNode fileNode =  ( FileNode ) node.getUserObject();

        if ( !fileNode.isExpanded() )
        {
            node.removeAllChildren();
            expandNode( false, 1, node );
        }

        for( int i = 0; i < node.getChildCount(); i++ )
        {

            DefaultMutableTreeNode childNode = ( DefaultMutableTreeNode ) node.getChildAt( i );
            FileNode childFileNode = ( FileNode ) childNode.getUserObject();

            String childNodeName = childFileNode.getFile().getName();

            if ( childNodeName.equals( childName ) )
                return childNode;

        }

        return null;
    }

    private void expandNode( boolean showHidden, int depth, DefaultMutableTreeNode node )
    {
        FileNode fileNode = ( FileNode ) node.getUserObject();

        if ( fileNode.isExpanded() )
            return;

        File dir = fileNode.getFile();
        File[] files = dir.listFiles();

        if ( files == null )
            return;

        Arrays.sort( files );

        for ( File file : files )
        {
            if ( !file.isDirectory() || ( !showHidden && file.isHidden() ) )
                continue;

            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode( new FileNode( file ) );
            node.add( newNode );

            if ( depth > 0 )
                expandNode( showHidden, depth - 1, newNode );


        }

        fileNode.setExpanded( true );


    }

    private void searchFile( File folder, String fileName, boolean isRegex, Pattern pattern )
    {
        File[] files = folder.listFiles();

        if ( files == null )
            return;

        for ( File file : files )
        {
            final File f = file;

            if ( file.isHidden() )
                continue;

            if ( isRegex )
            {
                Matcher fileMatcher = pattern.matcher( file.getName() );

                if ( fileMatcher.find() )
                {

                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run()
                        {
                            foundFiles++;
                            searchProgress.setText( "Searching... " + foundFiles + " found" );
                            listModel.addElement( f );
                        }
                    });
                }

            }
            else
            {
                if ( file.getName().contains( fileName ) )
                {
                    foundFiles++;
                    searchProgress.setText( "Searching... " + foundFiles + " found");

                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run()
                        {
                            listModel.addElement( f );
                        }
                    });
                }
            }

            if ( file.isDirectory() )
                searchFile( file, fileName, isRegex, pattern );
        }

    }

    private void deleteDirectory()
    {
        if ( selectedFolderName == null )
        {
            JOptionPane.showMessageDialog( this, "No folder selected", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        File folderToDelete = new File( currentFolder.getFile().getPath() + "/" + selectedFolderName );

        int answer = JOptionPane.showConfirmDialog( null, "Are you sure you want to delete " + selectedFolderName, "Confirm Deletion", JOptionPane.YES_NO_OPTION );

        if ( answer == JOptionPane.NO_OPTION )
            return;

        if ( !isFolderDeleted( folderToDelete ) )
        {
            JOptionPane.showMessageDialog( this, "Error while deleting folder", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        currentFolder.setExpanded( false );

        String path = currentFolder.getFile().getPath();

        panel.removeAll();
        createButtons();

        deletedFolder = folderToDelete.getPath();
        setPath( path );
        selectedFolderName = null;

        tree.revalidate();
        tree.repaint();
        tree.updateUI();

        deletedFolder = null;

    }

    private void deleteFile()
    {
        if ( selectedFileName == null )
        {
            JOptionPane.showMessageDialog( this, "No file selected", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        File fileToDelete = new File( currentFolder.getFile().getPath() + "/" + selectedFileName );

        int answer = JOptionPane.showConfirmDialog( null, "Are you sure you want to delete " + selectedFileName, "Confirm Deletion", JOptionPane.YES_NO_OPTION );

        if ( answer == JOptionPane.NO_OPTION )
            return;

        if ( !fileToDelete.delete() )
        {
            JOptionPane.showMessageDialog( this, "Error while deleting file", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        selectedFileName = null;

        panel.removeAll();
        createButtons();

    }

    private boolean isFolderDeleted( File folder )
    {
        boolean isDeleted;

        if ( !folder.exists() )
            return true;

        if ( folder.isDirectory() )
        {
            File[] files = folder.listFiles();

            if ( files == null )
                return false;

            for ( File file : files )
            {
                isDeleted = isFolderDeleted( file );

                if ( !isDeleted )
                    return false;
            }
        }

        return folder.delete();
    }

    private void renameFolder()
    {
        if ( selectedFolderName == null )
        {
            JOptionPane.showMessageDialog( this, "No folder selected", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        File oldFolder = new File( currentFolder.getFile().getPath() + "/" + selectedFolderName );
        String folderName = JOptionPane.showInputDialog( this, "Enter new folder name: ", oldFolder.getName() );

        if ( folderName == null )
            return;

        File newFolder = new File( currentFolder.getFile().getPath() + "/" + folderName );

        if ( newFolder.exists() )
        {
            JOptionPane.showMessageDialog( this, "This folder already exists", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        if ( !oldFolder.renameTo( newFolder ) )
        {
            JOptionPane.showMessageDialog( this, "Error while renaming folder", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        selectedFolderName = null;

        currentFolder.setExpanded(false);

        String path = currentFolder.getFile().getPath();

        panel.removeAll();
        createButtons();

        setPath( path + "/" + folderName );
        setPath( path );

        tree.revalidate();
        tree.repaint();
        tree.updateUI();
    }

    private void renameFile()
    {
        if ( selectedFileName == null )
        {
            JOptionPane.showMessageDialog( this, "No file selected", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        File oldFile = new File(currentFolder.getFile().getPath() + "/" + selectedFileName);

        String fileName = JOptionPane.showInputDialog( this, "Enter new file name: ", oldFile.getName() );

        if ( fileName == null )
            return;

        File newFile = new File( currentFolder.getFile().getPath() + "/" + fileName );

        if ( newFile.exists() )
        {
            JOptionPane.showMessageDialog( this, "This file already exists", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        if ( !oldFile.renameTo( newFile ) )
        {
            JOptionPane.showMessageDialog( this, "Error while renaming file", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
            return;
        }

        selectedFileName = null;

        panel.removeAll();
        createButtons();
    }

    private void newFile()
    {
        String fileName = JOptionPane.showInputDialog( this, "Enter file name: ", "New File.txt" );

        try
        {
            File newFile = new File( currentFolder.getFile().getPath() + "/" + fileName );

            if ( fileName == null )
                return;

            if ( newFile.createNewFile() )
            {
                panel.removeAll();
                createButtons();
            }
            else
                JOptionPane.showMessageDialog( this, "This file already exists", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );

        }
        catch ( IOException ex )
        {
            JOptionPane.showMessageDialog( this, ex, "Warning!!!", JOptionPane.ERROR_MESSAGE );
        }

    }

    private void newFolder()
    {
        String folderName = JOptionPane.showInputDialog( this, "Enter folder name: ", "untitled folder" );
        File folder = new File( currentFolder.getFile().getPath() + "/" + folderName );

        if ( folderName == null )
            return;

        if ( !folder.mkdir() )
            JOptionPane.showMessageDialog( this, "Error when creating folder", "Warning!!!", JOptionPane.INFORMATION_MESSAGE );
        else
        {
            currentFolder.setExpanded( false );

            String path = currentFolder.getFile().getPath();

            panel.removeAll();
            createButtons();

            setPath( path + "/" + folderName );
            setPath( path );

            tree.revalidate();
            tree.repaint();
            tree.updateUI();
        }

    }

    private void executeFile ( String path )
    {
        if ( System.getProperty("os.name").contains("Windows") || System.getProperty("os.name").contains("Mac") )
        {
            try
            {
                Desktop.getDesktop().open( new File( path ) );
            }
            catch ( IOException ex )
            {
                System.err.println( ex.toString() );
            }
        }
        else
        {
            String ext;
            String execPath;

            int index = path.lastIndexOf( "." );

            if ( index == -1 )
                execPath = "/usr/bin/kate"; // unknown type, has no extension
            else
            {
                ext = path.substring( index );
                execPath = FileBrowser.e.getProperty( ext.toLowerCase() );

                if ( execPath == null )
                    execPath = "/usr/bin/kate"; // unknown type
            }

            try
            {
                ProcessBuilder processBuilder = new ProcessBuilder( execPath, path );
                processBuilder.start();
            }
            catch ( IOException ex )
            {
                System.err.println( ex.toString() );
            }
        }

    }

    /*******************************************/
    /*        Helper Utility Functions         */
    /*******************************************/

    private boolean isRegex( String fileName )
    {
        Pattern pattern = null;

        try
        {
            pattern = Pattern.compile( fileName );
            return true;
        }
        catch ( PatternSyntaxException ex )
        {
            return false;
        }
    }

    private void setTreeIcons()
    {
        DefaultTreeCellRenderer tRenderer = new DefaultTreeCellRenderer();

        ImageIcon folderIcon = createImageIcon( "/icons/treeFolder.png" );
        Image icon = folderIcon.getImage();
        icon = icon.getScaledInstance( 24, 20, Image.SCALE_SMOOTH );
        folderIcon = new ImageIcon( icon );

        tRenderer.setLeafIcon( folderIcon );
        tRenderer.setClosedIcon( folderIcon );
        tRenderer.setOpenIcon( folderIcon );
        tRenderer.setTextSelectionColor( Color.LIGHT_GRAY );

        tree.setCellRenderer( tRenderer );
    }

    private ImageIcon createImageIcon( String path )
    {
        java.net.URL imgURL = FileBrowser.class.getResource( path );

        if ( imgURL != null )
            return new ImageIcon( imgURL );
        else
        {
            System.err.println( "Couldn't find file: " + path );
            return null;
        }
    }

    private String getFileSize( File file )
    {
        double bytes = file.length();
        double kilobytes = ( bytes / 1024 );
        double megabytes = ( kilobytes / 1024 );
        double gigabytes = ( megabytes / 1024 );

        if ( gigabytes >= 1 )
            return String.format( "%.1f GB", gigabytes );
        else if ( megabytes >= 1 )
            return String.format( "%.1f MB", megabytes );
        else if ( kilobytes >= 1 )
            return String.format( "%.1f KB", kilobytes );
        else
            return String.format( "%.1f B", bytes );
    }

    static String getRootDir()
    {
        if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
            return "C:\\";
        else
            return "/";
    }

    private String getImagePath( String fileName )
    {

        String ext;

        int index = fileName.lastIndexOf( "." );

        if ( index == -1 )
            return "/icons/default.png"; // unknown type, has no extension

        ext = fileName.substring( index );

        String iconPath = FileBrowser.p.getProperty( ext.toLowerCase() );

        if ( iconPath == null )
            return "/icons/default.png"; // unknown type
        else
            return iconPath;
    }

}