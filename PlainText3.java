/*
  Plain Text #3 - Convert Unicode Characters to Plain Text
  Written by: Keith Fenske, http://kwfenske.github.io/
  Tuesday, 28 May 2019
  Java class name: PlainText3
  Copyright (c) 2019 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 graphical (GUI) application to convert Unicode characters
  to plain text characters, for example, to convert left and right quotation
  marks into plain quotes for web pages.  Since everyone has a different idea
  about what "plain text" means, a configuration file called "PlainText3.txt"
  is expected to be in the current working directory and contains the character
  conversion table.  Edit this file so that it has only the changes you want.
  Please read comments in the file for further instructions.  See also any
  "Character Map" application.

  The program presents you with a single large text area and a few buttons or
  options above.  You may edit the text area in a normal manner, including
  Control-C and Control-V keys for selective copy and paste.  The "Copy" and
  "Paste" buttons above the text area affect the entire text: the "Copy" button
  copies all text to the system clipboard ignoring any current selection, and
  the "Paste" button replaces the entire text area with the contents of the
  clipboard.  No conversion takes place until you click the "Convert" button.
  The typical sequence of actions is to copy text from a Unicode-aware
  application such as Microsoft Word, switch to this Java application, click
  the "Paste" and "Convert" buttons, then copy the converted text to another
  application that expects a more limited character set.

  Characters above U+FFFF are converted in extended Unicode form (UTF-32), if
  the Java run-time co-operates (Java 5.0 or 7 or later, not Java 6), while
  character counts are reported to the user in their UTF-16 form, which is
  double for each extended Unicode character.

  Apache License or GNU General Public License
  --------------------------------------------
  PlainText3 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options for the position and size of the
  application window, and the size of the display font.  See the "-?" option
  for a help summary:

      java  PlainText3  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.

  Restrictions and Limitations
  ----------------------------
  There are many ways of converting text from one format to another.  This
  program converts the full range of Unicode characters into alternate forms
  more suitable for use in e-mail, on web pages, or in program source code.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support

public class PlainText3
{
  /* constants */

  static final int ACTION_ACCEPT = 1; // accept, delete, replace action codes
