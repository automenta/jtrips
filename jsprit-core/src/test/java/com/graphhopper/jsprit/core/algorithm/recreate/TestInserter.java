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

import com.graphhopper.jsprit.core.algorithm.recreate.listener.InsertionListeners;
import com.graphhopper.jsprit.core.algorithm.state.UpdateEndLocationIfRouteIsOpen;
import com.graphhopper.jsprit.core.problem.Capacity;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestInserter {


    @Test
    public void whenInsertingServiceAndRouteIsClosed_itInsertsCorrectly() {
        Service service = mock(Service.class);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.start()).thenReturn(loc("vehLoc"));
        when(vehicle.end()).thenReturn(loc("vehLoc"));
        when(vehicle.isReturnToDepot()).thenReturn(true);
        when(vehicle.id()).thenReturn("vehId");

        when(service.timeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addService(service).build();
        //start - pick(shipment) - del(shipment) - end
        Service serviceToInsert = mock(Service.class);
        when(serviceToInsert.location()).thenReturn(loc("delLoc"));

        InsertionData iData = mock(InsertionData.class);
        when(iData.getDeliveryInsertionIndex()).thenReturn(1);
        when(iData.getSelectedVehicle()).thenReturn(vehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        List<JobActivity> acts = new ArrayList<>();
        PickupService act = new PickupService(serviceToInsert);
        acts.add(act);
        when(vehicleRoutingProblem.copyAndGetActivities(serviceToInsert)).thenReturn(acts);
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(serviceToInsert, iData, route);

        assertEquals(2, route.tourActivities().activities().size());
        assertEquals(route.tourActivities().activities().get(1).location().id, serviceToInsert.location.id);
        assertEquals(route.end.location().id, vehicle.end().id);
    }

    private Location loc(String vehLoc) {
        return Location.Builder.the().setId(vehLoc).build();
    }

    @Test
    public void whenInsertingServiceAndRouteIsOpen_itInsertsCorrectlyAndSwitchesEndLocation() {
        Service service = mock(Service.class);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.start()).thenReturn(Location.the("vehLoc"));
        when(vehicle.end()).thenReturn(Location.the("vehLoc"));
        when(vehicle.isReturnToDepot()).thenReturn(false);
        when(vehicle.id()).thenReturn("vehId");

        when(service.timeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addService(service).build();
        Service serviceToInsert = mock(Service.class);
        when(serviceToInsert.location()).thenReturn(Location.Builder.the().setId("delLoc").build());

        InsertionData iData = mock(InsertionData.class);
        when(iData.getDeliveryInsertionIndex()).thenReturn(1);
        when(iData.getSelectedVehicle()).thenReturn(vehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(serviceToInsert)).thenReturn(getTourActivities(serviceToInsert));

        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(serviceToInsert, iData, route);

        assertEquals(2, route.tourActivities().activities().size());
        assertEquals(route.tourActivities().activities().get(1).location().id, serviceToInsert.location.id);
        assertEquals(route.end.location().id, serviceToInsert.location.id);
    }

    private List<JobActivity> getTourActivities(Service serviceToInsert) {
        List<JobActivity> acts = new ArrayList<>(1);
        acts.add(new PickupService(serviceToInsert));
        return acts;
    }


    @Test
    public void whenInsertingShipmentAndRouteIsClosed_itInsertsCorrectly() {
        Shipment shipment = mock(Shipment.class);
        Capacity capacity = Capacity.Builder.get().build();
        when(shipment.size()).thenReturn(capacity);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.start()).thenReturn(loc("vehLoc"));
        when(vehicle.end()).thenReturn(loc("vehLoc"));
        when(vehicle.isReturnToDepot()).thenReturn(true);
        when(vehicle.id()).thenReturn("vehId");

        when(shipment.getPickupTimeWindow()).thenReturn(mock(TimeWindow.class));
        when(shipment.getDeliveryTimeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addPickup(shipment).addDelivery(shipment).build();
        //start - pick(shipment) - del(shipment) - end
        Shipment shipmentToInsert = Shipment.Builder.newInstance("s").setDeliveryLocation(Location.the("delLoc")).setPickupLocation(Location.Builder.the().setId("pickLoc").build()).build();

        InsertionData iData = mock(InsertionData.class);
        when(iData.getPickupInsertionIndex()).thenReturn(2);
        when(iData.getDeliveryInsertionIndex()).thenReturn(2);
        when(iData.getSelectedVehicle()).thenReturn(vehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(shipmentToInsert)).thenReturn(getTourActivities(shipmentToInsert));
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(shipmentToInsert, iData, route);

        assertEquals(4, route.tourActivities().activities().size());
        assertEquals(route.tourActivities().activities().get(2).location().id, shipmentToInsert.getPickupLocation().id);
        assertEquals(route.tourActivities().activities().get(3).location().id, shipmentToInsert.getDeliveryLocation().id);
        assertEquals(route.end.location().id, vehicle.end().id);
    }

    private List<JobActivity> getTourActivities(Shipment shipmentToInsert) {
        List<JobActivity> acts = new ArrayList<>();
        acts.add(new PickupShipment(shipmentToInsert));
        acts.add(new DeliverShipment(shipmentToInsert));
        return acts;
    }

    @Test
    public void whenInsertingShipmentAndRouteIsOpen_itInsertsCorrectlyAndSwitchesEndLocation() {
        Shipment shipment = mock(Shipment.class);
        Capacity capacity = Capacity.Builder.get().build();
        when(shipment.size()).thenReturn(capacity);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.isReturnToDepot()).thenReturn(false);
        when(vehicle.id()).thenReturn("vehId");

        when(shipment.getPickupTimeWindow()).thenReturn(mock(TimeWindow.class));
        when(shipment.getDeliveryTimeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addPickup(shipment).addDelivery(shipment).build();
        //start - pick(shipment) - del(shipment) - end
        Shipment shipmentToInsert = Shipment.Builder.newInstance("s").setDeliveryLocation(Location.the("delLoc")).setPickupLocation(Location.Builder.the().setId("pickLoc").build()).build();
        InsertionData iData = mock(InsertionData.class);
        when(iData.getPickupInsertionIndex()).thenReturn(2);
        when(iData.getDeliveryInsertionIndex()).thenReturn(2);
        when(iData.getSelectedVehicle()).thenReturn(vehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(shipmentToInsert)).thenReturn(getTourActivities(shipmentToInsert));
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(shipmentToInsert, iData, route);

        assertEquals(4, route.tourActivities().activities().size());
        assertEquals(route.tourActivities().activities().get(2).location().id, shipmentToInsert.getPickupLocation().id);
        assertEquals(route.tourActivities().activities().get(3).location().id, shipmentToInsert.getDeliveryLocation().id);
        assertEquals(route.end.location().id, shipmentToInsert.getDeliveryLocation().id);
    }

    @Test
    public void whenSwitchingVehicleAndRouteIsClosed_newStartAndEndShouldBeTheLocationOfNewVehicle() {
        Shipment shipment = mock(Shipment.class);
        Capacity capacity = Capacity.Builder.get().build();
        when(shipment.size()).thenReturn(capacity);
        Vehicle vehicle = VehicleImpl.Builder.newInstance("vehId").setStartLocation(Location.the("vehLoc")).setType(mock(VehicleType.class)).build();
        Vehicle newVehicle = VehicleImpl.Builder.newInstance("newVehId").setStartLocation(Location.the("newVehLoc")).setType(mock(VehicleType.class)).build();

        when(shipment.getPickupTimeWindow()).thenReturn(mock(TimeWindow.class));
        when(shipment.getDeliveryTimeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addPickup(shipment).addDelivery(shipment).build();
        //start - pick(shipment) - del(shipment) - end
        Shipment shipmentToInsert = Shipment.Builder.newInstance("s").setPickupLocation(Location.Builder.the().setId("pickLoc").build()).setDeliveryLocation(Location.the("delLoc")).build();

        InsertionData iData = mock(InsertionData.class);
        when(iData.getPickupInsertionIndex()).thenReturn(2);
        when(iData.getDeliveryInsertionIndex()).thenReturn(2);
        when(iData.getSelectedVehicle()).thenReturn(newVehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(shipmentToInsert)).thenReturn(getTourActivities(shipmentToInsert));
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(shipmentToInsert, iData, route);

        assertEquals(route.end.location().id, newVehicle.end().id);
    }

    @Test
    public void whenSwitchingVehicleAndRouteIsOpen_endLocationShouldBeTheLocationOfTheLastActivity() {
        Shipment shipment = mock(Shipment.class);
        Capacity capacity = Capacity.Builder.get().build();
        when(shipment.size()).thenReturn(capacity);
        Vehicle vehicle = VehicleImpl.Builder.newInstance("vehId").setReturnToDepot(false).setStartLocation(Location.the("vehLoc")).setType(mock(VehicleType.class)).build();
        Vehicle newVehicle = VehicleImpl.Builder.newInstance("newVehId").setReturnToDepot(false).setStartLocation(Location.the("newVehLoc")).setType(mock(VehicleType.class)).build();

        when(shipment.getPickupTimeWindow()).thenReturn(mock(TimeWindow.class));
        when(shipment.getDeliveryTimeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addPickup(shipment).addDelivery(shipment).build();
        //start - pick(shipment) - del(shipment) - end
        Shipment shipmentToInsert = Shipment.Builder.newInstance("s").setPickupLocation(Location.Builder.the().setId("pickLoc").build()).setDeliveryLocation(Location.the("delLoc")).build();

        InsertionData iData = mock(InsertionData.class);
        when(iData.getPickupInsertionIndex()).thenReturn(2);
        when(iData.getDeliveryInsertionIndex()).thenReturn(2);
        when(iData.getSelectedVehicle()).thenReturn(newVehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(shipmentToInsert)).thenReturn(getTourActivities(shipmentToInsert));
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(shipmentToInsert, iData, route);

        assertEquals("delLoc", route.end.location().id);
    }

    @Test
    public void whenInsertingShipmentAtBeginningAndSwitchingVehicleAndRouteIsOpen_endLocationShouldBeTheLocationOfTheLastActivity() {
        Shipment shipment = mock(Shipment.class);
        Capacity capacity = Capacity.Builder.get().build();
        when(shipment.size()).thenReturn(capacity);
        when(shipment.getDeliveryLocation()).thenReturn(Location.Builder.the().setId("oldShipmentDelLoc").build());
        Vehicle vehicle = VehicleImpl.Builder.newInstance("vehId").setReturnToDepot(false).setStartLocation(Location.Builder.the().setId("vehLoc").build()).setType(mock(VehicleType.class)).build();
        Vehicle newVehicle = VehicleImpl.Builder.newInstance("newVehId").setReturnToDepot(false).setStartLocation(Location.Builder.the().setId("newVehLoc").build()).setType(mock(VehicleType.class)).build();

        when(shipment.getPickupTimeWindow()).thenReturn(mock(TimeWindow.class));
        when(shipment.getDeliveryTimeWindow()).thenReturn(mock(TimeWindow.class));

        VehicleRoute route = VehicleRoute.Builder.newInstance(vehicle, mock(Driver.class)).addPickup(shipment).addDelivery(shipment).build();
        //start - pick(shipment) - del(shipment) - end
        Shipment shipmentToInsert = Shipment.Builder.newInstance("s").setPickupLocation(Location.Builder.the().setId("pickLoc").build()).setDeliveryLocation(Location.the("delLoc")).build();

        InsertionData iData = mock(InsertionData.class);
        when(iData.getPickupInsertionIndex()).thenReturn(0);
        when(iData.getDeliveryInsertionIndex()).thenReturn(0);
        when(iData.getSelectedVehicle()).thenReturn(newVehicle);

        VehicleRoutingProblem vehicleRoutingProblem = mock(VehicleRoutingProblem.class);
        when(vehicleRoutingProblem.copyAndGetActivities(shipmentToInsert)).thenReturn(getTourActivities(shipmentToInsert));
        Inserter inserter = new Inserter(mock(InsertionListeners.class), vehicleRoutingProblem);
        inserter.insertJob(shipmentToInsert, iData, route);

        UpdateEndLocationIfRouteIsOpen updateEnd = new UpdateEndLocationIfRouteIsOpen();
        updateEnd.visit(route);

        assertEquals("oldShipmentDelLoc", route.end.location().id);
    }

}
