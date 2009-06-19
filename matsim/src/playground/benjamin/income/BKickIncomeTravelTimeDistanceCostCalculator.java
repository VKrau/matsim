/* *********************************************************************** *
 * project: org.matsim.*
 * BKickIncomeTravelTimeDistanceCostCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.benjamin.income;

import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.households.BasicIncome;
import org.matsim.core.basic.v01.households.BasicIncome.IncomePeriod;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.AbstractTravelTimeCalculator;


/**
 * @author dgrether
 *
 */
public class BKickIncomeTravelTimeDistanceCostCalculator implements TravelCost {

	private static double betaIncomeCar = 1.31;
	
	protected TravelTime timeCalculator;
	private double travelCostFactor;
	private double marginalUtlOfDistance;
	
	private double income;

	public BKickIncomeTravelTimeDistanceCostCalculator(AbstractTravelTimeCalculator travelTimeCalculator,
			CharyparNagelScoringConfigGroup charyparNagelScoring) {
		this.timeCalculator = travelTimeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = (- charyparNagelScoring.getTraveling() / 3600.0) + (charyparNagelScoring.getPerforming() / 3600.0);
		this.marginalUtlOfDistance = charyparNagelScoring.getMarginalUtlOfDistanceCar() * 1.31;
	
	
	}

	/**
	 * @see org.matsim.core.router.util.TravelCost#getLinkTravelCost(org.matsim.core.api.network.Link, double)
	 */
	public double getLinkTravelCost(Link link, double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - (this.marginalUtlOfDistance * link.getLength() / this.income);
	}
	
	
	public void setIncome(BasicIncome income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			this.income = income.getIncome() / (240 * 3.5);
		}
		else {
			throw new UnsupportedOperationException("Can't calculate income per trip");
		}
	}
	

}
