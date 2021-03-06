/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.jsprit.core.problem.solution.route.activity;

import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Service;

public final class PickupService extends PickupActivity {

    private final Service pickup;

    private double arrTime;

    private double depTime;

    private double theoreticalEarliest;

    private double theoreticalLatest = Double.MAX_VALUE;

    public PickupService(Pickup pickup) {
        this.pickup = pickup;
    }

    public PickupService(Service service) {
        this.pickup = service;
    }

    private PickupService(PickupService pickupActivity) {
        this.pickup = pickupActivity.job();
        this.arrTime = pickupActivity.arrTime();
        this.depTime = pickupActivity.end();
        index(pickupActivity.index());
        this.theoreticalEarliest = pickupActivity.startEarliest();
        this.theoreticalLatest = pickupActivity.startLatest();
    }

    @Override
    public String name() {
        return pickup.type;
    }

    @Override
    public Location location() {
        return pickup.location;
    }

    @Override
    public double startEarliest() {
        return theoreticalEarliest;
    }

    @Override
    public double startLatest() {
        return theoreticalLatest;
    }

    @Override
    public void startEarliest(double earliest) {
        this.theoreticalEarliest = earliest;
    }

    @Override
    public void startLatest(double latest) {
        this.theoreticalLatest = latest;
    }

    @Override
    public double operationTime() {
        return pickup.serviceTime;
    }

    @Override
    public double arrTime() {
        return arrTime;
    }

    @Override
    public double end() {
        return depTime;
    }

    @Override
    public void arrTime(double arrTime) {
        this.arrTime = arrTime;
    }

    @Override
    public void end(double endTime) {
        this.depTime = endTime;
    }

    @Override
    public PickupService clone() {
        return new PickupService(this);
    }

    @Override
    public Service job() {
        return pickup;
    }

    public String toString() {
        return "[type=" + name() + "][locationId=" + location().id
            + "][size=" + size()
            + "][twStart=" + Activities.round(startEarliest())
            + "][twEnd=" + Activities.round(startLatest()) + ']';
    }

    @Override
    public Capacity size() {
        return pickup.size;
    }

}
