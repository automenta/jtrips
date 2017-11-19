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
package com.graphhopper.jsprit.core.algorithm.ruin;

import com.graphhopper.jsprit.core.algorithm.ruin.distance.EuclideanServiceDistance;
import com.graphhopper.jsprit.core.algorithm.ruin.distance.JobDistance;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class JobNeighborhoodsImplTest {

    VehicleRoutingProblem vrp;

    JobDistance jobDistance;

    Service target;
    Service s2;
    Service s3;
    Service s4;
    Service s5;
    Service s6;
    Service s7;

    @Before
    public void doBefore() {
        VehicleRoutingProblem.Builder builder = VehicleRoutingProblem.Builder.get();
        target = Service.Builder.newInstance("s1").sizeDimension(0, 1).location(Location.the(0, 5)).build();
        s2 = Service.Builder.newInstance("s2").sizeDimension(0, 1).location(Location.the(0, 4)).build();
        s3 = Service.Builder.newInstance("s3").sizeDimension(0, 1).location(Location.the(0, 3)).build();
        s4 = Service.Builder.newInstance("s4").sizeDimension(0, 1).location(Location.the(0, 2)).build();

        s5 = Service.Builder.newInstance("s5").sizeDimension(0, 1).location(Location.the(0, 6)).build();
        s6 = Service.Builder.newInstance("s6").sizeDimension(0, 1).location(Location.the(0, 7)).build();
        s7 = Service.Builder.newInstance("s7").sizeDimension(0, 1).location(Location.the(0, 8)).build();

        vrp = builder.addJob(target).addJob(s2).addJob(s3).addJob(s4).addJob(s5).addJob(s6).addJob(s7).build();

        jobDistance = new EuclideanServiceDistance();
    }

    @Test
    public void whenRequestingNeighborhoodOfTargetJob_nNeighborsShouldBeTwo() {
        JobNeighborhoodsImpl jn = new JobNeighborhoodsImpl(vrp, jobDistance);
        jn.initialise();
        Iterator<Job> iter = jn.getNearestNeighborsIterator(2, target);
        List<Service> services = new ArrayList<Service>();
        while (iter.hasNext()) {
            services.add((Service) iter.next());
        }
        assertEquals(2, services.size());
    }

    @Test
    public void whenRequestingNeighborhoodOfTargetJob_s2ShouldBeNeighbor() {
        JobNeighborhoodsImpl jn = new JobNeighborhoodsImpl(vrp, jobDistance);
        jn.initialise();
        Iterator<Job> iter = jn.getNearestNeighborsIterator(2, target);
        List<Service> services = new ArrayList<Service>();
        while (iter.hasNext()) {
            services.add((Service) iter.next());
        }
        assertTrue(services.contains(s2));
    }

    @Test
    public void whenRequestingNeighborhoodOfTargetJob_s4ShouldBeNeighbor() {
        JobNeighborhoodsImpl jn = new JobNeighborhoodsImpl(vrp, jobDistance);
        jn.initialise();
        Iterator<Job> iter = jn.getNearestNeighborsIterator(2, target);
        List<Service> services = new ArrayList<Service>();
        while (iter.hasNext()) {
            services.add((Service) iter.next());
        }
        assertTrue(services.contains(s5));
    }

    @Test
    public void whenRequestingNeighborhoodOfTargetJob_sizeShouldBe4() {
        JobNeighborhoodsImpl jn = new JobNeighborhoodsImpl(vrp, jobDistance);
        jn.initialise();
        Iterator<Job> iter = jn.getNearestNeighborsIterator(4, target);
        List<Service> services = new ArrayList<Service>();
        while (iter.hasNext()) {
            services.add((Service) iter.next());
        }
        assertEquals(4, services.size());
    }

    @Test
    public void whenRequestingMoreNeighborsThanExisting_itShouldReturnMaxNeighbors() {
        JobNeighborhoodsImpl jn = new JobNeighborhoodsImpl(vrp, jobDistance);
        jn.initialise();
        Iterator<Job> iter = jn.getNearestNeighborsIterator(100, target);
        List<Service> services = new ArrayList<Service>();
        while (iter.hasNext()) {
            services.add((Service) iter.next());
        }
        assertEquals(6, services.size());
    }

}
