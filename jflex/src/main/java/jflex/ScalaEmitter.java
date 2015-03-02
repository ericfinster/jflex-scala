package jflex;

import java.io.*;
import java.util.Map;

/**
 * Created by strubell on 3/2/15.
 */
public class ScalaEmitter extends Emitter {

  public ScalaEmitter(File inputFile, LexParse parser, DFA dfa) throws IOException {

    String name = getBaseName(parser.scanner.className) + ".scala";

    File outputFile = normalize(name, inputFile);

    Out.println("Writing code to \""+outputFile+"\"");

    this.out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
    this.parser = parser;
    this.scanner = parser.scanner;
    this.inputFile = inputFile;
    this.dfa = dfa;
    this.skel = new Skeleton(out);
  }

    protected void emitLookBuffer() {
        if (!hasGenLookAhead()) return;

        println("  /** For the backwards DFA of general lookahead statements */");
        println("  private boolean [] zzFin = new boolean [ZZ_BUFFERSIZE+1];");
        println();
    }

    protected void emitScanError() {
        print("  private void zzScanError(int errorCode)");

        if (scanner.scanErrorException != null)
            print(" throws "+scanner.scanErrorException);

        println(" {");

        skel.emitNext();

        if (scanner.scanErrorException == null)
            println("    throw new Error(message);");
        else
            println("    throw new "+scanner.scanErrorException+"(message);");

        skel.emitNext();

        print(" void yypushback(int number) ");

        if (scanner.scanErrorException == null)
            println(" {");
        else
            println(" throws "+scanner.scanErrorException+" {");
    }

