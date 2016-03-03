package org.koala.runnersFramework.runners.bot.util;

import java.math.BigInteger;

public class Combinations {
	BigInteger[][] comb;
	
	Combinations(int size) {
		comb = new BigInteger[size][size];
		// combinari de i luate cate j
		comb[0][0] = BigInteger.ONE;
		comb[1][0] = BigInteger.ONE;
		for (int i = 0; i < size; i++) {
			comb[i][0] = BigInteger.ONE;
			for (int j = 1; j < size; j++)
				comb[i][j] = BigInteger.ZERO;
		}				
				
		for (int i = 1; i < size; i++) 
			for (int j = 1; j <= i; j++){
				comb[i][j] = comb[i-1][j].add(comb[i-1][j-1]);	
			}
		/*for (int i = 0; i < size; i++) { 
			for (int j = 0; j <= i; j++){
				System.out.print(comb[i][j] + " ");
			}
			System.out.println();
		}*/
	}
}
