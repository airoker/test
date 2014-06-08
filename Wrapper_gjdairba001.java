
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.*;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.developer.QFGetMethod;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;
import com.travelco.rdf.infocenter.InfoCenter;

/**
 * Initial Created by ruilin.zhao at 14-6-8
 * <p/>
 * Description:
 */
public class Wrapper_gjdairba001 implements QunarCrawler {

    private static final String CODEBASE = "gjdairba001";

    public static void main(String[] args) {
        Wrapper_gjdairba001 p = new Wrapper_gjdairba001();
        FlightSearchParam flightSearchParam = new FlightSearchParam();
        flightSearchParam.setDep("PEK");
        flightSearchParam.setArr("NYC");
        flightSearchParam.setDepDate("2014-07-14");
        String html = p.getHtml(flightSearchParam);
        System.out.println(p.process(html, flightSearchParam));
    }

    @Override
    public String getHtml(FlightSearchParam flightSearchParam) {
        String dep = flightSearchParam.getDep();
        String arr = flightSearchParam.getArr();
        String depD = flightSearchParam.getDepDate();

        QFHttpClient httpClient = new QFHttpClient(flightSearchParam, false);
        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        QFGetMethod homeGet = new QFGetMethod("http://www.britishairways.com/travel/home/public/zh_cn");
        QFGetMethod get = null;
        QFPostMethod midPostMethod = new QFPostMethod("http://www.britishairways.com/travel/fx/public/zh_cn");
        String depCity = InfoCenter.getCityFromAirportCode(dep);
        String depCountry = InfoCenter.getCountryFromCity(depCity, "cn");
        String depCountryCode = InfoCenter.getCountry2CodeFromNameZh(depCountry);
        System.out.println("depCity code: " + depCity);
        System.out.println("depCountry code: " + depCountry);
        System.out.println("country code: " + depCountryCode);
        String depDate = changeDateForm(depD);
        try {
            httpClient.executeMethod(homeGet);
            Cookie[] cookies = httpClient.getState().getCookies();
            String tmpcookies = "";
            for (Cookie c : cookies) {
                tmpcookies = tmpcookies + c.toString() + ";";
            }
            tmpcookies = tmpcookies + "BA_COUNTRY_CHOICE_COOKIE=CN;" + "Allow_BA_Cookies=accepted;" + "depDate="
                    + depDate + ";" + " FO_DESTINATION_COOKIE=" + arr + "%7C1375200000%7C%7COWFLT" + "FS_DEPT_AIRPORT="
                    + arr + ";" + "FS_DEPT_COUNTRY=" + depCountryCode + ";";
            NameValuePair[] params = { new NameValuePair("eId", "111002"), new NameValuePair("saleOption", "FO"),
                    new NameValuePair("depCountry", depCountryCode), new NameValuePair("from", dep),
                    new NameValuePair("journeyType", "OWFLT"), new NameValuePair("to", arr),
                    new NameValuePair("depDate", depDate), new NameValuePair("cabin", "M"),
                    new NameValuePair("restrictionType", "LOWEST"), new NameValuePair("ad", "1"),
                    new NameValuePair("ch", "0"), new NameValuePair("inf", "0"),
                    new NameValuePair("getFlights", "Find flights") };
            midPostMethod.setRequestBody(params);
            midPostMethod.setRequestHeader("Cookie", tmpcookies);
            midPostMethod.setRequestHeader("Host", "www.britishairways.com");
            midPostMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            midPostMethod.setRequestHeader("Referer", "http://www.britishairways.com/travel/home/public/zh_cn");

            httpClient.executeMethod(midPostMethod);
            String midResult = midPostMethod.getResponseBodyAsString();
            System.out.println("###############:  " + midResult);
            if (midResult.contains("var replaceURL = '")) {
                String replaceURL = getValue(midResult, "var replaceURL = '", "';")
                        + getValues(midResult, "replaceURL += '", "';")[0]
                        + getValues(midResult, "replaceURL += '", "';")[1]
                        + getValues(midResult, "replaceURL += '", "';")[2];

                get = new QFGetMethod(replaceURL);
                get.setRequestHeader("Cookie", tmpcookies);
                httpClient.executeMethod(get);
                return get.getResponseBodyAsString();
            } else {
                System.out.println("qqqqqqqq");
                return Constants.NO_RESULT;
            }

        } catch (Exception e) {
            if (!e.getMessage().equals("Connection refused: connect")) return Constants.CONNECTION_FAIL;
        } finally {
            midPostMethod.releaseConnection();
            homeGet.releaseConnection();
            if (get != null) {
                get.releaseConnection();
            }
        }
        return "Exception";
    }