    protected void emitMain() {
        if ( !(scanner.standalone || scanner.debugOption || scanner.cupDebug) ) return;

        if ( scanner.cupDebug ) {
            println("  /**");
            println("   * Converts an int token code into the name of the");
            println("   * token by reflection on the cup symbol class/interface "+scanner.cupSymbol);
            println("   *");
            println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>");
            println("   */");
            println("  private String getTokenName(int token) {");
            println("    try {");
            println("      java.lang.reflect.Field [] classFields = " + scanner.cupSymbol + ".class.getFields();");
            println("      for (int i = 0; i < classFields.length; i++) {");
            println("        if (classFields[i].getInt(null) == token) {");
            println("          return classFields[i].getName();");
            println("        }");
            println("      }");
            println("    } catch (Exception e) {");
            println("      e.printStackTrace(System.err);");
            println("    }");
            println("");
            println("    return \"UNKNOWN TOKEN\";");
            println("  }");
            println("");
            println("  /**");
            println("   * Same as "+scanner.functionName+" but also prints the token to standard out");
            println("   * for debugging.");
            println("   *");
            println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>");
            println("   */");

            if ( scanner.tokenType == null ) {
                if ( scanner.isInteger )
                    print( "int" );
                else
                if ( scanner.isIntWrap )
                    print( "Integer" );
                else
                    print( "Yytoken" );
            }
            else
                print( scanner.tokenType );

            print(" debug_");

            print(scanner.functionName);

            print("() throws java.io.IOException");

            if ( scanner.lexThrow != null ) {
                print(", ");
                print(scanner.lexThrow);
            }

            if ( scanner.scanErrorException != null ) {
                print(", ");
                print(scanner.scanErrorException);
            }

            println(" {");

            println("    "+scanner.tokenType+" s = "+scanner.functionName+"();");
            print("    System.out.println( ");
            if (scanner.lineCount) print("\"line:\" + (yyline+1) + ");
            if (scanner.columnCount) print("\" col:\" + (yycolumn+1) + ");
            println("\" --\"+ yytext() + \"--\" + getTokenName(s.sym) + \"--\");");
            println("    return s;");
            println("  }");
            println("");
        }

        if ( scanner.standalone ) {
            println("  /**");
            println("   * Runs the scanner on input files.");
            println("   *");
            println("   * This is a standalone scanner, it will print any unmatched");
            println("   * text to System.out unchanged.");
            println("   *");
            println("   * @param argv   the command line, contains the filenames to run");
            println("   *               the scanner on.");
            println("   */");
        }
        else {
            println("  /**");
            println("   * Runs the scanner on input files.");
            println("   *");
            println("   * This main method is the debugging routine for the scanner.");
            println("   * It prints debugging information about each returned token to");
            println("   * System.out until the end of file is reached, or an error occured.");
            println("   *");
            println("   * @param argv   the command line, contains the filenames to run");
            println("   *               the scanner on.");
            println("   */");
        }

        String className = getBaseName(scanner.className);

        println("  public static void main(String argv[]) {");
        println("    if (argv.length == 0) {");
        println("      System.out.println(\"Usage : java "+className+" [ --encoding <name> ] <inputfile(s)>\");");
        println("    }");
        println("    else {");
        println("      int firstFilePos = 0;");
        println("      String encodingName = \"UTF-8\";");
        println("      if (argv[0].equals(\"--encoding\")) {");
        println("        firstFilePos = 2;");
        println("        encodingName = argv[1];");
        println("        try {");
        println("          java.nio.charset.Charset.forName(encodingName); // Side-effect: is encodingName valid? ");
        println("        } catch (Exception e) {");
        println("          System.out.println(\"Invalid encoding '\" + encodingName + \"'\");");
        println("          return;");
        println("        }");
        println("      }");
        println("      for (int i = firstFilePos; i < argv.length; i++) {");
        println("        "+className+" scanner = null;");
        println("        try {");
        println("          java.io.FileInputStream stream = new java.io.FileInputStream(argv[i]);");
        println("          java.io.Reader reader = new java.io.InputStreamReader(stream, encodingName);");
        println("          scanner = new "+className+"(reader);");
        if ( scanner.standalone ) {
            println("          while ( !scanner.zzAtEOF ) scanner."+scanner.functionName+"();");
        }
        else if (scanner.cupDebug ) {
            println("          while ( !scanner.zzAtEOF ) scanner.debug_"+scanner.functionName+"();");
        }
        else {
            println("          do {");
            println("            System.out.println(scanner."+scanner.functionName+"());");
            println("          } while (!scanner.zzAtEOF);");
            println("");
        }

        println("        }");
        println("        catch (java.io.FileNotFoundException e) {");
        println("          System.out.println(\"File not found : \\\"\"+argv[i]+\"\\\"\");");
        println("        }");
        println("        catch (java.io.IOException e) {");
        println("          System.out.println(\"IO error scanning file \\\"\"+argv[i]+\"\\\"\");");
        println("          System.out.println(e);");
        println("        }");
        println("        catch (Exception e) {");
        println("          System.out.println(\"Unexpected exception:\");");
        println("          e.printStackTrace();");
        println("        }");
        println("      }");
        println("    }");
        println("  }");
        println("");
    }

    protected void emitNoMatch() {
        println("            zzScanError(ZZ_NO_MATCH);");
    }

    protected void emitNextInput() {
        println("          if (zzCurrentPosL < zzEndReadL) {");
        println("            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);");
        println("            zzCurrentPosL += Character.charCount(zzInput);");
        println("          }");
        println("          else if (zzAtEOF) {");
        println("            zzInput = YYEOF;");
        println("            break zzForAction;");
        println("          }");
        println("          else {");
        println("            // store back cached positions");
        println("            zzCurrentPos  = zzCurrentPosL;");
        println("            zzMarkedPos   = zzMarkedPosL;");
        println("            boolean eof = zzRefill();");
        println("            // get translated positions and possibly new buffer");
        println("            zzCurrentPosL  = zzCurrentPos;");
        println("            zzMarkedPosL   = zzMarkedPos;");
        println("            zzBufferL      = zzBuffer;");
        println("            zzEndReadL     = zzEndRead;");
        println("            if (eof) {");
        println("              zzInput = YYEOF;");
        println("              break zzForAction;");
        println("            }");
        println("            else {");
        println("              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL);");
        println("              zzCurrentPosL += Character.charCount(zzInput);");
        println("            }");
        println("          }");
    }