//static final int ACTION_DELETE = 2; // delete is replace with empty string
  static final int ACTION_REPLACE = 3;
  static final int ACTION_CUSTOM = 4; // placeholder for special requests
  static final int ACTION_DECNUM = 5; // decimal character number (generic)
  static final int ACTION_HEXNUM = 6; // hexadecimal character number (generic)
  static final int ACTION_JAVA16 = 7; // Java UTF-16 backslash notation
  static final int ACTION_OCTNUM = 8; // octal character number (generic)
  static final int ACTION_UNINUM = 9; // Unicode character number (notation)
  static final int ACTION_XMLDEC = 10; // XML decimal character reference
  static final int ACTION_XMLHEX = 11; // XML hexadecimal character reference
  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2019 by Keith Fenske.  Apache License or GNU GPL.";
  static final String DEFAULT_FILE = "PlainText3.txt"; // configuration data
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = " "; // message when no status to display
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final String PROGRAM_TITLE =
    "Convert Unicode Characters to Plain Text - by: Keith Fenske";
  static final char REPLACE_CHAR = '?'; // bad Unicode replacement character
  static final double SPLIT_DIVIDER = 0.8; // big output text, small error text
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final String[] WRAP_CHOICES = {"None", "Characters", "Words"};
                                  // descriptions for <wrapIndex> values

  /* Internally, Java characters are 16-bit Unicode and strings are encoded as
  UTF-16 with high and low surrogate pairs for anything beyond U+FFFF.  We need
  UTF-32 integers during conversion and briefly while parsing the configuration
  file.  Replacement strings, for example, are parsed into an array of UTF-32
  integers, then saved back as standard Java strings (UTF-16), because that is
  what the Java run-time expects for text in a JTextArea. */

  static final int UTF16_HIGH_BEGIN = 0xD800; // UTF-16 high surrogate start
  static final int UTF16_HIGH_END = 0xDBFF; // UTF-16 high surrogate stop
  static final int UTF16_LOW_BEGIN = 0xDC00; // UTF-16 low surrogate start
  static final int UTF16_LOW_END = 0xDFFF; // UTF-16 low surrogate stop
  static final int UTF16_MASK = 0x03FF; // bit mask for high, low surrogate
  static final int UTF16_MAX = 0xFFFF; // maximum 16-bit character number
  static final int UTF16_SHIFT = 10; // bit shift combine high, low surrogate
  static final int UTF32_ERROR = 0xBADABAD; // must be more than <UTF32_MAX>
  static final int UTF32_MAX = 0x10FFFF; // maximum Unicode character number
  static final int UTF32_OFFSET = 0x010000; // start of extended Unicode

  /* class variables */

  static JButton convertButton;   // "Convert" button: conversion of text area
  static String convertText;      // saved status message from "Convert" button
  static JButton copyButton;      // "Copy" button: copy all text to clipboard
  static String dataFile;         // text file with configuration data
  static PlainText3Data dataFirst; // first parsed entry in conversion table
  static PlainText3Data dataLast; // last parsed entry in conversion table
  static JTextArea errorText;     // error messages for GUI applications
  static JButton exitButton;      // "Exit" button for ending this application
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static boolean jbnDataFlag;     // Java backslash notation in our data file
  static boolean jbnUserFlag;     // Java backslash notation in user input/text
  static JFrame mainFrame;        // this application's window if GUI
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton multiButton;     // multiple button: Paste + Convert + Copy
  static JTextArea outputText;    // text area where we convert, copy, paste
  static JButton pasteButton;     // "Paste" button: get text from clipboard
  static JSplitPane splitPanel;   // split pane between output text and error
  static JLabel statusDialog;     // status message during extended processing
  static JComboBox wrapDialog;    // graphical option for <wrapIndex>
  static int wrapIndex;           // how we wrap text lines in main dialog box
  static boolean xmlDataFlag;     // true if XML characters in our data file
  static boolean xmlUserFlag;     // true if XML characters in user input/text

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    int gapSize;                  // basis for pixel gap between GUI elements
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    dataFile = DEFAULT_FILE;      // default file name for configuration data
    dataFirst = null;             // no first parsed entry in conversion table
    dataLast = null;              // no last parsed entry in conversion table
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    gapSize = 12;                 // default pixel gap if no font size given
    jbnDataFlag = jbnUserFlag = false; // disable all Java backslash notation
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;
    wrapIndex = 2;                // try to wrap text lines at word boundaries
    xmlDataFlag = xmlUserFlag = false; // disable all XML character references

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(0);           // exit application after printing help
      }

      else if (word.startsWith("-d") || (mswinFlag && word.startsWith("/d")))
        dataFile = args[i].substring(2); // accept anything for data file name

      else if (word.equals("-j") || (mswinFlag && word.equals("/j"))
        || word.equals("-j1") || (mswinFlag && word.equals("/j1")))
      {
        jbnDataFlag = true;       // enable Java backslash in our data file
        jbnUserFlag = false;      // disable Java backslash in user input/text
      }
      else if (word.equals("-j0") || (mswinFlag && word.equals("/j0")))
        jbnDataFlag = jbnUserFlag = false; // disable all Java backslash
      else if (word.equals("-j2") || (mswinFlag && word.equals("/j2")))
      {
        /* Enable Java backslash notation in the user's input (JTextArea).
        This command-line option is hidden because (1) it is not stable, and
        (2) needs to be combined with rules that convert special text back into
        character references. */

        jbnDataFlag = false;      // disable Java backslash in our data file
        jbnUserFlag = true;       // enable Java backslash in user input/text
      }
      else if (word.equals("-j3") || (mswinFlag && word.equals("/j3")))
        jbnDataFlag = jbnUserFlag = true; // enable all Java backslash

      else if (word.equals("-m") || (mswinFlag && word.equals("/m"))
        || word.equals("-m1") || (mswinFlag && word.equals("/m1")))
      {
        xmlDataFlag = true;       // enable XML characters in our data file
        xmlUserFlag = false;      // disable XML characters in user input/text
      }
      else if (word.equals("-m0") || (mswinFlag && word.equals("/m0")))
        xmlDataFlag = xmlUserFlag = false; // disable all XML characters
      else if (word.equals("-m2") || (mswinFlag && word.equals("/m2")))
      {
        /* Enable XML character references in the user's input.  This option is
        hidden for the same reasons as Java backslash notation above. */

        xmlDataFlag = false;      // disable XML characters in our data file
        xmlUserFlag = true;       // enable XML characters in user input/text
      }
      else if (word.equals("-m3") || (mswinFlag && word.equals("/m3")))
        xmlDataFlag = xmlUserFlag = true; // enable all XML characters

      else if (word.equals("-r0") || (mswinFlag && word.equals("/r0")))
        wrapIndex = 0;            // don't wrap text in main dialog box
      else if (word.equals("-r1") || (mswinFlag && word.equals("/r1")))
        wrapIndex = 1;            // wrap text at arbitrary characters
      else if (word.equals("-r2") || (mswinFlag && word.equals("/r2")))
        wrapIndex = 2;            // wrap text at word boundaries

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
        gapSize = size;           // same size becomes basis for GUI pixel gap
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(-1);        // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else                        // parameter is not a recognized option
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(-1);          // exit application after printing help
      }
    }

    /* Open the graphical user interface (GUI).  The standard Java style is the
    most reliable, but you can switch to something closer to the local system,
    if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new PlainText3User(); // create our shared action listener

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel1, panel2, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
    int gapLine = (gapSize / 2) + 6; // vertical space between lines
    panel1.add(Box.createVerticalStrut(gapLine + 2)); // extra space at top

    /* Create a horizontal panel for the action buttons. */

    JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    pasteButton = new JButton("Paste");
    pasteButton.addActionListener(action);
    if (buttonFont != null) pasteButton.setFont(buttonFont);
    pasteButton.setMnemonic(KeyEvent.VK_P);
    pasteButton.setToolTipText("Replace entire text area with clipboard.");
    panel2.add(pasteButton);

    int gapButton = (2 * gapSize) + 6; // horizontal space between buttons
    panel2.add(Box.createHorizontalStrut(gapButton));

    convertButton = new JButton("Convert");
    convertButton.addActionListener(action);
    if (buttonFont != null) convertButton.setFont(buttonFont);
    convertButton.setMnemonic(KeyEvent.VK_N);
    convertButton.setToolTipText("Convert entire text area to plain text.");
    panel2.add(convertButton);

    panel2.add(Box.createHorizontalStrut(gapButton));

    copyButton = new JButton("Copy");
    copyButton.addActionListener(action);
    if (buttonFont != null) copyButton.setFont(buttonFont);
    copyButton.setMnemonic(KeyEvent.VK_C);
    copyButton.setToolTipText("Replace clipboard with entire text area.");
    panel2.add(copyButton);

    panel2.add(Box.createHorizontalStrut(gapButton));

    multiButton = new JButton("P+C+C");
    multiButton.addActionListener(action);
    if (buttonFont != null) multiButton.setFont(buttonFont);
    multiButton.setMnemonic(KeyEvent.VK_A);
    multiButton.setToolTipText("Paste + Convert + Copy buttons.");
    panel2.add(multiButton);

    panel2.add(Box.createHorizontalStrut(gapButton));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel2.add(exitButton);

    panel1.add(panel2);
    panel1.add(Box.createVerticalStrut(gapLine)); // space between panels

    /* Create a horizontal panel for the options. */

    JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label1 = new JLabel("Font:", JLabel.RIGHT);
    if (buttonFont != null) label1.setFont(buttonFont);
    panel3.add(label1);

    int gapLabel = (gapSize / 3) + 2; // horizontal space between labels
    panel3.add(Box.createHorizontalStrut(gapLabel));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for display text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel3.add(fontNameDialog);

    panel3.add(Box.createHorizontalStrut(gapLabel));

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    fontSizeDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for display text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel3.add(fontSizeDialog);

    panel3.add(Box.createHorizontalStrut(gapButton));

    JLabel label2 = new JLabel("Text Wrap:", JLabel.RIGHT);
    if (buttonFont != null) label2.setFont(buttonFont);
    panel3.add(label2);

    panel3.add(Box.createHorizontalStrut(gapLabel));

    wrapDialog = new JComboBox(WRAP_CHOICES);
    wrapDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) wrapDialog.setFont(buttonFont);
    wrapDialog.setSelectedIndex(wrapIndex); // select default level
    wrapDialog.setToolTipText("Select scroll lines or wrap text.");
    wrapDialog.addActionListener(action); // do last so don't fire early
    panel3.add(wrapDialog);

    panel1.add(panel3);
    panel1.add(Box.createVerticalStrut(gapLine)); // extra space at bottom

    /* Put above boxed options in a panel that is centered horizontally.  Use
    FlowLayout's horizontal gap to add padding on the left and right sides. */

    JPanel panel4 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel4.add(panel1);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(20, 40);
    outputText.setEditable(true); // user can change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(wrapIndex > 0); // if we allow text lines to wrap
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setWrapStyleWord(wrapIndex > 1); // wrap at word boundaries
    outputText.setText(
      "\nConvert Unicode characters to plain text characters, for example,"
      + " left and right quotation marks into plain quotes for web pages."
      + "\n\nType or paste your Unicode text here, then click the Convert"
      + " button.  Buttons affect the entire text area.  Use the Control-C or"
      + " Control-V keys if you want to copy or paste selected characters."
      + "\n\nCopyright (c) 2019 by Keith Fenske.  By using this program, you"
      + " agree to terms and conditions of the Apache License and/or GNU"
      + " General Public License.\n\n");

    /* Create a smaller scrolling text area for error messages, etc.  Use a
    slightly smaller width than <outputText> above, to avoid some horizontal
    scroll bars that may appear when not necessary. */

    errorText = new JTextArea(3, 36);
    errorText.setEditable(false); // user can't change this text area
    errorText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    errorText.setLineWrap(false); // don't wrap text lines
    errorText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    errorText.setText("");        // completely empty to begin with
    errorText.setToolTipText(
      "Errors in configuration data file or alternate character notations.");

    /* Create a split panel for the output text (top) and errors (bottom). */

    splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
      new JScrollPane(outputText), new JScrollPane(errorText));
    splitPanel.setOneTouchExpandable(true); // fast widgets to expand/contract
    splitPanel.setResizeWeight(SPLIT_DIVIDER); // big output text, small error

    /* Create an entire panel just for the status message.  We do this so that
    we have some control over the margins.  Put the status text in the middle
    of a BorderLayout so that it expands with the window size. */

    JPanel panel6 = new JPanel(new BorderLayout(0, 0));
    statusDialog = new JLabel(EMPTY_STATUS, JLabel.LEFT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);
    statusDialog.setToolTipText(
      "Replace count (substitutions) or copy-and-paste characters (UTF-16).");
    panel6.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel6.add(statusDialog, BorderLayout.CENTER);
    panel6.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panel6.add(Box.createVerticalStrut(4), BorderLayout.SOUTH);

    /* Create the main window frame for this application.  Stack buttons and
    options above the text area.  Keep text in the center so that it expands
    horizontally and vertically.  Put status message at the bottom, which also
    expands. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel7 = mainFrame.getContentPane(); // where content meets frame
    panel7.setLayout(new BorderLayout(0, 0));
    panel7.add(panel4, BorderLayout.NORTH); // buttons and options
    panel7.add(splitPanel, BorderLayout.CENTER); // output, error text areas
    panel7.add(panel6, BorderLayout.SOUTH); // status message

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    outputText.requestFocusInWindow(); // give keyboard focus to text area
    splitPanel.setDividerLocation(1.0); // all output text, hide error text
    loadConfig();                 // always load configuration data file here

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  addConversion() method

  This helper method makes it easier to add characters to the conversion table,
  and is called by the loadConfig() method.  An original string (our left side)
  must have at least one character, or be a low-high range.  The replacement
  string (our right side) may be empty (delete action) or null (accept action).
*/
  static void addConversion(      // convenience method for one character
    int ch,                       // one UTF-32 character (left side)
    String newText)               // arbitrary replacement string (right side)
  {
    int[] array = { ch };         // temporary array with one element
    addConversion(ACTION_REPLACE, false, array, newText); // general method
  }

  static void addConversion(      // general method for array of characters
    int actionCode,               // accept, delete, replace action code
    boolean rangeFlag,            // true if <oldText> is low-high range
    int[] oldText,                // non-empty UTF-32 array (left side)
    String newText)               // arbitrary replacement string (right side)
  {
    if ((oldText == null)
      || ((rangeFlag == false) && (oldText.length == 0))
      || ((rangeFlag == true) && (oldText.length != 2)))
    {
      System.err.println(
        "Invalid <oldText> parameter in addConversion() method.");
    }
    else
    {
      PlainText3Data data = new PlainText3Data(); // create new data entry
      data.action = actionCode;   // copy whatever parameters caller gave us
      data.isRange = rangeFlag;   // ... to the new data entry (rule)
      data.left = oldText;
      data.right = newText;
      if (dataFirst == null)      // is this the first data entry (rule)?
        dataFirst = dataLast = data; // yes, first and last entries are same
      else                        // no, we already have some entries
      {
        dataLast.next = data;     // link new entry to current end of list
        dataLast = data;          // and new entry becomes new end of list
      }
    }
  } // end of addConversion() method


