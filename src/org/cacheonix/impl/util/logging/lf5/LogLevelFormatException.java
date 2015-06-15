/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cacheonix.impl.util.logging.lf5;

/**
 * Thrown to indicate that the client has attempted to convert a string to one the LogLevel types, but the string does
 * not have the appropriate format.
 *
 * @author Michael J. Sikorsky<
 * @author Robert Shaw
 */

// Contributed by ThoughtWorks Inc.

public final class LogLevelFormatException extends Exception {

   //--------------------------------------------------------------------------
   //   Constants:
   //--------------------------------------------------------------------------
   private static final long serialVersionUID = 0L;

   //--------------------------------------------------------------------------
   //   Protected Variables:
   //--------------------------------------------------------------------------

   //--------------------------------------------------------------------------
   //   Private Variables:
   //--------------------------------------------------------------------------

   //--------------------------------------------------------------------------
   //   Constructors:
   //--------------------------------------------------------------------------


   public LogLevelFormatException(final String message) {
      super(message);
   }

   //--------------------------------------------------------------------------
   //   Public Methods:
   //--------------------------------------------------------------------------

   //--------------------------------------------------------------------------
   //   Protected Methods:
   //--------------------------------------------------------------------------

   //--------------------------------------------------------------------------
   //   Private Methods:
   //--------------------------------------------------------------------------

   //--------------------------------------------------------------------------
   //   Nested Top-Level Classes or Interfaces:
   //--------------------------------------------------------------------------

}