    protected void emitUserCode() {
        if ( scanner.userCode.length() > 0 )
            println(scanner.userCode.toString());

        if (scanner.cup2Compatible) {
            println();
            println("/* CUP2 imports */");
            println("import edu.tum.cup2.scanner.*;");
            println("import edu.tum.cup2.grammar.*;");
            println();
        }
    }

    protected void emitClassName() {
        if (!endsWithJavadoc(scanner.userCode)) {
            String path = inputFile.toString();
            // slashify path (avoid backslash u sequence = unicode escape)
            if (File.separatorChar != '/') {
                path = path.replace(File.separatorChar, '/');
            }

            println("/**");
            println(" * This class is a scanner generated by ");
            println(" * <a href=\"http://www.jflex.de/\">JFlex</a> "+Main.version);
            println(" * from the specification file <tt>"+path+"</tt>");
            println(" */");
        }

        if ( scanner.isPublic ) print("public ");

        if ( scanner.isAbstract) print("abstract ");

        if ( scanner.isFinal ) print("final ");

        print("class ");
        print(scanner.className);

        if ( scanner.isExtending != null ) {
            print(" extends ");
            print(scanner.isExtending);
        }

        if ( scanner.isImplementing != null ) {
            print(" implements ");
            print(scanner.isImplementing);
        }

        println(" {");
    }

    protected void emitLexicalStates() {
        for (String name : scanner.states.names()) {
            int num = scanner.states.getNumber(name);

            println(" static final int "+name+" = "+2*num+";");
        }

        // can't quite get rid of the indirection, even for non-bol lex states:
        // their DFA states might be the same, but their EOF actions might be different
        // (see bug #1540228)
        println("");
        println("  /**");
        println("   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l");
        println("   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l");
        println("   *                  at the beginning of a line");
        println("   * l is of the form l = 2*k, k a non negative integer");
        println("   */");
        println("  private static final int ZZ_LEXSTATE[] = { ");

        int i, j = 0;
        print("    ");

        for (i = 0; i < 2*dfa.numLexStates-1; i++) {
            print( dfa.entryState[i], 2 );

            print(", ");

            if (++j >= 16) {
                println();
                print("    ");
                j = 0;
            }
        }

        println( dfa.entryState[i] );
        println("  };");
    }


    protected void emitCharMapInitFunction(int packedCharMapPairs) {

        CharClasses cl = parser.getCharClasses();

        if ( cl.getMaxCharCode() < 256 ) return;

        println("");
        println("  /** ");
        println("   * Unpacks the compressed character translation table.");
        println("   *");
        println("   * @param packed   the packed character translation table");
        println("   * @return         the unpacked character translation table");
        println("   */");
        println("  private static char [] zzUnpackCMap(String packed) {");
        println("    char [] map = new char[0x" + Integer.toHexString(cl.getMaxCharCode() + 1) + "];");
        println("    int i = 0;  /* index in packed string  */");
        println("    int j = 0;  /* index in unpacked array */");
        println("    while (i < " + 2 * packedCharMapPairs + ") {");
        println("      int  count = packed.charAt(i++);");
        println("      char value = packed.charAt(i++);");
        println("      do map[j++] = value; while (--count > 0);");
        println("    }");
        println("    return map;");
        println("  }");
    }

    protected void emitCharMapArrayUnPacked() {

        CharClasses cl = parser.getCharClasses();

        println("");
        println("  /** ");
        println("   * Translates characters to character classes");
        println("   */");
        println("  private static final char [] ZZ_CMAP = {");

        int n = 0;  // numbers of entries in current line
        print("    ");

        int max =  cl.getMaxCharCode();

        // not very efficient, but good enough for <= 255 characters
        for (char c = 0; c <= max; c++) {
            print(colMap[cl.getClassCode(c)],2);

            if (c < max) {
                print(", ");
                if ( ++n >= 16 ) {
                    println();
                    print("    ");
                    n = 0;
                }
            }
        }

        println();
        println("  };");
        println();
    }

