/*
  Plain Text Parse #1 - Character Map Captions to Plain Text Descriptions
  Written by: Keith Fenske, http://kwfenske.github.io/
  Thursday, 26 September 2019
  Java class name: PlainTextParse1
  Copyright (c) 2019 by Keith Fenske.  Apache License or GNU GPL.

  This is a quick-and-dirty Java 1.4 console application to extract captions
  from the Character Map data file ("CharMap4.txt") and reformat as detailed
  descriptions for an extended test version of the Plain Text data file.

  There are no parameters.  The input data file must be in the current working
  directory.  Output goes into a file called "parsed-captions.txt" encoded as
  UTF-8 text (mostly US-ASCII).  Add this to your "PlainText3.txt" data file.

  General users have no need for the PlainTextParse1 application.  THIS CODE IS
  UGLY AND SHOULD *NOT* BE USED AS THE BASIS FOR ANY OTHER PROGRAMS.

  Apache License or GNU General Public License
  --------------------------------------------
  PlainTextParse1 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.
*/

import java.io.*;                 // standard I/O
import java.util.regex.*;         // regular expressions

public class PlainTextParse1
{
  public static void main(String[] args)
  {
    String caption;               // mouse caption from character map data
    BufferedReader input;         // input character stream
    String line;                  // one line of text from input file
    Matcher matcher;              // pattern matcher for <pattern>
    String number;                // Unicode character number in hexadecimal
    PrintWriter output;           // output character stream
    Pattern pattern;              // compiled regular expression

    try                           // catch specific and general I/O errors
    {
      input = new BufferedReader(new InputStreamReader(new FileInputStream(
        "CharMap4.txt"), "UTF-8")); // mouse captions for Unicode characters
      output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new
        FileOutputStream("parsed-captions.txt"), "UTF-8")));
      pattern = Pattern.compile(
        "^\\s*[Uu]\\+([0-9A-Fa-f]+)\\s*=\\s*(.*\\S)\\s*$");
      while ((line = input.readLine()) != null) // read one line from file
      {
        matcher = pattern.matcher(line); // only want specific lines
        if (matcher.find())       // if this line has correct syntax
        {
          number = matcher.group(1).toUpperCase(); // Unicode character number
          caption = matcher.group(2); // mouse caption or description
          output.println("replace U+" + number + " = \"<U+" + number + " "
            + unquote(caption) + ">\"");
        }
      }
      input.close();              // try to close input file
      output.close();             // try to close output file
    }
    catch (IOException ioe)       // all other I/O errors
    {
      System.err.println("File I/O error: " + ioe.getMessage());
    }
  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  unquote() method

  Double quotes have a special meaning to the parser that reads the Plain Text
  data file.  If they are in a mouse caption or character description, replace
  them with an acceptable escape sequence (which will not be optimal if there
  are leading, trailing, or consecutive quotes).
*/
  static String unquote(String input)
  {
    StringBuffer buffer;          // faster than String for multiple appends
    char ch;                      // one character from input string
    int i;                        // index variable
    int length;                   // size of input string in characters

    buffer = new StringBuffer();  // allocate empty string buffer for result
    length = input.length();      // get size of input string in characters
    for (i = 0; i < length; i ++)
    {
      ch = input.charAt(i);       // get one character from input string
      if (ch == '"')              // the only character we worry about
        buffer.append("\" U+0022 \""); // escape sequence
      else
        buffer.append(ch);        // accept this character as-is
    }
    return(buffer.toString());    // give caller our converted string

  } // end of unquote() method

} // end of PlainTextParse1 class

/* Copyright (c) 2019 by Keith Fenske.  Apache License or GNU GPL. */
