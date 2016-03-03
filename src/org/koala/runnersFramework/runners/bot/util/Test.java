package org.koala.runnersFramework.runners.bot.util;

public class Test {

	int field;
	Test(int f) {
		field=f;
	}
	
	static void modify(Test t, int f1) {
		Test tmp = new Test(f1);		
		t = tmp;
	}
	
	public static void main(String[] args) {
		Test t = new Test(4);
		Test.modify(t, 9);
		System.out.println(t.field);
	}
}