    /**
     * Returns the number of elements in the packed char map
     * array, or zero if the char map array will be not be packed.
     *
     * This will be more than intervals.length if the count
     * for any of the values is more than 0xFFFF, since
     * the number of char map array entries per value is
     * ceil(count / 0xFFFF)
     */
    protected int emitCharMapArray() {
        CharClasses cl = parser.getCharClasses();

        if ( cl.getMaxCharCode() < 256 ) {
            emitCharMapArrayUnPacked();
            return 0; // the char map array will not be packed
        }

        // ignores cl.getMaxCharCode(), emits all intervals instead

        intervals = cl.getIntervals();

        println("");
        println("  /** ");
        println("   * Translates characters to character classes");
        println("   */");
        println("  private static final String ZZ_CMAP_PACKED = ");

        int n = 0;  // numbers of entries in current line
        print("    \"");

        int i = 0, numPairs = 0;
        int count, value;
        while ( i < intervals.length ) {
            count = intervals[i].end-intervals[i].start+1;
            value = colMap[intervals[i].charClass];

            // count could be >= 0x10000
            while (count > 0xFFFF) {
                printUC(0xFFFF);
                printUC(value);
                count -= 0xFFFF;
                numPairs++;
                n++;
            }
            numPairs++;
            printUC(count);
            printUC(value);

            if (i < intervals.length-1) {
                if ( ++n >= 10 ) {
                    println("\"+");
                    print("    \"");
                    n = 0;
                }
            }

            i++;
        }

        println("\";");
        println();

        println("  /** ");
        println("   * Translates characters to character classes");
        println("   */");
        println("  private static final char [] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);");
        println();
        return numPairs;
    }

  protected void emitClassCode() {
        if ( scanner.classCode != null ) {
            println("  /* user code: */");
            println(scanner.classCode);
        }

        if (scanner.cup2Compatible) {
            // convenience methods for CUP2
            println();
            println("  /* CUP2 code: */");
            println("  private <T> ScannerToken<T> token(Terminal terminal, T value) {");
            println("    return new ScannerToken<T>(terminal, value, yyline, yycolumn);");
            println("  }");
            println();
            println("  private ScannerToken<Object> token(Terminal terminal) {");
            println("    return new ScannerToken<Object>(terminal, yyline, yycolumn);");
            println("  }");
            println();
        }
    }


  protected void emitConstructorDecl() {
        emitConstructorDecl(true);

        if ((scanner.standalone || scanner.debugOption) &&
                scanner.ctorArgs.size() > 0) {
            Out.warning(ErrorMessages.get(ErrorMessages.CTOR_DEBUG));
            println();
            emitConstructorDecl(false);
        }
    }

  protected void emitConstructorDecl(boolean printCtorArgs) {
        println("  /**");
        println("   * Creates a new scanner");
        if (scanner.emitInputStreamCtor) {
            println("   * There is also a java.io.InputStream version of this constructor.");
        }
        println("   *");
        println("   * @param   in  the java.io.Reader to read input from.");
        println("   */");

        String warn =
                "// WARNING: this is a default constructor for " +
                        "debug/standalone only. Has no custom parameters or init code.";

        if (!printCtorArgs) println(warn);

        print("  ");

        if ( scanner.isPublic ) print("public ");
        print( getBaseName(scanner.className) );
        print("(java.io.Reader in");
        if (printCtorArgs) emitCtorArgs();
        print(")");

        if ( scanner.initThrow != null && printCtorArgs) {
            print(" throws ");
            print( scanner.initThrow );
        }

        println(" {");

        if ( scanner.initCode != null && printCtorArgs) {
            print("  ");
            print( scanner.initCode );
        }

        println("    this.zzReader = in;");

        println("  }");
        println();


        if (scanner.emitInputStreamCtor) {
            Out.warning(ErrorMessages.EMITTING_INPUTSTREAM_CTOR, -1);
            println("  /**");
            println("   * Creates a new scanner.");
            println("   * There is also java.io.Reader version of this constructor.");
            println("   *");
            println("   * @param   in  the java.io.Inputstream to read input from.");
            println("   */");
            if (!printCtorArgs) println(warn);

            print("  ");
            if ( scanner.isPublic ) print("public ");
            print( getBaseName(scanner.className) );
            print("(java.io.InputStream in");
            if (printCtorArgs) emitCtorArgs();
            print(")");

            if ( scanner.initThrow != null && printCtorArgs ) {
                print(" throws ");
                print( scanner.initThrow );
            }

            println(" {");

            println("    this(new java.io.InputStreamReader");
            print("             (in, java.nio.charset.Charset.forName(\"UTF-8\"))");
            if (printCtorArgs) {
                for (int i=0; i < scanner.ctorArgs.size(); i++) {
                    print(", "+scanner.ctorArgs.get(i));
                }
            }
            println(");");
            println("  }");
        }
    }

