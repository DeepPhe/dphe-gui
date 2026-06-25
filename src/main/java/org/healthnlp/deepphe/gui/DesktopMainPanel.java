package org.healthnlp.deepphe.gui;

import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.util.ParameterHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
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
        DPHE( "NLP Summarizer", "NLP_3_100.png" ),
        ETL( "Data Merge Tool", "ETL_3_100.png" ),
        VIZ( "Visualization GUI", "Viz_4_100.png" ),
        WIKI( "Wiki", "Wiki_1_80.png" ),
        SITE( "Web Site", "Website_1_80.png" );
        private final String _name;
        private final String _icon;
        ButtonInfo( final String name, final String icon ) {
            _name = name;
            _icon = icon;
        }
        private String getName() {
            return _name;
        }
        private String getIcon() {
            return "org/healthnlp/deepphe/desktop/icon/" + _icon;
        }
    }


    private static final String HTTPS_DEEPPHE_NLP_WIKI = "https://deepphe.github.io/";
    private static final String HTTPS_DEEPPHE_GITHUB_IO = "https://deepphe.github.io/";

    private final Map<String,String> _parameterMap = new HashMap<>();

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

    public void readParameterFile( final String... args ) {
        if ( args.length != 1 ) {
            logBadArgs( args );
            return;
        }
       String parameterFile = args[ 0 ];
        if ( !ParameterHandler.readMapFromFile( parameterFile, _parameterMap ) ) {
            LOGGER.error( "Cannot read the parameter file: " + parameterFile );
            LOGGER.error( "Please exit the application and correct your parameter file." );
            return;
        }
        final String stopViz
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "StopViz", "StopVis" );
        if ( !ParameterHandler.isValueValid( stopViz ) ) {
            return;
        }
        final String vizDir
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "VizDir", "VisDir" );
        if ( !ParameterHandler.isValueValid( vizDir ) ) {
            return;
        }
        registerShutdownHook( "DeepPhe Viz", stopViz, vizDir );
        final String startDphe
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "StartDphe", "StartDeepPhe" );
        if ( !ParameterHandler.isValueValid( startDphe ) ) {
            return;
        }
        final String dpheDir
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "DpheDir", "DeepPheDir" );
        if ( !ParameterHandler.isValueValid( dpheDir ) ) {
            return;
        }
        _dpheButton.addActionListener( new StartAction( DPHE, startDphe, dpheDir, _projectPanel::getPiperGuiParms ) );
