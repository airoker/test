
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.lang.StringUtils;

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


public class Wrapper_gjsairtr001 implements QunarCrawler {

    private static final String WrapperId = "gjsairtr001";

    public static void main(String[] args) {
        Wrapper_gjsairtr001 wrapper_gjsairtr001 = new Wrapper_gjsairtr001();
        FlightSearchParam flightSearchParam = new FlightSearchParam();
        flightSearchParam.setDep("PER");
        flightSearchParam.setArr("SIN");
        flightSearchParam.setWrapperid("gjsairtr001");
        flightSearchParam.setDepDate("2014-09-14");
        flightSearchParam.setRetDate("2014-09-20");
        String html = wrapper_gjsairtr001.getHtml(flightSearchParam);
        System.out.println(html);
        System.out.println("");
        ProcessResultInfo processResultInfo = wrapper_gjsairtr001.process(html, flightSearchParam);
        System.out.println(processResultInfo.toString());
    }

    @Override
    public String getHtml(FlightSearchParam flightSearchParam) {
        // get all query parameters from the url set by wrapperSearchInterface
        String dep = flightSearchParam.getDep();
        String arr = flightSearchParam.getArr();
        String date = flightSearchParam.getDepDate();
        String retDate = flightSearchParam.getRetDate();

        QFHttpClient httpClient = new QFHttpClient(flightSearchParam, false);

        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        String type;
        if (StringUtils.isNotBlank(retDate) && !"Qunar".equals(retDate)) {
            type = "RoundTrip";
        } else {
            type = "OneWay";
            retDate = date;
        }
        String[] _date = date.split("-");
        String[] _redate = retDate.split("-");

        String body = "__EVENTTARGET=&__EVENTARGUMENT=&__VIEWSTATE=/wEPDwUBMGRk7p3dDtvn3PMYYJ9u4RznKUiVx98=&pageToken=&ControlGroupSearchView$AvailabilitySearchInputSearchView$RadioButtonMarketStructure="
                + type
                + "&ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation1="
                + dep
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin1="
                + dep
                + "&ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation1="
                + arr
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination1="
                + arr
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay1="
                + _date[2]
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth1="
                + _date[0]
                + "-"
                + _date[1]
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange1=1|1&date_picker="
                + date
                + "&ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation2=&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin2="
                + "&ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation2=&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination2="
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay2="
                + _redate[2]
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth2="
                + _redate[0]
                + "-"
                + _redate[1]
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange2=1|1&date_picker="
                + _redate
                + "&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_ADT=1&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_CHD=0&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_INFANT=0&hiddendAdultSelection=1&hiddendChildSelection=0&ControlGroupSearchView$ButtonSubmit=Get Flights";
        QFPostMethod post = new QFPostMethod("http://booking.tigerair.com/Search.aspx");

        post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        post.getParams().setContentCharset("utf-8");
        post.setRequestEntity(new ByteArrayRequestEntity(body.getBytes()));
        try {
            httpClient.executeMethod(post);

            Header header = post.getResponseHeader("QProxy-Router");
            String proxy = (header == null ? "null" : header.getValue());

            //获得登陆后的 Cookie
            Cookie[] cookies = httpClient.getState().getCookies();
            String tmpcookies = "";
            for (Cookie c : cookies) {
                tmpcookies += c.toString() + ";";
            }

            QFGetMethod get = new QFGetMethod("http://booking.tigerair.com/Select.aspx");
            //每次访问需授权的网址时需带上前面的 cookie 作为通行证
            get.setRequestHeader("cookie", tmpcookies);
            //get.setRequestHeader("Referer","http://booking.tigerairways.com/Search.aspx");
            get.getParams().setContentCharset("utf-8");
            get.addRequestHeader("qttl", "20000");
            get.addRequestHeader("qaddr", proxy);

            httpClient.executeMethod(get);

            return get.getResponseBodyAsString() + "cookie=" + tmpcookies;
        } catch (Exception e) {
            if (!e.getMessage().equals("Connection refused: connect")) return Constants.CONNECTION_FAIL;
        } finally {
            post.releaseConnection();
        }
        return "Exception";
    }