  protected void emitCtorArgs() {
        for (int i = 0; i < scanner.ctorArgs.size(); i++) {
            print(", "+scanner.ctorTypes.get(i));
            print(" "+scanner.ctorArgs.get(i));
        }
    }

  protected void emitDoEOF() {
        if ( scanner.eofCode == null ) return;

        println("  /**");
        println("   * Contains user EOF-code, which will be executed exactly once,");
        println("   * when the end of file is reached");
        println("   */");

        print("  private void zzDoEOF()");

        if ( scanner.eofThrow != null ) {
            print(" throws ");
            print(scanner.eofThrow);
        }

        println(" {");

        println("    if (!zzEOFDone) {");
        println("      zzEOFDone = true;");
        println("    "+scanner.eofCode );
        println("    }");
        println("  }");
        println("");
        println("");
    }

  protected void emitLexFunctHeader() {

        if ( scanner.tokenType == null ) {
            if ( scanner.isInteger )
                print( "int" );
            else
            if ( scanner.isIntWrap )
                print( "Integer" );
            else
                print( "Yytoken" );
        }
        else
            print( scanner.tokenType );

        print(" ");

        print(scanner.functionName);

        print("() throws java.io.IOException");

        if ( scanner.lexThrow != null ) {
            print(", ");
            print(scanner.lexThrow);
        }

        if ( scanner.scanErrorException != null ) {
            print(", ");
            print(scanner.scanErrorException);
        }

        println(" {");

        skel.emitNext();

        println("    int [] zzTransL = ZZ_TRANS;");
        println("    int [] zzRowMapL = ZZ_ROWMAP;");
        println("    int [] zzAttrL = ZZ_ATTRIBUTE;");

        skel.emitNext();

        if ( scanner.charCount ) {
            println("      yychar+= zzMarkedPosL-zzStartRead;");
            println("");
        }

        if ( scanner.lineCount || scanner.columnCount ) {
            println("      boolean zzR = false;");
            println("      int zzCh;");
            println("      int zzCharCount;");
            println("      for (zzCurrentPosL = zzStartRead  ;");
            println("           zzCurrentPosL < zzMarkedPosL ;");
            println("           zzCurrentPosL += zzCharCount ) {");
            println("        zzCh = Character.codePointAt(zzBufferL, zzCurrentPosL, zzMarkedPosL);");
            println("        zzCharCount = Character.charCount(zzCh);");
            println("        switch (zzCh) {");
            println("        case '\\u000B':");
            println("        case '\\u000C':");
            println("        case '\\u0085':");
            println("        case '\\u2028':");
            println("        case '\\u2029':");
            if ( scanner.lineCount )
                println("          yyline++;");
            if ( scanner.columnCount )
                println("          yycolumn = 0;");
            println("          zzR = false;");
            println("          break;");
            println("        case '\\r':");
            if ( scanner.lineCount )
                println("          yyline++;");
            if ( scanner.columnCount )
                println("          yycolumn = 0;");
            println("          zzR = true;");
            println("          break;");
            println("        case '\\n':");
            println("          if (zzR)");
            println("            zzR = false;");
            println("          else {");
            if ( scanner.lineCount )
                println("            yyline++;");
            if ( scanner.columnCount )
                println("            yycolumn = 0;");
            println("          }");
            println("          break;");
            println("        default:");
            println("          zzR = false;");
            if ( scanner.columnCount )
                println("          yycolumn += zzCharCount;");
            println("        }");
            println("      }");
            println();

            if ( scanner.lineCount ) {
                println("      if (zzR) {");
                println("        // peek one character ahead if it is \\n (if we have counted one line too much)");
                println("        boolean zzPeek;");
                println("        if (zzMarkedPosL < zzEndReadL)");
                println("          zzPeek = zzBufferL[zzMarkedPosL] == '\\n';");
                println("        else if (zzAtEOF)");
                println("          zzPeek = false;");
                println("        else {");
                println("          boolean eof = zzRefill();");
                println("          zzEndReadL = zzEndRead;");
                println("          zzMarkedPosL = zzMarkedPos;");
                println("          zzBufferL = zzBuffer;");
                println("          if (eof) ");
                println("            zzPeek = false;");
                println("          else ");
                println("            zzPeek = zzBufferL[zzMarkedPosL] == '\\n';");
                println("        }");
                println("        if (zzPeek) yyline--;");
                println("      }");
            }
        }

        if ( scanner.bolUsed ) {
            // zzMarkedPos > zzStartRead <=> last match was not empty
            // if match was empty, last value of zzAtBOL can be used
            // zzStartRead is always >= 0
            println("      if (zzMarkedPosL > zzStartRead) {");
            println("        switch (zzBufferL[zzMarkedPosL-1]) {");
            println("        case '\\n':");
            println("        case '\\u000B':");
            println("        case '\\u000C':");
            println("        case '\\u0085':");
            println("        case '\\u2028':");
            println("        case '\\u2029':");
            println("          zzAtBOL = true;");
            println("          break;");
            println("        case '\\r': ");
            println("          if (zzMarkedPosL < zzEndReadL)");
            println("            zzAtBOL = zzBufferL[zzMarkedPosL] != '\\n';");
            println("          else if (zzAtEOF)");
            println("            zzAtBOL = false;");
            println("          else {");
            println("            boolean eof = zzRefill();");
            println("            zzMarkedPosL = zzMarkedPos;");
            println("            zzEndReadL = zzEndRead;");
            println("            zzBufferL = zzBuffer;");
            println("            if (eof) ");
            println("              zzAtBOL = false;");
            println("            else ");
            println("              zzAtBOL = zzBufferL[zzMarkedPosL] != '\\n';");
            println("          }");
            println("          break;");
            println("        default:");
            println("          zzAtBOL = false;");
            println("        }");
            println("      }");
        }

        skel.emitNext();

        if (scanner.bolUsed) {
            println("      if (zzAtBOL)");
            println("        zzState = ZZ_LEXSTATE[zzLexicalState+1];");
            println("      else");
            println("        zzState = ZZ_LEXSTATE[zzLexicalState];");
            println();
        }
        else {
            println("      zzState = ZZ_LEXSTATE[zzLexicalState];");
            println();
        }

        println("      // set up zzAction for empty match case:");
        println("      int zzAttributes = zzAttrL[zzState];");
        println("      if ( (zzAttributes & 1) == 1 ) {");
        println("        zzAction = zzState;");
        println("      }");
        println();

        skel.emitNext();
    }


