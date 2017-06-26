/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This is a Time Course Expression entry
*/

public class ExpressionEntry implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  // Order is important - greater expression should be higher value.
  
  // i.e. doing tests like (matchExpr <= ExpressionEntry.NOT_EXPRESSED)!
  
 
  public static final int NO_REGION           = -1;  
  public static final int NO_DATA              = 0;
  public static final int NOT_EXPRESSED        = 1;
  public static final int WEAK_EXPRESSION      = 2;
  public static final int EXPRESSED            = 3;
  public static final int VARIABLE             = 4;  // FIX ME??
  public static final int NUM_EXPRESSIONS      = 5;
  
  // Source of expression

  public enum Source {NO_SOURCE_SPECIFIED, MATERNAL_SOURCE, ZYGOTIC_SOURCE, MATERNAL_AND_ZYGOTIC};
 
  // Boundary extrapolation strategies

  public static final int NO_STRATEGY_SPECIFIED          = 0;  
  public static final int ON_AT_BOUNDARY_WITH_SLOW_RAMP  = 1;
  public static final int ON_AT_BOUNDARY_WITH_FAST_RAMP  = 2;  
  public static final int OFF_AT_BOUNDARY_WITH_SLOW_RAMP = 3;
  public static final int OFF_AT_BOUNDARY_WITH_FAST_RAMP = 4;
  public static final int NUM_STRATEGIES                 = 5;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String region_;
  private int time_;
  private int expr_;
  private Source source_;
  private int confidence_;
  private Source strategySource_;
  private int startStrategy_;
  private int endStrategy_;
  private double variable_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public ExpressionEntry(ExpressionEntry other) {
    copyInto(other);
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ExpressionEntry(String region, int time, int expr, Source source, int confidence, 
                         Source strategySource, int startStrategy, int endStrategy, double variable) {
    region_ = region;
    time_ = time;
    expr_ = expr;
    confidence_ = confidence;
    source_ = source;
    strategySource_ = strategySource;
    startStrategy_ = startStrategy;
    endStrategy_ = endStrategy;
    if ((expr != VARIABLE) && (variable != 0.0)) {
      throw new IllegalArgumentException();
    }
    variable_ = variable;
    if ((variable_ < 0.0) || (variable_ > 1.0)) {
      throw new IllegalArgumentException();
    }
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public ExpressionEntry(String region, int time) {
    this(region, time, NOT_EXPRESSED, Source.NO_SOURCE_SPECIFIED, TimeCourseGene.USE_BASE_CONFIDENCE, 
         Source.NO_SOURCE_SPECIFIED, NO_STRATEGY_SPECIFIED, NO_STRATEGY_SPECIFIED, 0.0);
  }  
 
  /***************************************************************************
  **
  ** Constructor
  */

  public ExpressionEntry(String region, int time, int expr, int confidence) {
    this(region, time, expr, Source.NO_SOURCE_SPECIFIED, confidence, 
         Source.NO_SOURCE_SPECIFIED, NO_STRATEGY_SPECIFIED, NO_STRATEGY_SPECIFIED, 0.0);
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ExpressionEntry(DataAccessContext dacx, String region, String time, 
                         String expr, String source, String confidence,
                         String strategySource, String startStrategy, String endStrategy, String variable) 
    throws IOException {

    region_ = region;
    try {
      time_ = Integer.parseInt(time);
    } catch (NumberFormatException nfex) {
      throw new IOException();
    }
   
    TimeAxisDefinition tad = dacx.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.timeIsOk(time_)) {      
      throw new IOException();
    }    

    if (expr == null) {
      throw new IOException();
    } else {
      expr = expr.trim();
      try {
        expr_ = mapFromExpressionTag(expr);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
    
    if (expr_ == VARIABLE) {
      if (variable == null) {
        throw new IOException();
      }
      try {
        variable_ = Double.parseDouble(variable);
      } catch (NumberFormatException nfex) {
        throw new IOException();
      }
      if ((variable_ < 0.0) || (variable_ > 1.0)) {
        throw new IOException();
      }
    }
    
    if (source == null) {
      source_ = Source.NO_SOURCE_SPECIFIED;
    } else {
      source = source.trim();
      try {
        source_ = mapFromSourceTag(source);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
     
    if (strategySource == null) {
      strategySource_ = Source.NO_SOURCE_SPECIFIED;
    } else {
      strategySource = strategySource.trim();
      try {
        strategySource_ = mapFromSourceTag(strategySource);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }
 
    if (confidence == null) {
      confidence_ = TimeCourseGene.USE_BASE_CONFIDENCE;
    } else {
      confidence = confidence.trim();
      if (confidence.equalsIgnoreCase("normal")) {
        confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
      } else if (confidence.equalsIgnoreCase("interpolated")) {
        confidence_ = TimeCourseGene.INTERPOLATED;
      } else if (confidence.equalsIgnoreCase("assumption")) {
        confidence_ = TimeCourseGene.ASSUMPTION;        
      } else if (confidence.equalsIgnoreCase("inferred")) {
        confidence_ = TimeCourseGene.INFERRED;
      } else if (confidence.equalsIgnoreCase("questionable")) {
        confidence_ = TimeCourseGene.QUESTIONABLE;
      } else {
        throw new IOException();
      }
    }
    
    try {
      startStrategy_ = calculateStrategy(startStrategy, true);
      endStrategy_ = calculateStrategy(endStrategy, false);
    } catch (IllegalArgumentException iae) {
      throw new IOException();
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Discrete to a variable value. May be null.
  */

  public static Double discreteToVar(int expVal, Double varVal, double weakVal) { 
    switch (expVal) {
      case NO_DATA:
        return (null);
      case NOT_EXPRESSED:
        return (Double.valueOf(0.0));
      case WEAK_EXPRESSION:
        return (Double.valueOf(weakVal));
      case EXPRESSED:
        return (Double.valueOf(1.0));
      case VARIABLE:
        if (varVal == null) {
          throw new IllegalArgumentException();
        }
        return (varVal);
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Replacement copy
  */

  public void copyInto(ExpressionEntry other) { 
    this.region_ = other.region_;
    this.time_ = other.time_;
    this.expr_ = other.expr_;
    this.source_ = other.source_;
    this.confidence_ = other.confidence_;
    this.strategySource_ = other.strategySource_;
    this.startStrategy_ = other.startStrategy_;
    this.endStrategy_ = other.endStrategy_;
    this.variable_ = other.variable_;
    return;
  }
  
  /***************************************************************************
  **
  ** Needed for merging checks
  */

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof ExpressionEntry)) {
      return (false);
    }
    ExpressionEntry otherEE = (ExpressionEntry)other;
    if (!DataUtil.stringsEqual(this.region_, otherEE.region_)) {
      return (false);
    }
    if (this.time_ != otherEE.time_) {
      return (false);
    }
    if (this.expr_ != otherEE.expr_) {
      return (false);
    }
    if (this.source_ != otherEE.source_) {
      return (false);
    }
    if (this.confidence_ != otherEE.confidence_) {
      return (false);
    }
    if (this.strategySource_ != otherEE.strategySource_) {
      return (false);
    }
    if (this.startStrategy_ != otherEE.startStrategy_) {
      return (false);
    }
    if (this.endStrategy_ != otherEE.endStrategy_) {
      return (false);
    }
    if (this.variable_ != otherEE.variable_) {
      return (false);
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public ExpressionEntry clone() {
    try {
      ExpressionEntry retval = (ExpressionEntry)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Update the region and time
  **
  */
  
  public void updateRegionAndTime(String region, int time) {
    region_ = region;
    time_ = time;
    return;
  } 
  
  /***************************************************************************
  **
  ** Update the region 
  **
  */
  
  public void updateRegion(String region) {
    region_ = region;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the region
  **
  */
  
  public String getRegion() {
    return (region_);
  }
  
  /***************************************************************************
  **
  ** Get the time
  **
  */
  
  public int getTime() {
    return (time_);
  }
  
  /***************************************************************************
  **
  ** Get the RAW expression
  **
  */
  
  public int getRawExpression() {
    return (expr_);
  } 
  
  /***************************************************************************
  **
  ** Get the expression FOR THE SPECIFIC SOURCE!
  **
  */
  
  public int getExpressionForSource(Source whichSource) {
    if ((source_ == Source.NO_SOURCE_SPECIFIED) || (whichSource == Source.NO_SOURCE_SPECIFIED)) {
      return (expr_);
    }
    switch (whichSource) {
      case MATERNAL_SOURCE:
        return (((source_ == Source.MATERNAL_SOURCE) || (source_ == Source.MATERNAL_AND_ZYGOTIC)) ? expr_ : NOT_EXPRESSED);
      case ZYGOTIC_SOURCE:
        return (((source_ == Source.ZYGOTIC_SOURCE) || (source_ == Source.MATERNAL_AND_ZYGOTIC)) ? expr_ : NOT_EXPRESSED);
      case NO_SOURCE_SPECIFIED:
      case MATERNAL_AND_ZYGOTIC:
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the source
  **
  */
  
  public Source getSource() {
    return (source_);
  }
  
  /***************************************************************************
  **
  ** Get the source for the edge strategy
  **
  */
  
  public Source getStrategySource() {
    return (strategySource_);
  }
  
  /***************************************************************************
  **
  ** Get the qualified source, i.e. if not expressing, we return null!
  **
  */
  
  public Source getQualifiedSource() {
    return ((expr_ > NOT_EXPRESSED) ? source_ : null);
  }
  
  /***************************************************************************
  **
  ** Get the variable expression for the given source channel:
  **
  */
  
  public double getVariableLevelForSource(Source whichSource) {
    if (expr_ != VARIABLE) {
      throw new IllegalStateException();
    }
    if ((source_ == Source.NO_SOURCE_SPECIFIED) || (whichSource == Source.NO_SOURCE_SPECIFIED)) {
      return (variable_);
    }
    switch (whichSource) {
      case MATERNAL_SOURCE:
        return (((source_ == Source.MATERNAL_SOURCE) || (source_ == Source.MATERNAL_AND_ZYGOTIC)) ? variable_ : 0.0);
      case ZYGOTIC_SOURCE:
        return (((source_ == Source.ZYGOTIC_SOURCE) || (source_ == Source.MATERNAL_AND_ZYGOTIC)) ? variable_ : 0.0);
      case NO_SOURCE_SPECIFIED:
      case MATERNAL_AND_ZYGOTIC:
      default:
        throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Get the confidence
  */
  
  public int getConfidence() {
    return (confidence_);
  }
  
  /***************************************************************************
  **
  ** Get the raw starting extrapolation strategy, regardless of source state
  */
  
  public int getRawStartStrategy() {
    return (startStrategy_);
  }
  
  /***************************************************************************
  **
  ** Get the raw ending extrapolation strategy, regardless of source state
  */
  
  public int getRawEndStrategy() {
    return (endStrategy_);
  }
    
  /***************************************************************************
  **
  ** Get the starting extrapolation strategy.  Note that in this case, the
  ** only time we get back the strategy is if we actually ask for it with the right source!
  */
  
  public int getStartStrategy(Source whichSource) {
    switch (whichSource) {
      case MATERNAL_SOURCE:
        return (((strategySource_ == Source.MATERNAL_SOURCE) || (strategySource_ == Source.MATERNAL_AND_ZYGOTIC)) ? startStrategy_ : NO_STRATEGY_SPECIFIED);
      case ZYGOTIC_SOURCE:
        return (((strategySource_ == Source.ZYGOTIC_SOURCE) || (strategySource_ == Source.MATERNAL_AND_ZYGOTIC)) ? startStrategy_ : NO_STRATEGY_SPECIFIED);
      case NO_SOURCE_SPECIFIED:
        return ((strategySource_ == Source.NO_SOURCE_SPECIFIED) ? startStrategy_ : NO_STRATEGY_SPECIFIED);
      case MATERNAL_AND_ZYGOTIC:
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Get the ending extrapolation strategy
  */
  
  public int getEndStrategy(Source whichSource) {
    switch (whichSource) {
      case MATERNAL_SOURCE:
        return (((strategySource_ == Source.MATERNAL_SOURCE) || (strategySource_ == Source.MATERNAL_AND_ZYGOTIC)) ? endStrategy_ : NO_STRATEGY_SPECIFIED);
      case ZYGOTIC_SOURCE:
        return (((strategySource_ == Source.ZYGOTIC_SOURCE) || (strategySource_ == Source.MATERNAL_AND_ZYGOTIC)) ? endStrategy_ : NO_STRATEGY_SPECIFIED);
      case NO_SOURCE_SPECIFIED:
        return ((strategySource_ == Source.NO_SOURCE_SPECIFIED) ? endStrategy_ : NO_STRATEGY_SPECIFIED);
      case MATERNAL_AND_ZYGOTIC:
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Set the starting extrapolation strategy
  */
  
  public void setStartAndEndStrategy(int startStrategy, int endStrategy, Source whichSource) {
    strategySource_ = whichSource;
    startStrategy_ = startStrategy;
    endStrategy_ = endStrategy;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the source
  **
  */
  
  public void setSource(Source src) {
    source_ = src;
    return;
  }
   
  /***************************************************************************
  **
  ** Set the expression
  **
  */
  
  public void setExpression(int expr) {
    expr_ = expr;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the confidence
  */
  
  public void setConfidence(int confidence) {
    confidence_ = confidence;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the variable expression level
  **
  */
  
  public void setVariableLevel(double lev) {
    variable_ = lev;
    return;
  }
   
  /***************************************************************************
  **
  ** Write the expression tables as a csv file:
  **
  */
  
  public void exportCSV(PrintWriter out, int resolvedConfidence, boolean encodeConfidence) {
    out.print(mapExpressionForCSV(expr_, resolvedConfidence, variable_, encodeConfidence));
    return;
  }
 
  /***************************************************************************
  **
  ** Write the expression to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<data region=\"");
    out.print(CharacterEntityMapper.mapEntities(region_, false));
    out.print("\" time=\"");
    out.print(time_); 
    out.print("\" expr=\"");
    out.print(mapExpression(expr_));    

    if (source_ != Source.NO_SOURCE_SPECIFIED) {
      out.print("\" source=\"");
      out.print(mapToSourceTag(source_));
    }
       
    if (confidence_ != TimeCourseGene.USE_BASE_CONFIDENCE) {
      out.print("\" confidence=\"");
      out.print(TimeCourseGene.mapConfidence(confidence_));
    }
    String invert = mapStrategy(startStrategy_);
    if (invert != null) {
      out.print("\" starttype=\"");
      out.print(invert);
    }
    invert = mapStrategy(endStrategy_);
    if (invert != null) {
      out.print("\" endtype=\"");
      out.print(invert);
    }
    
    if (strategySource_ != Source.NO_SOURCE_SPECIFIED) {
      out.print("\" stratSource=\"");
      out.print(mapToSourceTag(strategySource_));
    }
    
    if (expr_ == VARIABLE) {
      out.print("\" value=\"");
      out.print(variable_);
    }
   
    out.println("\" />");    
    return;
  }
  

 /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("Expression: region_ = " + region_ + " time = " + time_ +
            " expr = " + expr_ + " confidence = " + confidence_ + " variable = " + variable_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  ** 
  ** Answer if the set says a maternal channel is in use
  */

  public static boolean hasMaternalChannel(Set<ExpressionEntry.Source> sources) { 
    return (sources.contains(ExpressionEntry.Source.MATERNAL_SOURCE) ||
            sources.contains(ExpressionEntry.Source.MATERNAL_AND_ZYGOTIC));
  }
  
  /***************************************************************************
  ** 
  ** Answer if the set says a zygotic channel is in use
  */

  public static boolean hasZygoticChannel(Set<ExpressionEntry.Source> sources) { 
    return (sources.contains(ExpressionEntry.Source.ZYGOTIC_SOURCE) ||
            sources.contains(ExpressionEntry.Source.MATERNAL_AND_ZYGOTIC));
  }
  
  /***************************************************************************
  ** 
  ** Answer if the set says a no source channel is in use
  */

  public static boolean hasNoSourceChannel(Set<ExpressionEntry.Source> sources) { 
    return (sources.contains(ExpressionEntry.Source.NO_SOURCE_SPECIFIED));
  }
 
  /***************************************************************************
  ** 
  ** Build display name
  */

  public static String buildZygoticDisplayName(HandlerAndManagerSource hams, String name) { 
    String format = hams.getRMan().getString("expressionChannel.zygoticSource");    
    String desc = MessageFormat.format(format, new Object[] {name}); 
    return (desc);
  }
  
  /***************************************************************************
  ** 
  ** Build display name
  */

  public static String buildMaternalDisplayName(HandlerAndManagerSource hams, String name) { 
    String format = hams.getRMan().getString("expressionChannel.maternalSource");    
    String desc = MessageFormat.format(format, new Object[] {name}); 
    return (desc);
  }
 
  /***************************************************************************
  ** 
  ** Map the source value
  */

  public static String mapToSourceTag(Source value) {
    switch (value) {
      case NO_SOURCE_SPECIFIED:
        return ("noSource");
      case MATERNAL_SOURCE:
        return ("maternal");
      case ZYGOTIC_SOURCE:
        return ("zygotic");
      case MATERNAL_AND_ZYGOTIC:
        return ("matAndZyg");
      default:
        System.err.println("source was " + value);
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Map from the source tag
  */

  public static Source mapFromSourceTag(String source) {
    if (source.equalsIgnoreCase("noSource")) {
      return (Source.NO_SOURCE_SPECIFIED);
    } else if (source.equalsIgnoreCase("maternal")) {
      return (Source.MATERNAL_SOURCE);
    } else if (source.equalsIgnoreCase("zygotic")) {
      return (Source.ZYGOTIC_SOURCE);
    } else if (source.equalsIgnoreCase("matAndZyg")) {
      return (Source.MATERNAL_AND_ZYGOTIC);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Map the expression value
  */

  public static String mapExpression(int value) {
    switch (value) {
      case NO_DATA:
        return ("noData");
      case NOT_EXPRESSED:
        return ("no");
      case WEAK_EXPRESSION:
        return ("weak");
      case EXPRESSED:
        return ("yes");
      case VARIABLE:
        return ("variable");
      default:
        System.err.println("value was " + value);
        throw new IllegalArgumentException();
    }    
  }
  
  /***************************************************************************
  **
  ** Map from the expression tag
  */

  public static int mapFromExpressionTag(String expr) {
    if (expr.equalsIgnoreCase("noData")) {
      return (NO_DATA);
    } else if (expr.equalsIgnoreCase("no")) {
      return (NOT_EXPRESSED);
    } else if (expr.equalsIgnoreCase("weak")) {
      return (WEAK_EXPRESSION);
    } else if (expr.equalsIgnoreCase("yes")) {
      return (EXPRESSED);
    } else if (expr.equalsIgnoreCase("variable")) {
      return (VARIABLE);
    } else {
      throw new IllegalArgumentException();
    }
  }
 
  /***************************************************************************
  **
  ** Map the expression value for CSV
  */

  public static String mapExpressionForCSV(int value, int confidence, 
                                           double variable, boolean encodeConfidence) {
    
    if (value == NO_DATA) {
      return ("0");
    }   
    if (value == VARIABLE) {
      return (Double.toString(variable));
    }       
    int base = 1;
    if (encodeConfidence) {
      if (confidence == TimeCourseGene.NORMAL_CONFIDENCE) {
        base = 1;
      } else if (confidence == TimeCourseGene.INTERPOLATED) {
        base = 4;
      } else if (confidence == TimeCourseGene.INFERRED) {
        base = 7;
      } else if (confidence == TimeCourseGene.ASSUMPTION) {
        base = 10;
      } else if (confidence == TimeCourseGene.QUESTIONABLE) {
        base = 13;
      } else {
        throw new IllegalArgumentException();
      }
    }
    
    int level;
    if (value == NOT_EXPRESSED) {
      level = 0;
    } else if (value == WEAK_EXPRESSION) {
      level = 1;
    } else if (value == EXPRESSED) {
      level = 2;
    } else {
      throw new IllegalArgumentException();
    }   
    return (Integer.toString(base + level)); 
  }  
  
  /***************************************************************************
  **
  ** Show the expression key
  */

  public static void expressionKeyCSV(DataAccessContext dacx, PrintWriter out, boolean encodeConfidence) {
    ResourceManager rMan = dacx.getRMan();
    out.println("\"\"");
    out.println("\"\"");
    out.print("\"");
    out.print(rMan.getString("csvTcdExport.keyTitle"));
    out.println("\"");
    out.print("\"");
    out.print(rMan.getString((encodeConfidence) ? "csvTcdExport.var" : "csvTcdExport.varNC"));
    out.println("\"");
    out.print("\"");
    out.print(rMan.getString("csvTcdExport.number"));
    out.print("\",\"");
    out.print(rMan.getString("csvTcdExport.level"));
    if (encodeConfidence) {
      out.print("\",\"");
      out.print(rMan.getString("csvTcdExport.confidence"));
    }
    out.println("\"");
    out.print("0,\"");  
    out.print(rMan.getString("csvTcdExport." + mapExpression(NO_DATA)));
    if (encodeConfidence) {
      out.print("\",\"");
      out.print(rMan.getString("csvTcdExport.nA"));
    }
    out.println("\"");
    int topConf = (encodeConfidence) ? (TimeCourseGene.NUM_CONFIDENCE - 1) : TimeCourseGene.NORMAL_CONFIDENCE;
    int confLev = 1;
    for (int i = 0; i <= topConf; i++) {
      String conf = rMan.getString("csvTcdExportConf." + TimeCourseGene.mapConfidence(i));
      int expLev = 0;
      for (int j = 0; j < NUM_EXPRESSIONS; j++) {
        if ((j != NO_DATA) && (j != VARIABLE)) {
          String expr = rMan.getString("csvTcdExport." + mapExpression(j));
          int totLev = (expLev++) + confLev;
          out.print(totLev);
          out.print(",\"");
          out.print(expr);
          if (encodeConfidence) {
            out.print("\",\"");
            out.print(conf);
          }
          out.println("\"");
        }
      }
      confLev += expLev;
    }
    return;
  }
 
  /*
   =Key to Table Values
=(Variable expression levels are shown as floating point numbers from 0.0 to 1.0; confidence level is omitted.)
=(Variable expression levels are shown as floating point numbers from 0.0 to 1.0.)

  0	no data		
1	no expression		
2	weak expression		
3	expressed		
4	no expression	interpolated	
5	weak expression	interpolated	
6	expressed	interpolated	
7	no expression	inferred	
8	weak expression	inferred	
9	expressed	inferred	
  */
  
 
  /***************************************************************************
  **
  ** Parse Questions
  */
  
  public static boolean isQuestionable(String token) {
    if (token == null) {
      return (false);
    }
    return (token.indexOf("?") != -1); 
  }
  
  /***************************************************************************
  **
  ** Parse gene data tokens
  */
  
  public static int calculateExpression(String token) {
    if (token == null) {
      return (NO_DATA);
    }
    token = token.toUpperCase();
    if (token.equals("ND")) { 
      return (NO_DATA);
    }
    if (token.equals("-")) {
      return (NOT_EXPRESSED);
    }
    if (token.equals("X")) {
      return (EXPRESSED);
    }    
    if (token.equals("(X)")) {
      return (WEAK_EXPRESSION);
    }    
    if (token.equals("?")) {
      return (NO_DATA);  // FIX ME
    }
    if (token.equals("X(?)")) {
      return (EXPRESSED);  // FIX ME
    }        
    System.err.println(token);
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Figure out extrapolation strategy
  */
  
  @SuppressWarnings("unused")
  public static int calculateStrategy(String token, boolean isStart) {
  
    //
    // Defaults for start and stop:
    //
    
    if (token == null) {
      return (NO_STRATEGY_SPECIFIED);
    }
    
    if (token.equals("onWithSlowRamp")) {
      return (ON_AT_BOUNDARY_WITH_SLOW_RAMP);
    } else if (token.equals("onWithFastRamp")) {
      return (ON_AT_BOUNDARY_WITH_FAST_RAMP);
    } else if (token.equals("offWithSlowRamp")) {
      return (OFF_AT_BOUNDARY_WITH_SLOW_RAMP);
    } else if (token.equals("offWithFastRamp")) {
      return (OFF_AT_BOUNDARY_WITH_FAST_RAMP);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Figure out extrapolation strategy
  */
  
  public static String mapStrategy(int val) {  
    switch (val) {
      case NO_STRATEGY_SPECIFIED:
        return (null);      
      case ON_AT_BOUNDARY_WITH_SLOW_RAMP:
        return ("onWithSlowRamp");
      case ON_AT_BOUNDARY_WITH_FAST_RAMP: 
        return ("onWithFastRamp");
      case OFF_AT_BOUNDARY_WITH_SLOW_RAMP:      
        return ("offWithSlowRamp");
      case OFF_AT_BOUNDARY_WITH_FAST_RAMP:     
        return ("offWithFastRamp");
      default:
        throw new IllegalArgumentException();
    } 
  } 

  /***************************************************************************
  **
  ** Convenience function
  */
  
  public static boolean isOnAtBoundary(int val) {  
    return ((val == ON_AT_BOUNDARY_WITH_SLOW_RAMP) || 
            (val == ON_AT_BOUNDARY_WITH_FAST_RAMP) ||
            (val == NO_STRATEGY_SPECIFIED));
  }

  /***************************************************************************
  **
  ** Convenience function
  */
  
  public static boolean isOffAtBoundary(int val) {  
    return ((val == OFF_AT_BOUNDARY_WITH_SLOW_RAMP) || (val == OFF_AT_BOUNDARY_WITH_FAST_RAMP));
  }
  
  /***************************************************************************
  **
  ** Answer if strategies are inconsistent
  */
  
  public static boolean inconsistentStrategies(int startStrat, int endStrat) {
    if ((startStrat == NO_STRATEGY_SPECIFIED) && (endStrat == NO_STRATEGY_SPECIFIED)) {
      return (false);
    } else if (isOnAtBoundary(startStrat) && isOnAtBoundary(endStrat)) {
      return (false);
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("data");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static ExpressionEntry buildFromXML(DataAccessContext dacx, String elemName, 
                                             Attributes attrs) throws IOException {
    if (!elemName.equals("data")) {
      return (null);
    }
    
    String region = null; 
    String time = null; 
    String expr = null; 
    String confidence = null;
    String startStrategy = null;
    String endStrategy = null;
    String variable = null;
    String srcStr = null;
    String stratSrcStr = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("region")) {
          region = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("time")) {
          time = val;
        } else if (key.equals("expr")) {
          expr = val;
        } else if (key.equals("confidence")) {
          confidence = val;
        } else if (key.equals("starttype")) {
          startStrategy = val;
        } else if (key.equals("endtype")) {
          endStrategy = val;
        } else if (key.equals("value")) {
          variable = val;
        } else if (key.equals("source")) {
          srcStr = val;
        } else if (key.equals("stratSource")) {
          stratSrcStr = val;
        }
      }
    }
    
    if ((region == null) || (time == null) || (expr == null)) {
      throw new IOException();
    }
    
    return (new ExpressionEntry(dacx, region, time, expr, srcStr, confidence, stratSrcStr, startStrategy, endStrategy, variable));
  }
}