    @Override
    public ProcessResultInfo process(String html, FlightSearchParam flightSearchParam) {
        ProcessResultInfo processResultInfo = new ProcessResultInfo();

        String date = flightSearchParam.getDepDate();
        String retDate = flightSearchParam.getRetDate();
        String dateSplits[] = date.split("-");
        String reDateSplits[] = retDate.split("-");
        // if there's just an "Exception" returned, record the error as
        // connection_fail in ecLogger

        if ("Exception".equals(html) || Constants.CONNECTION_FAIL.equals(html)) {
            processResultInfo.setStatus(Constants.CONNECTION_FAIL);
            return processResultInfo;
        }
        if (!html.contains("<td class=\"footnote\"")) {
            processResultInfo.setStatus(Constants.PARSING_FAIL);
            return processResultInfo;
        }

        String html1[] = html.split("\">Departing:|\">Returning:");
        String str = StringUtils.substringBetween(html1[1], "from", "</th");
        String[] out_airports = getValues(str, "\\(.*?\\)");
        String depout = "";
        String arrout = "";
        for (String airport : out_airports) {
            String airportstr = airport.replaceAll("[\\(|\\)]", "").trim();
            if (3 == airportstr.length()) {
                if ("".equals(depout)) {
                    depout = airportstr;
                } else if ("".equals(arrout)) {
                    arrout = airportstr;
                }
            }
        }
        String[] depHtml = html1[1].split("<td class=\"footnote\"");
        ArrayList<TRCorp> out = new ArrayList<TRCorp>();
        for (int i = 1, j = 0; i < depHtml.length; i++) {
            if (isDate(depHtml[i], dateSplits)) {
                if (depHtml[i].contains("Sold Out")) {
                    continue;
                }
                out.add(j, parse(depHtml[i], depout, arrout));
                j++;
            }
        }
        if (out.size() < 1) {
            processResultInfo.setRet(true);
            processResultInfo.setStatus(Constants.NO_RESULT);
            return processResultInfo;
        }
        ArrayList<TRCorp> ret = new ArrayList<TRCorp>();
        if (html1.length > 2) {
            String str1 = StringUtils.substringBetween(html1[2], "from", "</th");
            String[] ret_airports = getValues(str1, "\\(.*?\\)");
            String depret = "";
            String arrret = "";
            for (String airport : ret_airports) {
                String airportstr = airport.replaceAll("[\\(|\\)]", "").trim();
                if (3 == airportstr.length()) {
                    if ("".equals(depret)) {
                        depret = airportstr;
                    } else if ("".equals(arrret)) {
                        arrret = airportstr;
                    }
                }
            }
            String[] retHtml = html1[2].split("<td class=\"footnote\"");
            for (int i = 1, j = 0; i < retHtml.length; i++) {
                if (isDate(retHtml[i], reDateSplits)) {
                    if (retHtml[i].contains("Sold Out")) {
                        continue;
                    }
                    ret.add(j, parse(retHtml[i], depret, arrret));
                    j++;
                }
            }
            if (ret.size() < 1) {
                processResultInfo.setRet(true);
                processResultInfo.setStatus(Constants.NO_RESULT);
                return processResultInfo;
            }
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (html1.length < 3) {
            List<OneWayFlightInfo> data = Lists.newArrayList();
            for (int j = 0; j < out.size(); j++) {
                OneWayFlightInfo oneWayFlightInfo = getData(flightSearchParam, out.get(j), now);
                data.add(oneWayFlightInfo);
            }
            processResultInfo.setData(data);
            processResultInfo.setRet(true);
            processResultInfo.setStatus(Constants.SUCCESS);
            return processResultInfo;
        } else {
            List<RoundTripFlightInfo> data = Lists.newArrayList();
            for (int i = 0; i < out.size(); i++) {
                for (int j = 0; j < ret.size(); j++) {
                    RoundTripFlightInfo roundTripFlightInfo = getData(flightSearchParam, out.get(i), ret.get(j), now);
                    data.add(roundTripFlightInfo);
                }
            }
            processResultInfo.setRet(true);
            processResultInfo.setStatus(Constants.SUCCESS);
            processResultInfo.setData(data);
            return processResultInfo;
        }
    }

    private RoundTripFlightInfo getData(FlightSearchParam flightSearchParam, TRCorp trCorp, TRCorp retTrCorp,
            Timestamp now) {
        RoundTripFlightInfo roundTripFlightInfo = new RoundTripFlightInfo();
        FlightDetail detail = getFlightDetail(flightSearchParam, trCorp, retTrCorp, now);
        List<FlightSegement> flightSegements = getFlightSegements(flightSearchParam.getDepDate(), trCorp);
        List<FlightSegement> retFlightSegements = getFlightSegements(flightSearchParam.getRetDate(), retTrCorp);
        roundTripFlightInfo.setDetail(detail);
        roundTripFlightInfo.setInfo(flightSegements);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date retTime;
        try {
            retTime = sdf.parse(flightSearchParam.getRetDate());
        } catch (ParseException e) {
            retTime = null;
        }
        roundTripFlightInfo.setRetdepdate(retTime);
        roundTripFlightInfo.setRetinfo(retFlightSegements);
        List<String> flightNos = Lists.newArrayList();
        flightNos.add(retTrCorp.code);
        roundTripFlightInfo.setRetflightno(flightNos);
        roundTripFlightInfo.setOutboundPrice(trCorp.retailprice);
        roundTripFlightInfo.setReturnedPrice(retTrCorp.retailprice);
        return roundTripFlightInfo;
    }

    private FlightDetail getFlightDetail(FlightSearchParam flightSearchParam, TRCorp trCorp, TRCorp retTrCorp,
            Timestamp now) {
        FlightDetail detail = new FlightDetail();
        String wrapper = flightSearchParam.getWrapperid() == null ? WrapperId : flightSearchParam.getWrapperid();
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
        detail.setMonetaryunit(trCorp.CurrencyCode);
        detail.setCreatetime(now);
        detail.setUpdatetime(now);
        detail.setPrice(trCorp.retailprice + retTrCorp.retailprice);
        List<String> flightNos = Lists.newArrayList();
        flightNos.add(trCorp.code);
        flightNos.add(retTrCorp.code);
        detail.setFlightno(flightNos);
        return detail;
    }

    private OneWayFlightInfo getData(FlightSearchParam flightSearchParam, TRCorp out, Timestamp now) {
        OneWayFlightInfo oneWayFlightInfo = new OneWayFlightInfo();
        FlightDetail detail = getFlightDetail(flightSearchParam, out, now);
        oneWayFlightInfo.setDetail(detail);
        List<FlightSegement> flightSegements = getFlightSegements(flightSearchParam.getDepDate(), out);
        oneWayFlightInfo.setInfo(flightSegements);
        return oneWayFlightInfo;
    }

    private List<FlightSegement> getFlightSegements(String depDate, TRCorp out) {
        List<FlightSegement> flightSegements = Lists.newArrayList();
        FlightSegement flightSegement = new FlightSegement();
        flightSegement.setArrairport(out.arr);
        flightSegement.setDepairport(out.dep);
        flightSegement.setArrtime(out.arrt);
        flightSegement.setDepDate(depDate);
        flightSegement.setFlightno(out.code);
        flightSegement.setArrDate("1970-01-01");
        flightSegement.setDeptime(out.dept);
        flightSegements.add(flightSegement);
        return flightSegements;
    }

    private FlightDetail getFlightDetail(FlightSearchParam flightSearchParam, TRCorp out, Timestamp now) {
        FlightDetail detail = new FlightDetail();
        String wrapper = flightSearchParam.getWrapperid() == null ? WrapperId : flightSearchParam.getWrapperid();
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
        detail.setMonetaryunit(out.CurrencyCode);
        detail.setCreatetime(now);
        detail.setUpdatetime(now);
        detail.setPrice(out.retailprice);
        List<String> flightNos = Lists.newArrayList();
        flightNos.add(out.code);
        detail.setFlightno(flightNos);
        return detail;
    }

    @Override
    public BookingResult getBookingInfo(FlightSearchParam flightSearchParam) {
        BookingResult result = new BookingResult();
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setAction("http://booking.tigerair.com/Search.aspx");
        bookingInfo.setMethod("POST");
        //        String body = "__EVENTTARGET=&__EVENTARGUMENT=&__VIEWSTATE=/wEPDwUBMGRk7p3dDtvn3PMYYJ9u4RznKUiVx98=&pageToken=&ControlGroupSearchView$AvailabilitySearchInputSearchView$RadioButtonMarketStructure=OneWay&ControlGroupSearchView$AvailabilitySearchInputSearchView$HiddenFieldExternalRateId=&ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation1=$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin1=$$&ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation1=$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination1=$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay1=$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth1=$$-$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange1=1|1&date_picker=$$&ControlGroupSearchView$AvailabilitySearchInputSearchView$HiddenFieldExternalRateId=&ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation2=&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin2=&ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation2=&ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination2=&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay2=13&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth2=2014-06&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange2=1|1&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_ADT=1&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_CHD=0&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_INFANT=0&hiddendAdultSelection=1&hiddendChildSelection=0&ControlGroupSearchView$AvailabilitySearchInputSearchView$HIDDENPROMOCODE=&ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMCCCurrency=default&ControlGroupSearchView$ButtonSubmit=get+flights";

        String date[] = flightSearchParam.getDepDate().split("-");
        String retDate[] = flightSearchParam.getRetDate().split("-");
        Map<String, String> param = Maps.newHashMap();
        param.put("__EVENTTARGET", "");
        param.put("__EVENTARGUMENT", "");
        param.put("__VIEWSTATE", "/wEPDwUBMGRk7p3dDtvn3PMYYJ9u4RznKUiVx98=");
        param.put("pageToken", "");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$RadioButtonMarketStructure", "RoundTrip");
        param.put("ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation1", flightSearchParam.getDep());
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin1",
                flightSearchParam.getDep());
        param.put("ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation1",
                flightSearchParam.getArr());
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination1",
                flightSearchParam.getArr());
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay1", date[2]);
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth1",
                date[0].concat("-").concat(date[1]));
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange1", "1|1");
        param.put("date_picker", flightSearchParam.getDepDate());
        param.put("ControlGroupSearchView_AvailabilitySearchInputSearchVieworiginStation2", "");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketOrigin2", "");
        param.put("ControlGroupSearchView_AvailabilitySearchInputSearchViewdestinationStation2", "");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$TextBoxMarketDestination2", "");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDay2", retDate[2]);
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketMonth2", retDate[0].concat("-").concat(retDate[1]));
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListMarketDateRange2", "1|1");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_ADT", "1");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_CHD", "0");
        param.put("ControlGroupSearchView$AvailabilitySearchInputSearchView$DropDownListPassengerType_INFANT", "0");
        param.put("ControlGroupSearchView$ButtonSubmit", "Get flights");

        bookingInfo.setInputs(param);
        bookingInfo.setContentType("application/x-www-form-urlencoded");
        result.setData(bookingInfo);
        result.setRet(true);
        return result;
    }

    private String[] getValues(String source, String regEx) {
        Vector<String> vec = new Vector<String>(5);
        Matcher mm = Pattern.compile(regEx).matcher(source);
        while (mm.find()) {
            vec.add(mm.group(mm.groupCount() > 0 ? 1 : 0));
        }
        return vec.toArray(new String[0]);
    }

    public static String getValue(String source, String regEx) {
        Matcher mm = Pattern.compile(regEx).matcher(source);
        return mm.find() ? mm.group(mm.groupCount() > 0 ? 1 : 0) : "";
    }

    public static String[] getValues(String source, String st, String end) {
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

    boolean isDate(String str, String dat[]) {
        String date = StringUtils.substringBetween(str, ">", "</td>");
        date = date.replaceAll("[^0-9]", "");
        if (date.equals(dat[2] + dat[0])) {
            return true;
        } else return false;
    }

    TRCorp parse(String str, String dep, String arr) {
        TRCorp info = new TRCorp();
        //String tmp=StringUtils.getValue(str, ">", "<div class=");
        //String tm[]=StringUtils.getValues(tmp, "<td>", ">");
        info.dep = dep; //tm[0].substring(0, 3);
        info.arr = arr; //tm[1].substring(0, 3);
        String codtm[] = getValues(str, "flight: </span></td>", "/td>");
        for (int i = 0; i < codtm.length; i++) {
            codtm[i] = StringUtils.substringBetween(codtm[i], "<td>", "<");
            codtm[i] = codtm[i].replaceAll("\\s+", "");
            codtm[i] = codtm[i].substring(0, 2) + codtm[i].substring(3);
            if (i == 0) {
                info.code = codtm[i];
            } else {
                info.code = info.code + "/" + codtm[i];
            }
        }
        String dtt = StringUtils.substringBetween(str, "depart: </span></td>", "</td>").trim();
        info.dept = getValue(dtt, "\\d\\d:\\d\\d");
        String att[] = getValues(str, "arrive: </span></td>", "</td>");
        String arrtime = att[0];
        if (att.length > 1) {
            arrtime = att[att.length - 1];
        }
        info.arrt = getValue(arrtime, "\\d\\d:\\d\\d");
        String temp = StringUtils.substringBetween(str, "<td class=\"fareCol1\">", "/label>");

        temp = temp.replaceAll("<.*?>", "<>").replaceAll("\n|\t|\r", "");
        String[] tempArray = getValues(temp, ">", "<");
        //		System.out.println("tempArray:"+org.apache.commons.lang.StringUtils.join(tempArray, "@@"));

        info.CurrencyCode = tempArray[6].trim();
        String price = tempArray[11];
        info.retailprice = Float.parseFloat(price.replaceAll(",", ""));
        String airports = "";
        if (2 == codtm.length) {
            String valuestr = StringUtils.substringBetween(str, "value=\"", "\"");
            String[] citys = getValues(valuestr, "~([A-Z]{3})~");
            airports = citys[1] + "," + citys[2];
        }
        info.transferAirport = airports;
        return info;
    }

    class TRCorp {

        String CurrencyCode;

        float retailprice;

        // String planetype;
        String dept;

        String arrt;

        String code;

        String dep;

        String arr;

        String flightKeys;

        String transferAirport;
    }
}

