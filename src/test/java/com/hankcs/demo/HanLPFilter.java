package com.hankcs.demo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class HanLPFilter {

	public List<String> filter(List<String> filter) throws IOException {

		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("acm.txt")));
			String data = null;
			while ((data = br.readLine()) != null) {
				sb.append(data);
			}
		} finally {
			br.close();
		}
		String[] exs = sb.toString().split(";");
		return null;
	}
}
