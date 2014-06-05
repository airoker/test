import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.JSONTools;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;


public class Wrapper_gjdweb00016 implements QunarCrawler {

	@Override
	public BookingResult getBookingInfo(FlightSearchParam arg0) {
		String bookingUrlPre = "http://www.ebookers.com/shop/airsearch";
		BookingResult bookingResult = new BookingResult();
		
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("get");
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("type", "air");
		map.put("ar.type", "oneWay");
		map.put("strm", "true");
		map.put("ar.ow.leaveSlice.orig.key", arg0.getDep());
		map.put("ar.ow.leaveSlice.dest.key", arg0.getArr());
		map.put("ar.ow.leaveSlice.date", arg0.getDepDate().replaceAll("..(..)-(..)-(..)", "$3/$2/$1"));
		map.put("ar.ow.leaveSlice.time", "Anytime");
		map.put("ar.ow.numAdult", "1");
		map.put("ar.ow.numSenior", "0");
		map.put("ar.ow.numChild", "0");
		map.put("ar.ow.child[0]", "");
		map.put("ar.ow.child[1]", "");
		map.put("ar.ow.child[2]", "");
		map.put("ar.ow.child[3]", "");
		map.put("ar.ow.child[4]", "");
		map.put("ar.ow.child[5]", "");
		map.put("ar.ow.child[6]", "");
		map.put("ar.ow.child[7]", "");
		map.put("search", "Search Flights");
		map.put("_ar.ow.nonStop", "0");
		map.put("_ar.ow.narrowSel", "0");
		map.put("ar.ow.narrow", "airlines");
		map.put("ar.ow.carriers[0]", "");
		map.put("ar.ow.carriers[1]", "");
		map.put("ar.ow.carriers[2]", "");
		map.put("ar.ow.cabin", "C");
		bookingInfo.setInputs(map);		
		bookingResult.setData(bookingInfo);
		bookingResult.setRet(true);
		return bookingResult;
	}

	@Override
	public String getHtml(FlightSearchParam arg0) {
		QFHttpClient httpClient = new QFHttpClient(arg0,false);
		
		//对于需要cookie的网站，请自己处理cookie（必须）
		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		String getUrl = String.format("http://www.ebookers.com/shop/airsearch?type=air&ar.type=oneWay&strm=true&ar.ow.leaveSlice.orig.key=%s&ar.ow.leaveSlice.dest.key=%s&ar.ow.leaveSlice.date=%s&ar.ow.leaveSlice.time=Anytime&ar.ow.numAdult=1&ar.ow.numSenior=0&ar.ow.numChild=0&search=Search+Flights&_ar.ow.nonStop=0&_ar.ow.narrowSel=0&ar.ow.narrow=airlines&ar.ow.cabin=C", arg0.getDep(), arg0.getArr(), arg0.getDepDate().replaceAll("..(..)-(..)-(..)", "$3/$2/$1"));
		QFGetMethod get = null;		
		try {				
			get = new QFGetMethod(getUrl);
			int status = httpClient.executeMethod(get);
			if(status>=400){
				return "StatusError"+status;
			}
			String cookies = StringUtils.join(httpClient.getState().getCookies(),"; ");
			String firstResponse = get.getResponseBodyAsString();
			//System.out.println(firstResponse);
			//System.out.println(StringUtils.join(get.getResponseHeaders(),"\n"));
			//System.out.println("cookie:"+cookies);
			String rKy = StringUtils.substringBetween(firstResponse, ";rKy=", "&");
			String rCt = StringUtils.substringBetween(firstResponse, ";rCt=", "&");
			
			if(StringUtils.isEmpty(rKy)||StringUtils.isEmpty(rCt)){
				return "ParamError";
			}
			
			String secUrl = String.format("http://www.ebookers.com/shop/airsearch?type=air&ar.type=oneWay&strm=true&ar.ow.leaveSlice.orig.key=%s&ar.ow.leaveSlice.dest.key=%s&ar.ow.leaveSlice.date=%s&ar.ow.leaveSlice.time=Anytime&ar.ow.numAdult=1&ar.ow.numSenior=0&ar.ow.numChild=0&search=Search+Flights&_ar.ow.nonStop=0&_ar.ow.narrowSel=0&ar.ow.narrow=airlines&ar.ow.cabin=C&rKy=%s&rCt=%s", arg0.getDep(), arg0.getArr(), arg0.getDepDate().replaceAll("..(..)-(..)-(..)", "$3/$2/$1"),rKy,rCt);
			get = new QFGetMethod(secUrl);
			httpClient.getState().clearCookies();
			get.addRequestHeader("Cookie",cookies);
			get.addRequestHeader("X-Requested-With","XMLHttpRequest");
			get.addRequestHeader("Referer",getUrl);
		    status = httpClient.executeMethod(get);
		    if(status>=400){
				return "StatusError2_"+status;
			}
		    int count = 0;
		    String response = get.getResponseBodyAsString();
		    if(response.contains("Sorry, no flights were found. Please")){
		    	return response;
		    }
		    while (response.length()<5000 && count<5){
		    	Thread.sleep(1000);
		    	status = httpClient.executeMethod(get);
			    if(status>=400){
					return "StatusError3_"+status;
				}
			    response = get.getResponseBodyAsString();
			    count++;
		    }
		    return response;
		} catch (Exception e) {			
			e.printStackTrace();
		} finally{
			if (null != get){
				get.releaseConnection();
			}
		}
		return "Exception";
	}

