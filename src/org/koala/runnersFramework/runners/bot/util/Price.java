package org.koala.runnersFramework.runners.bot.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class Price implements Comparable<Price>{ 
	double price;
	Date date;
	double roc = 1.0;
	double expMovingAverage;
	
	public Price(double price, String date) {
		super();
		this.price = price;
		this.expMovingAverage = price;
		try {
			this.date = new SimpleDateFormat("yyyy-MM-dd:H:m:s", Locale.ENGLISH).parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public Price(double price, Date date) {
		this.price = price;
		this.expMovingAverage = price;
		this.date = date;
	}
	
	public int getIntPrice() {
		return (int)(price*1000);
	}
	
	@Override
	public int compareTo(Price arg0) {
		return (this.date.compareTo(arg0.date)) ;
	}
	
	public void computeROC(double price){
		roc = this.price/price;
	}
	
	public void computeExpMovingAverage(double ema, double exp) {
		expMovingAverage = this.price * exp + ema * (1 - exp);
	}
	
}
