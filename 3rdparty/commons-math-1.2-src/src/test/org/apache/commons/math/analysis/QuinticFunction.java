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
package org.apache.commons.math.analysis;

import java.io.Serializable;

import org.apache.commons.math.FunctionEvaluationException;

/**
 * Auxillary class for testing solvers.
 *
 * @version $Revision: 480442 $ $Date: 2006-11-29 00:21:22 -0700 (Wed, 29 Nov 2006) $ 
 */
public class QuinticFunction implements DifferentiableUnivariateRealFunction, Serializable {

    private static final long serialVersionUID = -8866263034920607152L;

    /* Evaluate quintic.
     * @see org.apache.commons.math.UnivariateRealFunction#value(double)
     */
    public double value(double x) throws FunctionEvaluationException {
        return (x-1)*(x-0.5)*x*(x+0.5)*(x+1);
    }

    public UnivariateRealFunction derivative() {
        return new UnivariateRealFunction() {
            public double value(double x) throws FunctionEvaluationException {
                return (5*x*x-3.75)*x*x+0.25;
            }
        };
    }
}
