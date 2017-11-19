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
package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.AbstractActivity;
import com.graphhopper.jsprit.core.problem.JobActivityFactory;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint.ConstraintsStatus;
import com.graphhopper.jsprit.core.problem.constraint.SoftActivityConstraint;
import com.graphhopper.jsprit.core.problem.constraint.SoftRouteConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.misc.ActivityContext;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.Start;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


final class ShipmentInsertionCalculator extends AbstractInsertionCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentInsertionCalculator.class);

    private final ConstraintManager constraintManager;

//    private HardRouteConstraint hardRouteLevelConstraint;
//
//    private HardActivityConstraint hardActivityLevelConstraint;

    private final SoftRouteConstraint softRouteConstraint;

    private final SoftActivityConstraint softActivityConstraint;

    private final ActivityInsertionCostsCalculator activityInsertionCostsCalculator;

    private final VehicleRoutingTransportCosts transportCosts;

    private final VehicleRoutingActivityCosts activityCosts;

    private JobActivityFactory activityFactory;

    private final AdditionalAccessEgressCalculator additionalAccessEgressCalculator;

    public ShipmentInsertionCalculator(VehicleRoutingTransportCosts routingCosts, VehicleRoutingActivityCosts activityCosts, ActivityInsertionCostsCalculator activityInsertionCostsCalculator, ConstraintManager constraintManager) {
        this.activityInsertionCostsCalculator = activityInsertionCostsCalculator;
        this.constraintManager = constraintManager;
        this.softActivityConstraint = constraintManager;
        this.softRouteConstraint = constraintManager;
        this.transportCosts = routingCosts;
        this.activityCosts = activityCosts;
        additionalAccessEgressCalculator = new AdditionalAccessEgressCalculator(routingCosts);
        logger.debug("initialise {}", this);
    }

    public void setJobActivityFactory(JobActivityFactory activityFactory) {
        this.activityFactory = activityFactory;
    }

    @Override
    public String toString() {
        return "[name=calculatesServiceInsertion]";
    }

    /**
     * Calculates the marginal cost of inserting job i locally. This is based on the
     * assumption that cost changes can entirely covered by only looking at the predecessor i-1 and its successor i+1.
     */
    @Override
    public InsertionData getInsertionData(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle, double newVehicleDepartureTime, final Driver newDriver, final double bestKnownCosts) {
        JobInsertionContext insertionContext = new JobInsertionContext(currentRoute, jobToInsert, newVehicle, newDriver, newVehicleDepartureTime);
        Shipment shipment = (Shipment) jobToInsert;
        AbstractActivity pickupShipment = activityFactory.the(shipment).get(0);
        AbstractActivity deliverShipment = activityFactory.the(shipment).get(1);
        insertionContext.getAssociatedActivities().add(pickupShipment);
        insertionContext.getAssociatedActivities().add(deliverShipment);

        /*
        check hard route constraints
         */
        InsertionData noInsertion = checkRouteContraints(insertionContext, constraintManager);
        if (noInsertion != null) return noInsertion;
        /*
        check soft route constraints
         */
        double additionalICostsAtRouteLevel = softRouteConstraint.getCosts(insertionContext);

        double bestCost = bestKnownCosts;
        additionalICostsAtRouteLevel += additionalAccessEgressCalculator.getCosts(insertionContext);

        int pickupInsertionIndex = InsertionData.NO_INDEX;
        int deliveryInsertionIndex = InsertionData.NO_INDEX;

        TimeWindow bestPickupTimeWindow = null;
        TimeWindow bestDeliveryTimeWindow = null;

        Start start = new Start(newVehicle.start(), newVehicle.earliestDeparture(), newVehicle.latestArrival());
        start.end(newVehicleDepartureTime);

        End end = new End(newVehicle.end(), 0.0, newVehicle.latestArrival());

        ActivityContext pickupContext = new ActivityContext();

        AbstractActivity prevAct = start;
        double prevActEndTime = newVehicleDepartureTime;

        //loops
        int i = 0;
        boolean tourEnd = false;
        //pickupShipmentLoop
        List<AbstractActivity> activities = currentRoute.tourActivities().activities();

        Collection<String> failedActivityConstraints = new ArrayList<>();
        while (!tourEnd) {
            AbstractActivity nextAct;
            if (i < activities.size()) {
                nextAct = activities.get(i);
            } else {
                nextAct = end;
                tourEnd = true;
            }

            boolean pickupInsertionNotFulfilledBreak = true;
            for(TimeWindow pickupTimeWindow : shipment.getPickupTimeWindows()) {
                pickupShipment.startEarliest(pickupTimeWindow.start);
                pickupShipment.startLatest(pickupTimeWindow.end);
                ActivityContext activityContext = new ActivityContext();
                activityContext.setInsertionIndex(i);
                insertionContext.setActivityContext(activityContext);
                ConstraintsStatus pickupShipmentConstraintStatus = fulfilled(insertionContext, prevAct, pickupShipment, nextAct, prevActEndTime, failedActivityConstraints, constraintManager);
                if (pickupShipmentConstraintStatus == ConstraintsStatus.NOT_FULFILLED) {
                    pickupInsertionNotFulfilledBreak = false;
                    continue;
                } else if(pickupShipmentConstraintStatus == ConstraintsStatus.NOT_FULFILLED_BREAK) {
                    continue;
                }
                else if (pickupShipmentConstraintStatus == ConstraintsStatus.FULFILLED) {
                    pickupInsertionNotFulfilledBreak = false;
                }
                double additionalPickupICosts = softActivityConstraint.getCosts(insertionContext, prevAct, pickupShipment, nextAct, prevActEndTime);
                double pickupAIC = calculate(insertionContext, prevAct, pickupShipment, nextAct, prevActEndTime);

                AbstractActivity prevAct_deliveryLoop = pickupShipment;
                double shipmentPickupArrTime = prevActEndTime + transportCosts.transportTime(prevAct.location(), pickupShipment.location(), prevActEndTime, newDriver, newVehicle);
                double shipmentPickupEndTime = Math.max(shipmentPickupArrTime, pickupShipment.startEarliest()) + activityCosts.getActivityDuration(pickupShipment, shipmentPickupArrTime, newDriver, newVehicle);

                pickupContext.setArrivalTime(shipmentPickupArrTime);
                pickupContext.setEndTime(shipmentPickupEndTime);
                pickupContext.setInsertionIndex(i);
                insertionContext.setRelatedActivityContext(pickupContext);

                double prevActEndTime_deliveryLoop = shipmentPickupEndTime;

			/*
            --------------------------------
			 */
                //deliverShipmentLoop
                int j = i;
                boolean tourEnd_deliveryLoop = false;
                while (!tourEnd_deliveryLoop) {
                    AbstractActivity nextAct_deliveryLoop;
                    if (j < activities.size()) {
                        nextAct_deliveryLoop = activities.get(j);
                    } else {
                        nextAct_deliveryLoop = end;
                        tourEnd_deliveryLoop = true;
                    }

                    boolean deliveryInsertionNotFulfilledBreak = true;
                    for (TimeWindow deliveryTimeWindow : shipment.getDeliveryTimeWindows()) {
                        deliverShipment.startEarliest(deliveryTimeWindow.start);
                        deliverShipment.startLatest(deliveryTimeWindow.end);
                        ActivityContext activityContext_ = new ActivityContext();
                        activityContext_.setInsertionIndex(j);
                        insertionContext.setActivityContext(activityContext_);
                        ConstraintsStatus deliverShipmentConstraintStatus = fulfilled(insertionContext, prevAct_deliveryLoop, deliverShipment, nextAct_deliveryLoop, prevActEndTime_deliveryLoop, failedActivityConstraints, constraintManager);
                        if (deliverShipmentConstraintStatus == ConstraintsStatus.FULFILLED) {
                            double additionalDeliveryICosts = softActivityConstraint.getCosts(insertionContext, prevAct_deliveryLoop, deliverShipment, nextAct_deliveryLoop, prevActEndTime_deliveryLoop);
                            double deliveryAIC = calculate(insertionContext, prevAct_deliveryLoop, deliverShipment, nextAct_deliveryLoop, prevActEndTime_deliveryLoop);
                            double totalActivityInsertionCosts = pickupAIC + deliveryAIC
                                + additionalICostsAtRouteLevel + additionalPickupICosts + additionalDeliveryICosts;
                            if (totalActivityInsertionCosts < bestCost) {
                                bestCost = totalActivityInsertionCosts;
                                pickupInsertionIndex = i;
                                deliveryInsertionIndex = j;
                                bestPickupTimeWindow = pickupTimeWindow;
                                bestDeliveryTimeWindow = deliveryTimeWindow;
                            }
                            deliveryInsertionNotFulfilledBreak = false;
                        } else if (deliverShipmentConstraintStatus == ConstraintsStatus.NOT_FULFILLED) {
                            deliveryInsertionNotFulfilledBreak = false;
                        }
                    }
                    if (deliveryInsertionNotFulfilledBreak) break;
                    //update prevAct and endTime
                    double nextActArrTime = prevActEndTime_deliveryLoop + transportCosts.transportTime(prevAct_deliveryLoop.location(), nextAct_deliveryLoop.location(), prevActEndTime_deliveryLoop, newDriver, newVehicle);
                    prevActEndTime_deliveryLoop = Math.max(nextActArrTime, nextAct_deliveryLoop.startEarliest()) + activityCosts.getActivityDuration(nextAct_deliveryLoop,nextActArrTime,newDriver,newVehicle);
                    prevAct_deliveryLoop = nextAct_deliveryLoop;
                    j++;
                }
            }
            if(pickupInsertionNotFulfilledBreak){
                break;
            }
            //update prevAct and endTime
            double nextActArrTime = prevActEndTime + transportCosts.transportTime(prevAct.location(), nextAct.location(), prevActEndTime, newDriver, newVehicle);
            prevActEndTime = Math.max(nextActArrTime, nextAct.startEarliest()) + activityCosts.getActivityDuration(nextAct,nextActArrTime,newDriver,newVehicle);
            prevAct = nextAct;
            i++;
        }
        if (pickupInsertionIndex == InsertionData.NO_INDEX) {
            InsertionData emptyInsertionData = new InsertionData.NoInsertionFound();
            emptyInsertionData.getFailedConstraintNames().addAll(failedActivityConstraints);
            return emptyInsertionData;
        }
        InsertionData insertionData = new InsertionData(bestCost, pickupInsertionIndex, deliveryInsertionIndex, newVehicle, newDriver);
        pickupShipment.startEarliest(bestPickupTimeWindow.start);
        pickupShipment.startLatest(bestPickupTimeWindow.end);
        deliverShipment.startEarliest(bestDeliveryTimeWindow.start);
        deliverShipment.startLatest(bestDeliveryTimeWindow.end);
        insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
        insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, deliverShipment, deliveryInsertionIndex));
        insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, pickupShipment, pickupInsertionIndex));
        insertionData.getEvents().add(new SwitchVehicle(currentRoute, newVehicle, newVehicleDepartureTime));
        return insertionData;
    }

    private double calculate(JobInsertionContext iFacts, AbstractActivity prevAct, AbstractActivity newAct, AbstractActivity nextAct, double departureTimeAtPrevAct) {
        return activityInsertionCostsCalculator.getCosts(iFacts, prevAct, nextAct, newAct, departureTimeAtPrevAct);

    }
}