    @Override
    public ProcessResultInfo process(String html, FlightSearchParam param) {
        ProcessResultInfo processResultInfo = new ProcessResultInfo();

        if ("Exception".equals(html) || Constants.CONNECTION_FAIL.equals(html)) {
            processResultInfo.setStatus(Constants.CONNECTION_FAIL);
            return processResultInfo;
        }
        if (html.contains("我们无法为您要求的行程安排座位") || Constants.NO_RESULT.equals(html)) {
            processResultInfo.setRet(true);
            processResultInfo.setStatus(Constants.NO_RESULT);
            System.out.println("xxxxx:   NO_RESULT");
            return processResultInfo;
        }
        String CurrencyCode = getValue(html, "CountryCurrency='", "'").trim();

        String outbound = getValue(html, "<div id=\"outBoundFlightList", "<div id=\"outboundpointer");//出发航班的信息。

        String[] outFlights = outbound.split("<td class=\"\\s?s?e?l?e?c?t?e?d?\\s?departure[^a-zA-Z<]");

        List<OneWayFlightInfo> data = Lists.newArrayList();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (String outFlight : outFlights) {
            if (outFlight.contains("<span class=\"time\">") && outFlight.contains("<span class=\"Carrier\">")
                    && outFlight.contains("<span class=\"FlightNumber\">")) {
                OneWayFlightInfo oneWayFlightInfo = getTable(param, outFlight, CurrencyCode, now);
                data.add(oneWayFlightInfo);
            }
        }
        if (data.size() == 0) {
            System.out.printf("YYYYY: NO_RESULT");
            processResultInfo.setStatus(Constants.NO_RESULT);
        } else {
            processResultInfo.setStatus(Constants.SUCCESS);
        }
        processResultInfo.setRet(true);
        processResultInfo.setData(data);
        System.out.println("******: SUCCESS");
        return processResultInfo;
    }

    @Override
    public BookingResult getBookingInfo(FlightSearchParam flightSearchParam) {
        BookingResult result = new BookingResult();
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setAction("http://www.britishairways.com/travel/fx/public/zh_cn");
        bookingInfo.setMethod("POST");

        String dep = flightSearchParam.getDep();
        String arr = flightSearchParam.getArr();
        String date = flightSearchParam.getDepDate();

        String depCity = InfoCenter.getCityFromAirportCode(dep);
        String depCountry = InfoCenter.getCountryFromCity(depCity, "cn");
        String depCountryCode = InfoCenter.getCountry2CodeFromNameZh(depCountry);
        String[] depdate = date.split("-");
        String depDate = depdate[2] + "/" + depdate[1] + "/" + depdate[0].substring(2, 4);

        Map<String, String> param = Maps.newHashMap();
        param.put("eId", "111002");
        param.put("saleOption", "FO");
        param.put("depCountry", depCountryCode);
        param.put("from", dep);
        param.put("journeyType", "OWFLT");
        param.put("to", arr);
        param.put("depDate", depDate);
        param.put("cabin", "M");
        param.put("restrictionType", "LOWEST");
        param.put("ad", "1");
        param.put("ch", "0");
        param.put("inf", "0");
        param.put("getFlights", "Find+flights");

        bookingInfo.setInputs(param);
        result.setData(bookingInfo);
        result.setRet(true);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.travelco.html.HtmlPageProcessor#process(java.lang.String,
     * java.lang.String)
     */
    public OneWayFlightInfo getTable(FlightSearchParam flightSearchParam, String airLineInfo, String CurrencyCode,
            Timestamp now) {
        String departureTime = getValues(airLineInfo, "<span class=\"time\">", "</span>")[0].trim();
        String arrivalTime = getValues(airLineInfo, "<span class=\"time\">", "</span>")[1].trim();
        String[] departureAirport = getValues(airLineInfo, "<span class=\"from\">", "</span>");
        String[] arrivalAirport = getValues(airLineInfo, "<span class=\"to\">", "</span>");
        String[] retailPrices = getValues(airLineInfo, "<label style=\"display: inline;\"", "</label>");

        Double retailPrice = Double.parseDouble(retailPrices[0].trim().split("[^0-9]")[retailPrices[0].trim().split(
                "[^0-9]").length - 1]);;
        for (String price : retailPrices) {
            String[] Prices = price.trim().split("[^0-9]");
            double Price = Double.parseDouble(Prices[Prices.length - 1]);
            if (Price < retailPrice) {
                retailPrice = Price;
            }
        }
        String[] Carriers = getValues(airLineInfo, "<span class=\"Carrier\">", "</span>");
        String[] FlightNumbers = getValues(airLineInfo, "<span class=\"FlightNumber\">", "</span>");
        String tax = "0";
        String planeType = "0";

        OneWayFlightInfo result = new OneWayFlightInfo();

        /**************** 航班号 *******************/
        String temp = "";
        for (int k = 0; k < Carriers.length; k++) {
            temp = temp + Carriers[k].trim() + FlightNumbers[k].trim() + "/";
        }
        temp = temp.substring(0, temp.length() - 1);
        /*****************************************/
        String airports = "";
        if (temp.contains("/") && temp.replaceFirst("/", "").indexOf("/") < 0) {
            airports = arrivalAirport[0] + "," + departureAirport[1];
        }
        FlightDetail detail = getDetail(flightSearchParam, CurrencyCode, new Float(retailPrice), 0.0f, temp, now);
        result.setDetail(detail);
        List<FlightSegement> flightSegements = Lists.newArrayList();
        FlightSegement flightSegement = getFlightSegement(flightSearchParam.getDepDate(), flightSearchParam.getDep(),
                flightSearchParam.getArr(), departureTime, arrivalTime, temp);
        flightSegements.add(flightSegement);
        result.setInfo(flightSegements);

        return result;
    }

