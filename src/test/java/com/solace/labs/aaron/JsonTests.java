package com.solace.labs.aaron;

import java.util.Iterator;

import org.json.JSONObject;
import org.junit.Test;

import com.google.gson.JsonObject;

public class JsonTests {

	
	
	
	@Test
	public void test1() {
		JsonObject object = new JsonObject();
		
		for (int i=50;i>0;i--) {
			object.addProperty(Integer.toString(i), i);
		}
//		object.addProperty("1", 1);
//		object.addProperty("2", 2);
//		object.addProperty("3", 3);
//		object.addProperty("4", 4);
//		object.addProperty("5", 5);
//		object.addProperty("6", 6);
//		object.addProperty("7", 7);
//		object.addProperty("8", 8);
//		object.addProperty("9", 9);
		System.out.println("With com.google.gson.JsonObject");
		System.out.println(object.toString());
	}

	@Test
	public void test2() {
		JSONObject object = new JSONObject();
		object.put("1", 1);
		object.put("2", 2);
		object.put("3", 3);
		object.put("4", 4);
		object.put("5", 5);
		object.put("6", 6);
		object.put("7", 7);
		object.put("8", 8);
		object.put("9", 9);
		System.out.println(object.toString());
		// verify order
		Iterator<String> i = object.keys();
		int index = 1;
		while (i.hasNext()) {
			int val = object.getInt(i.next());
//			if (val != index) 
		}
	}






}