  protected void emitGetRowMapNext() {
        println("          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMapL[zzInput] ];");
        println("          if (zzNext == "+DFA.NO_TARGET+") break zzForAction;");
        println("          zzState = zzNext;");
        println();

        println("          zzAttributes = zzAttrL[zzState];");

        println("          if ( (zzAttributes & "+FINAL+") == "+FINAL+" ) {");

        skel.emitNext();

        println("            if ( (zzAttributes & "+NOLOOK+") == "+NOLOOK+" ) break zzForAction;");

        skel.emitNext();
    }

  protected void emitActions() {
        println("        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {");

        int i = actionTable.size()+1;

        for (Map.Entry<Action,Integer> entry : actionTable.entrySet()) {
            Action action = entry.getKey();
            int label = entry.getValue();

            println("          case "+label+": ");

            if (action.lookAhead() == Action.FIXED_BASE) {
                println("            // lookahead expression with fixed base length");
                println("            zzMarkedPos = Character.offsetByCodePoints");
                println("                (zzBufferL, zzStartRead, zzEndRead - zzStartRead, zzStartRead, " + action.getLookLength() + ");");
            }

            if (action.lookAhead() == Action.FIXED_LOOK ||
                    action.lookAhead() == Action.FINITE_CHOICE) {
                println("            // lookahead expression with fixed lookahead length");
                println("            zzMarkedPos = Character.offsetByCodePoints");
                println("                (zzBufferL, zzStartRead, zzEndRead - zzStartRead, zzMarkedPos, -" + action.getLookLength() + ");");
            }

            if (action.lookAhead() == Action.GENERAL_LOOK) {
                println("            // general lookahead, find correct zzMarkedPos");
                println("            { int zzFState = "+dfa.entryState[action.getEntryState()]+";");
                println("              int zzFPos = zzStartRead;");
                println("              if (zzFin.length <= zzBufferL.length) { zzFin = new boolean[zzBufferL.length+1]; }");
                println("              boolean zzFinL[] = zzFin;");
                println("              while (zzFState != -1 && zzFPos < zzMarkedPos) {");
                println("                zzFinL[zzFPos] = ((zzAttrL[zzFState] & 1) == 1);");
                println("                zzInput = Character.codePointAt(zzBufferL, zzFPos, zzMarkedPos);");
                println("                zzFPos += Character.charCount(zzInput);");
                println("                zzFState = zzTransL[ zzRowMapL[zzFState] + zzCMapL[zzInput] ];");
                println("              }");
                println("              if (zzFState != -1) { zzFinL[zzFPos++] = ((zzAttrL[zzFState] & 1) == 1); } ");
                println("              while (zzFPos <= zzMarkedPos) {");
                println("                zzFinL[zzFPos++] = false;");
                println("              }");
                println();
                println("              zzFState = "+dfa.entryState[action.getEntryState()+1]+";");
                println("              zzFPos = zzMarkedPos;");
                println("              while (!zzFinL[zzFPos] || (zzAttrL[zzFState] & 1) != 1) {");
                println("                zzInput = Character.codePointBefore(zzBufferL, zzFPos, zzStartRead);");
                println("                zzFPos -= Character.charCount(zzInput);");
                println("                zzFState = zzTransL[ zzRowMapL[zzFState] + zzCMapL[zzInput] ];");
                println("              };");
                println("              zzMarkedPos = zzFPos;");
                println("            }");
            }

            if ( scanner.debugOption ) {
                print("            System.out.println(");
                if ( scanner.lineCount )
                    print("\"line: \"+(yyline+1)+\" \"+");
                if ( scanner.columnCount )
                    print("\"col: \"+(yycolumn+1)+\" \"+");
                println("\"match: --\"+zzToPrintable(yytext())+\"--\");");
                print("            System.out.println(\"action ["+action.priority+"] { ");
                print(escapify(action.content));
                println(" }\");");
            }

            println("            { "+action.content);
            println("            }");
            println("          case "+(i++)+": break;");
        }
    }

