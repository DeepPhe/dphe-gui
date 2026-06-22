package org.healthnlp.deepphe.gui;

import org.apache.ctakes.gui.component.FileTableCellEditor;
import org.apache.ctakes.gui.component.SmoothTipTable;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.util.ParameterHandler;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {6/22/2026}
 */
public class ProjectPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "ProjectPanel" );

   static private final String CWD = System.getProperty( "user.dir" );

   static private final String EXAMPLE_PROJECT = "ExampleProject";
   static private final String PROJECTS_DIR = CWD + "/resources/projects/";

   static private final String PROJECT = "PROJECT";
   static private final String PIPELINE = "PIPELINE";
   static private final String INPUT_DIR = "INPUT_DIR";
   static private final String OUTPUT_DIR = "OUTPUT_DIR";


   static private final String PROJECT_NAME = "Project:";
   static private final String[] ROW_NAMES = { " Pipeline", " Input Directory", " Output Directory" };

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

   private String getProjectName() {
      return _projectMap.computeIfAbsent( "Project", k -> "ExampleProject" );
   }

   private void setProjectName( final String name ) {
      if ( !name.equals( getProjectName() ) ) {
         writeProjectFile();
      }
      _projectMap.put( "Project", name );
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


   private String getPiperFile() {
      return _projectMap.computeIfAbsent(PIPELINE, k -> CWD + "/resources/pipeline/DeepPheDefault.piper" );
   }

   private void setPiperFile( final String path ) {
      if ( !path.toLowerCase().endsWith( ".piper" ) ) {
         setPiperFile( getPiperFile() );
         return;
      }
      final File piper = new File( path );
      if ( piper.canRead() ) {
         _projectMap.put( PIPELINE, path );
      } else {
         setPiperFile( getPiperFile() );
      }
   }

   private String getInputDir() {
      return _projectMap.computeIfAbsent(INPUT_DIR, k -> CWD + "/resources/examples/example_cohort" );
   }

   private void setInputDir( final String path ) {
      final File dir = new File( path );
      if ( dir.isDirectory() ) {
         _projectMap.put( INPUT_DIR, path );
      } else {
         setInputDir( getInputDir() );
      }
   }

   private String getOutputDir() {
      return _projectMap.computeIfAbsent(OUTPUT_DIR, k -> CWD + "/resources/examples/example_output"  );
   }

   private void setOutputDir( final String path ) {
      final File dir = new File( path );
      if ( dir.isDirectory() ) {
         _projectMap.put( OUTPUT_DIR, path );
      } else {
         setOutputDir( getOutputDir() );
      }
   }

   private JComponent createProjectTable() {
      final JTable table = new SmoothTipTable( _tableModel );
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
      final FileTableCellEditor fileEditor = new FileTableCellEditor();
      fileEditor.getFileChooser()
                .setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
      table.setDefaultRenderer( File.class, fileEditor );
      table.setDefaultEditor( File.class, fileEditor );
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

      private final String[] COLUMN_NAMES = { "Name", "Value", "" };
      private final Class<?>[] COLUMN_CLASSES = { String.class, String.class, File.class };
      private final EventListenerList _listenerList = new EventListenerList();

      private void reset() {
         fireTableChanged( new TableModelEvent( this ) );
      }
      @Override
      public int getRowCount() {
         return 3;
      }
      @Override
      public int getColumnCount() {
         return 3;
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
            switch ( row ) {
               case 0 : return getPiperFile();
               case 1 : return getInputDir();
               case 2 : return getOutputDir();
            }
         } else if ( column == 2 ) {
            final String path = (String) getValueAt( row, 1 );
            return new File( path );
         }
         return "ERROR";
      }
      @Override
      public boolean isCellEditable( final int row, final int column ) {
         return column != 0;
      }
      @Override
      public void setValueAt( final Object aValue, final int row, final int column ) {
         if ( column == 1 ) {
            final String text = aValue.toString().trim();
            switch ( row ) {
               case 0 : setPiperFile( text );
               case 1 : setInputDir( text );
               case 2 : setOutputDir( text );
            }
            fireTableChanged( new TableModelEvent( this, row, row, column ) );
         } else if ( column == 2 && aValue instanceof File ) {
            final String filePath = ((File)aValue).getPath();
            switch ( row ) {
               case 0 : setPiperFile( filePath );
               case 1 : setInputDir( filePath );
               case 2 : setOutputDir( filePath );
            }
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
      boolean listOk = ParameterHandler.readMapFromFile( _projectListPath, _projectFileMap );
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
      LOGGER.info( "Writing Project List: " + _projectListPath + " ...");
      try ( Writer writer = new BufferedWriter( new FileWriter( _projectListPath ) ) ) {
         for ( String project : _projectList ) {
            final String projectFile = getProjectFile( project );
            writer.write( project + "=" + projectFile + "\n" );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write project list to: " + _projectListPath );
         LOGGER.error( ioE.getMessage() );
      }
   }

   private void readProjectFile() {
      final String file = getProjectFile();
      if ( ParameterHandler.readMapFromFile( file, _projectMap ) ) {
         LOGGER.info( "Loaded Project File: " + file );
      }
   }

   private void writeProjectFile() {
      final String file = getProjectFile();
      LOGGER.info( "Writing Project File: " + file + " ...");
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( "// Project file saved " + LocalDate.now() + "\n\n");
         writer.write( PROJECT + "=" + getProjectName() + "\n" );
         writer.write( PIPELINE + "=" + getPiperFile() + "\n" );
         writer.write( INPUT_DIR + "=" + getInputDir() + "\n" );
         writer.write( OUTPUT_DIR + "=" + getOutputDir() + "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not write " + file, ioE );
      }
   }


   private void registerShutdownHook() {
      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
         writeProjectFile();
         writeProjectList();
      } ) );
   }

}
