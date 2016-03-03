package org.koala.runnersFramework.runners.bot.util;

import java.util.ArrayList;

public class DistributionPredictor {

	
	ArrayList<Double> x, y;
	double meanX, varXsq, meanY, varYsq;
	double beta0, beta1;
	int N;
	
	public DistributionPredictor(int n) {
		
		N=n;
		x = new ArrayList<Double>();
		x.add(new Double(561)); x.add(new Double(610));
		x.add(new Double(618)); x.add(new Double(560)); x.add(new Double(581)); x.add(new Double(612));
		x.add(new Double(550)); x.add(new Double(618)); x.add(new Double(630)); x.add(new Double(626));
		x.add(new Double(576)); x.add(new Double(596)); x.add(new Double(534)); x.add(new Double(647));
		x.add(new Double(604)); 
		/*x.add(new Double()); x.add(new Double()); x.add(new Double());
		x.add(new Double()); x.add(new Double()); x.add(new Double()); x.add(new Double());
		x.add(new Double()); x.add(new Double()); x.add(new Double()); x.add(new Double());
		x.add(new Double()); x.add(new Double()); x.add(new Double()); x.add(new Double());*/
		y = new ArrayList<Double>();
		y.add(new Double(842)); y.add(new Double(915)); y.add(new Double(927)); y.add(new Double(840)); 
		y.add(new Double(872)); y.add(new Double(919));
		/*y.add(new Double()); y.add(new Double());
		y.add(new Double()); y.add(new Double()); y.add(new Double()); y.add(new Double());
		y.add(new Double()); y.add(new Double()); y.add(new Double()); y.add(new Double());
		y.add(new Double()); y.add(new Double()); y.add(new Double()); y.add(new Double());*/
		
	}
	
	public DistributionPredictor(ArrayList<Double> x) {
		this.x = x;
	}
	
	public void estimateX() {
	
		double sumX = 0.0;
		double sumvarsq = 0.0;
		for (Double xi : x) {
			sumX += xi.doubleValue();
		}
		meanX = sumX / x.size();
		
		for(Double xi : x) {
			sumvarsq += Math.pow(xi.doubleValue()-meanX, 2);
		}
		
		varXsq = sumvarsq / x.size();
	}
	
	public void linearRegression() {
		
		double sumX = 0.0;
		double sumY = 0.0;
		
		for(int i=0; i<y.size() ; i++) {
			sumX += x.get(i).doubleValue();
			sumY += y.get(i).doubleValue();			
		}
		
		double xBar = sumX / y.size();
		double yBar = sumY / y.size();
		
		double sumProdVars = 0.0;
		double sumvarXsq = 0.0;
		
		for(int i=0; i<y.size(); i++) {
			sumProdVars += (x.get(i).doubleValue() - xBar) * (y.get(i).doubleValue() - yBar);
			sumvarXsq += Math.pow(x.get(i).doubleValue() - xBar, 2);
		}
		
		beta1 = sumProdVars / sumvarXsq;
		beta0 = yBar - beta1*xBar;
	}
	
	public void estimateY() {
		
		meanY = beta0 + beta1 * meanX;
		varYsq = beta1*beta1*varXsq;
	}
	
	public void confInterval() {
		
		
	}
	
	public void print() {
		System.out.println("meanX=" + meanX + "; sigmaX=" + Math.sqrt(varXsq));
		System.out.println("meanY=" + meanY + "; sigmaY=" + Math.sqrt(varYsq));
		System.out.println("beta0=" + beta0 + "; beta1=" + beta1);
	}
	
	public static void main(String args[]) {
		
		DistributionPredictor dp = new DistributionPredictor(2);
		dp.estimateX();
		dp.linearRegression();
		dp.estimateY();
		dp.print();
	}
}
