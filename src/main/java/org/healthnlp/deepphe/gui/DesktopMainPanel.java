package org.healthnlp.deepphe.gui;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static org.healthnlp.deepphe.gui.DesktopMainPanel.ButtonInfo.*;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2022}
 */
public class DesktopMainPanel extends JPanel {

    static private final Logger LOGGER = Logger.getLogger( "DeepPhe Desktop" );

    enum ButtonInfo {
        DPHE( "NLP Summarizer","NLP_3_100.png",
              "Runs a text corpus through the DeepPhe phenotype summarizer pipeline." ),
        ETL( "Data Merge Tool",  "ETL_3_100.png",
              "Merges NLP summarizer output and an OMOP database into a database that the Viz tool can use." ),
        VIZ( "Visualization GUI", "Viz_4_100.png",
              "Displays information about the patient cohort, individual patients, and documents." ),
        WIKI( "Wiki", "Wiki_1_80.png" ,"Manuals for the DeepPhe tools." ),
        SITE( "Web Site", "Website_1_80.png", "The main DeepPhe web site." );
        private final String _name;
        private final String _icon;
        private final String _tip;
        ButtonInfo( final String name, final String icon, final String tip ) {
            _name = name;
            _icon = icon;
            _tip = tip;
        }
        private String getIconPath() {
            return "org/healthnlp/deepphe/desktop/icon/" + _icon;
        }
    }

    private final ProjectPanel _projectPanel;
    private JButton _dpheButton;
    private JButton _etlButton;
    private JButton _vizButton;
    private JButton _wikiButton;
    private JButton _siteButton;

    DesktopMainPanel() {
        super( new BorderLayout() );
        final JPanel subPanel = new JPanel( new BorderLayout() );
        _projectPanel = createProjectPanel();
        subPanel.add( _projectPanel, BorderLayout.NORTH );
        subPanel.add( createToolBar(), BorderLayout.CENTER );
        add( subPanel, BorderLayout.NORTH );
        add( createLogPanel(), BorderLayout.CENTER );
        SwingUtilities.invokeLater( new ButtonIconLoader() );
    }

    public void initialize() {
         _dpheButton.addActionListener(
               new ToolAction( ButtonInfo.DPHE, DpheDesktop.ToolConfig.DPHE, _projectPanel::getPiperGuiParms ) );
        _etlButton.addActionListener(
              new ToolAction( ButtonInfo.ETL, DpheDesktop.ToolConfig.ETL, _projectPanel::getEtlParms ) );
        _vizButton.addActionListener(
              new ToolAction( ButtonInfo.VIZ, DpheDesktop.ToolConfig.VIZ, _projectPanel::getVizParms ) );
        _wikiButton.addActionListener( new WebAction( DpheDesktop.HTTPS_DEEPPHE_NLP_WIKI ) );
        _siteButton.addActionListener( new WebAction( DpheDesktop.HTTPS_DEEPPHE_GITHUB_IO ) );
    }

    private ProjectPanel createProjectPanel() {
        return new ProjectPanel();
    }

    private JToolBar createToolBar() {
        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable( false );
        toolBar.setRollover( true );
        _dpheButton = addButton( toolBar, DPHE, 60, 30 );
        _etlButton = addButton( toolBar, ETL, 30, 30 );
        _vizButton = addButton( toolBar, VIZ, 30, 60 );
        toolBar.add( new JSeparator( SwingConstants.VERTICAL ) );
        _wikiButton = addButton( toolBar, WIKI, 60, 10 );
        _siteButton = addButton( toolBar, SITE, 10, 60 );
        return toolBar;
    }

    static private JButton addButton( final JToolBar toolBar, final ButtonInfo buttonInfo,
                                      final int lpad, final int rpad ) {
        toolBar.addSeparator( new Dimension( lpad, 0 ) );
        final JButton button = new JButton();
        button.setFocusPainted( false );
        // prevents first button from having a painted border
        button.setFocusable( false );
        button.setToolTipText( buttonInfo._tip );
        button.setHorizontalTextPosition( SwingConstants.CENTER );
        button.setVerticalTextPosition( SwingConstants.BOTTOM );
        button.setFont( new Font(Font.SANS_SERIF, Font.BOLD, 16 ) );
        button.setText( buttonInfo._name );
        toolBar.add( button );
        toolBar.addSeparator( new Dimension( rpad, 0 ) );
        return button;
    }

