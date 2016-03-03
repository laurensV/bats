import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Random;

class State {
	//etaj lift
	int fl1, fl2;
	//numar pasageri lift
	int dim1, dim2;
	//unitatea de timp curenta
	int timeUnit;
	int prevState1, prevState2;
	int reward;
	ArrayList<Passenger> passIn1 = new ArrayList<Passenger>();
	ArrayList<Passenger> passIn2 = new ArrayList<Passenger>();
	ArrayList<Passenger> passWaiting = new ArrayList<Passenger>();
	String state;
	ArrayList<State> nextStateList = new ArrayList<State>();
	ArrayList<Double> qOld = new ArrayList<Double>();
	ArrayList<Double> qNew = new ArrayList<Double>();
	
	//componente pentru a calcula reward
	//timpul total minim de intarziere al pasagerilor daca
	//lifturile ar face astfel inca cererea fiecaruia sa
	//fie indeplinita imediat
	int minTotalLateTime;
	//timpul minim de asteptare al pasagerilor
	int minTotalTimeWaiting;
	
	//numarul de pasageri debarcati in acest moment de timp
	int passSolvedNo;
	
	int totalWaitingTime;
	
	public void addPassengerWaiting(ArrayList<Passenger> al){
		for (Passenger p : al)
			passWaiting.add(p);
	}
	
	public void updateWaiting(){
		for (Passenger p : passIn1)
			if (passWaiting.contains(p))
				passWaiting.remove(p);
		
		for (Passenger p : passIn2)
			if (passWaiting.contains(p))
				passWaiting.remove(p);
		
	}
	
	public void computeReward(){
		reward = (-100) * minTotalLateTime + (-10) * minTotalTimeWaiting + 100 * passSolvedNo; 
	}
	
	public void initQ(){
		qNew = new ArrayList<Double>();
		qOld = new ArrayList<Double>();
		for (int i = 0; i < nextStateList.size(); i++){
			qNew.add(0.0);
			qOld.add(0.0);
		}
	}
}

class Passenger {
	int flInit;
	int flDest;
	int reqTime;
	
	public Passenger(int reqTime, int flInit, int flDest) {
		this.reqTime = reqTime;
		this.flInit = flInit;
		this.flDest = flDest;
	}
}

public class Sarsa {

	final static int floorNo = 5;
	static double eps = 300;
	static double delta = 0.5;
	static double alfa = 1;
	final static int up = 0, down = 1, open = 2, close = 3;
	final static int timeInit = 0;
	final static int limit = 10;
	static ArrayList<State> sl = new ArrayList<State>();
	static ArrayList<ArrayList<Passenger>> passList = new ArrayList<ArrayList<Passenger>>();
	static Hashtable<String, Double> stateHt = new Hashtable<String, Double>(); 
	static int nr_ep;
	
	
	public static void main(String[] args) {
		nr_ep = Integer.parseInt(args[0]);
		init();
		SARSA();
	}