/*
  appendArray() method

  Given two integer arrays, return a new array with the second array appended
  (concatenated) to the end of the first array.  Arrays are character numbers
  for what would be a UTF-32 string.

  This is not an efficient way of dealing with characters as integers, and is
  used in loadConfig() while parsing the configuration data file.  Most calls
  have an empty first array.  Both arrays are usually very small.
*/
  static int[] appendArray(int[] firstArray, int[] secondArray)
  {
    int firstLength;              // number of elements in <firstArray>
    int i;                        // index variable
    int[] resultArray;            // our converted result
    int resultCount;              // total elements copied to <resultArray>
    int secondLength;             // number of elements in <secondArray>

    firstLength = firstArray.length; // get number of elements in arrays
    secondLength = secondArray.length;
    if (firstLength == 0)         // if first array is empty, ...
      resultArray = secondArray;  // return second without even looking
    else if (secondLength == 0)
      resultArray = firstArray;   // return first without even looking
    else
    {
      resultArray = new int[firstLength + secondLength]; // size of new array
      resultCount = 0;            // no elements copied yet
      for (i = 0; i < firstLength; i ++)
        resultArray[resultCount ++] = firstArray[i];
      for (i = 0; i < secondLength; i ++)
        resultArray[resultCount ++] = secondArray[i];
    }
    return(resultArray);          // give caller our converted result

  } // end of appendArray() method


/*
  doConvertButton() method

  Convert the text area from Unicode characters to plain text characters.  The
  current caret position (selection) is reset to the end of the text.
*/
  static void doConvertButton()
  {
    int changeCount;              // number of changes made (not characters)
    PlainText3Data dataEntry;     // current entry in conversion table
    int i;                        // index variable
    int inputIndex;               // current character in <inputText>
    int inputLength;              // number of characters in <inputText>
    int[] inputText;              // UTF-32 input text from our JTextArea
    boolean matchFlag;            // true if all characters match data entry
    StringBuffer resultBuffer;    // faster than String for multiple appends
    int searchIndex;              // current look-ahead index in <inputText>

    errorText.setText("");        // clear error text area

    /* Use the conversion table to replace characters in a given string.  The
    input is scanned from beginning to end, in a linear fashion, and starting
    from any given point, the first rule that matches the input characters is
    always used. */

    changeCount = 0;              // no strings have been replaced yet
    inputIndex = 0;               // start with the very first character
    inputText = utfParse16(outputText.getText(), jbnUserFlag, xmlUserFlag);
                                  // get input string from user's text area
    inputLength = inputText.length; // get size of input string in characters
    resultBuffer = new StringBuffer(); // allocate empty buffer for result

    while (inputIndex < inputLength)
    {
      dataEntry = dataFirst;      // start search with first conversion rule
      searchIndex = inputIndex;   // just to keep compiler happy

      /* Find the first matching entry (rule) in the conversion table.  Even
      though we should never have an empty left side (zero UTF-32 characters),
      we can easily check for this to avoid a possible infinite loop. */

      while (dataEntry != null)
      {
        searchIndex = inputIndex; // restart search at this input character
        if (dataEntry.isRange)    // is left side a low-high range?
        {
          if ((inputText[searchIndex] >= dataEntry.left[0])
            && (inputText[searchIndex] <= dataEntry.left[1]))
          {
            searchIndex ++;       // matched one character, advance to next
            break;                // exit early from <while> loop
          }
        }
        else if ((dataEntry.left.length > 0) // avoid possible infinite loop
          && (inputIndex + dataEntry.left.length) <= inputLength)
        {                         // left side is array of UTF-32 characters
          i = 0;                  // start with first character
          matchFlag = true;       // assume all characters match
          while (matchFlag && (i < dataEntry.left.length))
            matchFlag &= (inputText[searchIndex ++] == dataEntry.left[i ++]);
          if (matchFlag)          // does input match complete data entry?
            break;                // yes, exit early from <while> loop
        }
        dataEntry = dataEntry.next; // get next possible conversion rule
      }

      /* Did we find a matching entry in the conversion table? */

      if (dataEntry == null)      // no match found, advance one character
      {
        utfAppend32(resultBuffer, inputText[inputIndex ++]); // to UTF-16
      }
      else if (dataEntry.action == ACTION_ACCEPT) // accept input as-is?
      {
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          utfAppend32(resultBuffer, inputText[inputIndex ++]); // to UTF-16
      }
      else if (dataEntry.action == ACTION_REPLACE) // replace or delete
      {
        changeCount ++;           // one more substitution has been made
        inputIndex = searchIndex; // where next input comes from
        resultBuffer.append(dataEntry.right); // copy UTF-16, may be empty
      }
      else if (dataEntry.action == ACTION_CUSTOM) // special requests
      {
        /* Placeholder for customizing special requests.  The sample code is
        similar to ACTION_UNINUM with an optional string appended, such as a
        Unicode block name for non-European languages. */

        changeCount ++;           // one more substitution has been made
        boolean addSpace = false; // no space before first character (item)
        resultBuffer.append("<"); // leading delimiter
        while (inputIndex < searchIndex) // for each UTF-32 character matched
        {
          if (addSpace) resultBuffer.append(" "); // space between items
          resultBuffer.append(formatUnicodeNumber(inputText[inputIndex ++]));
          addSpace = true;        // insert spaces after first item
        }
        if ((dataEntry.right != null) && (dataEntry.right.length() > 0))
        {
          resultBuffer.append(" "); // space between items
          resultBuffer.append(dataEntry.right); // UTF-16 string, once only
        }
        resultBuffer.append(">"); // trailing delimiter
      }
      else if (dataEntry.action == ACTION_DECNUM) // decimal number
      {
        /* Format as desired for decimal character number (generic). */

        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("<" + inputText[inputIndex ++] + ">");
      }
      else if (dataEntry.action == ACTION_HEXNUM) // hexadecimal number
      {
        /* Format as desired for hexadecimal character number (generic). */

        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("<"
            + Integer.toHexString(inputText[inputIndex ++]).toUpperCase()
            + ">");
      }
      else if (dataEntry.action == ACTION_JAVA16) // Java backslash notation
      {
        /* Convert the matched UTF-32 characters into Java UTF-16 backslash
        notation (text) that can be used as source in a Java program. */

        changeCount ++;           // one more substitution has been made
        StringBuffer sb = new StringBuffer(); // to convert UTF-32 to UTF-16
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          utfAppend32(sb, inputText[inputIndex ++]); // copy UTF-32 as UTF-16
        for (i = 0; i < sb.length(); i ++) // for each new UTF-16 character
        {
          String hx = "0000" + Integer.toHexString(sb.charAt(i)).toUpperCase();
                                  // format as hex with extra leading zeros
          resultBuffer.append("\\u" + hx.substring(hx.length() - 4));
                                  // insert prefix, keep last four hex digits
        }
      }
      else if (dataEntry.action == ACTION_OCTNUM) // octal number
      {
        /* Format as desired for octal character number (generic). */

        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("<"
            + Integer.toString(inputText[inputIndex ++], 8) + ">");
      }
      else if (dataEntry.action == ACTION_UNINUM) // Unicode character number
      {
        /* Unicode character number (notation) with delimiters. */

        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("<"
            + formatUnicodeNumber(inputText[inputIndex ++]) + ">");
      }
      else if (dataEntry.action == ACTION_XMLDEC) // XML decimal reference
      {
        /* We support XML character references in the configuration data file.
        They are delimited with a clear beginning and end, unlike Java UTF-16
        backslash notation or Unicode U+nnnn character numbers. */

        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("&#" + inputText[inputIndex ++] + ";");
      }
      else if (dataEntry.action == ACTION_XMLHEX) // XML hexadecimal reference
      {
        changeCount ++;           // one more substitution has been made
        while (inputIndex < searchIndex) // for each UTF-32 character matched
          resultBuffer.append("&#x"
            + Integer.toHexString(inputText[inputIndex ++]).toUpperCase()
            + ";");
      }
      else                        // bad data entry, advance one character
      {
        System.err.println(
          "Invalid <dataEntry.action> in doConvertButton() method: "
          + dataEntry.action);
        utfAppend32(resultBuffer, inputText[inputIndex ++]); // accept one
      }
    }

    /* Set the output text area to our result, and indicate how many changes we
    made.  Changes count substitutions, not an exact number of characters. */

    outputText.setText(resultBuffer.toString()); // show user converted string
    outputText.requestFocusInWindow(); // give keyboard focus to text area
    convertText = "Text has " + prettyPlural(outputText.getText().length(),
      "character") + " with " + prettyPlural(changeCount, "change") + ".";
    statusDialog.setText(convertText); // set status and save message text

  } // end of doConvertButton() method