    private final class ToolAction implements ActionListener {
        private final ButtonInfo _buttonInfo;
        private final DpheDesktop.ToolConfig _toolConfig;
        private final Supplier<String> _parmFx;
        private boolean _paused = false;

        private ToolAction( final ButtonInfo buttonInfo, final
                            DpheDesktop.ToolConfig toolConfig,
                            final Supplier<String> parmFx ) {
            _buttonInfo = buttonInfo;
            _toolConfig = toolConfig;
            _parmFx = parmFx;
        }

        @Override
        synchronized public void actionPerformed( final ActionEvent event ) {
            if ( _dpheButton == null || _paused ) {
                return;
            }
            _paused = true;
            final String logFile = _toolConfig.getLogFile();
            final String command = _toolConfig.getFullCommand() + " " + _parmFx.get();
            LOGGER.info( "Starting " + _buttonInfo._name + " ..." );
            LOGGER.info( command );
            LOGGER.info( "\n     Initializing may require several seconds.\n     Please Wait.\n" );
            if ( _buttonInfo == ETL ) {
                runVisibleLoggingCommand( command, _toolConfig.getFullDir(), logFile );
                return;
            }
            final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
            runner.setDirectory( _toolConfig.getFullDir() );
            runner.setLogFiles( logFile );
            try {
                SystemUtil.run( runner );
            } catch ( IOException ioE ) {
                LOGGER.error( ioE.getMessage() );
            }
            scheduleUnpause();
        }

        private void runVisibleLoggingCommand( final String command, final String directory, final String logFile ) {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute( () -> {
                final File commandLog = getCommandLog( directory, logFile );
                try {
                    LOGGER.info( "Writing " + _buttonInfo._name + " output to " + commandLog.getPath() );
                    final int exitCode = runProcess( command, directory, commandLog );
                    if ( exitCode == 0 ) {
                        LOGGER.info( _buttonInfo._name + " completed successfully." );
                    } else {
                        LOGGER.error( _buttonInfo._name + " exited with code " + exitCode + "." );
                    }
                } catch ( IOException ioE ) {
                    LOGGER.error( "Could not run " + _buttonInfo._name + ".", ioE );
                } catch ( InterruptedException intE ) {
                    Thread.currentThread().interrupt();
                    LOGGER.error( _buttonInfo._name + " was interrupted.", intE );
                } finally {
                    unpause();
                    executor.shutdown();
                }
            } );
        }

        private int runProcess( final String command, final String directory, final File commandLog )
                throws IOException, InterruptedException {
            final File parent = commandLog.getParentFile();
            if ( parent != null ) {
                parent.mkdirs();
            }
            final ProcessBuilder processBuilder = createProcessBuilder( command );
            processBuilder.redirectErrorStream( true );
            if ( directory != null && !directory.isEmpty() ) {
                final File commandDir = new File( directory );
                if ( !commandDir.exists() ) {
                    commandDir.mkdirs();
                }
                processBuilder.directory( commandDir );
            }
            ensureEnvironment( processBuilder );
            final Process process = processBuilder.start();
            try ( BufferedReader reader = new BufferedReader(
                    new InputStreamReader( process.getInputStream(), StandardCharsets.UTF_8 ) );
                  BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter( new FileOutputStream( commandLog, false ), StandardCharsets.UTF_8 ) ) ) {
                String line = reader.readLine();
                while ( line != null ) {
                    writer.write( line );
                    writer.newLine();
                    writer.flush();
                    LOGGER.info( line );
                    line = reader.readLine();
                }
            }
            return process.waitFor();
        }

        private File getCommandLog( final String directory, final String logFile ) {
            final File file = new File( logFile );
            if ( file.isAbsolute() || directory == null || directory.isEmpty() ) {
                return file;
            }
            return new File( directory, logFile );
        }

