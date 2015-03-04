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

    Out.println("Writing code to \"" + outputFile + "\"");

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
    println("  val zzFin: Array[Boolean] = new Array[Boolean](ZZ_BUFFERSIZE+1)");
    println();
  }

  protected void emitScanError() {
    if (scanner.scanErrorException != null)
      print("  @throws classOf[" + scanner.scanErrorException + "]");

    print("  def zzScanError(errorCode: Int) = {");

    skel.emitNext();

    if (scanner.scanErrorException == null)
      println("    throw new Error(message)");
    else
      println("    throw new " + scanner.scanErrorException + "(message)");

    skel.emitNext();

    if (scanner.scanErrorException != null)
      print(" @throws classOf[" + scanner.scanErrorException + "]");

    print(" def yypushback(number: Int) = {");
  }

  protected void emitMain() {
    if (!(scanner.standalone || scanner.debugOption || scanner.cupDebug)) return;

    if (scanner.cupDebug) {
      println("  /**");
      println("   * Converts an int token code into the name of the");
      println("   * token by reflection on the cup symbol class/interface " + scanner.cupSymbol);
      println("   *");
      println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>");
      println("   */");
      println("  def getTokenName(token: Int): String = {");
      println("    try {");
      println("      val classFields: Array[java.lang.reflect.Field] = " + scanner.cupSymbol + ".class.getFields()");
      println("      for (i <- 0 until classFields.length) {");
      println("        if (classFields(i).getInt(null) == token) {");
      println("          return classFields[i].getName()");
      println("        }");
      println("      }");
      println("    } catch {");
      println("      case e: Exception => e.printStackTrace(System.err)");
      println("    }");
      println("");
      println("    \"UNKNOWN TOKEN\"");
      println("  }");
      println("");
      println("  /**");
      println("   * Same as " + scanner.functionName + " but also prints the token to standard out");
      println("   * for debugging.");
      println("   *");
      println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>");
      println("   */");

      print("@throws classOf[java.io.IOException]");
      if (scanner.scanErrorException != null) {
        print("@throws classOf["+scanner.scanErrorException+"]");
      }
      if (scanner.scanErrorException != null) {
        print("@throws classOf["+scanner.lexThrow+"]");
      }

      print("def debug_" + scanner.functionName + ": ");

      if (scanner.tokenType == null) {
        if (scanner.isInteger)
          print("Int");
        else if (scanner.isIntWrap)
          print("Integer");
        else
          print("Yytoken");
      } else
        print(scanner.tokenType);

      println(" = {");

      println("    val s: " + scanner.tokenType + " = " + scanner.functionName + "()");
      print("    println( ");
      if (scanner.lineCount) print("\"line:\" + (yyline+1) + ");
      if (scanner.columnCount) print("\" col:\" + (yycolumn+1) + ");
      println("\" --\"+ yytext() + \"--\" + getTokenName(s.sym) + \"--\")");
      println("    s");
      println("  }");
      println("");
    }

    if (scanner.standalone) {
      println("  /**");
      println("   * Runs the scanner on input files.");
      println("   *");
      println("   * This is a standalone scanner, it will print any unmatched");
      println("   * text to System.out unchanged.");
      println("   *");
      println("   * @param argv   the command line, contains the filenames to run");
      println("   *               the scanner on.");
      println("   */");
    } else {
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

    println("  def main(args: Array[String]) {");
    println("    if (args.length == 0) {");
    println("      println(\"Usage : java " + className + " [ --encoding <name> ] <inputfile(s)>\");");
    println("    }");
    println("    else {");
    println("      val firstFilePos = 0");
    println("      val encodingName = \"UTF-8\"");
    println("      if (args(0) == \"--encoding\") {");
    println("        firstFilePos = 2");
    println("        encodingName = argv(1)");
    println("        try {");
    println("          java.nio.charset.Charset.forName(encodingName) // Side-effect: is encodingName valid? ");
    println("        } catch{");
    println("            case e: Exception => println(\"Invalid encoding '\" + encodingName + \"'\")");
    println("          return");
    println("        }");
    println("      }");
    println("      for (i <- 0 until args.length) {");
    println("        " + className + " scanner = null");
    println("        try {");
    println("          val stream = new java.io.FileInputStream(argv(i))");
    println("          val reader = new java.io.InputStreamReader(stream, encodingName)");
    println("          scanner = new " + className + "(reader)");
    if (scanner.standalone) {
      println("          while ( !scanner.zzAtEOF ) scanner." + scanner.functionName + "()");
    } else if (scanner.cupDebug) {
      println("          while ( !scanner.zzAtEOF ) scanner.debug_" + scanner.functionName + "()");
    } else {
      println("          do {");
      println("            println(scanner." + scanner.functionName + "())");
      println("          } while (!scanner.zzAtEOF);");
      println("");
    }
    println("        }");
    println("        catch {");
    println("          case e: java.io.FileNotFoundException => println(\"File not found : \\\"\"+argv[i]+\"\\\"\")");
    println("          case e: java.io.IOException =>  println(\"IO error scanning file \\\"\"+argv[i]+\"\\\"\"); println(e)");
    println("          case e: Exception => println(\"Unexpected exception:\"); e.printStackTrace()");
    println("        }");
    println("      }");
    println("    }");
    println("  }");
    println("");
  }

  protected void emitNoMatch() {
    println("            zzScanError(ZZ_NO_MATCH)");
  }

  // TODO these breaks will not work
  protected void emitNextInput() {
    println("          if (zzCurrentPosL < zzEndReadL) {");
    println("            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL)");
    println("            zzCurrentPosL += Character.charCount(zzInput)");
    println("          }");
    println("          else if (zzAtEOF) {");
    println("            zzInput = YYEOF");
    println("            break zzForAction");
    println("          }");
    println("          else {");
    println("            // store back cached positions");
    println("            zzCurrentPos  = zzCurrentPosL");
    println("            zzMarkedPos   = zzMarkedPosL");
    println("            val eof = zzRefill()");
    println("            // get translated positions and possibly new buffer");
    println("            zzCurrentPosL  = zzCurrentPos");
    println("            zzMarkedPosL   = zzMarkedPos");
    println("            zzBufferL      = zzBuffer");
    println("            zzEndReadL     = zzEndRead");
    println("            if (eof) {");
    println("              zzInput = YYEOF");
    println("              break zzForAction");
    println("            }");
    println("            else {");
    println("              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL, zzEndReadL)");
    println("              zzCurrentPosL += Character.charCount(zzInput)");
    println("            }");
    println("          }");
  }

  protected void emitUserCode() {
    if (scanner.userCode.length() > 0)
      println(scanner.userCode.toString());

    if (scanner.cup2Compatible) {
      println();
      println("/* CUP2 imports */");
      println("import edu.tum.cup2.scanner._");
      println("import edu.tum.cup2.grammar._");
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
      println(" * <a href=\"http://www.jflex.de/\">JFlex</a> " + Main.version);
      println(" * from the specification file <tt>" + path + "</tt>");
      println(" */");
    }

    if (scanner.isAbstract) print("abstract ");

    if (scanner.isFinal) print("final ");

    print("class ");
    print(scanner.className);

    if (scanner.isExtending != null) {
      print(" extends ");
      print(scanner.isExtending);
    }

    if (scanner.isImplementing != null) {
      print(" implements ");
      print(scanner.isImplementing);
    }

    println(" {");
  }

  protected void emitLexicalStates() {
    for (String name : scanner.states.names()) {
      int num = scanner.states.getNumber(name);

      println("  final val " + name + " = " + 2 * num);
    }

    // can't quite get rid of the indirection, even for non-bol lex states:
    // their DFA states might be the same, but their EOF actions might be different
    // (see bug #1540228)
    println("");
    println("  /**");
    println("   * ZZ_LEXSTATE(l) is the state in the DFA for the lexical state l");
    println("   * ZZ_LEXSTATE(l+1) is the state in the DFA for the lexical state l");
    println("   *                  at the beginning of a line");
    println("   * l is of the form l = 2*k, k a non negative integer");
    println("   */");
    println("  final val ZZ_LEXSTATE: Array[Int]( ");

    int i, j = 0;
    print("    ");

    for (i = 0; i < 2 * dfa.numLexStates - 1; i++) {
      print(dfa.entryState[i], 2);

      print(", ");

      if (++j >= 16) {
        println();
        print("    ");
        j = 0;
      }
    }

    println(dfa.entryState[i]);
    println("  )");
  }


  protected void emitCharMapInitFunction(int packedCharMapPairs) {

    CharClasses cl = parser.getCharClasses();

    if (cl.getMaxCharCode() < 256) return;

    println("");
    println("  /** ");
    println("   * Unpacks the compressed character translation table.");
    println("   *");
    println("   * @param packed   the packed character translation table");
    println("   * @return         the unpacked character translation table");
    println("   */");
    println("  def zzUnpackCMap(packed: String): Array[Char] = {");
    println("    val map = new Array[Char](0x" + Integer.toHexString(cl.getMaxCharCode() + 1) + ")");
    println("    var i = 0  /* index in packed string  */");
    println("    var j = 0  /* index in unpacked array */");
    println("    while (i < " + 2 * packedCharMapPairs + ") {");
    println("      val count = packed.charAt(i); i++");
    println("      val value = packed.charAt(i); i++");
    println("      map(j) = value; j++; count -= 1");
    println("      while(count > 0){");
    println("        map(j) = value; j++; count -= 1"); // todo this -- wont work
    println("    }");
    println("    map");
    println("  }");
  }

  protected void emitCharMapArrayUnPacked() {

    CharClasses cl = parser.getCharClasses();

    println("");
    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    println("  final val ZZ_CMAP: Array[Char](");

    int n = 0;  // numbers of entries in current line
    print("    ");

    int max = cl.getMaxCharCode();

    // not very efficient, but good enough for <= 255 characters
    for (char c = 0; c <= max; c++) {
      print(colMap[cl.getClassCode(c)], 2);

      if (c < max) {
        print(", ");
        if (++n >= 16) {
          println();
          print("    ");
          n = 0;
        }
      }
    }

    println();
    println("  )");
    println();
  }

  /**
   * Returns the number of elements in the packed char map
   * array, or zero if the char map array will be not be packed.
   * <p/>
   * This will be more than intervals.length if the count
   * for any of the values is more than 0xFFFF, since
   * the number of char map array entries per value is
   * ceil(count / 0xFFFF)
   */
  protected int emitCharMapArray() {
    CharClasses cl = parser.getCharClasses();

    if (cl.getMaxCharCode() < 256) {
      emitCharMapArrayUnPacked();
      return 0; // the char map array will not be packed
    }

    // ignores cl.getMaxCharCode(), emits all intervals instead

    intervals = cl.getIntervals();

    println("");
    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    println("  final val ZZ_CMAP_PACKED: String = ");

    int n = 0;  // numbers of entries in current line
    print("    \"");

    int i = 0, numPairs = 0;
    int count, value;
    while (i < intervals.length) {
      count = intervals[i].end - intervals[i].start + 1;
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

      if (i < intervals.length - 1) {
        if (++n >= 10) {
          println("\"+");
          print("    \"");
          n = 0;
        }
      }

      i++;
    }

    println("\"");
    println();

    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    println("  final val ZZ_CMAP: Array[Char] = zzUnpackCMap(ZZ_CMAP_PACKED)");
    println();
    return numPairs;
  }

  protected void emitClassCode() {
    if (scanner.classCode != null) {
      println("  /* user code: */");
      println(scanner.classCode);
    }

    // todo cup?
    if (scanner.cup2Compatible) {
      // convenience methods for CUP2
      println();
      println("  /* CUP2 code: */");
      println("  def token[T](Terminal terminal, T value): ScannerToken[T] = ");
      println("    new ScannerToken[T](terminal, value, yyline, yycolumn)");
      println();
      println("  def token[Object](Terminal terminal) = ");
      println("    new ScannerToken[Object](terminal, yyline, yycolumn)");
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

    if (scanner.initThrow != null && printCtorArgs) {
      println("  @throws " + scanner.initThrow);
    }
    print("  def this(in: java.io.Reader");
    if (printCtorArgs) emitCtorArgs();
    print(")");

    println(" {");

    if (scanner.initCode != null && printCtorArgs) {
      print("  ");
      print(scanner.initCode);
    }

    println("    this.zzReader = in");

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

      if (scanner.initThrow != null && printCtorArgs) {
        println("  @throws " + scanner.initThrow);
      }
      print("  def this(in: java.io.InputStream");
      if (printCtorArgs) emitCtorArgs();
      print(")");

      println(" {");

      println("    this(new java.io.InputStreamReader");
      print("             (in, java.nio.charset.Charset.forName(\"UTF-8\"))");
      if (printCtorArgs) {
        for (int i = 0; i < scanner.ctorArgs.size(); i++) {
          print(", " + scanner.ctorArgs.get(i));
        }
      }
      println(")");
      println("  }");
    }
  }

  protected void emitCtorArgs() {
    // todo might need to map java to scala primitive types here / otherwise
    for (int i = 0; i < scanner.ctorArgs.size(); i++) {
      print(", " + scanner.ctorArgs.get(i));
      print(": " + scanner.ctorTypes.get(i));
    }
  }

  protected void emitDoEOF() {
    if (scanner.eofCode == null) return;

    println("  /**");
    println("   * Contains user EOF-code, which will be executed exactly once,");
    println("   * when the end of file is reached");
    println("   */");

    if (scanner.eofThrow != null) {
      print(" @throws ");
      println(scanner.eofThrow);
    }

    print("  def zzDoEOF() = {");

    println("    if (!zzEOFDone) {");
    println("      zzEOFDone = true");
    println("    " + scanner.eofCode);
    println("    }");
    println("  }");
    println("");
    println("");
  }

  protected void emitLexFunctHeader() {

    // todo make a function to emit throws
    print(" @throws java.io.IOException");

    if (scanner.lexThrow != null) {
      print(" @throws " + scanner.lexThrow);
    }

    if (scanner.scanErrorException != null) {
      print(" @throws " + scanner.scanErrorException);
    }

    // todo make a function for this
    print(" def " + scanner.functionName + "(): ");

    // todo make a function for this
    if (scanner.tokenType == null) {
      if (scanner.isInteger)
        print("Int");
      else if (scanner.isIntWrap)
        print("Integer");
      else
        print("Yytoken");
    } else
      print(scanner.tokenType);

    print(" = {");

    skel.emitNext();

    println("    val zzTransL: Array[Int] = ZZ_TRANS");
    println("    val zzRowMapL: Array[Int] = ZZ_ROWMAP");
    println("    val zzAttrL: Array[Int] = ZZ_ATTRIBUTE");

    skel.emitNext();

    if (scanner.charCount) {
      println("      yychar += zzMarkedPosL-zzStartRead");
      println("");
    }

    if (scanner.lineCount || scanner.columnCount) {
      println("      var zzR = false");
      println("      var zzCh: Int");
      println("      var zzCharCount: Int");
      println("      for (zzCurrentPosL = zzStartRead  ;");
      println("           zzCurrentPosL < zzMarkedPosL ;");
      println("           zzCurrentPosL += zzCharCount ) {");
      println("        zzCh = Character.codePointAt(zzBufferL, zzCurrentPosL, zzMarkedPosL)");
      println("        zzCharCount = Character.charCount(zzCh)");
      println("        zzCh match {");
      println("        case '\\u000B' | '\\u000C' | '\\u0085' | '\\u2028' | '\\u2029' =>");
      if (scanner.lineCount)
        println("          yyline += 1");
      if (scanner.columnCount)
        println("          yycolumn = 0");
      println("          zzR = false");
      println("        case '\\r' =>");
      if (scanner.lineCount)
        println("          yyline += 1");
      if (scanner.columnCount)
        println("          yycolumn = 0");
      println("          zzR = true");
      println("        case '\\n' => ");
      println("          if (zzR)");
      println("            zzR = false");
      println("          else {");
      if (scanner.lineCount)
        println("            yyline += 1");
      if (scanner.columnCount)
        println("            yycolumn = 0");
      println("          }");
      println("        case _ =>");
      println("          zzR = false");
      if (scanner.columnCount)
        println("          yycolumn += zzCharCount");
      println("        }");
      println("      }");
      println();

      if (scanner.lineCount) {
        println("      if (zzR) {");
        println("        // peek one character ahead if it is \\n (if we have counted one line too much)");
        println("        val zzPeek: Boolean");
        println("        if (zzMarkedPosL < zzEndReadL)");
        println("          zzPeek = zzBufferL(zzMarkedPosL) == '\\n'");
        println("        else if (zzAtEOF)");
        println("          zzPeek = false");
        println("        else {");
        println("          val eof = zzRefill()");
        println("          zzEndReadL = zzEndRead");
        println("          zzMarkedPosL = zzMarkedPos");
        println("          zzBufferL = zzBuffer");
        println("          if (eof) ");
        println("            zzPeek = false");
        println("          else ");
        println("            zzPeek = zzBufferL[zzMarkedPosL] == '\\n'");
        println("        }");
        println("        if (zzPeek) yyline -= 1");
        println("      }");
      }
    }

    if (scanner.bolUsed) {
      // zzMarkedPos > zzStartRead <=> last match was not empty
      // if match was empty, last value of zzAtBOL can be used
      // zzStartRead is always >= 0
      println("      if (zzMarkedPosL > zzStartRead) {");
      println("        zzBufferL(zzMarkedPosL-1) match {");
      println("        case '\\n' | '\\u000B' | '\\u000C' | '\\u0085' | '\\u2028' | '\\u2029' => ");
      println("          zzAtBOL = true");
      println("        case '\\r' =>");
      println("          if (zzMarkedPosL < zzEndReadL)");
      println("            zzAtBOL = zzBufferL(zzMarkedPosL) != '\\n'");
      println("          else if (zzAtEOF)");
      println("            zzAtBOL = false");
      println("          else {");
      println("            val eof = zzRefill()");
      println("            zzMarkedPosL = zzMarkedPos");
      println("            zzEndReadL = zzEndRead");
      println("            zzBufferL = zzBuffer");
      println("            if (eof) ");
      println("              zzAtBOL = false");
      println("            else ");
      println("              zzAtBOL = zzBufferL(zzMarkedPosL) != '\\n'");
      println("          }");
      println("        case _ => zzAtBOL = false");
      println("        }");
      println("      }");
    }

    skel.emitNext();

    if (scanner.bolUsed) {
      println("      if (zzAtBOL)");
      println("        zzState = ZZ_LEXSTATE(zzLexicalState+1)");
      println("      else");
      println("        zzState = ZZ_LEXSTATE(zzLexicalState)");
      println();
    } else {
      println("      zzState = ZZ_LEXSTATE(zzLexicalState)");
      println();
    }

    println("      // set up zzAction for empty match case:");
    println("      var zzAttributes = zzAttrL(zzState);");
    println("      if ( (zzAttributes & 1) == 1 ) {");
    println("        zzAction = zzState");
    println("      }");
    println();

    skel.emitNext();
  }


  protected void emitGetRowMapNext() {
    println("          var zzNext = zzTransL(zzRowMapL(zzState) + zzCMapL(zzInput))");
    println("          if (zzNext == " + DFA.NO_TARGET + ") break zzForAction"); // todo this won't work
    println("          zzState = zzNext");
    println();

    println("          zzAttributes = zzAttrL(zzState)");

    println("          if ( (zzAttributes & " + FINAL + ") == " + FINAL + " ) {");

    skel.emitNext();

    println("            if ( (zzAttributes & " + NOLOOK + ") == " + NOLOOK + " ) break zzForAction");

    skel.emitNext();
  }

  protected void emitActions() {
    println("        if (zzAction < 0) zzAction else ZZ_ACTION(zzAction) match {");

    int i = actionTable.size() + 1;

    for (Map.Entry<Action, Integer> entry : actionTable.entrySet()) {
      Action action = entry.getKey();
      int label = entry.getValue();

      println("          case " + label + "=> ");

      if (action.lookAhead() == Action.FIXED_BASE) {
        println("            // lookahead expression with fixed base length");
        println("            zzMarkedPos = Character.offsetByCodePoints");
        println("                (zzBufferL, zzStartRead, zzEndRead - zzStartRead, zzStartRead, " + action.getLookLength() + ")");
      }

      if (action.lookAhead() == Action.FIXED_LOOK ||
              action.lookAhead() == Action.FINITE_CHOICE) {
        println("            // lookahead expression with fixed lookahead length");
        println("            zzMarkedPos = Character.offsetByCodePoints");
        println("                (zzBufferL, zzStartRead, zzEndRead - zzStartRead, zzMarkedPos, -" + action.getLookLength() + ")");
      }

      if (action.lookAhead() == Action.GENERAL_LOOK) {
        println("            // general lookahead, find correct zzMarkedPos");
        println("            { var zzFState = " + dfa.entryState[action.getEntryState()]);
        println("              var zzFPos = zzStartRead");
        println("              if (zzFin.length <= zzBufferL.length) { zzFin = new Array[Boolean](zzBufferL.length+1) }");
        println("              val zzFinL = zzFin");
        println("              while (zzFState != -1 && zzFPos < zzMarkedPos) {");
        println("                zzFinL(zzFPos) = ((zzAttrL(zzFState) & 1) == 1)");
        println("                zzInput = Character.codePointAt(zzBufferL, zzFPos, zzMarkedPos)");
        println("                zzFPos += Character.charCount(zzInput)");
        println("                zzFState = zzTransL( zzRowMapL(zzFState) + zzCMapL(zzInput) )");
        println("              }");
        println("              if (zzFState != -1) { zzFinL(zzFPos) = ((zzAttrL(zzFState) & 1) == 1) } "); // incorrect place to increment zzFPos?
        println("              zzFPos += 1");
        println("              while (zzFPos <= zzMarkedPos) {");
        println("                zzFinL[zzFPos] = false");
        println("                zzFPos += 1");
        println("              }");
        println();
        println("              zzFState = " + dfa.entryState[action.getEntryState() + 1]);
        println("              zzFPos = zzMarkedPos");
        println("              while (!zzFinL[zzFPos] || (zzAttrL[zzFState] & 1) != 1) {");
        println("                zzInput = Character.codePointBefore(zzBufferL, zzFPos, zzStartRead)");
        println("                zzFPos -= Character.charCount(zzInput)");
        println("                zzFState = zzTransL( zzRowMapL(zzFState) + zzCMapL(zzInput) )");
        println("              }");
        println("              zzMarkedPos = zzFPos");
        println("            }");
      }

      if (scanner.debugOption) {
        print("            println(");
        if (scanner.lineCount)
          print("\"line: \"+(yyline+1)+\" \"+");
        if (scanner.columnCount)
          print("\"col: \"+(yycolumn+1)+\" \"+");
        println("\"match: --\"+zzToPrintable(yytext())+\"--\");");
        print("            println(\"action [" + action.priority + "] { ");
        print(escapify(action.content));
        println(" }\");");
      }

      println("            { " + action.content);
      println("            }");
      println("          case " + (i++) + "=> // noop");
    }
  }

  protected void emitEOFVal() {
    EOFActions eofActions = parser.getEOFActions();

    if (scanner.eofCode != null)
      println("            zzDoEOF()");

    if (eofActions.numActions() > 0) {
      println("            zzLexicalState match {");

      // pick a start value for break case labels.
      // must be larger than any value of a lex state:
      int last = dfa.numStates;

      for (String name : scanner.states.names()) {
        int num = scanner.states.getNumber(name);
        Action action = eofActions.getAction(num);

        if (action != null) {
          println("            case " + name + "=> {");
          if (scanner.debugOption) {
            print("              println(");
            if (scanner.lineCount)
              print("\"line: \"+(yyline+1)+\" \"+");
            if (scanner.columnCount)
              print("\"col: \"+(yycolumn+1)+\" \"+");
            println("\"match: <<EOF>>\");");
            print("              println(\"action [" + action.priority + "] { ");
            print(escapify(action.content));
            println(" }\")");
          }
          println("              " + action.content);
          println("            }");
          println("            case " + (++last) + "=> //noop");
        }
      }

      println("            case _ => ");
    }

    Action defaultAction = eofActions.getDefault();

    if (defaultAction != null) {
      println("              {");
      if (scanner.debugOption) {
        print("                println(");
        if (scanner.lineCount)
          print("\"line: \"+(yyline+1)+\" \"+");
        if (scanner.columnCount)
          print("\"col: \"+(yycolumn+1)+\" \"+");
        println("\"match: <<EOF>>\");");
        print("                println(\"action [" + defaultAction.priority + "] { ");
        print(escapify(defaultAction.content));
        println(" }\")");
      }
      println("                " + defaultAction.content);
      println("              }");
    } else if (scanner.eofVal != null)
      println("          { " + scanner.eofVal + " }");
    else if (scanner.isInteger) {
      if (scanner.tokenType != null) {
        Out.error(ErrorMessages.INT_AND_TYPE);
        throw new GeneratorException();
      }
      println("        return YYEOF");
    } else
      println("        return null");

    if (eofActions.numActions() > 0)
      println("        }");
  }

  /**
   * Set up EOF code section according to scanner.eofcode
   */
  protected void setupEOFCode() {
    if (scanner.eofclose) {
      scanner.eofCode = LexScan.conc(scanner.eofCode, "  yyclose()");
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

    // this is where things become java
    skel.emitNext();

    println("  final val ZZ_BUFFERSIZE = " + scanner.bufferSize);

    if (scanner.debugOption) {
      println("  final val ZZ_NL = System.getProperty(\"line.separator\")");
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
      println("  def zzToPrintable(str: String): String = {");
      println("    StringBuilder builder = new StringBuilder()");
      println("    var n = 0");
      println("    while (n < str.length()) {");
      println("      val ch = str.codePointAt(n)");
      println("      val charCount = Character.charCount(ch)");
      println("      n += charCount");
      println("      if (ch > 31 && ch < 127) {");
      println("        builder.append((char)ch)");
      println("      } else if (charCount == 1) {");
      println("        builder.append(String.format(\"\\\\u%04X\", ch))");
      println("      } else {");
      println("        builder.append(String.format(\"\\\\U%06X\", ch))");
      println("      }");
      println("    }");
      println("    builder.toString()");
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
