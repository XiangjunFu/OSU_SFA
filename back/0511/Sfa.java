package edu.osu.sfal.util;

import static edu.osu.sfal.rest.EntityUtil.getEntityJson;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.google.gson.JsonObject;

import edu.osu.sfal.rest.RequestAndOutputMappings;

public class Sfa extends Restlet {

	@Override
	public final void handle(Request request, Response response) {
		Method method = request.getMethod();
		if (method.equals(Method.POST)) {
			if (!response.getStatus().isError()) {
				handlePost(request, response);
			} else {
				
			}
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
	}

	private void handlePost(Request request, Response response) {
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
