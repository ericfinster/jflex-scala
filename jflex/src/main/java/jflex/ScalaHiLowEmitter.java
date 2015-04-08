/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.6.1-SNAPSHOT                                                    *
 * Copyright (C) 1998-2014  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package jflex;

/**
 * HiLowEmitter
 *
 * @author Gerwin Klein
 * @version JFlex 1.6.1-SNAPSHOT
 */
public class ScalaHiLowEmitter extends ScalaPackEmitter {

  /** number of entries in expanded array */
  private int numEntries;

  /**
   * Create new emitter for values in [0, 0xFFFFFFFF] using hi/low encoding.
   *
   * @param name   the name of the generated array
   */
  public ScalaHiLowEmitter(String name) {
    super(name);
  }

  /**
   * Emits hi/low pair unpacking code for the generated array. 
   *
   * @see JavaPackEmitter#emitUnpack()
   */
  public void emitUnpack() {
    // close last string chunk:
    println("'E');");
    nl();
    println("  def zzUnpack"+name+"(): Array[Int] = {");
    println("    val result = new Array[Int]("+numEntries+")");
    println("    var offset = 0");

    for (int i = 0; i < chunks; i++) {
      println("    offset = zzUnpack"+name+"("+constName()+"_PACKED_"+i+", offset, result)");
    }

    println("    result");
    println("  }");

    nl();
    println("  def zzUnpack"+name+"(packed: Array[Char], offset: Int, result: Array[Int]): Int = {");
    println("    var i = 0  /* index in packed string  */");
    println("    var j = offset  /* index in unpacked array */");
    println("    val l = packed.length() - 1");
    println("    while (i < l) {");
    println("      val high = packed.charAt(i) << 16; i += 1");
    println("      result(j) = high | packed.charAt(i); j += 1; i += 1");
    println("    }");
    println("    j");
    println("  }");
  }

  /**
   * Emit one value using two characters. 
   *
   * @param val  the value to emit
   * @prec  0 <= val <= 0xFFFFFFFF 
   */
  public void emit(int val) {
    numEntries+= 1;
    breaks();
    emitUC(val >> 16);
    emitUC(val & 0xFFFF);
  }
}
