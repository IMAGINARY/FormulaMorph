/*
 *    Copyright 2012 Christian Stussak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mfo.formulamorph;

public class JSurfFilter extends javax.swing.filechooser.FileFilter
{
    public static String getExtension( java.io.File f )
    {
        String ext = "";
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 &&  i < s.length() - 1)
            ext = s.substring(i+1).toLowerCase();
        return ext;
    }

    public static java.io.File ensureExtension( java.io.File f )
    {
        if( !getExtension( f ).equals( "jsurf" ) )
            f = new java.io.File( f.getAbsolutePath() + ".jsurf" );
        return f;
    }

    //Accept all png files.
    public boolean accept( java.io.File f )
    {
        return f.isDirectory() || getExtension( f ).equals( "jsurf" );
    }

    //The description of this filter
    public String getDescription() {
        return "*.jsurf (jsurf SurferFile)";
    }
}




