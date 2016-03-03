import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;


class Sablon{
	double lungS, latS, lungP, latP;
	int clasa;
}

class Neuron{
	double lungS, latS, lungP, latP;
}

public class Kohonen {
	
	static ArrayList<Sablon> as = new ArrayList<Sablon>();
	static Neuron[][] ret = new Neuron[10][10];
	static double alfa = 0.1;
	static int r0 = 7;
	static int raza = r0;
	static int iter;
	
	public static void readSablon(String file) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while((line = br.readLine())!=null){
			String[] data = line.split(",");
			Sablon s = new Sablon();
			s.lungS = Double.parseDouble(data[0]);
			s.latS = Double.parseDouble(data[1]);
			s.lungP = Double.parseDouble(data[2]);
			s.latP = Double.parseDouble(data[3]);
			s.clasa = Integer.parseInt(data[4]);
			as.add(s);
		}
	}
	
	public static void printSablon(){
		for (Sablon s : as){
			System.out.println(s.lungS + " " + s.latS + " " + s.lungP + " " + s.latP + " " + s.clasa);
		}
	}
	
	public static void initRet(){
		Random r = new Random();
		int var;
		boolean sign;
		for (int i = 0; i < 10; i++)
			for (int j = 0; j < 10 ; j++){
				ret[i][j] =  new Neuron();
				var = r.nextInt(Integer.MAX_VALUE);
				sign = r.nextBoolean();
				ret[i][j].lungS = (sign ? -1:1) * var/(double)Integer.MAX_VALUE;
				var = r.nextInt(100);
				sign = r.nextBoolean();
				ret[i][j].latS = (sign ? -1:1) * var/(double)Integer.MAX_VALUE;
				var = r.nextInt(100);
				sign = r.nextBoolean();
				ret[i][j].lungP = (sign ? -1:1) * var/(double)Integer.MAX_VALUE;
				var = r.nextInt(100);
				sign = r.nextBoolean();
				ret[i][j].latP = (sign ? -1:1) * var/(double)Integer.MAX_VALUE;
			}
	}
	
	public static void kohonen(){
		double[][] dist = new double[10][10];
		for (int k = 0; k < iter; k++){
			for (Sablon s: as){
				int iMin = 0 , jMin = 0;
				double distMin = 1000;
				for (int i = 0; i < 10; i++)
					for (int j = 0; j < 10 ; j++){
						dist[i][j] = Math.pow(s.lungS -ret[i][j].lungS,2) +
						Math.pow(s.latS -ret[i][j].latS,2) +
						Math.pow(s.lungP -ret[i][j].lungP,2) +
						Math.pow(s.latP -ret[i][j].latP,2);
						if (dist[i][j] < distMin){
							distMin = dist[i][j];
							iMin = i;
							jMin = j;
						}
					}
				//make borders
				int im = iMin - raza;
				int iM = iMin + raza;
				int jm = jMin - raza;
				int jM = jMin + raza;
				for (int i = 0; i < 10; i++)
					for (int j = 0; j < 10 ; j++){
						if ( i >= im && i <= iM && j >= jm && j <= jM){
							ret[i][j].lungS = ret[i][j].lungS + alfa * (s.lungS - ret[i][j].lungS);
							ret[i][j].latS = ret[i][j].latS + alfa * (s.latS - ret[i][j].latS);
							ret[i][j].lungP = ret[i][j].lungP + alfa * (s.lungP - ret[i][j].lungP);
							ret[i][j].latP = ret[i][j].latP + alfa * (s.latP - ret[i][j].latP);
						}
					}
			}
			double exp = ((double)(-1)*(k/(iter/r0)));
			raza = (int)Math.pow(r0, exp);
		}
		
	}
	
	public static void printRet(){
		double[] dist = new double[as.size()];
		for (int i = 0; i < 10; i++){
			for (int j = 0; j < 10 ; j++){
		
				int kMin = 0;
				double distMin = 1000;
				for (int k = 0; k < as.size(); k ++){
					dist[k] = Math.pow(as.get(k).lungS -ret[i][j].lungS, 2) +
					Math.pow(as.get(k).latS -ret[i][j].latS, 2) +
					Math.pow(as.get(k).lungP -ret[i][j].lungP, 2) +
					Math.pow(as.get(k).latP -ret[i][j].latP, 2);
					if (dist[k] < distMin){
						distMin = dist[k];
						kMin = k;
					}
				}
				
			//	System.out.print(" " + as.get(kMin).clasa);
			}
			//System.out.println();
		}
	}
	
	public static void main(String args[]) throws IOException{
		iter = Integer.parseInt(args[0]) * 100;
		readSablon(args[1]);
		//printSablon();
		initRet();
		kohonen();
		printRet();
	}
}
