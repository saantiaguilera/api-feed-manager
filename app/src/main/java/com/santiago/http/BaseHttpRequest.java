package com.santiago.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public abstract class BaseHttpRequest<E> extends OkHttpAsyncParsedRequest<E> {
	
	private Context context = null;
	
	private HttpRequestSuccessListener<E> successListener = null;
    private HttpRequestFailureListener<E> failureListener = null;
	
	public BaseHttpRequest(Context context) {
		
		if(context == null)
			throw new NullPointerException("Context cannot be null in BaseHttpRequest");
		
		this.context = context;
		
	}
	
	public Context getContext(){
		return context;
	}
	
	public boolean hasInternetConnection() {
		
	    ConnectivityManager cm =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    
	    return false;
	}
	
	public void setSuccessListener(HttpRequestSuccessListener<E> successListener){
		this.successListener = successListener;
	}

    public void setFailureListener(HttpRequestFailureListener<E> failureListener){
        this.failureListener = failureListener;
    }

	@Override
	public void execute() {
		
		if(hasInternetConnection())
			super.execute();
		else
			callFailureListener(null, new NoConnectionException("Context has no network connection"));
	}
	
	@Override
	protected Request getRequest() {
		
		Request result;
		
		try {

			Request.Builder requestBuilder = new Request.Builder();

			requestBuilder.url(getRequestUrl());

			Headers headers = getRequestHeaders();

			if(headers != null)
				requestBuilder.headers(headers);

			RequestBody body = getRequestBody();

			String method =getHttpMethod();

			//Workaround for issue of Null body excep: (@see: https://github.com/square/retrofit/issues/854
			if(body==null && com.squareup.okhttp.internal.http.HttpMethod.requiresRequestBody(method))
				body = RequestBody.create(null, new byte[0]);


			requestBuilder.method(method, body);

			result = requestBuilder.build();
			
		} catch (IllegalArgumentException exception){
			
			exception.printStackTrace();
			result = null;
			
		}
		
		return result;
	}
	
	@Override
	protected void onRequestCompleted(Response httpResponse, E parsedResponse, Exception exception) {
		
		if(exception == null) {
			
			callSuccessListener(parsedResponse);
			
		} else {
			
			callFailureListener(httpResponse, exception);
			
		}
		
	}
	
	protected abstract String getRequestUrl();
	
	protected abstract String getHttpMethod();

	@Override
	protected Authenticator getAuthenticator() {
		return null;
	}

	protected RequestBody getRequestBody() {
		return null;
	}
	
	protected Headers getRequestHeaders() {
		return null;
	}

	protected void callSuccessListener(E parsedResponse){
		
		if(successListener != null)
			successListener.onHttpRequestSuccess(this, parsedResponse);
		
	}
	
	protected void callFailureListener(Response httpResponse, Exception exception){
		
		if(failureListener != null)
			failureListener.onHttpRequestFailure(this, httpResponse, exception);
		
	}

    public interface HttpRequestSuccessListener<E> {
        void onHttpRequestSuccess(BaseHttpRequest<E> request, E result);
    }

    public interface HttpRequestFailureListener<E> {
        void onHttpRequestFailure(BaseHttpRequest<E> request, Response httpResponse, Exception exception);
    }
	
}
