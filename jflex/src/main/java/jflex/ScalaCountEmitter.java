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
 * An emitter for an array encoded as count/value pairs in a string.
 *
 * @author Gerwin Klein
 * @version JFlex 1.6.1-SNAPSHOT
 */
public class ScalaCountEmitter extends ScalaPackEmitter {
  /** number of entries in expanded array */
  private int numEntries;

  /** translate all values by this amount */
  private int translate = 0;


  /**
   * Create a count/value emitter for a specific field.
   *
   * @param name   name of the generated array
   */
  protected ScalaCountEmitter(String name) {
    super(name);
  }

  /**
   * Emits count/value unpacking code for the generated array. 
   *
   * @see JavaPackEmitter#emitUnpack()
   */
  public void emitUnpack() {
    // close last string chunk:
    println("\";");

    nl();
    println("  def zzUnpack"+name+"(): Array[Int] = {");
    println("    val result = new Array[Int]("+numEntries+")");
    println("    var offset = 0");

    for (int i = 0; i < chunks; i++) {
      println("    offset = zzUnpack" + name + "("+constName()+"_PACKED_"+i+", offset, result)");
    }

    println("    result");
    println("  }");
    nl();

    println("  def zzUnpack" + name + "(packed: String, offset: Int, result: Array[Int]): Int = {");
    println("    var i = 0       /* index in packed string  */");
    println("    var j = offset  /* index in unpacked array */");
    println("    val l = packed.length()");
    println("    while (i < l) {");
    println("      var count = packed.charAt(i); i += 1");
    println("      var value = packed.charAt(i); i += 1");
    if (translate == 1) {
      println("      value -= 1");
    }
    else if (translate != 0) {
      println("      value -= " + translate);
    }
    println("      result(j) = value; j+=1");
    println("      count -= 1");
    println("      while (count > 0){");
    println("        result(j) = value; j+=1");
    println("        count -= 1");
    println("      }");
    println("    }");
    println("    j");
    println("  }");
  }

  /**
   * Translate all values by given amount.
   *
   * Use to move value interval from [0, 0xFFFF] to something different.
   *
   * @param i   amount the value will be translated by. 
   *            Example: <code>i = 1</code> allows values in [-1, 0xFFFE].
   */
  public void setValTranslation(int i) {
    this.translate = i;
  }

  /**
   * Emit one count/value pair. 
   *
   * Automatically translates value by the <code>translate</code> value. 
   *
   * @param count
   * @param value
   *
   * @see JavaCountEmitter#setValTranslation(int)
   */
  public void emit(int count, int value) {
    numEntries+= count;
    breaks();
    emitUC(count);
    emitUC(value+translate);
  }
}
