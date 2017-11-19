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
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;

public final class PickupShipment extends PickupActivity {

    private final Shipment shipment;

    private double endTime;

    private double arrTime;

    private double earliest;

    private double latest = Double.MAX_VALUE;

    public PickupShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    private PickupShipment(PickupShipment pickupShipmentActivity) {
        this.shipment = (Shipment) pickupShipmentActivity.job();
        this.arrTime = pickupShipmentActivity.arrTime();
        this.endTime = pickupShipmentActivity.end();
        index(pickupShipmentActivity.index());
        this.earliest = pickupShipmentActivity.startEarliest();
        this.latest = pickupShipmentActivity.startLatest();
    }

    @Override
    public Job job() {
        return shipment;
    }

    @Override
    public void startEarliest(double earliest) {
        this.earliest = earliest;
    }

    @Override
    public void startLatest(double latest) {
        this.latest = latest;
    }

    @Override
    public String name() {
        return "pickupShipment";
    }

    @Override
    public Location location() {
        return shipment.getPickupLocation();
    }

    @Override
    public double startEarliest() {
        return earliest;
    }

    @Override
    public double startLatest() {
        return latest;
    }

    @Override
    public double operationTime() {
        return shipment.getPickupServiceTime();
    }

    @Override
    public double arrTime() {
        return arrTime;
    }

    @Override
    public double end() {
        return endTime;
    }

    @Override
    public void arrTime(double arrTime) {
        this.arrTime = arrTime;
    }

    @Override
    public void end(double endTime) {
        this.endTime = endTime;
    }

    @Override
    public PickupShipment clone() {
        return new PickupShipment(this);
    }

    public String toString() {
        return "[type=" + name() + "][locationId=" + location().id
            + "][size=" + size()
            + "][twStart=" + Activities.round(startEarliest())
            + "][twEnd=" + Activities.round(startLatest()) + ']';
    }

    @Override
    public Capacity size() {
        return shipment.size();
    }


}
