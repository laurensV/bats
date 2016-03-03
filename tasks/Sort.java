import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;




public class Sort {
	
	static ArrayList<String> al = new ArrayList<String>();

	public static void readData(String file) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while((line = br.readLine())!=null){
			String[] data = line.split(" ");
			for (int i=0; i < data.length; i++)
				al.add(data[i]);
		}
	}
	
	public static void main(String[] args) throws IOException {
		readData(args[1]);
		int iter = Integer.parseInt(args[0]);
		for (int i = 0; i < iter; i++){
			Collections.sort(al);
			Collections.shuffle(al);
		}
		
	}

}
