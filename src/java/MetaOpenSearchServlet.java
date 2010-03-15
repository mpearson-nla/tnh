/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

/** 
 * 
 */   
public class MetaOpenSearchServlet extends HttpServlet 
{
  public static final String NS_OPENSEARCH = "http://a9.com/-/spec/opensearchrss/1.0/";
  public static final String NS_ARCHIVE   = "http://web.archive.org/-/spec/opensearchrss/1.0/";
 
  MetaOpenSearch meta;
  
  int hitsPerSite = 0;

  public void init( ServletConfig config )
    throws ServletException 
  {
    String rossFile = config.getInitParameter( "ross" );

    if ( rossFile == null || rossFile.trim().length() == 0 )
      {
        throw new ServletException( "Required init parameter missing: ross" );
      }

    int timeout     = getInteger( config.getInitParameter( "timeout"     ), 0 );
    int hitsPerSite = getInteger( config.getInitParameter( "hitsPerSite" ), 0 );

    try
      {
        this.meta = new MetaOpenSearch( rossFile, timeout );
      }
    catch ( IOException ioe )
      {
        throw new ServletException( ioe );
      }
    
  }

  public void destroy( )
  {
    
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException 
  {
    long responseTime = System.nanoTime( );

    request.setCharacterEncoding( "UTF-8" );

    String query       = getString ( request.getParameter( "q" ), "" );
    int    numHits     = getInteger( request.getParameter( "n" ), 10 );
    int    startIndex  = getInteger( request.getParameter( "p" ), 0  );
    int    hitsPerSite = getInteger( request.getParameter( "h" ), this.hitsPerSite );

    Document doc = this.meta.query( query, startIndex, numHits, hitsPerSite );

    Element eUrlParams = new Element( "urlParams", Namespace.getNamespace( NS_ARCHIVE ) );

    for ( Map.Entry<String,String[]> e : ((Map<String,String[]>) request.getParameterMap( )).entrySet( ) )
      {
        String key = e.getKey( );
        for ( String value : e.getValue( ) )
          {
            Element eParam = new Element( "param", Namespace.getNamespace( NS_ARCHIVE ) );
            eParam.setAttribute( "name",  key   );
            eParam.setAttribute( "value", value );
            eUrlParams.addContent( eParam );
          }
      }

    doc.getRootElement( ).getChild( "channel" ).addContent( eUrlParams );

    (new XMLOutputter()).output( doc, response.getOutputStream( ) );
  }

  String getString ( String value, String defaultValue )
  {
    if ( value != null )
      {
        value = value.trim();

        if ( value.length( ) != 0 )
          {
            return value;
          }
      }
    
    return defaultValue;
  }

  int getInteger( String value, int defaultValue )
  {
    if ( value != null )
      {
        value = value.trim();
        
        if ( value.length( ) != 0 )
          {
            try
              {
                int i = Integer.parseInt( value );

                return i;
              }
            catch ( NumberFormatException nfe )
              {
                // TODO: log?
              }
          }
      }
    
    return defaultValue;
  }

}