    private FlightSegement getFlightSegement(String depDate, String departure, String arrival, String dept,
            String arrt, String code) {

        FlightSegement flightSegement = new FlightSegement(code);
        flightSegement.setDepDate(depDate);
        flightSegement.setDeptime(dept);
        flightSegement.setDepairport(departure);
        flightSegement.setArrairport(arrival);
        flightSegement.setArrtime(arrt);
        return flightSegement;
    }

    FlightDetail getDetail(FlightSearchParam flightSearchParam, String currency, float price, float tax, String code,
            Timestamp now) {
        FlightDetail detail = new FlightDetail();
        String wrapper = flightSearchParam.getWrapperid() == null ? CODEBASE : flightSearchParam.getWrapperid();
        detail.setWrapperid(wrapper);
        detail.setArrcity(flightSearchParam.getArr());
        detail.setDepcity(flightSearchParam.getDep());
        String depDate = flightSearchParam.getDepDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date depTime;
        try {
            depTime = sdf.parse(depDate);
        } catch (ParseException e) {
            depTime = null;
        }
        detail.setDepdate(depTime);
        detail.setMonetaryunit(currency);
        detail.setCreatetime(now);
        detail.setUpdatetime(now);
        detail.setPrice(price);
        detail.setTax(tax);
        List<String> flightNos = Lists.newArrayList(code.split("/"));
        detail.setFlightno(flightNos);
        return detail;
    }

    public String changeDateForm(String date) {
        String[] dates = date.split("-");
        String newDate = dates[2] + "/" + dates[1] + "/" + dates[0].substring(2, 4);
        return newDate;
    }

    public String getValue(String source, String st, String end) {
        int a = source.indexOf(st);
        if (a == -1) return "";
        int b = source.indexOf(end, a + st.length());
        if (b == -1) return "";
        return source.substring(a + st.length(), b);
    }

    public String getValue(String source, String regEx) {
        Matcher mm = Pattern.compile(regEx).matcher(source);
        return mm.find() ? mm.group(mm.groupCount() > 0 ? 1 : 0) : "";
    }

    public String[] getValues(String source, String st, String end) {
        String target = "";
        int a, b;
        while (true) {
            a = source.indexOf(st);
            if (a == -1) break;
            b = source.indexOf(end, a + st.length());
            if (b == -1) break;
            target += source.substring(a + st.length(), b) + "##@@##";
            source = source.substring(b);
        }
        return target.split("##@@##");
    }

    public String[] getValues(String source, String regEx) {
        Vector<String> vec = new Vector<String>(5);
        Matcher mm = Pattern.compile(regEx).matcher(source);
        while (mm.find()) {
            vec.add(mm.group(mm.groupCount() > 0 ? 1 : 0));
        }
        return vec.toArray(new String[0]);
    }
}