/*
  doCopyButton() method

  Copy the entire text area to the system clipboard.  Remember the current
  caret position (selection) and restore that afterwards.  No conversion takes
  place here.
*/
  static void doCopyButton()
  {
    int end, start;               // text positions for caret and/or selection

    end = outputText.getSelectionEnd(); // remember current position in text
    start = outputText.getSelectionStart();
    outputText.selectAll();       // select all text in the dialog box
    outputText.copy();            // place that text onto the clipboard
    outputText.select(start, end); // restore previous caret position
    outputText.requestFocusInWindow(); // give keyboard focus to text area
    statusDialog.setText("Copied " + prettyPlural(outputText.getText()
      .length(), "character") + " to the clipboard.");
  }


/*
  doPasteButton() method

  Replace the entire text area with the contents of the system clipboard.  If
  the clipboard is empty (or does not contain text), we paste zero characters
  and all text is effectively removed from the dialog box.  The current caret
  position (selection) is immaterial.  No conversion takes place here.
*/
  static void doPasteButton()
  {
    outputText.setText(null);     // delete all text currently in dialog box
    outputText.paste();           // replace that text with the clipboard
    outputText.requestFocusInWindow(); // give keyboard focus to text area
    statusDialog.setText("Copied " + prettyPlural(outputText.getText()
      .length(), "character") + " from the clipboard.");
  }


/*
  formatUnicodeNumber() method

  Format an integer as a Unicode character number: uppercase hexadecimal with
  at least four digits.
*/
  static String formatUnicodeNumber(int number)
  {
    String hx = "00000000" + Integer.toHexString(number).toUpperCase();
    return("U+" + hx.substring(Math.min(8, (hx.length() - 4))));
  }