	private static ArrayList<State> computeNextStateList(State s){
		int prevState1, prevState2;
		ArrayList<State> al = new ArrayList<State>();
		
		int fl1 = s.fl1, fl2 = s.fl2;
		if (s.prevState1 == up) fl1 = s.fl1+1;
		if (s.prevState2 == up) fl2 = s.fl2+1;
		if (s.prevState1 == down) fl1 = s.fl1-1;
		if (s.prevState2 == down) fl2 = s.fl2-1;
		
		//ambele lifturi merg sus
		State s1 = new State();
		if (fl1 + 1 != floorNo && fl2 + 1 != floorNo && s.prevState1 != open && s.prevState2 != open){
			s1.fl1 = fl1;
			s1.fl2 = fl2;
			s1.prevState1 = up;
			s1.prevState2 = up;
			s1.timeUnit = s.timeUnit + 1;
			s1.passIn1 = new ArrayList<Passenger>(s.passIn1);
			s1.passIn2 = new ArrayList<Passenger>(s.passIn2);
			s1.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s1.addPassengerWaiting(passList.get(s1.timeUnit));
			s1.minTotalLateTime = computeMinTotalLateTime(s1);
			s1.minTotalTimeWaiting = computeMinTotalTimeWaiting(s1);
			s1.passSolvedNo = 0;
			al.add(s1);
		}
		
		//ambele lifturi merg jos
		State s2 = new State();
		if (fl1 != 0 && fl2 != 0 && s.prevState1 != open && s.prevState2 != open){
			s2.fl1 = fl1;
			s2.fl2 = fl2;
			s2.prevState1 = down;
			s2.prevState2 = down;
			s2.timeUnit = s.timeUnit + 1;
			s2.passIn1 = new ArrayList<Passenger>(s.passIn1);
			s2.passIn2 = new ArrayList<Passenger>(s.passIn2);
			s2.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s2.addPassengerWaiting(passList.get(s2.timeUnit));
			s2.minTotalLateTime = computeMinTotalLateTime(s2);
			s2.minTotalTimeWaiting = computeMinTotalTimeWaiting(s2);
			s2.passSolvedNo = 0;
			al.add(s2);
		}
		
		//liftul 1 merge sus si liftul 2 jos
		State s3 = new State();
		if (fl1 + 1 != floorNo && fl2 != 0 && s.prevState1 != open && s.prevState2 != open){
			s3.fl1 = fl1;
			s3.fl2 = fl2;
			s3.prevState1 = up;
			s3.prevState2 = down;
			s3.timeUnit = s.timeUnit+1;
			s3.passIn1 = new ArrayList<Passenger>(s.passIn1);
			s3.passIn2 = new ArrayList<Passenger>(s.passIn2);
			s3.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s3.addPassengerWaiting(passList.get(s3.timeUnit));
			s3.minTotalLateTime = computeMinTotalLateTime(s3);
			s3.minTotalTimeWaiting = computeMinTotalTimeWaiting(s3);
			s3.passSolvedNo = 0;
			al.add(s3);
		}
		
		//liftul 1 merge sus si liftul 2 jos
		State s4 = new State();
		if (fl1 != 0 && fl2 + 1!= floorNo && s.prevState1 != open && s.prevState2 != open){
			s4.fl1 = fl1;
			s4.fl2 = fl2;
			s4.prevState1 = down;
			s4.prevState2 = up;
			s4.timeUnit = s.timeUnit+1;
			s4.passIn1 = new ArrayList<Passenger>(s.passIn1);
			s4.passIn2 = new ArrayList<Passenger>(s.passIn2);
			s4.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s4.addPassengerWaiting(passList.get(s4.timeUnit));
			s4.minTotalLateTime = computeMinTotalLateTime(s4);
			s4.minTotalTimeWaiting = computeMinTotalTimeWaiting(s4);
			s4.passSolvedNo = 0;
			al.add(s4);
		}
		
		//liftul 1 merge sus si liftul 2 va deschide usile si coboara pasageri
		State s5 = new State();
		if (fl1 +1 != floorNo && s.prevState1 != open){
			s5.fl1 = fl1;
			s5.fl2 = fl2;
			s5.prevState1 = up;
			s5.prevState2 = open;
			s5.timeUnit = s.timeUnit+1;
			s5.passIn1 = new ArrayList<Passenger>(s.passIn1);
			
			//pasagerii din liftul 2 coboara
			ArrayList<Passenger> passIn = new ArrayList<Passenger>();
			s5.passSolvedNo = 0;
			for (Passenger p : s.passIn2)
				if (p.flDest != fl2)
					passIn.add(p);
				else {
					s5.passSolvedNo++;
					s5.totalWaitingTime = s.totalWaitingTime + (s5.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s5.passIn2 = new ArrayList<Passenger>(passIn);
			s5.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s5.addPassengerWaiting(passList.get(s5.timeUnit));
			s5.minTotalLateTime = computeMinTotalLateTime(s5);
			s5.minTotalTimeWaiting = computeMinTotalTimeWaiting(s5);
			al.add(s5);
		}
		
		//liftul 2 merge sus si liftul 1 va deschide usile si coboara pasageri
		State s6 = new State();
		if (fl2 +1 != floorNo && s.prevState2 != open){
			s6.fl1 = fl1;
			s6.fl2 = fl2;
			s6.prevState1 = open;
			s6.prevState2 = up;
			s6.timeUnit = s.timeUnit+1;
			s6.passIn2 = new ArrayList<Passenger>(s.passIn2);
			
			//pasagerii din liftul 2 coboara
			ArrayList<Passenger> passIn = new ArrayList<Passenger>();
			s6.passSolvedNo = 0;
			for (Passenger p : s.passIn1)
				if (p.flDest != fl1)
					passIn.add(p);
				else {
					s6.passSolvedNo++;
					s6.totalWaitingTime = s.totalWaitingTime + (s6.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s6.passIn1 = new ArrayList<Passenger>(passIn);
			s6.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s6.addPassengerWaiting(passList.get(s6.timeUnit));
			s6.minTotalLateTime = computeMinTotalLateTime(s6);
			s6.minTotalTimeWaiting = computeMinTotalTimeWaiting(s6);
			al.add(s6);
		}
		
		//liftul 1 merge jos si liftul 2 va deschide usile si coboara pasageri
		State s7 = new State();
		if (fl1 != 0 && s.prevState1 != open){
			s7.fl1 = fl1;
			s7.fl2 = fl2;
			s7.prevState1 = down;
			s7.prevState2 = open;
			s7.timeUnit = s.timeUnit+1;
			s7.passIn1 = new ArrayList<Passenger>(s.passIn1);
			
			//pasagerii din liftul 2 coboara
			ArrayList<Passenger> passIn = new ArrayList<Passenger>();
			s7.passSolvedNo = 0;
			for (Passenger p : s.passIn2)
				if (p.flDest != fl2)
					passIn.add(p);
				else {
					s7.passSolvedNo++;
					s7.totalWaitingTime = s.totalWaitingTime + (s7.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s7.passIn2 = new ArrayList<Passenger>(passIn);
			s7.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s7.addPassengerWaiting(passList.get(s7.timeUnit));
			s7.minTotalLateTime = computeMinTotalLateTime(s7);
			s7.minTotalTimeWaiting = computeMinTotalTimeWaiting(s7);
			al.add(s7);
		}
		//liftul 2 merge jos si liftul 1 va deschide usile si coboara pasageri
		State s8 = new State();
		if (fl2 != 0 && s.prevState2 != open){
			s8.fl1 = fl1;
			s8.fl2 = fl2;
			s8.prevState1 = open;
			s8.prevState2 = down;
			s8.timeUnit = s.timeUnit+1;
			s8.passIn2 = new ArrayList<Passenger>(s.passIn2);
			
			//pasagerii din liftul 1 coboara
			ArrayList<Passenger> passIn = new ArrayList<Passenger>();
			s8.passSolvedNo = 0;
			for (Passenger p : s.passIn1)
				if (p.flDest != fl1)
					passIn.add(p);
				else {
					s8.passSolvedNo++;
					s8.totalWaitingTime = s.totalWaitingTime + (s7.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s8.passIn1 = new ArrayList<Passenger>(passIn);
			s8.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s8.addPassengerWaiting(passList.get(s8.timeUnit));
			s8.minTotalLateTime = computeMinTotalLateTime(s8);
			s8.minTotalTimeWaiting = computeMinTotalTimeWaiting(s8);
			al.add(s8);
		}
		
		//liftul 1 merge sus si liftul 2 va inchide usile
		State s9 = new State();
		if (fl1 + 1 != floorNo && s.prevState1 != open && s.prevState2 == open){
			s9.fl1 = fl1;
			s9.fl2 = fl2;
			s9.prevState1 = up;
			s9.prevState2 = close;
			s9.timeUnit = s.timeUnit+1;
			s9.passIn1 = new ArrayList<Passenger>(s.passIn1);
			//intra pasagerii in liftul 2
			s9.passIn2 = new ArrayList<Passenger>(s.passIn2);
			for (Passenger p : s.passWaiting){
				if (s9.passIn2.size() == limit) break;
				if (p.flInit == fl2)
					s9.passIn2.add(p);
			}
			s9.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s9.addPassengerWaiting(passList.get(s9.timeUnit));
			s9.minTotalLateTime = computeMinTotalLateTime(s9);
			s9.minTotalTimeWaiting = computeMinTotalTimeWaiting(s9);
			s9.passSolvedNo = 0;
			s9.updateWaiting();
			al.add(s9);
		}
		
		//liftul 1 merge jos si liftul 2 va inchide usile
		State s10 = new State();
		if (fl1 != 0 && s.prevState1 != open && s.prevState2 == open){
			s10.fl1 = fl1;
			s10.fl2 = fl2;
			s10.prevState1 = down;
			s10.prevState2 = close;
			s10.timeUnit = s.timeUnit+1;
			s10.passIn1 = new ArrayList<Passenger>(s.passIn1);
			//intra pasagerii in liftul 2
			s10.passIn2 = new ArrayList<Passenger>(s.passIn2);
			for (Passenger p : s.passWaiting){
				if (s10.passIn2.size() == limit) break;
				if (p.flInit == fl2)
					s10.passIn2.add(p);
			}
			s10.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s10.addPassengerWaiting(passList.get(s10.timeUnit));
			s10.minTotalLateTime = computeMinTotalLateTime(s10);
			s10.minTotalTimeWaiting = computeMinTotalTimeWaiting(s10);
			s10.passSolvedNo = 0;
			s10.updateWaiting();
			al.add(s10);
		}
		
		//liftul 2 merge sus si liftul 1 va inchide usile
		State s11 = new State();
		if (fl2 + 1 != floorNo && s.prevState2 != open && s.prevState1 == open){
			s11.fl1 = fl1;
			s11.fl2 = fl2;
			s11.prevState1 = close;
			s11.prevState2 = up;
			s11.timeUnit = s.timeUnit+1;
			s11.passIn2 = new ArrayList<Passenger>(s.passIn2);
			//intra pasagerii in liftul 1
			s11.passIn1 = new ArrayList<Passenger>(s.passIn1);
			for (Passenger p : s.passWaiting){
				if (s11.passIn1.size() == limit) break;
				if (p.flInit == fl1)
					s11.passIn1.add(p);
			}
			s11.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s11.addPassengerWaiting(passList.get(s11.timeUnit));
			s11.minTotalLateTime = computeMinTotalLateTime(s11);
			s11.minTotalTimeWaiting = computeMinTotalTimeWaiting(s11);
			s11.passSolvedNo = 0;
			s11.updateWaiting();
			al.add(s11);
		}
		
		//liftul 2 merge jos si liftul 1 va inchide usile
		State s12 = new State();
		if (fl2 != 0 && s.prevState2 != open && s.prevState1 == open){
			s12.fl1 = fl1;
			s12.fl2 = fl2;
			s12.prevState2 = down;
			s12.prevState1 = close;
			s12.timeUnit = s.timeUnit+1;
			s12.passIn2 = new ArrayList<Passenger>(s.passIn2);
			//intra pasagerii in liftul 1
			s12.passIn1 = new ArrayList<Passenger>(s.passIn1);
			for (Passenger p : s.passWaiting){
				if (s12.passIn1.size() == limit) break;
				if (p.flInit == fl1)
					s12.passIn1.add(p);
			}
			s12.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s12.addPassengerWaiting(passList.get(s12.timeUnit));
			s12.minTotalLateTime = computeMinTotalLateTime(s12);
			s12.minTotalTimeWaiting = computeMinTotalTimeWaiting(s12);
			s12.passSolvedNo = 0;
			s12.updateWaiting();
			al.add(s12);
		}
		
		//ambele lifturi deschid usile
		State s13 = new State();
		s13.fl1 = fl1;
		s13.fl2 = fl2;
		s13.prevState1 = open;
		s13.prevState2 = open;
		s13.timeUnit = s.timeUnit+1;
		s13.passSolvedNo = 0;
		
		//pasagerii din liftul 1 coboara
		ArrayList<Passenger> passIn1 = new ArrayList<Passenger>();
		for (Passenger p : s.passIn1)
			if (p.flDest != fl1)
				passIn1.add(p);
			else {
				s13.passSolvedNo++;
				s13.totalWaitingTime = s.totalWaitingTime + (s13.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
			}
		s13.passIn1 = new ArrayList<Passenger>(passIn1);
		
		//pasagerii din liftul 2 coboara
		ArrayList<Passenger> passIn2 = new ArrayList<Passenger>();
		for (Passenger p : s.passIn2)
			if (p.flDest != fl2)
				passIn2.add(p);
			else {
				s13.passSolvedNo++;
				s13.totalWaitingTime = s.totalWaitingTime + (s13.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
			}
		s13.passIn2 = new ArrayList<Passenger>(passIn2);
		s13.passWaiting = new ArrayList<Passenger>(s.passWaiting);
		s13.addPassengerWaiting(passList.get(s13.timeUnit));
		s13.minTotalLateTime = computeMinTotalLateTime(s13);
		s13.minTotalTimeWaiting = computeMinTotalTimeWaiting(s13);
		al.add(s13);
		
		//ambele lifturi inchid usile
		State s14 = new State();
		if (s.prevState1 == open && s.prevState2 == open){
			s14.fl1 = fl1;
			s14.fl2 = fl2;
			s14.prevState2 = close;
			s14.prevState1 = close;
			s14.timeUnit = s.timeUnit + 1;
			//intra pasagerii in liftul 2
			s14.passIn2 = new ArrayList<Passenger>(s.passIn2);
			for (Passenger p : s.passWaiting){
				if (s14.passIn2.size() == limit) break;
				if (p.flInit == fl2)
					s14.passIn2.add(p);
			}
			//intra pasagerii in liftul 1
			s14.passIn1 = new ArrayList<Passenger>(s.passIn1);
			for (Passenger p : s.passWaiting){
				if (s14.passIn1.size() == limit) break;
				if (p.flInit == fl1)
					s14.passIn1.add(p);
			}
			s14.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s14.addPassengerWaiting(passList.get(s14.timeUnit));
			s14.minTotalLateTime = computeMinTotalLateTime(s14);
			s14.minTotalTimeWaiting = computeMinTotalTimeWaiting(s14);
			s14.updateWaiting();
			s14.passSolvedNo = 0;
			al.add(s14);
		}
		
		//liftul 1 deschide usile si liftul 2 inchide usile
		State s15 = new State();
		if (s.prevState2 == open){
			s15.fl1 = fl1;
			s15.fl2 = fl2;
			s15.prevState2 = close;
			s15.prevState1 = open;
			s15.timeUnit = s.timeUnit + 1;
			//intra pasagerii in liftul 2
			s15.passIn2 = new ArrayList<Passenger>(s.passIn2);
			for (Passenger p : s.passWaiting){
				if (s15.passIn2.size() == limit) break;
				if (p.flInit == fl2)
					s15.passIn2.add(p);
			}
			//ies pasageri din liftul 1
			ArrayList<Passenger> passIn15 = new ArrayList<Passenger>();
			for (Passenger p : s.passIn1)
				if (p.flDest != fl1)
					passIn15.add(p);
				else {
					s15.passSolvedNo++;
					s15.totalWaitingTime = s.totalWaitingTime + (s15.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s15.passIn1 = new ArrayList<Passenger>(passIn15);
			
			s15.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s15.addPassengerWaiting(passList.get(s15.timeUnit));
			s15.minTotalLateTime = computeMinTotalLateTime(s15);
			s15.minTotalTimeWaiting = computeMinTotalTimeWaiting(s15);
			s15.updateWaiting();
			al.add(s15);
		}
		
		//liftul 1 inchide usile si liftul 2 deschide usile
		State s16 = new State();
		if (s.prevState1 == open){
			s16.fl1 = fl1;
			s16.fl2 = fl2;
			s16.prevState2 = close;
			s16.prevState1 = open;
			s16.timeUnit = s.timeUnit+1;
			//intra pasagerii in liftul 1
			s16.passIn1 = new ArrayList<Passenger>(s.passIn1);
			for (Passenger p : s.passWaiting){
				if (s16.passIn1.size() == limit) break;
				if (p.flInit == fl1)
					s16.passIn1.add(p);
			}
			//ies pasageri din liftul 2
			ArrayList<Passenger> passIn16 = new ArrayList<Passenger>();
			for (Passenger p : s.passIn2)
				if (p.flDest != fl2)
					passIn16.add(p);
				else {
					s16.passSolvedNo++;
					s16.totalWaitingTime = s.totalWaitingTime + (s16.timeUnit - p.reqTime - Math.abs(p.flDest-p.flInit));
				}
			s16.passIn2 = new ArrayList<Passenger>(passIn16);
			
			s16.passWaiting = new ArrayList<Passenger>(s.passWaiting);
			s16.addPassengerWaiting(passList.get(s16.timeUnit));
			s16.minTotalLateTime = computeMinTotalLateTime(s16);
			s16.minTotalTimeWaiting = computeMinTotalTimeWaiting(s16);
			s16.updateWaiting();
			al.add(s16);
		}
		
		for (State state : al){
			state.initQ();
			state.computeReward();
		}
		
		return al;
	}
	
	private static int computeMinTotalLateTime(State s){
		int sum = 0;
		for (Passenger p : s.passIn1){
			sum += s.timeUnit + Math.abs(s.fl1 - p.flDest)- p.reqTime - Math.abs(p.flInit-p.flDest);
		}
		for (Passenger p : s.passIn2){
			sum += s.timeUnit + Math.abs(s.fl2 - p.flDest)- p.reqTime - Math.abs(p.flInit-p.flDest);
		}
		return sum;
	}
	
	private static int computeMinTotalTimeWaiting(State s){
		int sum = 0;
		for (Passenger p : s.passWaiting){
			sum += (Math.abs(s.fl1-p.flInit) < Math.abs(s.fl2 -p.flInit)) ? s.fl1 : s.fl2 ;
		}
		return sum;
	}
	
	private static void SARSA() {
		Random r = new Random();
		State sInit = new State();
		
		sInit.passWaiting = passList.get(timeInit);
		sInit.passIn1 = new ArrayList<Passenger>();
		sInit.passIn2 = new ArrayList<Passenger>();
		sInit.fl1 = -1;
		sInit.fl2 = -1;
		sInit.fl1 = up;
		sInit.fl2 = up;
		sInit.timeUnit = timeInit;
		sInit.minTotalLateTime = computeMinTotalLateTime(sInit);
		sInit.minTotalTimeWaiting = computeMinTotalTimeWaiting(sInit);
		
		sInit.nextStateList = computeNextStateList(sInit);
		sInit.initQ();

		State s = sInit;
		s.initQ();
		for (int k = 0; k < nr_ep; k++){
			s = sInit;
			while(s.timeUnit!=passList.size()-2){
				//alege actiunea;
				int rd = r.nextInt(1000);
				double val = rd / (double) 1000;
				int i = 0;
				if (rd <= eps) {
					// aleator
					i = r.nextInt(s.nextStateList.size());
					
				}
				else {
					//max q
					double maxQ = 0;
					for (int j=0; j < s.nextStateList.size(); j++){
						if (s.qOld.get(j)>maxQ){
							i = j;
							maxQ = s.qOld.get(j);
						}
					}
				}
				
				double max = 0;
				int actIndex = 0;
				for (int p=0; p < s.nextStateList.get(i).qOld.size(); p++){
					if(s.nextStateList.get(i).qOld.get(p) > max){
						max = s.nextStateList.get(i).qOld.get(p);
					}
				}
				
				s.qNew.set(i,s.qOld.get(i) + alfa*(s.nextStateList.get(i).reward + delta*max - s.qOld.get(i)));
				s = s.nextStateList.get(i);
				s.nextStateList = computeNextStateList(s);
				s.initQ();
			}
			
			for (int i = 0; i < sl.size(); i++){
				for (int j=0; j < sl.get(i).qNew.size(); j++){
					sl.get(i).qOld.set(j, sl.get(i).qNew.get(j));
				}
			}
		//	System.out.println("Episod " + k + ": " + s.minTotalLateTime);
		}
	}

	private static void init() {
		generatePassengers(1000, 1200);
	}

	private static void generatePassengers(int timeUnits, int passNo) {
		Random r = new Random();
		passList.ensureCapacity(timeUnits);
		for (int i = 0; i < timeUnits; i++){
			ArrayList<Passenger> al = new ArrayList<Passenger>();
			passList.add(al);
		}
		for (int i = 0; i < passNo; i++){
			int tu = r.nextInt(timeUnits);
			
			int etInit = r.nextInt(floorNo);
			int etDest;
			do{
				etDest = r.nextInt(floorNo);
			} while(etInit == etDest);
			
			//System.out.println(tu + " " + etInit + " " + etDest);
			passList.get(tu).add(new Passenger(tu, etInit, etDest));
		}
	}

}
