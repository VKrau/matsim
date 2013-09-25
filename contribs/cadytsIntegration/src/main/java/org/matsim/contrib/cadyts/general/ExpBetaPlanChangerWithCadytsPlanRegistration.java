/**
 * 
 */
package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * @author nagel
 *
 */
public final class ExpBetaPlanChangerWithCadytsPlanRegistration<T> implements PlanSelector {

	private final PlanSelector delegate ;
	private final CadytsContextI<T> cContext;
	
	public ExpBetaPlanChangerWithCadytsPlanRegistration(double beta, CadytsContextI<T> cContext ) {
		delegate = new ExpBetaPlanChanger( beta ) ;
		this.cContext = cContext ;
	}
	

	@Override
	public Plan selectPlan(Person person) {
		Plan selectedPlan = delegate.selectPlan(person) ;
		cadyts.demand.Plan<T> cadytsPlan = cContext.getPlansTranslator().getPlanSteps( selectedPlan ) ;
		cContext.getCalibrator().addToDemand(cadytsPlan) ;
		return selectedPlan ;
	}
	
}
