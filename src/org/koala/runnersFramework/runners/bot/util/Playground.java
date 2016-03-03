package org.koala.runnersFramework.runners.bot.util;

public class Playground {

	int N,n, c, alpha;
	double v, beta;
	private double M_sample;
	private double B_sample;
	private double N_sample;
	
	double makespanBmin; 
	double Bmin;
	double profitableSpeed;
	int profitableCostUnit;
	
	public Playground(int N1, int c, double v, int alpha, double beta) {
		super();
		N = N1;
		double zeta_sq = 1.96 * 1.96;
		n = (int) Math.ceil(N * zeta_sq
				/ (zeta_sq + 2 * (N - 1) * 0.25 * 0.25));
		
		this.c = c;
		this.alpha = alpha;
		this.v = v;
		this.beta = beta;
		
	}
	
	public void setProfitableSpeed(double speed, int cunit) {
		profitableSpeed = speed;
		profitableCostUnit = cunit;
	}
	
	public int getDeltaNLT (int jobsLeft, int M, int Bmin, int a, int b, double stDev) {
		
		
		int deltaNLT = 0;
		double Tlj = (jobsLeft-a-b)/(a*v+b*v*beta);
		System.out.println("Tlj(min)=" + Tlj*60);
		if(Tlj*60 + (60/v + stDev) > M*60) deltaNLT=1;		
		
		System.out.println("Pg: alpha=" + alpha + "; beta=" + beta + "; v=" + v +
				"; a=" + a + "; b=" + b + "; M=" + M);
		
		return deltaNLT;
		
	}
	
	public void RR() {
		double Mrr=Math.ceil(N/(32*v*(1+beta)));
		double Brr=32*c*(1+alpha)*Mrr;
		System.out.println("Mrr="+Mrr+" ATU; Brr=" +Brr);
	}

	public void Sample() {
		M_sample=Math.ceil(1/v);
		B_sample=n*c*(1+alpha)*M_sample;
		N_sample=Math.floor(n*M_sample*v*(1+beta));
		System.out.println("Msample: " + M_sample + " ATU; B_sample: " + B_sample + "; N_sample: " + N_sample);
	}
	
	public void BaTS(int a, int b) {
		double Mab=M_sample+Math.ceil((N-N_sample)/(v*(a+beta*b)));		
		double Bab=B_sample+c*(a+alpha*b)*Math.ceil((N-N_sample)/(v*(a+beta*b)));
		System.out.println("a="+a+"; b="+b);
		System.out.println("Mab="+Mab+" ATU; Bab=" +Bab);
		System.out.println("Approx time (min): " + (60+(N-N_sample)/(v*(a+beta*b))*60));
	}

	public void BminNoSampling(boolean first) {
		double MBmin, Bmin;
		if(first) {
			MBmin=Math.ceil(N/v);
			Bmin=c*MBmin;
		} else {
			MBmin=Math.ceil(N/(v*beta));
			Bmin=c*alpha*MBmin;
		}
		System.out.println("MBminNoSample="+MBmin+" ATU; BminNoSample=" +Bmin);		
	}
	
	public static void main(String args[]){
		Playground pg = new Playground(1000,3,60/13.57,Integer.parseInt(args[0]),Double.parseDouble(args[1]));
		pg.RR();
		pg.Sample();
		pg.BminNoSampling(true);
		pg.BaTS(1, 0);
		pg.BaTS(32, 0);
		pg.BaTS(32,32);
		pg.BaTS(32,11);
		pg.BaTS(32,12);
		pg.BaTS(32,13);
		pg.BaTS(32,14); 
		pg.BaTS(32,15);
		pg.BaTS(32,16);
		System.out.println(Math.ceil((double)(1282634046208L-1282632876940L)/60000/60));
		double factor = 3.0;
		long et = (long)((double)Long.parseLong("124123481209")/factor);
		System.out.println(et);
		
		int deltaNLT = pg.getDeltaNLT(873, 2, 450, 0, 25, 9.58);
		System.out.println("deltaNLT=" + deltaNLT);
	}
}