/*
  loadConfig() method

  Load configuration data from a text file in the current working directory,
  which is usually the same folder as the program's *.class files.  Should we
  encounter an error, then print a message, but continue normal execution.
  None of the file data is critical to the operation of this program.
*/
  static void loadConfig()
  {
    int action;                   // accept, delete, replace action code
    Matcher commentMatcher;       // matcher for <commentPattern>
    Pattern commentPattern;       // regular expression for comments
    Matcher equalMatcher;         // matcher for <equalPattern>
    Pattern equalPattern;         // regular expression for equal sign
    String errorMess;             // null or an error message
    int i;                        // index variable
    BufferedReader inputFile;     // input character stream from text file
    int inputIndex;               // start of next character in <inputText>
    String inputText;             // one input line from file
    int itemCount;                // number of characters or strings found
    Matcher keywordMatcher;       // matcher for <keywordPattern>
    Pattern keywordPattern;       // regular expression for keyword verb
    int[] leftSide;               // common left side (range or UTF-32 array)
    int lineNumber;               // line number of current input text
    Matcher numberMatcher;        // matcher for <numberPattern>
    Pattern numberPattern;        // regular expression for Unicode number
    boolean rangeFlag;            // true if <leftSide) is low-high range
    Matcher rangeMatcher;         // matcher for <rangePattern>
    Pattern rangePattern;         // regular expression for low-high range
    String rightSide;             // replace right side (standard Java string)
    Matcher spaceMatcher;         // matcher for <spacePattern>
    Pattern spacePattern;         // regular expression for required space
    Matcher stringMatcher;        // matcher for <stringPattern>
    Pattern stringPattern;        // regular expression for quoted string
    int[] textArray;              // parsing array of UTF-32 characters

    /* Regular expressions can be long and complicated ways of describing the
    obvious.  Be careful when changing them!  The patterns below for character
    numbers and ranges allow Unicode U+nnnn notation, unsigned decimal numbers,
    and standard 0xFFFF hexadecimal notation. */

    commentPattern = Pattern.compile("\\G\\s*(#.*)?$");
    equalPattern = Pattern.compile("\\G\\s*=");
    keywordPattern = Pattern.compile("\\G\\s*([A-Za-z][0-9A-Za-z]+)");
                                  // must not match the "U" in "U+nnnn"
    lineNumber = 0;               // no lines have been read from file
    numberPattern = Pattern.compile(
      "\\G\\s*(?:(?:(\\d+)(?![Xx]))|(?:(?:(?:[Uu]\\+)|(?:0[Xx]))([0-9A-Fa-f]+)))");
    rangePattern = Pattern.compile(
      "\\G\\s*(?:(?:(\\d+)(?![Xx]))|(?:(?:(?:[Uu]\\+)|(?:0[Xx]))([0-9A-Fa-f]+)))\\s*(?:(?:\\.\\.)|(?:\\:))\\s*(?:(?:(\\d+)(?![Xx]))|(?:(?:(?:[Uu]\\+)|(?:0[Xx]))([0-9A-Fa-f]+)))");
    spacePattern = Pattern.compile("\\G\\s+");
    stringPattern = Pattern.compile(jbnDataFlag // quoted quotes are awkward
      ? "\\G\\s*\"((?:(?:\\\\.)|[^\"])*)\"" // allow Java backslash quotes
      : "\\G\\s*\"([^\"]*)\"");   // no escape sequence with quotation marks

    /* Open and read lines from the configuration data file. */

    try                           // catch specific and general I/O errors
    {
      inputFile = new BufferedReader(new InputStreamReader(new
        FileInputStream(dataFile), "UTF-8")); // UTF-8 encoded text file
      inputFile.mark(4);          // we may need to back up a few bytes
      i = inputFile.read();       // read byte-order marker if present
      if ((i >= 0) && (i != '\uFEFF')) // not end-of-file or byte-order mark?
        inputFile.reset();        // is regular text, go back to beginning

      while ((inputText = inputFile.readLine()) != null)
      {
        action = 0;               // no accept, delete, replace action code
        commentMatcher = commentPattern.matcher(inputText);
        equalMatcher = equalPattern.matcher(inputText);
        errorMess = null;         // no errors found yet on this line
        inputIndex = 0;           // start parsing at beginning of line
        keywordMatcher = keywordPattern.matcher(inputText);
        leftSide = null;          // just to keep compiler happy
        lineNumber ++;            // current line number in input file
        numberMatcher = numberPattern.matcher(inputText);
        rangeFlag = false;        // no low-high character range yet
        rangeMatcher = rangePattern.matcher(inputText);
        rightSide = null;         // no replacement string yet
        spaceMatcher = spacePattern.matcher(inputText);
        stringMatcher = stringPattern.matcher(inputText);

        /* An input line can be a comment with no command keyword. */

        if (commentMatcher.find(inputIndex))
          continue;               // restart from beginning of <while> loop

        /* The command keyword or verb is the first word on each line. */

        if (keywordMatcher.find(inputIndex))
        {
          inputIndex = keywordMatcher.end(); // reposition input after keyword
          String keyword = keywordMatcher.group(1).toLowerCase();
          if (keyword.equals("accept"))
            action = ACTION_ACCEPT;
          else if (keyword.equals("delete"))
          {
            action = ACTION_REPLACE; // delete is replace with empty string
            rightSide = "";
          }
          else if (keyword.equals("replace"))
            action = ACTION_REPLACE; // assume standard replace, no specials
          else
            errorMess = "unknown command keyword or verb: " + keyword;

          /* All command keywords have one or more parameters, with white space
          (blanks or tabs) after the keyword and before the parameters. */

          if (errorMess != null)
            { /* do nothing */ }
          else if (spaceMatcher.find(inputIndex)) // required white space
            inputIndex = spaceMatcher.end();
          else
            errorMess = "missing space after command keyword or verb";
        }
        else
          errorMess = "missing command keyword or verb";

        /* The left side is the same for all keywords: one or more Unicode
        character numbers or quoted strings, or one low-high range. */

        if (errorMess != null)
          { /* do nothing */ }
        else
        {
          /* First look for a low-high range.  We accept all character numbers
          here, even if they overlap the reserved range of surrogate pairs. */

          if (rangeMatcher.find(inputIndex)) // low-high range?
          {
            int low, high;        // lower and upper limits of range

            inputIndex = rangeMatcher.end(); // reposition input after range

            try                   // low part may have too many digits
            {
              if ((rangeMatcher.group(1) != null) // decimal capture group
                && (rangeMatcher.group(1).length() > 0))
              {
                low = Integer.parseInt(rangeMatcher.group(1)); // decimal
              }
              else
                low = Integer.parseInt(rangeMatcher.group(2), 16); // hex
            }
            catch (NumberFormatException nfe) { low = UTF32_ERROR; }

            try                   // high part may have too many digits
            {
              if ((rangeMatcher.group(3) != null) // decimal capture group
                && (rangeMatcher.group(3).length() > 0))
              {
                high = Integer.parseInt(rangeMatcher.group(3)); // decimal
              }
              else
                high = Integer.parseInt(rangeMatcher.group(4), 16); // hex
            }
            catch (NumberFormatException nfe) { high = UTF32_ERROR; }

            if ((low >= 0) && (low <= UTF32_MAX) && (low <= high)
              && (high >= 0) && (high <= UTF32_MAX))
            {
              leftSide = new int[] { low, high }; // array with two elements
              rangeFlag = true;   // and those elements have special meaning
            }
            else
              errorMess = "invalid low-high range from "
                + formatUnicodeNumber(low) + " to "
                + formatUnicodeNumber(high);
          }

          /* Look for one or more Unicode character numbers or strings. */

          else
          {
            itemCount = 0;        // no characters or strings found yet
            textArray = new int[0]; // append characters or strings here
            while (errorMess == null)
            {
              /* A leading space is required on second and later items. */

              if (itemCount > 0)
              {
                if (spaceMatcher.find(inputIndex))
                  inputIndex = spaceMatcher.end();
                else              // could be comment or equal sign
                  break;          // exit early from <while> loop
              }

              /* Look for a character number or a string. */

              if (numberMatcher.find(inputIndex)) // character number
              {
                inputIndex = numberMatcher.end();
                itemCount ++;     // one more character number found

                try               // number may have too many digits
                {
                  if ((numberMatcher.group(1) != null) // decimal capture group
                    && (numberMatcher.group(1).length() > 0))
                  {
                    i = Integer.parseInt(numberMatcher.group(1)); // decimal
                  }
                  else
                    i = Integer.parseInt(numberMatcher.group(2), 16); // hex
                }
                catch (NumberFormatException nfe) { i = UTF32_ERROR; }

                if ((i < 0) || (i > UTF32_MAX))
                {
                  errorMess = "maximum Unicode character number is "
                    + formatUnicodeNumber(UTF32_MAX);
                }
                else if (((i >= UTF16_HIGH_BEGIN) && (i <= UTF16_HIGH_END))
                  || ((i >= UTF16_LOW_BEGIN) && (i <= UTF16_LOW_END)))
                {
                  errorMess = "UTF-16 surrogate characters are reserved: "
                    + formatUnicodeNumber(i);
                }
                else              // valid Unicode character number
                  textArray = appendArray(textArray, new int[] { i });
              }
              else if (stringMatcher.find(inputIndex)) // quoted string
              {
                inputIndex = stringMatcher.end();
                itemCount ++;     // one more string found
                textArray = appendArray(textArray,
                  utfParse16(stringMatcher.group(1), jbnDataFlag,
                  xmlDataFlag));  // accept any string here, even empty
              }
              else                // could be comment or equal sign
                break;            // exit early from <while> loop
            }

            /* Did we find at least one character or non-empty string? */

            if (errorMess != null)
              { /* do nothing */ }
            else if (itemCount == 0) // find any characters or strings?
              errorMess = "missing character number, range, or quoted string";
            else if (textArray.length == 0)
              errorMess = "first parameter or left side may not be empty";
            else
              leftSide = textArray; // non-empty array of UTF-32 characters
          }
        }

        /* The "accept" and "delete" keywords are complete.  More work needs
        to be done for the right-hand side of the "replace" keyword. */

        if (errorMess != null)
          { /* do nothing */ }
        else if (action == ACTION_REPLACE)
        {
          /* The "replace" keyword also has an equal sign followed by Unicode
          character numbers or quoted strings for the right side.  Much of the
          code is a literal repeat from the left side.  To make copy and paste
          easier, the code is deliberately structured to have the same indent
          levels. */

          if (equalMatcher.find(inputIndex))
          {
            inputIndex = equalMatcher.end();
            String special = null; // no special pre-defined action yet

            if (keywordMatcher.find(inputIndex))
            {
              /* The right side of a "replace" can have a special word for a
              pre-defined action such as XML character references, optionally
              followed by a string, which may or may not get used when that
              action is performed. */

              inputIndex = keywordMatcher.end();
              special = keywordMatcher.group(1).toLowerCase();
              if (special.equals("custom"))
                action = ACTION_CUSTOM;
              else if (special.equals("decnum"))
                action = ACTION_DECNUM;
              else if (special.equals("hexnum"))
                action = ACTION_HEXNUM;
              else if (special.equals("java16"))
                action = ACTION_JAVA16;
              else if (special.equals("octnum"))
                action = ACTION_OCTNUM;
              else if (special.equals("uninum"))
                action = ACTION_UNINUM;
              else if (special.equals("xmldec"))
                action = ACTION_XMLDEC;
              else if (special.equals("xmlhex"))
                action = ACTION_XMLHEX;
              else
                errorMess = "unknown replace special action: " + special;
            }

            /* Look for one or more Unicode character numbers or strings. */

            itemCount = 0;        // no characters or strings found yet
            textArray = new int[0]; // append characters or strings here
            while (errorMess == null)
            {
              /* A leading space is required on second and later items. */

              if ((itemCount > 0) || (special != null))
              {
                if (spaceMatcher.find(inputIndex))
                  inputIndex = spaceMatcher.end();
                else              // could be comment
                  break;          // exit early from <while> loop
              }

              /* Look for a character number or a string. */

              if (numberMatcher.find(inputIndex)) // character number
              {
                inputIndex = numberMatcher.end();
                itemCount ++;     // one more character number found

                try               // number may have too many digits
                {
                  if ((numberMatcher.group(1) != null) // decimal capture group
                    && (numberMatcher.group(1).length() > 0))
                  {
                    i = Integer.parseInt(numberMatcher.group(1)); // decimal
                  }
                  else
                    i = Integer.parseInt(numberMatcher.group(2), 16); // hex
                }
                catch (NumberFormatException nfe) { i = UTF32_ERROR; }

                if ((i < 0) || (i > UTF32_MAX))
                {
                  errorMess = "maximum Unicode character number is "
                    + formatUnicodeNumber(UTF32_MAX);
                }
                else if (((i >= UTF16_HIGH_BEGIN) && (i <= UTF16_HIGH_END))
                  || ((i >= UTF16_LOW_BEGIN) && (i <= UTF16_LOW_END)))
                {
                  errorMess = "UTF-16 surrogate characters are reserved: "
                    + formatUnicodeNumber(i);
                }
                else              // valid Unicode character number
                  textArray = appendArray(textArray, new int[] { i });
              }
              else if (stringMatcher.find(inputIndex)) // quoted string
              {
                inputIndex = stringMatcher.end();
                itemCount ++;     // one more string found
                textArray = appendArray(textArray,
                  utfParse16(stringMatcher.group(1), jbnDataFlag,
                  xmlDataFlag));  // accept any string here, even empty
              }
              else                // could be comment or special action
                break;            // exit early from <while> loop
            }

            /* Did we find at least one character number or string? */

            if (errorMess != null)
              { /* do nothing */ }
            else if (itemCount > 0) // find any characters or strings?
              rightSide = utfCreate16(textArray); // to UTF-16 string
            else if (special != null) // was there a special action?
              { /* do nothing */ }
            else
              errorMess = "missing character number or quoted string";
          }
          else
            errorMess = "syntax error or unexpected input: "
              + inputText.substring(inputIndex);
        }

        /* Allow comments at the end of each line. */

        if (errorMess != null)
          { /* do nothing */ }
        else if (commentMatcher.find(inputIndex))
          inputIndex = commentMatcher.end(); // always goes to end of line
        else
          errorMess = "syntax error or unexpected input: "
            + inputText.substring(inputIndex);

        /* Add this conversion rule to our list, if there were no errors. */

        if (errorMess == null)    // no message text means no errors
        {
          addConversion(action, rangeFlag, leftSide, rightSide);
          if (false)              // helpful print statements for debugging
          {
            System.err.println();
            System.err.println("line " + lineNumber + ": " + inputText);
            System.err.println(" action = " + action + " range = "
              + rangeFlag);
            System.err.print(" left side ");
            if (leftSide == null)
              System.err.println("null");
            else
            {
              System.err.print("length " + leftSide.length + " =");
              for (i = 0; i < leftSide.length; i ++)
                System.err.print(" 0x" + Integer.toHexString(leftSide[i]));
              System.err.println();
            }
            System.err.print(" right side ");
            if (rightSide == null)
              System.err.println("null");
            else
            {
              System.err.print("length " + rightSide.length() + " =");
              for (i = 0; i < rightSide.length(); i ++)
                System.err.print(" 0x"
                  + Integer.toHexString(rightSide.charAt(i)));
              System.err.println();
            }
          }                       // end of debugging print statements
        }

        /* Finish this input line by showing an error message, if any. */

        if (errorMess != null)
        {
          putError("Note: " + errorMess);
          putError("Configuration line " + lineNumber + ": " + inputText);
        }
      }
      inputFile.close();          // try to close input file
    }

    catch (FileNotFoundException fnfe) // if the data file does not exist
    {
      if (dataFile.equals(DEFAULT_FILE)) // ignore our own data file
        { /* do nothing, say nothing */ }
      else                        // but warn if user gave file name
      {
        putError("Configuration data file not found: " + dataFile);
        putError("in current working directory "
          + System.getProperty("user.dir"));
      }
    }

    catch (IOException ioe)       // for all other file I/O errors
    {
      putError("Unable to read configuration data file: " + dataFile);
      putError("in current working directory "
        + System.getProperty("user.dir"));
      putError(ioe.getMessage());
    }

    /* Use our default data if the configuration file was not found. */

    if (dataFirst == null)        // if no conversions were defined
    {
      addConversion(0x2013, "-"); // En Dash
      addConversion(0x2014, "--"); // Em Dash
      addConversion(0x2018, "'"); // Left Single Quotation Mark
      addConversion(0x2019, "'"); // Right Single Quotation Mark
      addConversion(0x201A, "'"); // Single Low-9 Quotation Mark
      addConversion(0x201C, "\""); // Left Double Quotation Mark
      addConversion(0x201D, "\""); // Right Double Quotation Mark
      addConversion(0x201E, "\""); // Double Low-9 Quotation Mark
    }
  } // end of loadConfig() method