//        TODO - ETL Action
        final String startEtl
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "StartEtl", "StartDbLoad", "StartLoadDb" );
        if ( !ParameterHandler.isValueValid( startEtl ) ) {
            return;
        }
        final String etlDir
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "EtlDir", "DbLoadDir", "LoadDbDir" );
        if ( !ParameterHandler.isValueValid( etlDir ) ) {
            return;
        }
        _etlButton.addActionListener( new StartAction( ETL, startEtl, etlDir, _projectPanel::getEtlParms ) );
        final String startViz
              = ParameterHandler.getAndCheckParameter( _parameterMap, parameterFile, "StartViz", "StartVis" );
        if ( !ParameterHandler.isValueValid( startViz ) ) {
            return;
        }
        _vizButton.addActionListener( new StartAction( VIZ, startViz, vizDir, _projectPanel::getVizParms ) );
        _wikiButton.addActionListener( new WebAction( HTTPS_DEEPPHE_NLP_WIKI ) );
        _siteButton.addActionListener( new WebAction( HTTPS_DEEPPHE_GITHUB_IO ) );
    }


    static private void logBadArgs( final String... args ) {
        if ( args.length == 1 ) {
            return;
        }
        LOGGER.error( "A single argument pointing to a File containing run parameters is required." );
        LOGGER.info( "" );
        LOGGER.info( "Each line in the file should have the format:" );
        LOGGER.info( "Name=Value" );
        LOGGER.info( "" );
        LOGGER.info( "The required values are:" );
        LOGGER.info( "StartNeo4j" );
        LOGGER.info( "Neo4jDir" );
        LOGGER.info( "StopNeo4j" );
        LOGGER.info( "DpheDir or DeepPheDir" );
        LOGGER.info( "StartDphe or StartDeepPhe" );
        LOGGER.info( "VizDir or VisDir" );
        LOGGER.info( "StartViz or StartVis" );
        LOGGER.info( "" );
        LOGGER.error( "Please restart the Application with an argument pointing to a parameter file." );
    }


    private ProjectPanel createProjectPanel() {
        return new ProjectPanel();
    }


    private JToolBar createToolBar() {
        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable( false );
        toolBar.setRollover( true );
        toolBar.addSeparator( new Dimension( 50, 0 ) );
        _dpheButton = addButton( toolBar, DPHE );
        toolBar.addSeparator( new Dimension( 50, 0 ) );
        _etlButton = addButton( toolBar, ETL );
        toolBar.addSeparator( new Dimension( 50, 0 ) );
        _vizButton = addButton( toolBar, VIZ );
        toolBar.addSeparator( new Dimension( 50, 0 ) );
        toolBar.add( new JSeparator( SwingConstants.VERTICAL ) );
        _wikiButton = addButton( toolBar, WIKI );
        toolBar.addSeparator( new Dimension( 10, 0 ) );
        _siteButton = addButton( toolBar, SITE );
        toolBar.addSeparator( new Dimension( 50, 0 ) );
        return toolBar;
    }

    static private JButton addButton( final JToolBar toolBar, final ButtonInfo buttonInfo ) {
        final JButton button = new JButton();
        button.setFocusPainted( false );
        // prevents first button from having a painted border
        button.setFocusable( false );
        button.setToolTipText( buttonInfo.getName() );
        button.setHorizontalTextPosition( SwingConstants.CENTER );
        button.setVerticalTextPosition( SwingConstants.BOTTOM );
        button.setFont( new Font(Font.SANS_SERIF, Font.BOLD, 16 ) );
        button.setText( buttonInfo.getName() );
        toolBar.add( button );
        toolBar.addSeparator( new Dimension( 10, 0 ) );
        return button;
    }


    private final class StartAction implements ActionListener {
        private final String _name;
        private final String _command;
        private final String _dir;
        private final Supplier<String> _parmFx;
        private boolean _paused = false;

        private StartAction( final ButtonInfo buttonInfo, final String command, final String dir, final Supplier<String> parmFx ) {
            _name = buttonInfo.getName();
            _command = command;
            _dir = dir;
            _parmFx = parmFx;
        }

        @Override
        synchronized public void actionPerformed( final ActionEvent event ) {
            if ( _dpheButton == null || _paused ) {
                return;
            }
            _paused = true;
            final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( _command + " " + _parmFx.get() );
            runner.setLogger( LOGGER );
            if ( _dir != null && !_dir.isEmpty() ) {
                runner.setDirectory( _dir );
            }
            LOGGER.info( "Starting " + _name + " ..." );
            LOGGER.info( "\n     Initializing may require several seconds.\n     Please Wait.\n" );
            try {
                SystemUtil.run( runner );
            } catch ( IOException ioE ) {
                LOGGER.error( ioE.getMessage() );
            }
            Executors.newSingleThreadScheduledExecutor()
                    .schedule( () -> { _paused = false; }, 10, TimeUnit.SECONDS );
        }
    }

    private static final class WebAction implements ActionListener {
        private final String _site;
        private WebAction( final String site ) {
            _site = site;
        }
        @Override
        public void actionPerformed( final ActionEvent event ) {
//            if ( _siteButton == null ) {
//                return;
//            }
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
            final Icon dpheIcon = IconLoader.loadIcon( DPHE.getIcon() );
            final Icon etlIcon = IconLoader.loadIcon( ETL.getIcon() );
            final Icon vizIcon = IconLoader.loadIcon( VIZ.getIcon() );
            final Icon nlpWikiIcon = IconLoader.loadIcon( WIKI.getIcon() );
            final Icon siteIcon = IconLoader.loadIcon( SITE.getIcon() );
            _dpheButton.setIcon( dpheIcon );
            _etlButton.setIcon( etlIcon );
            _vizButton.setIcon( vizIcon );
            _wikiButton.setIcon( nlpWikiIcon );
            _siteButton.setIcon( siteIcon );
        }
    }


    /**
     * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits.
     * This includes kill signals and user actions like "Ctrl-C".
     */
    private void registerShutdownHook( final String name, final String command, final String dir ) {
        Runtime.getRuntime().addShutdownHook( new Thread( () -> {
            try {
                final SystemUtil.CommandRunner runner = new SystemUtil.CommandRunner( command );
                runner.setLogger( LOGGER );
                if ( dir != null && !dir.isEmpty() ) {
                    runner.setDirectory( dir );
                }
               LOGGER.info( "Stopping " + name + " ..." );
                try {
                    SystemUtil.run( runner );
                } catch ( IOException ioE ) {
                    LOGGER.error( ioE.getMessage() );
                }
//            } catch ( LifecycleException | RotationTimeoutException multE ) {
            } catch ( Exception multE ) {
                LOGGER.error( "Could not stop " + name + ".", multE );
            }
        } ) );
    }


    public void popHello() {
        JOptionPane.showMessageDialog( this,
                "Welcome to the DeepPhe Desktop.\n"
                        + "Enter your project settings at the top, then "
                        + "use the buttons in the center to process data, create a database, "
                        + "display results, or get help.",
//                        + "At this time the Neo4j Server is being started for "
//                        + "use by DeepPhe.\n"
//                        + "It will be ready when the log states:\n"
//                        + "... Remote interface available ...",
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