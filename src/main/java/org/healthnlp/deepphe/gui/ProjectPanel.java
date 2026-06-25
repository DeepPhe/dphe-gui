package org.healthnlp.deepphe.gui;

import org.apache.ctakes.gui.component.FileTableCellEditor;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.util.ParameterHandler;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javax.swing.JFileChooser.FILES_ONLY;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;


/**
 * @author SPF , chip-nlp
 * @since {6/22/2026}
 */
public class ProjectPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "ProjectPanel" );

   static private final String CWD = System.getProperty( "user.dir" );

   static private final String DEFAULT_PIPER_FILE = CWD + "/resources/pipeline/DeepPheDefault.piper";
   static private final String DEFAULT_TEXT_CORPUS = CWD + "/resources/examples/example_corpus";
   static private final String DEFAULT_OMOP_DB = CWD + "/resources/examples/example_omop/patient_demographics.json";
   static private final String DEFAULT_OUTPUT_DIR = CWD + "/resources/examples/example_output";

   static private final String EXAMPLE_PROJECT = "ExampleProject";
   static private final String PROJECTS_DIR = CWD + "/resources/projects/";

   static private final String PROJECT = "PROJECT";
   static private final String PIPER_FILE = "PIPER_FILE";
   static private final String TEXT_CORPUS = "TEXT_CORPUS";
   static private final String OMOP_DB = "OMOP_DB";
   static private final String OUTPUT_DIR = "OUTPUT_DIR";


   static private final String PROJECT_NAME = "Project:";

   static private final String _projectListPath = PROJECTS_DIR + "ProjectList.txt";
   private final ArrayList<String> _projectList = new ArrayList<>();
   private final Map<String,String> _projectFileMap = new HashMap<>();
   private final Map<String,String> _projectMap = new HashMap<>();

   private final ProjectTableModel _tableModel = new ProjectTableModel();



   static private final String FONT = "SansSerif";

   public ProjectPanel() {
      super( new BorderLayout( 0, 5 ) );
      readProjectList();
      setBorder( new EmptyBorder( 5, 20, 5, 20 ) );
      final JPanel projectPanel = new JPanel( new BorderLayout( 10, 0 ) );
      final JLabel label = new JLabel( PROJECT_NAME );
      label.setFont( new Font( Font.DIALOG, Font.BOLD, 14 ) );
      projectPanel.add( label, BorderLayout.WEST );
      JComboBox<String> projectCombo = new JComboBox<>( new ProjectComboModel() );
      projectCombo.setEditable( true );
      projectCombo.setFont( new Font( FONT, Font.PLAIN, 14 ) );
      projectPanel.add( projectCombo, BorderLayout.CENTER );
      add( projectPanel, BorderLayout.NORTH );
      add( createProjectTable(), BorderLayout.CENTER );
      add( new JSeparator( SwingConstants.HORIZONTAL ), BorderLayout.SOUTH );
      registerShutdownHook();
   }

   public String getProjectName() {
      return _projectMap.computeIfAbsent( PROJECT, k -> EXAMPLE_PROJECT );
   }

   private void setProjectName( final String name ) {
      if ( !name.equals( getProjectName() ) ) {
         writeProjectFile();
      }
      _projectMap.put( PROJECT, name );
      _projectList.remove( name );
      _projectList.add( 0, name );
      _projectFileMap.computeIfAbsent( name, p -> PROJECTS_DIR + p + ".txt" );
      readProjectFile();
      _tableModel.reset();
   }

   private String getProjectFile() {
      return getProjectFile( getProjectName() );
   }

   private String getProjectFile( final String name ) {
      return _projectMap.computeIfAbsent( name, p -> PROJECTS_DIR + p + ".txt" );
   }


   public String getPiperFile() {
      return _projectMap.computeIfAbsent( PIPER_FILE, k -> DEFAULT_PIPER_FILE );
   }

   private void setPiperFile( final String path ) {
      if ( !path.toLowerCase().endsWith( ".piper" ) ) {
         setPiperFile( DEFAULT_PIPER_FILE );
         return;
      }
      final File piper = new File( path );
      if ( piper.isFile() && piper.canRead() ) {
         _projectMap.put( PIPER_FILE, path );
      } else {
         setPiperFile( DEFAULT_PIPER_FILE );
      }
   }

   public String getTextCorpus() {
      return _projectMap.computeIfAbsent( TEXT_CORPUS, k -> DEFAULT_TEXT_CORPUS );
   }

   private void setTextCorpus( final String path ) {
      final File dir = new File( path );
      if ( dir.isDirectory() ) {
         _projectMap.put( TEXT_CORPUS, path );
      } else {
         setTextCorpus( DEFAULT_TEXT_CORPUS);
      }
   }

   public String getOmopDb() {
      return _projectMap.computeIfAbsent( OMOP_DB, k -> DEFAULT_OMOP_DB );
   }

   private void setOmopDb( final String path ) {
      if ( !path.toLowerCase().endsWith( ".json" ) ) {
         setOmopDb( DEFAULT_OMOP_DB );
         return;
      }
      final File json = new File( path );
      if ( json.isFile() ) {
         _projectMap.put( OMOP_DB, path );
      } else {
         setOmopDb( DEFAULT_OMOP_DB );
      }
   }

   public String getOutputDir() {
      return _projectMap.computeIfAbsent(OUTPUT_DIR, k -> DEFAULT_OUTPUT_DIR );
   }

   private void setOutputDir( final String path ) {
      final File dir = new File( path );
      if ( dir.isDirectory() ) {
         _projectMap.put( OUTPUT_DIR, path );
      } else {
         setOutputDir( DEFAULT_OUTPUT_DIR );
      }
   }

   private JComponent createProjectTable() {
//      final JTable table = new SmoothTipTable( _tableModel ) {
      final JTable table = new JTable( _tableModel ) {
         @Override
         public String getToolTipText( final MouseEvent event) {
            final Point p = event.getPoint();
            return _tableModel.getToolTip( rowAtPoint( p ), columnAtPoint( p ) );
         }
         @Override
         public TableCellEditor getCellEditor( final int row, final int column ) {
            if ( column == 2 ) {
               return _tableModel.getCellEditor( row, column );
            }
            return super.getCellEditor( row, column );
         }
         public TableCellRenderer getCellRenderer( final int row, final int column ) {
            if ( column == 2 ) {
               return _tableModel.getCellEditor( row, column );
            }
            return super.getCellRenderer( row, column );
         }
      };
      table.setFont( new Font( FONT, Font.PLAIN, 14 ) );
      table.setBorder( new BevelBorder( BevelBorder.LOWERED ) );
      table.putClientProperty( "terminateEditOnFocusLost", true );
      table.setRowHeight( 20 );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel()
               .getColumn( 0 )
               .setPreferredWidth( 200 );
      table.getColumnModel()
               .getColumn( 0 )
               .setMaxWidth( 200 );
      table.getColumnModel()
               .getColumn( 2 )
               .setMaxWidth( 25 );
      table.setRowSelectionAllowed( true );
      table.setCellSelectionEnabled( true );
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      return table;
   }



   private class ProjectComboModel extends AbstractListModel<String> implements ComboBoxModel<String> {

      @Override
      public void setSelectedItem( final Object item ) {
         if ( item == null ) {
            setSelectedItem( getSelectedItem() );
            return;
         }
         final String project = item.toString().trim();
         if ( project.isEmpty() ) {
            setSelectedItem( getSelectedItem() );
            return;
         }
         if ( project.equals( getSelectedItem() ) ) {
            return;
         }
         setProjectName( project );
         fireContentsChanged(this, -1, -1);
      }

      @Override
      public Object getSelectedItem() {
         return getProjectName();
      }

      @Override
      public int getSize() {
         return _projectList.size();
      }

      @Override
      public String getElementAt( final int index ) {
         return _projectList.get( index );
      }
   }


   private final class ProjectTableModel implements TableModel {
      private final Consumer<String> setPiperFile1 = ProjectPanel.this::setPiperFile;
      private final Consumer<String> setTextCorpus1 = ProjectPanel.this::setTextCorpus;
      private final Consumer<String> setOmopDb1 = ProjectPanel.this::setOmopDb;
      private final Consumer<String> setOutputDir1 = ProjectPanel.this::setOutputDir;
      private final EventListenerList _listenerList = new EventListenerList();
      private final FileTableCellEditor _piperChooser = new FileTableCellEditor();
      private final FileTableCellEditor _corpusChooser = new FileTableCellEditor();
      private final FileTableCellEditor _jsonChooser = new FileTableCellEditor();
      private final FileTableCellEditor _outDirChooser = new FileTableCellEditor();
      private final String[] COLUMN_NAMES = { "Name", "Value", "Explorer" };
      private final Class<?>[] COLUMN_CLASSES = { String.class, String.class, File.class };
      private final String[] ROW_NAMES = { " Piper File", " Corpus Directory",
                                           " OMOP Database", " Output Directory" };
      private final String[] TOOLTIPS = {  "A Piper File defining a Pipeline.",
                                           "A directory containing patient directories filled with document files.",
                                           "A JSON file with demographics in the required OMOP format.",
                                           "A directory for output from the Phenotype Summarizer and Database Loader." };
      private final String[] TYPES = { "file", "directory", "file", "directory" };
      private final Supplier<String>[] GETTERS = new Supplier[]{ ProjectPanel.this::getPiperFile,
                                                                 ProjectPanel.this::getTextCorpus,
                                                                 ProjectPanel.this::getOmopDb,
                                                                 ProjectPanel.this::getOutputDir };
      private final Consumer<String>[] SETTERS = new Consumer[]{ setPiperFile1,
                                                         setTextCorpus1,
                                                         setOmopDb1,
                                                         setOutputDir1 };
//      private final FileTableCellEditor[] CHOOSERS = { _piperChooser, _corpusChooser, _jsonChooser, _outDirChooser };
      private final FileTableCellEditor[] CHOOSERS = { createChooser( FILES_ONLY, "Piper Files", "piper" ),
                                                       createChooser( DIRECTORIES_ONLY, null, null ),
                                                       createChooser( FILES_ONLY, "OMOP JSON", "json" ),
                                                       createChooser( DIRECTORIES_ONLY, null, null ) };

//      private ProjectTableModel() {
//         _corpusChooser.getFileChooser().setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
//         _outDirChooser.getFileChooser().setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
//         _piperChooser.getFileChooser().setFileSelectionMode( JFileChooser.FILES_ONLY );
//         _piperChooser.getFileChooser().setFileFilter(
//               new FileNameExtensionFilter( "Piper Files", "piper" ) );
//         _jsonChooser.getFileChooser().setFileSelectionMode( JFileChooser.FILES_ONLY );
//         _jsonChooser.getFileChooser().setFileFilter(
//               new FileNameExtensionFilter( "OMOP Demographics JSON", "json" ) );
//      }
      private FileTableCellEditor createChooser( final int mode, final String filterName, final String filterExt ) {
         final FileTableCellEditor chooser = new FileTableCellEditor();
         chooser.getFileChooser().setFileSelectionMode( mode );
         if ( filterName != null && filterExt != null ) {
            chooser.getFileChooser().setFileFilter( new FileNameExtensionFilter( filterName, filterExt ) );
         }
         return chooser;
      }
      private void reset() {
         fireTableChanged( new TableModelEvent( this ) );
      }
      private String getToolTip( final int row, final int column ) {
         switch ( column ) {
            case 0 : return TOOLTIPS[ row ];
            case 1 : return "Type or paste a " + TYPES[ row ] + " path, or drag and drop.";
            case 2 : return "Click to open a file explorer.";
         }
         return "";
      }
      private FileTableCellEditor getCellEditor( final int row, final int column ) {
//         FileTableCellEditor editor;
//         switch ( row ) {
//            case 0 : editor = _piperChooser; break;
//            case 1 : editor = _corpusChooser; break;
//            case 2 : editor = _jsonChooser; break;
//            case 3 : editor = _outDirChooser; break;
//            default: editor = _defaultChooser;
//         }
//         editor.getFileChooser().setSelectedFile( (File)getValueAt( row, column ) );
//         return editor;
         CHOOSERS[ row ].getFileChooser().setSelectedFile( (File)getValueAt( row, column ) );
         return CHOOSERS[ row ];
      }

      @Override
      public int getRowCount() {
         return ROW_NAMES.length;
      }
      @Override
      public int getColumnCount() {
         return COLUMN_NAMES.length;
      }
      @Override
      public String getColumnName( final int column ) {
         return COLUMN_NAMES[ column ];
      }
      @Override
      public Class<?> getColumnClass( final int column ) {
         return COLUMN_CLASSES[ column ];
      }
      @Override
      public Object getValueAt( final int row, final int column ) {
         if ( column == 0 ) {
            return ROW_NAMES[ row ];
         } else if ( column == 1 ) {
            return normalizePath( GETTERS[ row ].get() );
//            switch ( row ) {
//               case 0 : return normalizePath( getPiperFile() );
//               case 1 : return normalizePath( getTextCorpus() );
//               case 2 : return normalizePath( getOmopDb() );
//               case 3 : return normalizePath( getOutputDir() );
//            }
         } else if ( column == 2 ) {
            return new File( (String) getValueAt( row, 1 ) );
         }
         return "ERROR";
      }
      private String normalizePath( final String filepath ) {
         return Paths.get( filepath ).toAbsolutePath().normalize().toString();
      }
      @Override
      public boolean isCellEditable( final int row, final int column ) {
         return column != 0;
      }
      @Override
      public void setValueAt( final Object aValue, final int row, final int column ) {
         if ( column == 1 ) {
            final String text = aValue.toString().trim();
            SETTERS[ row ].accept( text );
//            switch ( row ) {
//               case 0 : setPiperFile( text );
//               case 1 : setTextCorpus( text );
//               case 2 : setOmopDb( text );
//               case 3 : setOutputDir( text );
//            }
            fireTableChanged( new TableModelEvent( this, row, row, column ) );
         } else if ( column == 2 && aValue instanceof File ) {
            final String filePath = ((File)aValue).getPath();
            SETTERS[ row ].accept( filePath );
//            switch ( row ) {
//               case 0 : setPiperFile( filePath );
//               case 1 : setTextCorpus( filePath );
//               case 2 : setOmopDb( filePath );
//               case 3 : setOutputDir( filePath );
//            }
            fireTableChanged( new TableModelEvent( this, row, row, 1 ) );
         }
      }
      @Override
      public void addTableModelListener( final TableModelListener listener ) {
         _listenerList.add( TableModelListener.class, listener );
      }
      @Override
      public void removeTableModelListener( final TableModelListener listener ) {
         _listenerList.remove( TableModelListener.class, listener );
      }
      private void fireTableChanged( final TableModelEvent event ) {
         // Guaranteed to return a non-null array
         Object[] listeners = _listenerList.getListenerList();
         // Process the listeners last to first, notifying
         // those that are interested in this event
         for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[ i ] == TableModelListener.class ) {
               ((TableModelListener)listeners[ i + 1 ]).tableChanged( event );
            }
         }
      }
   }

   private void readProjectList() {
      boolean listOk = ParameterHandler.readMapFromFile( _projectListPath, _projectFileMap, false );
      listOk = listOk && !_projectFileMap.isEmpty();
      if ( listOk ) {
         ParameterHandler.readKeysFromFile( _projectListPath, _projectList );
      } else {
         _projectList.add( EXAMPLE_PROJECT );
         _projectFileMap.put( EXAMPLE_PROJECT, PROJECTS_DIR + EXAMPLE_PROJECT + ".txt" );
      }
      setProjectName( getProjectName() );
   }

   private void writeProjectList() {
      LOGGER.info( "Writing project list to " + _projectListPath + " ..." );
      try ( Writer writer = new BufferedWriter( new FileWriter( _projectListPath ) ) ) {
         for ( String project : _projectList ) {
            final String projectFile = getProjectFile( project );
            writer.write( project + "=" + projectFile + "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write project list to " + _projectListPath );
         LOGGER.error( ioE.getMessage() );
      }
   }

   private void readProjectFile() {
      final String file = getProjectFile();
      if ( ParameterHandler.readMapFromFile( file, _projectMap ) ) {
         LOGGER.info( "Loaded project parameters from " + file );
      }
   }

   private void writeProjectFile() {
      final String file = getProjectFile();
      LOGGER.info( "Writing current project parameters to " + file + " ..." );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( "// Project file saved " + LocalDate.now() + "\n\n");
         writer.write( PROJECT + "=" + getProjectName() + "\n" );
         writer.write( PIPER_FILE + "=" + getPiperFile() + "\n" );
         writer.write( TEXT_CORPUS + "=" + getTextCorpus() + "\n" );
         writer.write( OMOP_DB + "=" + getOmopDb() + "\n" );
         writer.write( OUTPUT_DIR + "=" + getOutputDir() + "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write " + file, ioE );
      }
   }

   public String getPiperGuiParms() {
      final File cliFile = new File( PROJECTS_DIR, getProjectName() + ".cli" );
      try ( Writer writer = new BufferedWriter( new FileWriter( cliFile ) ) ) {
         writer.write( "InputDirectory=" + getTextCorpus() + "\n" );
         writer.write( "OutputDirectory=" + getOutputDir() + "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write Piper CLI file with project parameters.", ioE );
      }
      return "-p " + getPiperFile() + " -c " + cliFile.getPath();
   }

   public String getEtlParms() {
      return getOutputDir() + " " + getOmopDb() + " " + getOutputDir() + "/vizDb/" + getProjectName();
   }

   public String getVizParms() {
      return "/vizDb/" + getProjectName();
   }


   private void registerShutdownHook() {
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         writeProjectFile();
         writeProjectList();
      } ) );
   }

}