	@Override
	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String html = arg0;
		/* ProcessResultInfo中，
		 * ret为true时，status可以为：SUCCESS(抓取到机票价格)|NO_RESULT(无结果，没有可卖的机票)
		 * ret为false时，status可以为:CONNECTION_FAIL|INVALID_DATE|INVALID_AIRLINE|PARSING_FAIL|PARAM_ERROR
		 */
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(html)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}
		if ("ParamError".equals(html)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}
		if (html.startsWith("StatusError")) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}
		//需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		if (html.contains("Sorry, no flights were found. Please")) {
			result.setRet(true);
			result.setStatus(Constants.NO_RESULT);
			return result;			
		}

		try {			
			List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
			JSONArray ajson = JSON.parseObject(html).getJSONObject("modules").getJSONArray("airResults");		
			String outJson = ajson.getString(0);
			String [] results = outJson.split("<div class=\"airResultsCard  optimizedCard \" data-context=\"airResultsCard\">");
			//System.out.println(results[1]);
			for (int i = 1; i < results.length; i++){				
				OneWayFlightInfo baseFlight = new OneWayFlightInfo();
				List<FlightSegement> segs = new ArrayList<FlightSegement>();
				FlightDetail flightDetail = new FlightDetail();
				
				List<String> flightNoList = new ArrayList<String>();
				
				String dept = StringUtils.substringBetween(results[i], "<span class=\"heading\" data-context=\"departureTime\">","</span>").trim();
				String arrt = StringUtils.substringBetween(results[i], "<span class=\"heading\" data-context=\"arrivalTime\">","</span>").trim();
				
				String allinfo = StringUtils.substringBetween(results[i], "Total cost","Select</a></div>").trim();
				String []priceInfo = StringUtils.substringBetween(allinfo, "userRate.price=","&").trim().split("\\|");
				String []keys = StringUtils.substringBetween(allinfo, "selectKey=","&").trim().split("_");
				
				for (int j = 0; j < keys.length; j++) {
					String code = keys[j].substring(0,keys[j].length()-11);
					String dep = keys[j].substring(code.length(),code.length()+3);
					String arr = keys[j].substring(code.length()+3,code.length()+6);
					String ymd = arg1.getDepDate().substring(0, 5)+keys[j].substring(code.length()+6,code.length()+8)+"-"+keys[j].substring(code.length()+8,code.length()+10);
					
					flightNoList.add(code);
					
					FlightSegement seg = new FlightSegement();
					seg.setFlightno(code);
					seg.setDepDate(ymd);
					seg.setArrDate(ymd);
					seg.setDepairport(dep);
					seg.setArrairport(arr);
					if(j==0){
						seg.setDeptime(dept);
					}else{
						seg.setDeptime("00:00");
					}
					if(j==keys.length-1){
						seg.setArrtime(arrt);
					}else{
						seg.setArrtime("00:00");
					}
					segs.add(seg);
				}
				
				flightDetail.setFlightno(flightNoList);
				flightDetail.setMonetaryunit(priceInfo[0]);
				flightDetail.setPrice(Math.round(Double.parseDouble(priceInfo[1])));
				flightDetail.setDepcity(arg1.getDep());
				flightDetail.setArrcity(arg1.getArr());
				flightDetail.setWrapperid(arg1.getWrapperid());
				flightDetail.setDepdate(Date.valueOf(arg1.getDepDate()));
				
				baseFlight.setDetail(flightDetail);
				baseFlight.setInfo(segs);
				flightList.add(baseFlight);
			}	
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;
		} catch(Exception e){
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}
	}
	
	public static void main(String[] args) {
		Wrapper_gjdweb00016 instance = new Wrapper_gjdweb00016();
		instance.test();
	}

	public void test() {
		FlightSearchParam p = new FlightSearchParam();
		p.setWrapperid("gjdweb00016");
		p.setDep("LHR");
		p.setArr("KHN");
		p.setDepDate("2014-06-04");
		p.setTimeOut("60000");
		//System.out.println(this.getHtml(p));
		if(false){
			try {
				String html = this.getHtml(p);
				Files.write(html, new File("F:\\006.html"),Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			String page = Files.toString(new File("F:\\006.html"),Charsets.UTF_8);
			ProcessResultInfo pr = process(page, p);
			System.out.println(JSONTools.fomatProcessResultToJSON(pr));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
