/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.model;

/**
 * Data access execution plan.
 *
 * @author enrico.olivelli
 */
public class ExecutionPlan {

    public Statement mainStatement;
    public Aggregator mainAggregator;
    public ScanLimits limits;
    public TupleComparator comparator;
    public DMLStatement mutator;

    private ExecutionPlan(Statement mainStatement, Aggregator mainAggregator, ScanLimits limits, TupleComparator comparator, DMLStatement mutator) {
        this.mainStatement = mainStatement;
        this.mainAggregator = mainAggregator;
        this.limits = limits;
        this.comparator = comparator;
        this.mutator = mutator;
    }

    public static ExecutionPlan simple(Statement statement) {
        return new ExecutionPlan(statement, null, null, null, null);
    }

    public static ExecutionPlan make(Statement statement, Aggregator aggregator, ScanLimits limits, TupleComparator comparator, DMLStatement mutator) {
        return new ExecutionPlan(statement, aggregator, limits, comparator, mutator);
    }

    @Override
    public String toString() {
        return "ExecutionPlan{" + "mainStatement=" + mainStatement + ", mainAggregator=" + mainAggregator + ", limits=" + limits + ", comparator=" + comparator + ", mutator=" + mutator + '}';
    }

}