package com.sandbox.runtime.converters;

import com.sandbox.common.models.http.HttpRuntimeRequest;
import com.sandbox.runtime.utils.MapUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by nickhoughton on 1/08/2014.
 */

public class HttpServletConverter extends RequestConverter{

    @Autowired
    MapUtils mapUtils;

    private static Logger logger = LoggerFactory.getLogger(HttpServletConverter.class);

    public HttpRuntimeRequest httpServletToInstanceHttpRequest(HttpServletRequest rawRequest) throws Exception {
        if(rawRequest == null) throw new Exception("Invalid route");

        HttpRuntimeRequest request = new HttpRuntimeRequest();

        //set temporary name, can overload to change it
        request.setSandboxName("sandbox");
        request.setMethod(rawRequest.getMethod());
        request.setUrl(rawRequest.getRequestURI());

        //use lb header
        String remoteIp = rawRequest.getHeader("X-Forwarded-For");
        request.setIp(remoteIp);

        if(rawRequest.getContentType() != null){
            String rawContentType = rawRequest.getContentType();

            if(jsonPattern.matcher(rawContentType).matches()){
                request.setContentType("json");
            }else if(xmlPattern.matcher(rawContentType).matches()){
                request.setContentType("xml");
            }else if(rawContentType.startsWith("application/x-www-form-urlencoded")){
                request.setContentType("urlencoded");
            }

        }

        request.setBody(IOUtils.toString(rawRequest.getInputStream()));

        Map queryParams = rawRequest.getParameterMap();
        request.setQuery(mapUtils.flattenMultiValue(queryParams));
        request.setRawQuery(rawRequest.getQueryString());

        Map<String, String> cookies = new HashMap<>();
        if(rawRequest.getCookies() != null) {
            for (javax.servlet.http.Cookie servletCookie : rawRequest.getCookies()) {
                cookies.put(servletCookie.getName(), servletCookie.getValue());
            }
        }
        request.setCookies(cookies);

        request.setAccepted(
                getAcceptedHeadersFromHeaders(rawRequest.getHeaders("Accept"))
        );

        request.setHeaders(
                getHeadersAsMap(rawRequest)
        );

        return request;
    }

    public static Map<String,String> getHeadersAsMap(HttpServletRequest request){
        Map<String,String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Enumeration<String> keys = request.getHeaderNames();
        while(keys.hasMoreElements()){
            String key = keys.nextElement();

            Enumeration<String> values = request.getHeaders(key);

            while(values.hasMoreElements()){
                String value = values.nextElement();
                //if the SOAPAction is wrapped in quotes, remove them to simplify matching.
                if(key.equalsIgnoreCase("SOAPAction") && value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length()-1);
                headers.put(key, value);
            }

        }

        return headers;
    }

    public static List<String> getAcceptedHeadersFromHeaders(Enumeration<String> acceptedHeaders){
        List<String> accepted = new ArrayList<String>();
        if(acceptedHeaders == null) return accepted;

        while(acceptedHeaders.hasMoreElements()){
            String acceptedValue = acceptedHeaders.nextElement();
            if(acceptedValue.indexOf(",")!=-1){
                for(String subValue : acceptedValue.split(",")){
                    accepted.add(subValue);
                }
            }else{
                accepted.add(acceptedValue);
            }

        }

        return accepted;

    }

    public MapUtils getMapUtils() {
        return mapUtils;
    }
}