  protected void emitEOFVal() {
        EOFActions eofActions = parser.getEOFActions();

        if ( scanner.eofCode != null )
            println("            zzDoEOF();");

        if ( eofActions.numActions() > 0 ) {
            println("            switch (zzLexicalState) {");

            // pick a start value for break case labels.
            // must be larger than any value of a lex state:
            int last = dfa.numStates;

            for (String name : scanner.states.names()) {
                int num = scanner.states.getNumber(name);
                Action action = eofActions.getAction(num);

                if (action != null) {
                    println("            case "+name+": {");
                    if ( scanner.debugOption ) {
                        print("              System.out.println(");
                        if ( scanner.lineCount )
                            print("\"line: \"+(yyline+1)+\" \"+");
                        if ( scanner.columnCount )
                            print("\"col: \"+(yycolumn+1)+\" \"+");
                        println("\"match: <<EOF>>\");");
                        print("              System.out.println(\"action ["+action.priority+"] { ");
                        print(escapify(action.content));
                        println(" }\");");
                    }
                    println("              "+action.content);
                    println("            }");
                    println("            case "+(++last)+": break;");
                }
            }

            println("            default:");
        }

        Action defaultAction = eofActions.getDefault();

        if (defaultAction != null) {
            println("              {");
            if ( scanner.debugOption ) {
                print("                System.out.println(");
                if ( scanner.lineCount )
                    print("\"line: \"+(yyline+1)+\" \"+");
                if ( scanner.columnCount )
                    print("\"col: \"+(yycolumn+1)+\" \"+");
                println("\"match: <<EOF>>\");");
                print("                System.out.println(\"action ["+defaultAction.priority+"] { ");
                print(escapify(defaultAction.content));
                println(" }\");");
            }
            println("                " + defaultAction.content);
            println("              }");
        }
        else if ( scanner.eofVal != null )
            println("          { " + scanner.eofVal + " }");
        else if ( scanner.isInteger ) {
            if ( scanner.tokenType != null ) {
                Out.error(ErrorMessages.INT_AND_TYPE);
                throw new GeneratorException();
            }
            println("        return YYEOF;");
        }
        else
            println("        return null;");

        if (eofActions.numActions() > 0)
            println("        }");
    }

