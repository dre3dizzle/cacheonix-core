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

package org.apache.commons.math.linear;

/**
 * Thrown when a system attempts an operation on a matrix, and
 * that matrix does not satisfy the preconditions for the
 * aforementioned operation.
 * @version $Revision: 480440 $ $Date: 2006-11-29 00:14:12 -0700 (Wed, 29 Nov 2006) $
 */
public class InvalidMatrixException extends RuntimeException {

    /** Serializable version identifier */
    private static final long serialVersionUID = 5318837237354354107L;

    /**
     * Default constructor.
     */
    public InvalidMatrixException() {
        this(null);
    }

    /**
     * Construct an exception with the given message.
     * @param message descriptive error message.
     */
    public InvalidMatrixException(String message) {
        super(message);
    }

}
