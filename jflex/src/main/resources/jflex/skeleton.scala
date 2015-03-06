
  /** This character denotes the end of file */
  final val YYEOF = -1

  /** initial size of the lookahead buffer */
--- final val ZZ_BUFFERSIZE = ...

  /** lexical states */
---  lexical states, charmap

  /* error codes */
  private final val ZZ_UNKNOWN_ERROR = 0
  private final val ZZ_NO_MATCH = 1
  private final val ZZ_PUSHBACK_2BIG = 2

  /* error messages for the codes above */
  private final val ZZ_ERROR_MSG = Array[String](
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  )

--- isFinal list
  /** the input device */
  var zzReader = in

  /** the current state of the DFA */
  var zzState: Int

  /** the current lexical state */
  var zzLexicalState = YYINITIAL

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  var zzBuffer = new Array[Char](ZZ_BUFFERSIZE)

  /** the textposition at the last accepting state */
  var zzMarkedPos = 0

  /** the current text position in the buffer */
  var zzCurrentPos = 0

  /** startRead marks the beginning of the yytext() string in the buffer */
  var zzStartRead = 0

  /** endRead marks the last character in the buffer, that has been read
      from input */
  var zzEndRead = 0

  /** number of newlines encountered up to the start of the matched text */
  var yyline = 0

  /** the number of characters up to the start of the matched text */
  var yychar = 0

  /**
   * the number of characters from the last newline up to the start of the 
   * matched text
   */
  var yycolumn = 0

  /** 
   * zzAtBOL == true <=> the scanner is currently at the beginning of a line
   */
  var zzAtBOL = true

  /** zzAtEOF == true <=> the scanner is at the EOF */
  var zzAtEOF = false

  /** denotes if the user-EOF-code has already been executed */
  var zzEOFDone = false
  
  /** 
   * The number of occupied positions in zzBuffer beyond zzEndRead.
   * When a lead/high surrogate has been read from the input stream
   * into the final zzBuffer position, this will have a value of 1;
   * otherwise, it will have a value of 0.
   */
  var zzFinalHighSurrogate = 0

--- user class code

--- constructor declaration


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   */
  @throws[java.io.IOException]("if any I/O-Error occurs")
  def zzRefill(): Boolean = {

    /* first: make room (if you can) */
    if (zzStartRead > 0) {
      zzEndRead += zzFinalHighSurrogate
      zzFinalHighSurrogate = 0
      System.arraycopy(zzBuffer, zzStartRead,
                       zzBuffer, 0,
                       zzEndRead-zzStartRead)

      /* translate stored positions */
      zzEndRead-= zzStartRead
      zzCurrentPos-= zzStartRead
      zzMarkedPos-= zzStartRead
      zzStartRead = 0
    }

    /* is the buffer big enough? */
    if (zzCurrentPos >= zzBuffer.length - zzFinalHighSurrogate) {
      /* if not: blow it up */
      val newBuffer = new Array[Char](zzBuffer.length*2)
      System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length)
      zzBuffer = newBuffer
      zzEndRead += zzFinalHighSurrogate
      zzFinalHighSurrogate = 0
    }

    /* fill the buffer with new input */
    val requested = zzBuffer.length - zzEndRead
    var numRead = zzReader.read(zzBuffer, zzEndRead, requested)

    // unlikely but not impossible: read 0 characters, but not at end of stream
    if (numRead == 0) {
      numRead = zzReader.read(zzBuffer, zzEndRead, requested)
    }
    if (numRead > 0) {
      zzEndRead += numRead
      if (numRead == requested) { /* possibly more input available */
        if (Character.isHighSurrogate(zzBuffer(zzEndRead - 1))) {
          zzEndRead -= 1
          zzFinalHighSurrogate = 1
        }
      }
      return false
    }

    true
  }

    
  /**
   * Closes the input stream.
   */
  @throws[java.io.IOException]
  def yyclose() = {
    zzAtEOF = true            /* indicate end of file */
    zzEndRead = zzStartRead  /* invalidate buffer    */

    if (zzReader != null)
      zzReader.close()
  }


  /**
   * Resets the scanner to read from a new input stream.
   * Does not close the old reader.
   *
   * All internal variables are reset, the old input stream 
   * <b>cannot</b> be reused (internal buffer is discarded and lost).
   * Lexical state is set to <tt>ZZ_INITIAL</tt>.
   *
   * Internal scan buffer is resized down to its initial length, if it has grown.
   *
   * @param reader   the new input stream 
   */
  def yyreset(reader: java.io.Reader) = {
    zzReader = reader
    zzAtBOL  = true
    zzAtEOF  = false
    zzEOFDone = false
    zzEndRead = 0
    zzStartRead = 0
    zzCurrentPos = 0
    zzMarkedPos = 0
    zzFinalHighSurrogate = 0
    yyline = 0
    yychar = 0
    yycolumn = 0
    zzLexicalState = YYINITIAL
    if (zzBuffer.length > ZZ_BUFFERSIZE)
      zzBuffer = new Array[Char](ZZ_BUFFERSIZE)
  }


  /**
   * Returns the current lexical state.
   */
  def yystate(): Int = zzLexicalState


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  def yybegin(newState: Int) = {
    zzLexicalState = newState
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  def yytext(): String = new String( zzBuffer, zzStartRead, zzMarkedPos-zzStartRead )


  /**
   * Returns the character at position <tt>pos</tt> from the 
   * matched text. 
   * 
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch. 
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  def yycharat(pos: Int): Char = zzBuffer(zzStartRead+pos)


  /**
   * Returns the length of the matched text region.
   */
  def yylength(): Int = zzMarkedPos-zzStartRead


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of 
   * yypushback(int) and a match-all fallback rule) this method 
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
--- zzScanError declaration
    var message = ""
    try {
      message = ZZ_ERROR_MSG(errorCode)
    }
    catch {
      case e: ArrayIndexOutOfBoundsException => message = ZZ_ERROR_MSG(ZZ_UNKNOWN_ERROR)
    }

--- throws clause
  } 


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
--- yypushback decl (contains zzScanError exception)
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG)

    zzMarkedPos -= number
  }


--- zzDoEOF
  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   */
--- yylex declaration
    var zzInput = -1
    var zzAction = -1

    // cached fields:
    var zzCurrentPosL = -1
    var zzMarkedPosL = -1
    var zzEndReadL = zzEndRead
    var zzBufferL = zzBuffer
    val zzCMapL = ZZ_CMAP

--- local declarations

    var ret: Object = null
    while ((ret == null) && !zzAtEOF) {
      zzMarkedPosL = zzMarkedPos

--- start admin (line, char, col count)
      zzAction = -1

      zzCurrentPosL = zzMarkedPosL
      zzCurrentPos = zzMarkedPosL
      zzStartRead = zzMarkedPosL
  
--- start admin (lexstate etc)

      breakable {
        while (true) {

--- next input, line, col, char count, next transition, isFinal action
            zzAction = zzState
            zzMarkedPosL = zzCurrentPosL
--- line count update
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL
--- char count update

      ret = if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true
--- eofvalue
      }
      else {
--- actions
          case _ =>
--- no match
        }
      }
    }
    ret
  }

--- main

}
