package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author SPF , chip-nlp
 * @since {6/19/2026}
 */
final public class ParameterHandler {
   private ParameterHandler() {}

   static public final String NO_VALUE = "NO_PARAMETER_VALUE_PROVIDED";

   static private final Logger LOGGER = Logger.getLogger( "ParameterHandler" );


   static public boolean readMapFromFile( final String filepath,
                                          final Map<String,String> map ) {
      return readMapFromFile( filepath, map, true );
   }

   static public boolean readMapFromFile( final String filepath,
                                          final Map<String,String> map,
                                          final boolean toUpperCase ) {
      if ( !new File( filepath ).canRead() ) {
         return false;
      }
      try ( BufferedReader reader = new BufferedReader( new FileReader( filepath ) ) ) {
         String line = "";
         while ( line != null ) {
            line = line.trim();
            if ( line.isEmpty() || line.startsWith( "//" ) ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '=' );
            if ( splits.length != 2 || splits[0].trim().isEmpty() || splits[1].trim().isEmpty() ) {
               LOGGER.warn( "Invalid map line: " + line );
               line = reader.readLine();
               continue;
            }
            final String key = toUpperCase ? splits[0].trim().toUpperCase() : splits[0].trim();
            map.put( key, splits[1].trim() );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static public boolean readKeysFromFile( final String filepath, final Collection<String> list ) {
      if ( !new File( filepath ).canRead() ) {
         return false;
      }
      try ( BufferedReader reader = new BufferedReader( new FileReader( filepath ) ) ) {
         String line = "";
         while ( line != null ) {
            line = line.trim();
            if ( line.isEmpty() || line.startsWith( "//" ) ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '=' );
            if ( splits.length != 2 || splits[0].trim().isEmpty() || splits[1].trim().isEmpty() ) {
               LOGGER.warn( "Invalid key line: " + line );
               line = reader.readLine();
               continue;
            }
            list.add( splits[0].trim() );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static public String getParameter( final Map<String,String> map, final String... names ) {
      return Arrays.stream( names )
                                 .map( String::toUpperCase )
                                 .map( map::get )
                                 .filter( Objects::nonNull ).findAny()
                                 .orElse( NO_VALUE );
   }

   static public String getAndCheckParameter( final Map<String,String> map, final String parameterFile,
                                              final String... names ) {
      final String value = getParameter( map, names );
      checkValueValid( value, parameterFile, names );
      return value;
   }

   static public String getOrDefault( final Map<String,String> map, final String defaultValue, final String... names ) {
      final String value = getParameter( map, names );
      if ( value.equals( NO_VALUE ) ) {
         return defaultValue;
      }
      return value;
   }

   static public void checkValueValid( final String value, final String parameterFile, final String... names ) {
      if ( isValueValid( value) ) {
         return;
      }
      LOGGER.error( "No Parameter Value specified for: " + String.join( ", ", names ) );
      LOGGER.error( "Please exit the application and correct your parameter file " + parameterFile );
   }

   static public boolean isValueValid( final String value ) {
      return !value.equals( NO_VALUE );
   }

}
