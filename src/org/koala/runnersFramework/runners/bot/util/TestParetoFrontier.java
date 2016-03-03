package org.koala.runnersFramework.runners.bot.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;

public class TestParetoFrontier {
	static ArrayList<Double> X = new ArrayList<Double>();
	static ArrayList<Double> Y = new ArrayList<Double>();
	static ArrayList<Double> rX = new ArrayList<Double>();
	static ArrayList<Double> rY = new ArrayList<Double>();
	
	static JFrame frame = null;
	
	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("input.txt"));
			
			String estim = br.readLine();
			if (estim.equals("estimated")) {
				String line = br.readLine();
				while(!line.equals("real")){
					String[] split = line.split(" ");
					X.add(Double.parseDouble(split[0]));
					Y.add(Double.parseDouble(split[1]));
					line = br.readLine();
				}
				line = br.readLine();
				while(line != null) {
					String[] split = line.split(" ");
					rX.add(Double.parseDouble(split[0]));
					rY.add(Double.parseDouble(split[1]));
					line = br.readLine();
				}
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		initFrame();
		plotParetoFrontier();
	}
	
	private static void initFrame() {
		// put the PlotPanel in a JFrame, as a JPanel
		frame = new JFrame("a plot panel");
		//frame.setContentPane(plot);
		frame.setSize(600, 600);
		//frame.setVisible(true);
		
	}
	
	public static  void plotParetoFrontier() {
		double[] x = new double[X.size()];
		double[] y = new double[Y.size()];
		
		
		
		
		for (int i = 0; i < X.size(); i++) {
			x[i] = X.get(i);
			y[i] = Y.get(i);
		}
		/*
		int ind = 0;
		while (rX.size() != 0 && y[0] < Y.get(ind) && y[0] < Y.get(ind+1)) {
			ind++;
		}*/
		double[] rx = new double[rX.size()];
		double[] ry = new double[rY.size()];
		for (int i = 0; i < rX.size(); i++) {
			rx[i] = rX.get(i);
			ry[i] = rY.get(i);	
		}
		
		// create your PlotPanel (you can use it as a JPanel)
		Plot2DPanel plot = new Plot2DPanel();
		initFrame();
		// add a line plot to the PlotPanel
		if (rX.size() != 0) {
			plot.addLinePlot("real", Color.blue, rx, ry);
			plot.addScatterPlot("real", Color.red, rx, ry);
		}
		
		// add a line plot to the PlotPanel
		plot.addScatterPlot("my plot", Color.yellow ,x, y);
		plot.addLinePlot("my plot", Color.green, x, y);
		//frame.getContentPane().removeAll();
		frame.setContentPane(plot);
		//((JPanel)frame.getContentPane()).revalidate();
		//frame.update(frame.getGraphics());
		frame.setVisible(true);
		
		
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			System.out.println("Error in thread sleep");
			e.printStackTrace();
		}
		frame.setVisible(false);
		frame.dispose();
		//frame.
	}
}