/*
  prettyPlural() method

  Return a string that formats a number and appends a lowercase "s" to a word
  if the number is plural (not one).  Also provide a more general method that
  accepts both a singular word and a plural word.
*/
  static String prettyPlural(
    long number,                  // number to be formatted
    String singular)              // singular word
  {
    return(prettyPlural(number, singular, (singular + "s")));
  }

  static String prettyPlural(
    long number,                  // number to be formatted
    String singular,              // singular word
    String plural)                // plural word
  {
    final String[] names = {"zero", "one", "two"};
                                  // names for small counting numbers
    String result;                // our converted result

    if ((number >= 0) && (number < names.length))
      result = names[(int) number]; // use names for small counting numbers
    else
      result = formatComma.format(number); // format number with digit grouping

    if (number == 1)              // is the number singular or plural?
      result += " " + singular;   // append singular word
    else
      result += " " + plural;     // append plural word

    return(result);               // give caller our converted string

  } // end of prettyPlural() method


/*
  putError() method

  Append a complete line of text to the end of the error text area.  We add a
  newline character, not the caller.  In this program, it does not matter if
  the text area automatically scrolls to the end or stays at the beginning.
*/
  static void putError(String text)
  {
    if (errorText.getText().length() > 0) // if the text area is not empty
      errorText.append("\n");     // separate this line from previous text
    errorText.append(text);       // put caller's line into text area

    if (splitPanel.getDividerLocation() // are we hiding error text area?
      >= splitPanel.getMaximumDividerLocation())
    {
      splitPanel.setDividerLocation(SPLIT_DIVIDER); // show error text area
    }
  } // end of putError() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("This is a graphical application.  You may give options on the command line:");
    System.err.println();
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -d# = text file with configuration data; default is -d\"" + DEFAULT_FILE + "\"");
    System.err.println("  -j0 = disable Java backslash notation in configuration file (default)");
    System.err.println("  -j1 = -j = enable Java backslash notation in configuration data file");
    System.err.println("  -m0 = disable XML character references in configuration file (default)");
    System.err.println("  -m1 = -m = enable XML character references in configuration data file");
    System.err.println("  -r0 = scroll, do not wrap text lines in main dialog box");
    System.err.println("  -r1 = wrap text lines at arbitrary character boundaries");
    System.err.println("  -r2 = wrap text at word boundaries when possible (default)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main PlainText3 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == convertButton)  // "Convert" button
    {
      doConvertButton();          // convert from Unicode to plain text
    }
    else if (source == copyButton) // "Copy" button
    {
      doCopyButton();             // trivial, but call someone else to do this
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == multiButton) // multiple button: Paste + Convert + Copy
    {
      pasteButton.doClick();      // pretend user clicked "Paste" button
      convertButton.doClick();    // pretend user clicked "Convert" button
      copyButton.doClick();       // pretend user clicked "Copy" button
      statusDialog.setText(convertText); // restore "Convert" message text
    }
    else if (source == pasteButton) // "Paste" button
    {
      doPasteButton();            // trivial, but call someone else to do this
    }
    else if (source == wrapDialog) // controls text line scrolling or wrapping
    {
      wrapIndex = wrapDialog.getSelectedIndex(); // index into choice list
      outputText.setLineWrap(wrapIndex > 0); // if we allow text lines to wrap
      outputText.setWrapStyleWord(wrapIndex > 1); // wrap at word boundaries
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Unknown ActionEvent in userButton() method: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  utfAppend32() method

  Append one UTF-32 character to a standard Java string buffer (UTF-16).  A
  StringBuffer object maintains its own length, so we don't need to tell the
  caller whether we use one or two "code units" (surrogate pair).
*/
  static void utfAppend32(
    StringBuffer buffer,          // standard Java string buffer (UTF-16)
    int ch)                       // Unicode character number (code point)
  {
    int offset;                   // offset from start of extended Unicode

    if ((ch < 0) || (ch > UTF32_MAX)
      || ((ch >= UTF16_HIGH_BEGIN) && (ch <= UTF16_HIGH_END))
      || ((ch >= UTF16_LOW_BEGIN) && (ch <= UTF16_LOW_END)))
    {
      buffer.append(REPLACE_CHAR); // not a valid Unicode character number
      System.err.println(
        "Invalid UTF-32 character number in utfAppend32() method: 0x"
        + Integer.toHexString(ch));
    }
    else if (ch <= UTF16_MAX)     // standard 16-bit Unicode character (BMP)
    {
      buffer.append((char) ch);   // no conversion necessary
    }
    else                          // extended Unicode character
    {                             // create high, low surrogate pair
      offset = ch - UTF32_OFFSET; // offset from start of extended Unicode
      buffer.append((char) (UTF16_HIGH_BEGIN + (offset >> UTF16_SHIFT)));
      buffer.append((char) (UTF16_LOW_BEGIN + (offset & UTF16_MASK)));
    }
  } // end of utfAppend32() method


/*
  utfCreate16() method

  Create a standard Java string (UTF-16) from an array of UTF-32 character
  numbers (code points).
*/
  static String utfCreate16(
    int[] array)                  // UTF-32 character numbers (code points)
  {
    StringBuffer buffer;          // temporary to collect UTF-16 characters
    int i;                        // index variable

    buffer = new StringBuffer();  // convert size varies UTF-32 to UTF-16
    for (i = 0; i < array.length; i ++) // for each UTF-32 integer given
    {
      utfAppend32(buffer, array[i]); // append UTF-32 to buffer in UTF-16
    }
    return(buffer.toString());    // give caller our converted string

  } // end of utfCreate16() method


/*
  utfParse16() method

  Convert a standard Java string (UTF-16 encoded) to an array of UTF-32
  character numbers.  As an option, parse XML character references.  The
  standard five character entity references are supported:

      &amp;   &   ampersand (U+0026)
      &apos;  '   apostrophe (U+0027)
      &gt;    >   greater-than sign (U+003E)
      &lt;    <   less-than sign (U+003C)
      &quot;  "   quotation mark (U+0022)

  Numeric XML character references are supported in decimal and hexadecimal:
  &#8225; and &#x2021; are both the double dagger symbol.  See the following
  web page:

      http://www.w3.org/TR/REC-xml/#sec-references

  The input string is usually in plain text (ASCII), but this is not assumed or
  required.  Invalid character references are ignored (copied as regular text).
*/
  static int[] utfParse16(
    String input,                 // standard Java string in UTF-16
    boolean jbnFlag,              // true if we parse Java backslash notation
    boolean xmlFlag)              // true if we parse XML character references
  {
    char ch;                      // one character from input string
    int[] copyArray;              // temporary for UTF-32 character numbers
    int copyCount;                // number of UTF-32 characters in array
    int digits;                   // number of decimal/hex/octal digits found
    boolean hexFlag;              // true if hexadecimal number, false decimal
    int i;                        // index variable
    int inputChar;                // current input or parsed character
    int inputLength;              // number of characters in UTF-16 input
    int inputNext;                // index of next UTF-16 input character
    int inputUsed;                // number of input characters consumed
    int[] result;                 // truncated (optimal size) UTF-32 array
    int savedChar;                // high surrogate in pair
    int value;                    // current numeric value during parsing

    inputLength = input.length(); // first, get size of UTF-16 input string
    copyArray = new int[inputLength]; // always big enough for UTF-32 result
    copyCount = 0;                // UTF-32 character array is empty
    inputNext = 0;                // start at beginning of input string
    savedChar = -1;               // no pending high surrogate in pair

    while (inputNext < inputLength) // do the whole input string
    {
      inputChar = input.charAt(inputNext); // get UTF-16 character from input
      inputUsed = 1;              // assume exactly one character consumed

      /* The sections below parse other notations that represent characters.
      They must either (1) do nothing and accept the defaults above, or (2)
      put exactly one character in <inputChar> and consume one or more input
      characters by setting <inputUsed> to a positive value.  There should be
      no conflicts between the syntax of each notation. */

      /* XML character references */

      if ((inputChar == '&') && xmlFlag) // start of XML character reference?
      {
        if (input.startsWith("&amp;", inputNext))
        {
          inputChar = '&';        // ampersand
          inputUsed = 5;
        }
        else if (input.startsWith("&apos;", inputNext))
        {
          inputChar = '\'';       // apostrophe
          inputUsed = 6;
        }
        else if (input.startsWith("&gt;", inputNext))
        {
          inputChar = '>';        // greater than
          inputUsed = 4;
        }
        else if (input.startsWith("&lt;", inputNext))
        {
          inputChar = '<';        // less than
          inputUsed = 4;
        }
        else if (input.startsWith("&quot;", inputNext))
        {
          inputChar = '"';        // quotation mark
          inputUsed = 6;
        }
        else if (input.startsWith("&#", inputNext)) // numeric char reference?
        {
          digits = 0;             // no digits found yet
          hexFlag = false;        // assume number is in decimal
          i = inputNext + 2;      // index of first numeric digit
          value = 0;              // start with zero for numeric value
          while ((digits >= 0) && (i < inputLength) && (value <= UTF32_MAX))
                                  // loop through chars in numeric reference
          {
            ch = input.charAt(i ++); // get one character from input
            if ((digits == 0) && (!hexFlag) && ((ch == 'X') || (ch == 'x')))
            {
              hexFlag = true;     // now parsing in hexadecimal
            }
            else if ((!hexFlag) && (ch >= '0') && (ch <= '9')) // decimal?
            {
              digits ++;          // one more digit found
              value = (value * 10) + (ch - '0'); // add to decimal value
            }
            else if (hexFlag && (ch >= '0') && (ch <= '9')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - '0'); // add to hex value
            }
            else if (hexFlag && (ch >= 'A') && (ch <= 'F')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - 'A' + 10); // add to hex value
            }
            else if (hexFlag && (ch >= 'a') && (ch <= 'f')) // hex digit?
            {
              digits ++;          // one more digit found
              value = (value << 4) + (ch - 'a' + 10); // add to hex value
            }
            else if ((digits > 0) && (ch == ';')) // end of numeric reference?
            {
              digits = -1;        // use <digits> to flag successful finish
            }
            else                  // not an acceptable character
              break;              // exit early from <while> loop
          }

          /* The number of digits gets set to negative if we properly finished
          the character reference. */

          if ((digits < 0) && (value >= 0) && (value <= UTF32_MAX))
          {
            inputChar = value;    // yes, save numeric reference
            inputUsed = i - inputNext;
          }
          else                    // bad numeric syntax or illegal value
            putError("Invalid XML numeric character reference: "
              + utfParseSubstring(input, inputNext));
        }
        else                      // "&" followed by something unknown
          putError("Unknown XML character entity reference: "
            + utfParseSubstring(input, inputNext));
      }

      /* Java backslash notation.  There are a confusing number of backslashes,
      because this is Java source code representing Java source code. */

      else if ((inputChar == '\\') && jbnFlag) // start of Java backslash?
      {
        if (input.startsWith("\\\"", inputNext))
        {
          inputChar = '"';        // Java double quote
          inputUsed = 2;
        }
        else if (input.startsWith("\\'", inputNext))
        {
          inputChar = '\'';       // Java single quote
          inputUsed = 2;
        }
        else if (input.startsWith("\\\\", inputNext))
        {
          inputChar = '\\';       // Java backslash
          inputUsed = 2;
        }
        else if (input.startsWith("\\b", inputNext))
        {
          inputChar = '\b';       // Java backspace
          inputUsed = 2;
        }
        else if (input.startsWith("\\f", inputNext))
        {
          inputChar = '\f';       // Java form feed
          inputUsed = 2;
        }
        else if (input.startsWith("\\n", inputNext))
        {
          inputChar = '\n';       // Java newline
          inputUsed = 2;
        }
        else if (input.startsWith("\\r", inputNext))
        {
          inputChar = '\r';       // Java carriage return
          inputUsed = 2;
        }
        else if (input.startsWith("\\t", inputNext))
        {
          inputChar = '\t';       // Java horizontal tab
          inputUsed = 2;
        }
        else if (input.startsWith("\\uu", inputNext))
        {
          inputChar = '\\';       // reduce by one "u" (Java standard)
          inputUsed = 2;
        }
        else if (input.startsWith("\\u", inputNext)) // Unicode hex number
        {
          /* Hexadecimal character number from 0000 to FFFF, always with
          exactly four hex digits. */

          digits = 0;             // no digits found yet
          i = inputNext + 2;      // index of first numeric digit
          value = 0;              // start with zero for numeric value
          while ((digits < 4) && (i < inputLength))
          {
            ch = input.charAt(i ++); // get one character from input
            if ((ch >= '0') && (ch <= '9')) // hex digit?
              value = (value << 4) + (ch - '0'); // add to hex value
            else if ((ch >= 'A') && (ch <= 'F')) // hex letter?
              value = (value << 4) + (ch - 'A' + 10);
            else if ((ch >= 'a') && (ch <= 'f')) // hex letter?
              value = (value << 4) + (ch - 'a' + 10);
            else                  // not a valid hex digit
              break;              // exit early from <while> loop
            digits ++;            // one more digit found
          }

          if (digits == 4)        // accept any four-digit hex number
          {
            inputChar = value;    // hex number becomes input character
            inputUsed = digits + 2; // consume backslash, "u", hex digits
          }
          else                    // invalid digits, not enough digits, etc
            putError("Invalid Java backslash hexadecimal number: "
              + utfParseSubstring(input, inputNext));
        }
        else
        {
          /* Could be an octal character number from 0 to 377 (base 8), which
          is 0 to 255 decimal.  Could be garbage.  Or both.  Octal is old. */

          digits = 0;             // no digits found yet
          i = inputNext + 1;      // index of first numeric digit
          value = 0;              // start with zero for numeric value
          while ((digits < 3) && (i < inputLength))
          {
            ch = input.charAt(i ++); // get one character from input
            if ((ch >= '0') && (ch <= '7'))
              value = (value << 3) + (ch - '0'); // add to octal value
            else                  // not a valid octal digit
              break;              // exit early from <while> loop
            digits ++;            // one more digit found
          }

          if ((digits > 0) && (value <= 0xFF)) // limit to 377 octal
          {
            inputChar = value;    // octal number becomes input character
            inputUsed = digits + 1; // consume backslash and octal digits
          }
          else                    // generic catch-all error message
            putError("Unknown Java backslash notation: "
              + utfParseSubstring(input, inputNext));
        }
      }

      /* There must now be a valid Unicode character number in <inputChar>.
      This character may be the second part of a surrogate pair.  We allow
      surrogate pairs to appear as Unicode text, to be generated by Java
      backslash notation and XML character references, or a mixture. */

      if (savedChar > 0)          // is there a pending high surrogate?
      {
        /* Do we have a complete UTF-16 surrogate pair? */

        if ((inputChar >= UTF16_LOW_BEGIN) && (inputChar <= UTF16_LOW_END))
        {
          copyArray[copyCount ++] = UTF32_OFFSET
            + ((savedChar - UTF16_HIGH_BEGIN) << UTF16_SHIFT)
            + (inputChar - UTF16_LOW_BEGIN);
          inputChar = savedChar = -1; // consume both characters
        }
        else                      // high surrogate followed by anything else
        {
          copyArray[copyCount ++] = REPLACE_CHAR; // unpaired high surrogate
          putError("Unpaired UTF-16 high surrogate: "
            + formatUnicodeNumber(savedChar) + " followed by "
            + formatUnicodeNumber(inputChar));
          savedChar = -1;         // consume only saved character
        }
      }

      /* We have an input character that could be the start of a new surrogate
      pair, or could be normal text to be returned to the caller. */

      if (inputChar < 0)          // was this input consumed above as pair?
        { /* do nothing */ }
      else if ((inputChar >= UTF16_HIGH_BEGIN) && (inputChar <= UTF16_HIGH_END))
      {
        savedChar = inputChar;    // should be start of new surrogate pair
      }
      else if ((inputChar >= UTF16_LOW_BEGIN) && (inputChar <= UTF16_LOW_END))
      {
        copyArray[copyCount ++] = REPLACE_CHAR; // unpaired low surrogate
        putError("Unpaired UTF-16 low surrogate: "
          + formatUnicodeNumber(inputChar));
      }
      else                        // long hard road to find real character
      {
        copyArray[copyCount ++] = inputChar; // copy one character as-is
      }

      /* Advance to the next position in the UTF-16 input string. */

      inputNext += inputUsed;     // index of next input character
    }

    /* Do we have an unpaired high surrogate at the end of the input text? */

    if (savedChar > 0)
    {
      copyArray[copyCount ++] = REPLACE_CHAR; // unpaired high surrogate
      putError("Unpaired UTF-16 high surrogate: "
        + formatUnicodeNumber(savedChar) + " at end of input");
    }

    /* Often the size of the UTF-16 input string is equal to the number of
    UTF-32 characters we put in the array.  Sometimes not all of the temporary
    array is used, and we need to truncate that array by copying it to another
    smaller array. */

    if (copyCount == inputLength) // same size for array and input string?
      result = copyArray;         // yes, return temporary array as result
    else                          // need to create a smaller array
    {
      result = new int[copyCount]; // required size for resulting array
      for (i = 0; i < copyCount; i ++) // copy only those characters used
        result[i] = copyArray[i];
    }
    return(result);               // give caller UTF-32 character numbers

  } // end of utfParse16() method