    /**
     * Set up EOF code section according to scanner.eofcode
     */
    protected void setupEOFCode() {
        if (scanner.eofclose) {
            scanner.eofCode = LexScan.conc(scanner.eofCode, "  yyclose();");
            scanner.eofThrow = LexScan.concExc(scanner.eofThrow, "java.io.IOException");
        }
    }


    /**
     * Main Emitter method.
     */
    public void emit() {

        setupEOFCode();

        if (scanner.functionName == null)
            scanner.functionName = "yylex";

        reduceColumns();
        findActionStates();

        emitHeader();
        emitUserCode();
        emitClassName();

        skel.emitNext();

        println("  private static final int ZZ_BUFFERSIZE = "+scanner.bufferSize+";");

        if (scanner.debugOption) {
            println("  private static final String ZZ_NL = System.getProperty(\"line.separator\");");
        }

        skel.emitNext();

        emitLexicalStates();

        int packedCharMapPairs = emitCharMapArray();

        emitActionTable();

        reduceRows();

        emitRowMapArray();

        emitDynamicInit();

        skel.emitNext();

        emitAttributes();

        skel.emitNext();

        emitLookBuffer();

        emitClassCode();

        skel.emitNext();

        emitConstructorDecl();

        emitCharMapInitFunction(packedCharMapPairs);

        if (scanner.debugOption) {
            println("");
            println("  private static String zzToPrintable(String str) {");
            println("    StringBuilder builder = new StringBuilder();");
            println("    for (int n = 0 ; n < str.length() ; ) {");
            println("      int ch = str.codePointAt(n);");
            println("      int charCount = Character.charCount(ch);");
            println("      n += charCount;");
            println("      if (ch > 31 && ch < 127) {");
            println("        builder.append((char)ch);");
            println("      } else if (charCount == 1) {");
            println("        builder.append(String.format(\"\\\\u%04X\", ch));");
            println("      } else {");
            println("        builder.append(String.format(\"\\\\U%06X\", ch));");
            println("      }");
            println("    }");
            println("    return builder.toString();");
            println("  }");
        }

        skel.emitNext();

        emitScanError();

        skel.emitNext();

        emitDoEOF();

        skel.emitNext();

        emitLexFunctHeader();

        emitNextInput();

        emitGetRowMapNext();

        skel.emitNext();

        emitEOFVal();

        skel.emitNext();

        emitActions();

        skel.emitNext();

        emitNoMatch();

        skel.emitNext();

        emitMain();

        skel.emitNext();

        out.close();
    }
}
