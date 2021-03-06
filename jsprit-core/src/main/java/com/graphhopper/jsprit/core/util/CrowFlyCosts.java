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
/**
 *
 */
package com.graphhopper.jsprit.core.util;

import com.graphhopper.jsprit.core.problem.Location;


/**
 * @author stefan schroeder
 */
public class CrowFlyCosts extends EuclideanCosts {

    private final Locations locations;

    public CrowFlyCosts(Locations locations) {
        this.locations = locations;
    }

    @Override
    double distance(Location fromLocation, Location toLocation) {
        v2 from = null;
        v2 to = null;
        if (fromLocation.coord != null && toLocation.coord != null) {
            from = fromLocation.coord;
            to = toLocation.coord;
        } else if (locations != null) {
            from = locations.coord(fromLocation.id);
            to = locations.coord(toLocation.id);
        }
        return distance(from, to);
    }
}