        private ProcessBuilder createProcessBuilder( final String command ) {
            final String osName = System.getProperty( "os.name", "" ).toLowerCase();
            if ( osName.contains( "windows" ) ) {
                return new ProcessBuilder( "cmd.exe", "/c", command );
            }
            return new ProcessBuilder( "bash", "-c", command );
        }

        private void ensureEnvironment( final ProcessBuilder processBuilder ) {
            final Map<String,String> environment = processBuilder.environment();
            final String javaHome = System.getProperty( "java.home" );
            if ( javaHome != null && !javaHome.isEmpty() ) {
                environment.put( "JAVA_HOME", javaHome );
            }
            if ( !environment.containsKey( "CTAKES_HOME" ) ) {
                String ctakesHome = System.getenv( "CTAKES_HOME" );
                if ( ctakesHome == null || ctakesHome.isEmpty() ) {
                    ctakesHome = System.getProperty( "user.dir" );
                }
                environment.put( "CTAKES_HOME", ctakesHome );
            }
            if ( !environment.containsKey( "CLASSPATH" ) ) {
                final String classPath = System.getProperty( "java.class.path" );
                if ( classPath != null && !classPath.isEmpty() ) {
                    environment.put( "CLASSPATH", classPath );
                }
            }
            for ( String propertyName : System.getProperties().stringPropertyNames() ) {
                if ( propertyName.startsWith( "ctakes.env." ) ) {
                    environment.put( propertyName.substring( "ctakes.env.".length() ),
                                     System.getProperty( propertyName ) );
                }
            }
        }

        private void scheduleUnpause() {
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule( () -> {
                unpause();
                executor.shutdown();
            }, 10, TimeUnit.SECONDS );
        }

        synchronized private void unpause() {
            _paused = false;
        }
    }

    private final class WebAction implements ActionListener {
        private final String _site;
        private WebAction( final String site ) {
            _site = site;
        }
        @Override
        public void actionPerformed( final ActionEvent event ) {
            if ( _dpheButton == null ) {
                return;
            }
            LOGGER.info( "Opening " + _site + " ..." );
            SystemUtil.openWebPage( _site );
        }
    }

    /**
     * Simple Runnable that loads icons
     *
     */
    private final class ButtonIconLoader implements Runnable {
        @Override
        public void run() {
            final Icon dpheIcon = IconLoader.loadIcon( ButtonInfo.DPHE.getIconPath() );
            final Icon etlIcon = IconLoader.loadIcon( ButtonInfo.ETL.getIconPath() );
            final Icon vizIcon = IconLoader.loadIcon( ButtonInfo.VIZ.getIconPath() );
            final Icon nlpWikiIcon = IconLoader.loadIcon( ButtonInfo.WIKI.getIconPath() );
            final Icon siteIcon = IconLoader.loadIcon( ButtonInfo.SITE.getIconPath() );
            _dpheButton.setIcon( dpheIcon );
            _etlButton.setIcon( etlIcon );
            _vizButton.setIcon( vizIcon );
            _wikiButton.setIcon( nlpWikiIcon );
            _siteButton.setIcon( siteIcon );
        }
    }

    public void popHello() {
        JOptionPane.showMessageDialog( this,
                "Welcome to the DeepPhe Desktop.\n"
                        + "Enter your project settings at the top, then "
                        + "use the buttons in the center to process data, create a database, "
                        + "display results, or get help.",
                "Welcome to DeepPhe Desktop",
                INFORMATION_MESSAGE );
    }

    static private JComponent createLogPanel() {
        final JPanel panel = new JPanel( new BorderLayout() );
        panel.setBorder( new EmptyBorder( 20, 5, 5, 5 ) );
        final JLabel label = new JLabel( "Desktop Activity Log:" );
        label.setFont( new Font(Font.DIALOG, Font.BOLD, 14 ) );
        label.setBorder( new EmptyBorder( 5, 20, 5, 5 ) );
        panel.add( label, BorderLayout.NORTH );
        panel.add( LoggerPanel.createLoggerPanel(), BorderLayout.CENTER );
        return panel;
    }

}
