package com.jonfhancock.jsontojava;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jsontojava.JsonToJava;

@SuppressWarnings("serial")
public class JsonToJavaServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		JsonToJava jsonToJava = new JsonToJava();
		boolean parcel = req.getParameter("p") != null
				&& req.getParameter("p").equalsIgnoreCase("true");
		boolean gson = req.getParameter("g") != null
				&& req.getParameter("g").equalsIgnoreCase("true");
		jsonToJava.setUrl(req.getParameter("url"));
		jsonToJava.setPackage(req.getParameter("package"));
		jsonToJava.setBaseType(req.getParameter("class"));
		jsonToJava.useGsonAnnotations(gson);
		jsonToJava.useParcelable(parcel);

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