/*
  utfParseSubstring() method

  Return a substring that goes to the end of a string, has a maximum length, or
  stops before a control character (newline).
*/
  static String utfParseSubstring(
    String input,                 // standard Java UTF-16 string
    int offset)                   // starting offset where we look
  {
    int i = offset;               // index variable
    int length = input.length();  // number of characters in input string
    int limit = offset + 25;      // arbitrary maximum size returned

    while ((i < length) && (i < limit)
      && (Character.isISOControl(input.charAt(i)) == false))
    {
      i ++;                       // this character is likely printable
    }
    return(input.substring(offset, i));

  } // end of utfParseSubstring() method

} // end of PlainText3 class

// ------------------------------------------------------------------------- //

/*
  PlainText3Data class

  Each possible action by the user (accept, delete, replace) requires more
  information than can be cleanly represented by arrays or pre-defined Java
  objects (lists, maps, vectors, etc).
*/

class PlainText3Data
{
  /* class variables */

  int action;                     // accept, delete, replace action code
  boolean isRange;                // true if <left> is a low-high range
  int[] left;                     // if <isRange>, then two limits: low, high
                                  // else non-empty array of UTF-32 characters
  String right;                   // standard Java UTF-16 string or null
  PlainText3Data next;            // next data object in linked list

  /* constructor */

  public PlainText3Data()
  {
    this.action = 0;              // no action code yet
    this.isRange = false;         // no low-high range defined
    this.left = null;             // no left array or range yet
    this.right = null;            // no replacement string
    this.next = null;             // no following element in list
  }

} // end of PlainText3Data class

// ------------------------------------------------------------------------- //

/*
  PlainText3User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class PlainText3User implements ActionListener
{
  /* empty constructor */

  public PlainText3User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    PlainText3.userButton(event);
  }

} // end of PlainText3User class

/* Copyright (c) 2019 by Keith Fenske.  Apache License or GNU GPL. */
