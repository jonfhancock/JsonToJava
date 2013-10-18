package com.jonfhancock.jsontojava;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jsontojava.JsonToJava;
import com.jsontojava.OutputOption;

@SuppressWarnings("serial")
public class JsonToJavaServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		JsonToJava jsonToJava = new JsonToJava();
		jsonToJava.setUrl(req.getParameter("url"));
		jsonToJava.setPackage(req.getParameter("package"));
		jsonToJava.setBaseType(req.getParameter("class"));
		String[] options = req.getParameterValues("options");
		if(options != null){
		for(String option:options){
			jsonToJava.addOutputOption(OutputOption.valueOf(option));
		}
		}
		System.out.println(options);
		jsonToJava.fetchJson();
		ByteArrayOutputStream out = (ByteArrayOutputStream) jsonToJava
				.outputZipFile(new ByteArrayOutputStream());
		byte[] data = out.toByteArray();
		resp.setContentType("application/zip");
		resp.setContentLength(data.length);
		resp.setHeader("Content-Disposition",
				"inline; filename=" + jsonToJava.getPackage() + ".zip");
		resp.getOutputStream().write(data);
	}
}
